<#
    File:    Seed-TestData.ps1
    Purpose: Fills a running instance of the API with realistic sample data for
             manual testing and the demo. Everything is created through the API
             itself, so passwords are hashed and every business rule is applied
             exactly as it would be for a real user.
    Author:  <your name>
    Created: 2026

    Usage:   .\tools\Seed-TestData.ps1 -AdminPassword "<Seed:AdminPassword>"
             .\tools\Seed-TestData.ps1 -BaseUrl "http://localhost:5062" -AdminPassword "..."
#>
param(
    [string] $BaseUrl = 'http://localhost:5199',
    [string] $AdminNic = '199000000001',
    [Parameter(Mandatory = $true)]
    [string] $AdminPassword
)

$ErrorActionPreference = 'Stop'

# Sends one request to the API and returns the parsed body. Returns $null on an
# expected failure (e.g. a user that already exists) so seeding can continue.
function Invoke-Api {
    param(
        [string] $Method,
        [string] $Path,
        $Body,
        [string] $Token,
        [switch] $TolerateFailure
    )

    $headers = @{}
    if ($Token) { $headers['Authorization'] = "Bearer $Token" }

    $params = @{
        Uri         = "$BaseUrl$Path"
        Method      = $Method
        Headers     = $headers
        ContentType = 'application/json'
    }
    if ($null -ne $Body) { $params['Body'] = ($Body | ConvertTo-Json -Depth 5) }

    try {
        return Invoke-RestMethod @params
    }
    catch {
        if ($TolerateFailure) {
            Write-Host "    (skipped: $($_.Exception.Message))" -ForegroundColor DarkGray
            return $null
        }
        throw
    }
}

# Returns a UTC timestamp the API accepts, offset from now by the given hours.
function Get-Utc {
    param([double] $AddHours)
    return (Get-Date).ToUniversalTime().AddHours($AddHours).ToString('yyyy-MM-ddTHH:mm:ssZ')
}

Write-Host "`nSeeding $BaseUrl" -ForegroundColor Cyan

# --- Sign in as the Backoffice officer -------------------------------------
$login = Invoke-Api -Method POST -Path '/api/auth/login' -Body @{
    identifier = $AdminNic
    password   = $AdminPassword
}
$admin = $login.token
Write-Host "Signed in as $($login.user.fullName) ($($login.user.role))" -ForegroundColor Green

# --- Users ------------------------------------------------------------------
Write-Host "`nUsers" -ForegroundColor Cyan

$operator = @{
    nic = '199512345678'; fullName = 'Kamal Silva'; email = 'kamal@microgrid.lk'
    phone = '0712345678'; address = '45 Kandy Road, Kadawatha'
    role = 'GridOperator'; password = 'Operator@123'
}
Invoke-Api -Method POST -Path '/api/users' -Body $operator -Token $admin -TolerateFailure | Out-Null
Write-Host "  GridOperator  199512345678  Kamal Silva"

$prosumers = @(
    @{ nic = '200012345678'; fullName = 'Nimal Perera';   email = 'nimal@example.com'; phone = '0771234567'; address = '12 Galle Road, Colombo'; role = 'Prosumer'; solarCapacityKW = 5.5;  password = 'Prosumer@123' },
    @{ nic = '199887654321'; fullName = 'Sunil Fernando'; email = 'sunil@example.com'; phone = '0763334444'; address = '88 Lake Drive, Kandy';   role = 'Prosumer'; solarCapacityKW = 12.0; password = 'Prosumer@123' }
)
foreach ($p in $prosumers) {
    Invoke-Api -Method POST -Path '/api/users' -Body $p -Token $admin -TolerateFailure | Out-Null
    Write-Host "  Prosumer      $($p.nic)  $($p.fullName)  (active)"
}

# Self-registered prosumer: stays PendingActivation so the Backoffice approval
# screen has something to show.
$pending = @{
    nic = '200156789012'; fullName = 'Dilani Jayasuriya'; email = 'dilani@example.com'
    phone = '0755556666'; address = '7 Temple Lane, Galle'; solarCapacityKW = 3.2
    password = 'Prosumer@123'
}
Invoke-Api -Method POST -Path '/api/auth/register' -Body $pending -TolerateFailure | Out-Null
Write-Host "  Prosumer      200156789012  Dilani Jayasuriya  (PENDING ACTIVATION)"

# --- Stations ---------------------------------------------------------------
Write-Host "`nStations" -ForegroundColor Cyan

$stationA = Invoke-Api -Method POST -Path '/api/stations' -Token $admin -Body @{
    stationName = 'Colombo Fort Hub'; latitude = 6.9344; longitude = 79.8428
    capacityKWh = 250; totalBatterySlots = 6; operatingSchedule = 'Mon-Sun 06:00-22:00'
}
Write-Host "  $($stationA.id)  Colombo Fort Hub     (6.9344, 79.8428)"

$stationB = Invoke-Api -Method POST -Path '/api/stations' -Token $admin -Body @{
    stationName = 'Kandy Lakeside Node'; latitude = 7.2906; longitude = 80.6337
    capacityKWh = 180; totalBatterySlots = 4; operatingSchedule = 'Mon-Fri 07:00-19:00'
}
Write-Host "  $($stationB.id)  Kandy Lakeside Node  (7.2906, 80.6337)"

# --- Booking slots ----------------------------------------------------------
# Offsets are chosen so the 7-day window and the 12-hour notice rule can both be
# demonstrated without editing any data by hand.
Write-Host "`nBooking slots" -ForegroundColor Cyan

$slotSpecs = @(
    @{ station = $stationA; battery = 1; hours = 48;  length = 2; kwh = 30; label = 'in 2 days   - bookable, over 12h notice' },
    @{ station = $stationA; battery = 2; hours = 72;  length = 2; kwh = 25; label = 'in 3 days   - reschedule target' },
    @{ station = $stationA; battery = 3; hours = 6;   length = 2; kwh = 20; label = 'in 6 hours  - bookable, but under 12h notice' },
    @{ station = $stationA; battery = 4; hours = 240; length = 2; kwh = 40; label = 'in 10 days  - booking must be REFUSED (over 7 days)' },
    @{ station = $stationB; battery = 1; hours = 96;  length = 3; kwh = 50; label = 'in 4 days   - for the QR walkthrough' },
    @{ station = $stationB; battery = 2; hours = 120; length = 3; kwh = 50; label = 'in 5 days   - spare' }
)

$slots = @()
foreach ($s in $slotSpecs) {
    $slot = Invoke-Api -Method POST -Path '/api/slots' -Token $admin -Body @{
        stationId         = $s.station.id
        batterySlotNumber = $s.battery
        startTime         = (Get-Utc $s.hours)
        endTime           = (Get-Utc ($s.hours + $s.length))
        capacityKWh       = $s.kwh
        isAvailable       = $true
    }
    $slots += $slot
    Write-Host "  $($slot.id)  battery $($s.battery)  $($s.kwh) kWh  $($s.label)"
}

# --- Reservations -----------------------------------------------------------
Write-Host "`nReservations" -ForegroundColor Cyan

$nimal = (Invoke-Api -Method POST -Path '/api/auth/login' -Body @{ identifier = '200012345678'; password = 'Prosumer@123' }).token
$sunil = (Invoke-Api -Method POST -Path '/api/auth/login' -Body @{ identifier = '199887654321'; password = 'Prosumer@123' }).token

# Left Pending so the approval step can be demonstrated.
$r1 = Invoke-Api -Method POST -Path '/api/reservations' -Token $nimal -Body @{
    slotId = $slots[0].id; type = 'DropOff'; energyKWh = 12.5
}
Write-Host "  $($r1.id)  Nimal  DropOff   12.5 kWh  -> $($r1.status)"

# Approved, so a QR token exists for the verify and complete endpoints.
$r2 = Invoke-Api -Method POST -Path '/api/reservations' -Token $sunil -Body @{
    slotId = $slots[4].id; type = 'Charging'; energyKWh = 35
}
Invoke-Api -Method POST -Path "/api/reservations/$($r2.id)/approve" -Token $admin | Out-Null

# The QR token is only ever returned to the prosumer who owns the reservation.
$r2Owner = Invoke-Api -Method GET -Path "/api/reservations/$($r2.id)" -Token $sunil
Write-Host "  $($r2.id)  Sunil  Charging  35 kWh    -> $($r2Owner.status)"

# --- Summary ----------------------------------------------------------------
Write-Host "`n--- Sign in with ---" -ForegroundColor Yellow
Write-Host "  Backoffice    $AdminNic / <your Seed:AdminPassword>"
Write-Host "  GridOperator  199512345678 / Operator@123"
Write-Host "  Prosumer      200012345678 / Prosumer@123  (Nimal)"
Write-Host "  Prosumer      199887654321 / Prosumer@123  (Sunil)"
Write-Host "  Prosumer      200156789012 / Prosumer@123  (Dilani - pending, login REFUSED until activated)"

Write-Host "`n--- Ids for the request bodies ---" -ForegroundColor Yellow
Write-Host "  stationA        $($stationA.id)"
Write-Host "  stationB        $($stationB.id)"
Write-Host "  slot 2 days     $($slots[0].id)  <- booked by Nimal"
Write-Host "  slot 3 days     $($slots[1].id)  <- free, reschedule target"
Write-Host "  slot 6 hours    $($slots[2].id)  <- free, under 12h notice"
Write-Host "  slot 10 days    $($slots[3].id)  <- free, outside the 7-day window"
Write-Host "  slot 4 days     $($slots[4].id)  <- booked by Sunil, approved"
Write-Host "  slot 5 days     $($slots[5].id)  <- free"
Write-Host "  reservation R1  $($r1.id)  Pending"
Write-Host "  reservation R2  $($r2.id)  Approved"
Write-Host "  QR token R2     $($r2Owner.qrToken)"
Write-Host ""
