# Deploying the Smart Solar Microgrid Web Service to IIS

The API reads every secret from configuration at startup and **refuses to start**
if they are missing (`Program.cs` throws for an empty MongoDB connection string
or a JWT key shorter than 32 bytes). On a developer machine those values come
from .NET user secrets. On the IIS server they come from environment variables.
Nothing secret is stored in `appsettings.json` in either case.

## Quick path: the automated script

On a Windows machine, the whole deployment is one elevated command. It enables
the IIS features, installs the Hosting Bundle if missing, publishes, creates the
app pool and site, sets the secrets as per-site environment variables and verifies
the result:

```powershell
# in PowerShell started with Run as Administrator
cd C:\Users\Shewon\OneDrive\Desktop\Solar-Grid-Project\backend\SmartMicrogrid
.\tools\Setup-IIS.ps1
```

It reads the MongoDB connection string and JWT key from your .NET user secrets,
so nothing has to be typed or stored in a file. Override with `-MongoConnectionString`
and `-JwtKey` on a machine that has no user secrets, and use `-Port` (default 8080),
`-SiteName` or `-CorsOrigins` to change the site.

The rest of this document is what the script does, for doing it by hand or for
explaining it in the report.

## 1. Prerequisites on the server

- IIS with the **ASP.NET Core 8 Hosting Bundle** installed
  (<https://dotnet.microsoft.com/download/dotnet/8.0> -> "Hosting Bundle").
  Install it *after* IIS, then run `iisreset`.
- Outbound network access to MongoDB Atlas, and the server's public IP added to
  the Atlas **Network Access** allow-list.

## 2. Publish

From the solution folder:

```
dotnet publish SmartMicrogrid/SmartMicrogrid.Api.csproj -c Release -o C:\inetpub\SmartMicrogridApi
```

This produces `web.config` automatically, merged with the one in the project
root (in-process hosting, stdout logging enabled).

## 3. Create the IIS site

1. **Application Pool** -> Add. .NET CLR version = **No Managed Code**
   (the API runs on .NET 8, not the .NET Framework CLR).
2. **Sites** -> Add Website, physical path `C:\inetpub\SmartMicrogridApi`,
   bound to the port you want.
3. Give the app pool identity read access to that folder, and write access to
   `C:\inetpub\SmartMicrogridApi\logs` (create the folder if it does not exist)
   so stdout logging can be written.

## 4. Required environment variables

ASP.NET Core maps `__` (double underscore) to the `:` used in `appsettings.json`.

| Variable | Value |
|---|---|
| `MongoDb__ConnectionString` | The Atlas SRV connection string |
| `Jwt__Key` | Random string, **at least 32 bytes**; startup fails otherwise |
| `Cors__AllowedOrigins__0` | Origin of the deployed web app, e.g. `https://microgrid.example.com` |
| `Seed__AdminPassword` | Only needed for a brand-new database (see step 6) |

Set them per-site so they do not leak to other applications:
IIS Manager -> select the site -> **Configuration Editor** ->
`system.webServer/aspNetCore` -> `environmentVariables` -> add each one ->
Apply, then recycle the app pool.

Do **not** put these in `web.config` or `appsettings.json` — that file is part
of the submission zip.

## 5. CORS

The web app cannot call the API until its origin is listed. `appsettings.json`
ships with `http://localhost:5173` and `http://localhost:3000` for local
development only. Add the deployed origin via `Cors__AllowedOrigins__0` as
above. Origins must match scheme, host and port exactly — no trailing slash.

## 6. First run against an empty database

`DatabaseInitializer` creates the indexes on every start, and seeds the first
Backoffice user **only if no Backoffice user exists**. For a fresh database,
set `Seed__AdminPassword` (plus `Seed__AdminNic` if you want a different NIC
from the default in `appsettings.json`) before the first request. Once that
user exists the variable is ignored and can be removed.

## 7. Verify

```
curl http://<server>/api/database/ping        -> {"status":"connected"}
curl -X POST http://<server>/api/auth/login -H "Content-Type: application/json" ^
     -d "{\"identifier\":\"<admin NIC>\",\"password\":\"<admin password>\"}"
```

A successful login returns a JWT, which the web and mobile clients send as
`Authorization: Bearer <token>` on every other call.

## Notes and troubleshooting

- **HTTP 500.30 on startup** almost always means a missing or invalid
  environment variable. Read `logs\stdout*.log` — the exception message names
  the setting. Set `stdoutLogEnabled="false"` in `web.config` once the site is
  stable, since the log grows without limit.
- **Swagger is not served in Production** (`Program.cs` only maps it in the
  Development environment). To demonstrate it on the server, set
  `ASPNETCORE_ENVIRONMENT=Development` temporarily, or move the
  `app.UseSwagger()` calls outside the `IsDevelopment()` check.
- **HTTPS**: `app.UseHttpsRedirection()` redirects every HTTP request, which breaks
  clients when the site has no HTTPS binding. The middleware is therefore behind a
  `UseHttpsRedirection` configuration switch (default `true`); `Setup-IIS.ps1` sets
  the environment variable `UseHttpsRedirection=false` on the HTTP-only site. Add an
  HTTPS binding with a certificate and remove that variable to turn it back on.
- **HTTP 411 on bodyless POSTs.** IIS rejects a POST that carries no
  `Content-Length` header, where Kestrel accepts it. This affects the endpoints
  that take no body: `/activate`, `/deactivate`, `/me/deactivation-request` and
  `/{id}/approve`. Browsers and most HTTP clients send `Content-Length: 0`
  automatically, but Android OkHttp needs an explicit empty body
  (`RequestBody.create(new byte[0], null)`), and `curl -X POST` needs
  `-H "Content-Length: 0"`. The API never sees these requests, so it cannot
  fix them server-side.
- **Times are UTC** throughout the API. Clients convert for display; the 7-day
  booking window and 12-hour notice rule are evaluated in UTC on the server.
