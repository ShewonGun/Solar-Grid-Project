# Testing the Web Service

Sample data and a request for every endpoint. `SmartMicrogrid.http` has the same
requests in runnable form (VS / VS Code REST Client); this file is the reference
and explains what each one should return.

## 1. Load the sample data

Start the API, then from the solution folder:

```powershell
.\tools\Seed-TestData.ps1 -AdminPassword "<your Seed:AdminPassword>"
```

It prints every id it creates. Ids are new on each run, so the tables below use
names like `{slot2days}` — substitute the printed value.

Run it against an empty database. Re-running adds a second set of stations and
slots; the users are skipped because they already exist.

## 2. Accounts it creates

| Role | NIC | Password | State |
|---|---|---|---|
| Backoffice | 199000000001 | your `Seed:AdminPassword` | Active (seeded at startup) |
| GridOperator | 199512345678 | `Operator@123` | Active |
| Prosumer — Nimal | 200012345678 | `Prosumer@123` | Active, has a Pending booking |
| Prosumer — Sunil | 199887654321 | `Prosumer@123` | Active, has an Approved booking + QR |
| Prosumer — Dilani | 200156789012 | `Prosumer@123` | **PendingActivation** — login is refused |

Log in, then send `Authorization: Bearer <token>` on everything else.

## 3. Stations and slots

Two stations: **Colombo Fort Hub** (6.9344, 79.8428) and **Kandy Lakeside Node**
(7.2906, 80.6337) — far enough apart to make `nearby` meaningful.

Six slots, timed so each rule can be demonstrated without editing data:

| Slot | Starts | Capacity | Purpose |
|---|---|---|---|
| `{slot2days}` | +2 days | 30 kWh | Booked by Nimal (Pending). Over 12h away, so it can be changed or cancelled |
| `{slot3days}` | +3 days | 25 kWh | Free — reschedule target |
| `{slot6hours}` | +6 hours | 20 kWh | Free — bookable, but under 12h notice, so it cannot then be changed |
| `{slot10days}` | +10 days | 40 kWh | Free — **booking is refused**, outside the 7-day window |
| `{slot4days}` | +4 days | 50 kWh | Booked by Sunil, **Approved**, has the QR token |
| `{slot5days}` | +5 days | 50 kWh | Free — spare |

## 4. Endpoints

`Any` = any signed-in, active account. `Staff` = Backoffice or GridOperator.

> **On IIS**, a POST with no body must send `Content-Length: 0` or IIS replies
> `411 Length Required` before the request reaches the API. Applies to
> `/activate`, `/deactivate`, `/me/deactivation-request` and `/{id}/approve`.

### Auth and health

| Endpoint | Role | Body / notes |
|---|---|---|
| `GET /api/database/ping` | none | `{"status":"connected"}` |
| `POST /api/auth/login` | none | `{"identifier":"200012345678","password":"Prosumer@123"}` — NIC or email |
| `POST /api/auth/register` | none | Prosumer self-registration; lands in PendingActivation |
| `GET /api/auth/me` | Any | The caller's own record, from the token |

### Users

| Endpoint | Role | Body / notes |
|---|---|---|
| `GET /api/users?role=Prosumer&status=Active` | Backoffice | `search=` also matches name, NIC, email |
| `GET /api/users/pending-activations` | Backoffice | Returns Dilani |
| `GET /api/users/{nic}` | self or Backoffice | A prosumer reading another NIC gets 403 |
| `POST /api/users` | Backoffice | `{"nic":"199012345670","fullName":"Ravi Alwis","email":"ravi@example.com","phone":"0701112222","address":"3 Hill St, Matara","role":"GridOperator","password":"Operator@123"}` — created Active |
| `PUT /api/users/{nic}` | self or Backoffice | `{"fullName":"Nimal Perera","email":"nimal@example.com","phone":"0771234567","address":"14 Galle Road, Colombo","solarCapacityKW":6.5}` |
| `PUT /api/users/me/password` | Any | `{"currentPassword":"Prosumer@123","newPassword":"NewPass@456"}` |
| `POST /api/users/me/deactivation-request` | Prosumer | No body. Status → DeactivationRequested |
| `POST /api/users/{nic}/activate` | Backoffice | Activates Dilani, or reactivates a deactivated account |
| `POST /api/users/{nic}/deactivate` | Backoffice | **Also cancels that prosumer's open bookings and frees the slots** |

### Stations

| Endpoint | Role | Body / notes |
|---|---|---|
| `GET /api/stations` | Any | Prosumers only ever see active stations |
| `GET /api/stations/nearby?latitude=6.9271&longitude=79.8612&radiusKm=25` | Any | Nearest first, with `distanceKm`. Radius 500 max |
| `GET /api/stations/{id}` | Any | A prosumer gets 404 for a deactivated station |
| `POST /api/stations` | Backoffice | `{"stationName":"Galle Coastal Node","latitude":6.0535,"longitude":80.2210,"capacityKWh":120,"totalBatterySlots":4,"operatingSchedule":"Mon-Sun 06:00-20:00"}` |
| `PUT /api/stations/{id}` | Backoffice | Same body. Reducing `totalBatterySlots` below an existing slot number is refused |
| `PATCH /api/stations/{id}/active` | Backoffice | `{"isActive":false}` — refused while active reservations exist |
| `DELETE /api/stations/{id}` | Backoffice | Refused if any reservation references it |

### Booking slots

| Endpoint | Role | Body / notes |
|---|---|---|
| `GET /api/slots/bookable` | Any | Available, active station, starting within 7 days. `?stationId=` to narrow |
| `GET /api/stations/{stationId}/slots` | Staff | `?from=&to=&status=` |
| `GET /api/slots/{id}` | Any | |
| `POST /api/slots` | Staff | `{"stationId":"{stationA}","batterySlotNumber":5,"startTime":"<UTC>","endTime":"<UTC>","capacityKWh":35,"isAvailable":true}` — UTC, must be future |
| `PUT /api/slots/{id}` | Staff | Same body minus `stationId`. Refused once Reserved |
| `PATCH /api/slots/{id}/availability` | Staff | `{"isAvailable":false}` — refused once Reserved |
| `DELETE /api/slots/{id}` | Staff | Refused once Reserved |

### Reservations

| Endpoint | Role | Body / notes |
|---|---|---|
| `GET /api/reservations` | Any | `?prosumerNic=&stationId=&status=&from=&to=`. Prosumers are always scoped to themselves |
| `GET /api/reservations/upcoming` | Any | Prosumer: own. Staff: `?prosumerNic=` required |
| `GET /api/reservations/history` | Any | Same |
| `GET /api/reservations/dashboard` | Any | `{"pending":1,"approvedUpcoming":1}`. `?stationId=` to narrow |
| `GET /api/reservations/{id}` | Any | **Only the owning prosumer sees `qrToken`** |
| `POST /api/reservations` | Any | `{"slotId":"{slot3days}","type":"DropOff","energyKWh":12.5}` — staff add `"prosumerNic"`. `type` is `DropOff` or `Charging` |
| `PUT /api/reservations/{id}` | owner or Staff | `{"slotId":"{slot3days}","energyKWh":15}` — any field optional. Returns to Pending, QR revoked |
| `POST /api/reservations/{id}/cancel` | owner or Staff | `{"reason":"Panels offline"}` — frees the slot |
| `POST /api/reservations/{id}/approve` | Staff | No body. Issues the QR token |
| `POST /api/reservations/verify-qr` | Staff | `{"qrToken":"<64 hex chars>"}` — check before finalising |
| `POST /api/reservations/complete` | Staff | Same body. Marks Completed |

### QR walkthrough

Sunil's reservation is already Approved, so:

1. As **Sunil**, `GET /api/reservations/{r2}` → copy `qrToken` (the mobile app renders this as the QR code).
2. As **Kamal** (operator), `POST /api/reservations/verify-qr` with that token → returns the reservation to check.
3. As **Kamal**, `POST /api/reservations/complete` → status becomes `Completed`.
4. Send complete again → `409`, already completed. The token is single-use.

## 5. Rule checks

Each of these is confirmed working against the seeded data:

| Send this | Expect |
|---|---|
| Book `{slot10days}` | `400` Reservations must be made within 7 days. |
| Book `{slot6hours}`, then cancel it | `400` Reservations can only be updated or cancelled at least 12 hours before they start. |
| Move a booking onto a slot under 12h away | `400` A reservation can only be moved to a slot that starts at least 12 hours from now. |
| Book with `energyKWh: 999` | `400` Energy must be greater than 0 and at most 50 kWh for this slot. |
| Log in as Dilani (200156789012) | `403` Your account is waiting for activation by a Backoffice officer. |
| Deactivate a station that has bookings | `409` This station has active energy reservations and cannot be deactivated. |
| Edit or delete a Reserved slot | `409` This slot is reserved and cannot be changed. |
| Deactivate Nimal, then read his reservation | Status `Cancelled`, reason `The prosumer's account was deactivated.`, slot back to `Available` |
| Reuse a deactivated user's token on any endpoint | `403` This account is not active. |
| Prosumer reads another prosumer's reservation | `404` (existence is not revealed) |
| Two prosumers book the same slot at once | One wins; the other gets `409` This slot has just been booked by someone else. |

## 6. Resetting

```powershell
# from SmartMicrogrid\SmartMicrogrid
$conn = (dotnet user-secrets list | Select-String '^MongoDb:ConnectionString = ').ToString().Split('=',2)[1].Trim()
mongosh $conn --quiet --eval "const d=db.getSiblingDB('SmartMicrogrid'); d.EnergyReservation.deleteMany({}); d.EnergyBookingSlots.deleteMany({}); d.SolarStationInfo.deleteMany({}); d.Users.deleteMany({role:{`$ne:'Backoffice'}});"
```

That clears the sample data but keeps the Backoffice account, so the seed script
can sign in and run again.
