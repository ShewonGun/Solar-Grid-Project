<#
    File:    Setup-IIS.ps1
    Purpose: Deploys the web service to IIS on this machine. Enables the IIS
             features, installs the ASP.NET Core Hosting Bundle if it is
             missing, publishes the API, creates the application pool and site,
             and sets the secrets as per-site environment variables so no
             credential is ever written into a file in the project.
    Author:  <your name>
    Created: 2026

    MUST be run from an elevated PowerShell (Run as Administrator):
        cd C:\Users\Shewon\OneDrive\Desktop\Solar-Grid-Project\backend\SmartMicrogrid
        .\tools\Setup-IIS.ps1

    Secrets default to the values already in .NET user secrets. Pass them
    explicitly if you are deploying on a machine that has none.
#>
param(
    [string] $SiteName = 'SmartMicrogridApi',
    [int]    $Port = 8080,
    [string] $SitePath = 'C:\inetpub\SmartMicrogridApi',
    [string] $MongoConnectionString,
    [string] $JwtKey,
    [string[]] $CorsOrigins = @('http://localhost:5173', 'http://localhost:3000')
)

$ErrorActionPreference = 'Stop'

# Refuses to continue unless the shell is elevated, because every step below needs it.
$identity = [Security.Principal.WindowsIdentity]::GetCurrent()
if (-not (New-Object Security.Principal.WindowsPrincipal($identity)).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    throw 'This script must be run from an elevated PowerShell (Run as Administrator).'
}

$repoRoot = Split-Path $PSScriptRoot -Parent
$project = Join-Path $repoRoot 'SmartMicrogrid\SmartMicrogrid.Api.csproj'
$projectDir = Join-Path $repoRoot 'SmartMicrogrid'

Write-Host "`n[1/7] Enabling IIS features" -ForegroundColor Cyan

# The minimum set needed to host an ASP.NET Core app, plus the management console.
$features = @(
    'IIS-WebServerRole', 'IIS-WebServer', 'IIS-CommonHttpFeatures', 'IIS-StaticContent',
    'IIS-DefaultDocument', 'IIS-HttpErrors', 'IIS-RequestFiltering', 'IIS-Security',
    'IIS-HttpLogging', 'IIS-ManagementConsole'
)
foreach ($f in $features) {
    $state = (Get-WindowsOptionalFeature -Online -FeatureName $f).State
    if ($state -ne 'Enabled') {
        Write-Host "  enabling $f"
        Enable-WindowsOptionalFeature -Online -FeatureName $f -All -NoRestart | Out-Null
    }
}
Write-Host "  IIS features ready" -ForegroundColor Green

Write-Host "`n[2/7] ASP.NET Core Hosting Bundle" -ForegroundColor Cyan

# The bundle installs the AspNetCoreModuleV2 handler that IIS uses to run the app.
$ancm = Join-Path $env:ProgramFiles 'IIS\Asp.Net Core Module\V2'
if (Test-Path $ancm) {
    Write-Host "  already installed" -ForegroundColor Green
}
else {
    $installer = Join-Path $env:TEMP 'dotnet-hosting-win.exe'
    Write-Host "  downloading (about 107 MB) ..."
    Invoke-WebRequest -Uri 'https://aka.ms/dotnet/8.0/dotnet-hosting-win.exe' -OutFile $installer -UseBasicParsing
    Write-Host "  installing ..."
    $p = Start-Process -FilePath $installer -ArgumentList '/quiet', '/norestart' -Wait -PassThru
    if ($p.ExitCode -ne 0 -and $p.ExitCode -ne 3010) {
        throw "Hosting Bundle installer failed with exit code $($p.ExitCode)."
    }
    Remove-Item $installer -Force -ErrorAction SilentlyContinue
    Write-Host "  installed" -ForegroundColor Green
    # IIS must be restarted before it picks up the new native module.
    net stop was /y | Out-Null
    net start w3svc | Out-Null
}

Import-Module WebAdministration

Write-Host "`n[3/7] Reading configuration" -ForegroundColor Cyan

# Falls back to the developer's user secrets so the same values are used locally
# and on the server, without either being written into a file in the project.
function Get-Secret {
    param([string] $Key)
    Push-Location $projectDir
    try {
        $line = dotnet user-secrets list 2>$null | Select-String "^$([regex]::Escape($Key)) = "
        if ($line) { return $line.ToString().Split('=', 2)[1].Trim() }
        return $null
    }
    finally { Pop-Location }
}

if (-not $MongoConnectionString) { $MongoConnectionString = Get-Secret 'MongoDb:ConnectionString' }
if (-not $JwtKey) { $JwtKey = Get-Secret 'Jwt:Key' }

if (-not $MongoConnectionString) { throw 'MongoDb:ConnectionString not found. Pass -MongoConnectionString.' }
if ([Text.Encoding]::UTF8.GetByteCount($JwtKey) -lt 32) { throw 'Jwt:Key must be at least 32 bytes. Pass -JwtKey.' }
Write-Host "  connection string and JWT key resolved" -ForegroundColor Green

Write-Host "`n[4/7] Publishing to $SitePath" -ForegroundColor Cyan

if (Test-Path $SitePath) {
    # Stop the site first, otherwise the running app locks its own DLLs.
    if (Get-Website -Name $SiteName -ErrorAction SilentlyContinue) { Stop-Website -Name $SiteName -ErrorAction SilentlyContinue }
    Start-Sleep -Seconds 2
}
New-Item -ItemType Directory -Force -Path $SitePath | Out-Null
dotnet publish $project -c Release -o $SitePath --nologo
if ($LASTEXITCODE -ne 0) { throw 'dotnet publish failed.' }

# stdout logging needs a writable folder; ANCM does not always create it.
$logs = Join-Path $SitePath 'logs'
New-Item -ItemType Directory -Force -Path $logs | Out-Null
Write-Host "  published" -ForegroundColor Green

Write-Host "`n[5/7] Application pool and site" -ForegroundColor Cyan

if (Test-Path "IIS:\AppPools\$SiteName") { Remove-WebAppPool -Name $SiteName }
New-WebAppPool -Name $SiteName | Out-Null
# "No Managed Code": the app runs on .NET 8, not the .NET Framework CLR.
Set-ItemProperty "IIS:\AppPools\$SiteName" -Name managedRuntimeVersion -Value ''
Set-ItemProperty "IIS:\AppPools\$SiteName" -Name startMode -Value 'AlwaysRunning'
Write-Host "  app pool $SiteName (No Managed Code)"

if (Get-Website -Name $SiteName -ErrorAction SilentlyContinue) { Remove-Website -Name $SiteName }
New-Website -Name $SiteName -Port $Port -PhysicalPath $SitePath -ApplicationPool $SiteName | Out-Null
Write-Host "  site $SiteName on port $Port"

# The app pool identity needs read access to the site and write access to logs.
$poolIdentity = "IIS AppPool\$SiteName"
icacls $SitePath /grant "${poolIdentity}:(OI)(CI)(RX)" /T /Q | Out-Null
icacls $logs /grant "${poolIdentity}:(OI)(CI)(M)" /T /Q | Out-Null
Write-Host "  permissions granted to $poolIdentity" -ForegroundColor Green

Write-Host "`n[6/7] Environment variables" -ForegroundColor Cyan

# Set on the site's aspNetCore section, so the values live in applicationHost.config
# (readable by administrators only) rather than in any file inside the project.
$envPath = "/system.webServer/aspNetCore/environmentVariables"
$vars = [ordered]@{
    'ASPNETCORE_ENVIRONMENT'    = 'Production'
    'MongoDb__ConnectionString' = $MongoConnectionString
    'Jwt__Key'                  = $JwtKey
    # No HTTPS binding on this site, so the redirect middleware stays off.
    'UseHttpsRedirection'       = 'false'
}
for ($i = 0; $i -lt $CorsOrigins.Count; $i++) { $vars["Cors__AllowedOrigins__$i"] = $CorsOrigins[$i] }

Clear-WebConfiguration -PSPath "IIS:\Sites\$SiteName" -Filter $envPath -ErrorAction SilentlyContinue
foreach ($name in $vars.Keys) {
    Add-WebConfiguration -PSPath "IIS:\Sites\$SiteName" -Filter $envPath -Value @{ name = $name; value = $vars[$name] }
    if ($name -match 'ConnectionString|Key') { Write-Host "  $name = <hidden>" } else { Write-Host "  $name = $($vars[$name])" }
}

Write-Host "`n[7/7] Starting and verifying" -ForegroundColor Cyan

Start-WebAppPool -Name $SiteName -ErrorAction SilentlyContinue
Start-Website -Name $SiteName
Start-Sleep -Seconds 8

$url = "http://localhost:$Port"
try {
    $ping = Invoke-RestMethod -Uri "$url/api/database/ping" -TimeoutSec 60
    Write-Host "  GET /api/database/ping -> $($ping.status)" -ForegroundColor Green
    Write-Host "`nDeployed. The API is live at $url" -ForegroundColor Green
    Write-Host "Try:  $url/api/database/ping`n"
}
catch {
    Write-Host "  verification FAILED: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "  check $logs\stdout*.log - the startup exception names the missing setting.`n" -ForegroundColor Yellow
    throw
}
