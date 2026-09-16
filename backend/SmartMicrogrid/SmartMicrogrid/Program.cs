/*
 * File: Program.cs
 * Purpose: Application entry point. Loads and validates configuration, and
 *          registers MongoDB, JWT authentication, authorization (signed-in
 *          users only by default), CORS for the web app, consistent error
 *          responses, Swagger and the application services. On startup it
 *          prepares the database, then runs the HTTP pipeline.
 * Author:  <your name>
 * Created: 2026
 */
using System.Text;
using System.Text.Json.Serialization;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi.Models;
using MongoDB.Driver;
using SmartMicrogrid.Api.Data;
using SmartMicrogrid.Api.Filters;
using SmartMicrogrid.Api.DTOs.Responses;
using SmartMicrogrid.Api.Helpers;
using SmartMicrogrid.Api.Middleware;
using SmartMicrogrid.Api.Services;
using SmartMicrogrid.Api.Services.Interfaces;
using SmartMicrogrid.Api.Settings;

var builder = WebApplication.CreateBuilder(args);

// Settings. Secrets (MongoDb:ConnectionString, Jwt:Key, Seed:AdminPassword) come from user
// secrets in development and from environment variables on the IIS server, never appsettings.json.
var mongoSection = builder.Configuration.GetSection(MongoDbSettings.SectionName);
var jwtSection = builder.Configuration.GetSection(JwtSettings.SectionName);
var corsSection = builder.Configuration.GetSection(CorsSettings.SectionName);
var mongoSettings = mongoSection.Get<MongoDbSettings>() ?? new MongoDbSettings();
var jwtSettings = jwtSection.Get<JwtSettings>() ?? new JwtSettings();
var corsSettings = corsSection.Get<CorsSettings>() ?? new CorsSettings();

if (string.IsNullOrWhiteSpace(mongoSettings.ConnectionString))
{
    throw new InvalidOperationException($"{MongoDbSettings.SectionName}:ConnectionString is not configured.");
}

if (Encoding.UTF8.GetByteCount(jwtSettings.Key) < JwtSettings.MinKeyBytes)
{
    throw new InvalidOperationException($"{JwtSettings.SectionName}:Key must be at least {JwtSettings.MinKeyBytes} bytes.");
}

builder.Services.Configure<MongoDbSettings>(mongoSection);
builder.Services.Configure<JwtSettings>(jwtSection);
builder.Services.Configure<SeedSettings>(builder.Configuration.GetSection(SeedSettings.SectionName));

// Controllers. Enums are sent and received by name (e.g. "Prosumer") rather than number, and
// invalid request bodies return the same ErrorResponse shape as every other error.
// ActiveAccountFilter then rejects any request made by a deactivated or not-yet-activated
// account, even if its login token is still within its lifetime.
builder.Services.AddControllers(options => options.Filters.Add<ActiveAccountFilter>())
    .AddJsonOptions(options => options.JsonSerializerOptions.Converters.Add(new JsonStringEnumConverter()))
    .ConfigureApiBehaviorOptions(options =>
    {
        options.InvalidModelStateResponseFactory = context =>
        {
            var errors = context.ModelState
                .Where(entry => entry.Value is { Errors.Count: > 0 })
                .ToDictionary(
                    entry => entry.Key,
                    entry => entry.Value!.Errors.Select(error => error.ErrorMessage).ToArray());

            return new BadRequestObjectResult(new ErrorResponse
            {
                StatusCode = StatusCodes.Status400BadRequest,
                Message = "One or more fields are invalid.",
                Errors = errors
            });
        };
    });

// Error handling. ServiceException becomes its HTTP status code with a JSON message.
builder.Services.AddExceptionHandler<GlobalExceptionHandler>();
builder.Services.AddProblemDetails();

// CORS. Lets the browser-based web app call the API from its configured origins.
builder.Services.AddCors(options =>
{
    options.AddPolicy(CorsSettings.PolicyName, policy => policy
        .WithOrigins(corsSettings.AllowedOrigins)
        .AllowAnyHeader()
        .AllowAnyMethod());
});

// Swagger, with an Authorize button for testing secured endpoints.
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(options =>
{
    options.AddSecurityDefinition(JwtBearerDefaults.AuthenticationScheme, new OpenApiSecurityScheme
    {
        Name = "Authorization",
        In = ParameterLocation.Header,
        Type = SecuritySchemeType.Http,
        Scheme = "bearer",
        BearerFormat = "JWT",
        Description = "Paste the token returned by POST /api/auth/login."
    });

    options.AddSecurityRequirement(new OpenApiSecurityRequirement
    {
        {
            new OpenApiSecurityScheme
            {
                Reference = new OpenApiReference { Type = ReferenceType.SecurityScheme, Id = JwtBearerDefaults.AuthenticationScheme }
            },
            Array.Empty<string>()
        }
    });
});

// Authentication. Every request carries a JWT; the user's NIC is read from its "sub" claim.
builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.MapInboundClaims = false;
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidIssuer = jwtSettings.Issuer,
            ValidateAudience = true,
            ValidAudience = jwtSettings.Audience,
            ValidateIssuerSigningKey = true,
            IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtSettings.Key)),
            ValidateLifetime = true,
            ClockSkew = TimeSpan.FromMinutes(1),
            NameClaimType = AppClaimTypes.Name,
            RoleClaimType = AppClaimTypes.Role
        };
    });

// Authorization. Endpoints require a signed-in user unless marked [AllowAnonymous].
builder.Services.AddAuthorization(options =>
{
    options.FallbackPolicy = new AuthorizationPolicyBuilder()
        .RequireAuthenticatedUser()
        .Build();
});

// MongoDB
builder.Services.AddSingleton<IMongoClient>(_ => new MongoClient(mongoSettings.ConnectionString));
builder.Services.AddSingleton<MongoDbContext>();
builder.Services.AddScoped<DatabaseInitializer>();

// Application services
builder.Services.AddSingleton<ITokenService, TokenService>();
builder.Services.AddScoped<IUserService, UserService>();
builder.Services.AddScoped<IStationService, StationService>();
builder.Services.AddScoped<IEnergyBookingSlotService, EnergyBookingSlotService>();
builder.Services.AddScoped<IEnergyReservationService, EnergyReservationService>();

var app = builder.Build();

// Create indexes and the first Backoffice user before accepting requests.
using (var scope = app.Services.CreateScope())
{
    await scope.ServiceProvider.GetRequiredService<DatabaseInitializer>().InitializeAsync();
}

// HTTP pipeline
app.UseExceptionHandler();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

// HTTPS redirection is skipped when the site has no HTTPS binding, for example an
// HTTP-only IIS site: redirecting to a port nothing is listening on breaks every
// client, and the Android app hardest. Set UseHttpsRedirection=false there.
if (builder.Configuration.GetValue("UseHttpsRedirection", true))
{
    app.UseHttpsRedirection();
}

app.UseCors(CorsSettings.PolicyName);

app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();

await app.RunAsync();
