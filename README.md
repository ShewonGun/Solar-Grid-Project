# Solar Grid Project

Smart Solar Microgrid — an ASP.NET Core 8 web service backed by MongoDB Atlas,
with a React web console and an Android mobile app.

## Repository layout

```
Solar-Grid-Project/
├── backend/SmartMicrogrid/     ASP.NET Core 8 Web API (the service)
├── frontend/                   React 19 + Vite + Tailwind web app
└── mobile/SmartGridMobile/     Android app (Kotlin + Jetpack Compose)
```

All three talk to the **same** running backend. Whichever of frontend/mobile
you're working on, **Part 1 (the backend) always has to be running first** —
come back to it before starting Part 2 or Part 3.

---

# Part 1 — Backend (ASP.NET Core Web API)

## 1.1 Prerequisites

| Tool | Notes |
|---|---|
| .NET SDK 8 or 9 | The project targets `net8.0`; either SDK builds it. [Download](https://dotnet.microsoft.com/download) |
| Git | |
| Visual Studio 2022 *or* VS Code | Optional — `dotnet run` works from any terminal |

Check what you have:

```bash
dotnet --version
```

## 1.2 Clone the repository

```bash
git clone https://github.com/ShewonGun/Solar-Grid-Project.git
cd Solar-Grid-Project
```

Everyone (backend, frontend, mobile) starts from this same clone — you don't
need three separate checkouts.

## 1.3 Get access to the database

**Ask the project owner for two things:**

1. The three secret values (see 1.4).
2. Your public IP added to the **MongoDB Atlas allow-list**.

The second one catches everybody. Atlas rejects connections from unknown IP
addresses, and the failure looks like a connection timeout rather than a
permissions error. If your API starts but hangs on the first database call,
this is why. Your IP also changes when you switch networks — campus WiFi, home,
mobile hotspot are all different.

> **Owner:** Atlas dashboard → **Network Access** → **Add IP Address**.

## 1.4 Set the secrets

No credentials are stored in this repository. `appsettings.json` ships with
empty placeholders, and the real values live in .NET **user secrets** on your
own machine, outside the repo, where they cannot be committed by accident.

Run these from the folder containing the `.csproj` — the path matters:

```bash
cd backend/SmartMicrogrid/SmartMicrogrid

dotnet user-secrets set "MongoDb:ConnectionString" "mongodb+srv://..."
dotnet user-secrets set "Jwt:Key" "<the shared 64-character key>"
dotnet user-secrets set "Seed:AdminPassword" "<the shared admin password>"
```

Verify:

```bash
dotnet user-secrets list
```

Notes:

- The separator is a **colon** (`MongoDb:ConnectionString`). Double underscores
  are only used for environment variables when deploying to IIS.
- In PowerShell, if a value contains `$`, wrap it in **single** quotes so
  PowerShell does not treat it as a variable.
- `MongoDb:ConnectionString` and `Jwt:Key` are required — the app refuses to
  start without them. `Jwt:Key` must be at least 32 bytes.
- `Seed:AdminPassword` is technically optional, but without it no Backoffice
  user is created and nobody can log in.

Visual Studio users can instead right-click the **SmartMicrogrid.Api** project →
**Manage User Secrets**, which opens the same file in the editor.

## 1.5 Run the backend

```bash
cd backend/SmartMicrogrid/SmartMicrogrid
dotnet run
```

Or open `backend/SmartMicrogrid/SmartMicrogrid.sln` in Visual Studio, make sure
the **http** launch profile is selected (the dropdown next to the Run button),
and press F5.

Plain `dotnet run` — and the `http` profile in Visual Studio — serve plain HTTP
on **port 8081**, which is what both the frontend and the mobile app are
already configured to call, with no per-developer setup needed:

| | URL |
|---|---|
| API | `http://localhost:8081` |
| Swagger UI | `http://localhost:8081/swagger` |
| Health check | `http://localhost:8081/api/database/ping` |

A working setup returns:

```json
{ "status": "connected" }
```

> There is also an `https` launch profile (`https://localhost:7068`) if you
> specifically want to test over HTTPS. Nothing in this repo points at it by
> default, so you only need it if you're debugging HTTPS behaviour on
> purpose.

Leave this terminal running. Move on to **Part 2** for the web console or
**Part 3** for the mobile app — both need it up.

---

# Part 2 — Frontend (React web console)

## 2.1 Prerequisites

| Tool | Notes |
|---|---|
| Node.js 20+ | [Download](https://nodejs.org/) |
| The backend running | See Part 1 — the console has nothing to talk to without it |

Check what you have:

```bash
node --version
```

## 2.2 Install and run

```bash
cd frontend
npm install
npm run dev
```

Opens on `http://localhost:5173`, which is already in the API's allowed CORS
origins, so no extra configuration is needed. It already knows to call the
backend at `http://localhost:8081/api` without any setup.

## 2.3 (Optional) Google Maps key

The dashboard's node map and the "pick location on a map" control on node
registration need a Google Maps JavaScript API key. Without one, those two
spots show a plain message explaining the key is missing instead of a map —
everything else in the console works fine.

To enable it:

```bash
cd frontend
cp .env.example .env
```

Then open `.env` and fill in:

```
VITE_GOOGLE_MAPS_API_KEY=<your key>
```

Get a key from the [Google Cloud Console](https://console.cloud.google.com/),
restricted to the Maps JavaScript API. Restart `npm run dev` after editing
`.env` — Vite only reads it at startup.

## 2.4 If Vite won't start on 5173

If port 5173 is already taken, Vite will pick 5174 instead — and CORS **will**
fail, because only 5173 (and 3000) are in the backend's allow-list. Free up
5173, or pin it in `frontend/vite.config.js`:

```js
server: { port: 5173, strictPort: true }
```

---

# Part 3 — Mobile (Android app)

## 3.1 Prerequisites

| Tool | Notes |
|---|---|
| Android Studio (latest stable) | Ships its own JDK — you don't need to install one separately. [Download](https://developer.android.com/studio) |
| Android SDK Platform 37 | The app targets API 37; Android Studio's SDK Manager will offer to download it the first time you open the project if you don't have it |
| An emulator or a physical Android device | Minimum Android 7.0 (API 24). A physical device needs USB debugging turned on |
| The backend running | See Part 1 |

## 3.2 Open the project

Don't open the repository root — open the **`mobile/SmartGridMobile`** folder
specifically:

1. Android Studio → **Open**.
2. Select `Solar-Grid-Project/mobile/SmartGridMobile`.
3. Let Gradle sync finish (bottom status bar). First sync downloads
   dependencies and can take a few minutes.

Gradle sync automatically creates `local.properties` with your local Android
SDK path (`sdk.dir`) — you don't set that by hand, and it's `.gitignore`d
because it's specific to your machine.

## 3.3 Add the Google Maps key

The node map screen needs a Google Maps key too, kept out of the repo the same
way as the backend secrets. Open (or create) `local.properties` in
`mobile/SmartGridMobile/` — the same file Gradle just generated — and add a
line:

```
MAPS_API_KEY=<your key>
```

Without it the app still runs; the map screen just explains that no key is
configured instead of showing a map. Re-sync Gradle after adding the key
(**File → Sync Project with Gradle Files**) since it's read at build time.

## 3.4 Run it

1. Start the backend first (Part 1) — plain `dotnet run`, serving on 8081.
2. Pick a device in Android Studio's device dropdown:
   - **Emulator** — works immediately. The app is already configured to reach
     the backend at `http://10.0.2.2:8081/api/`, which is how the emulator
     sees your machine's `localhost`.
   - **Physical device** — see 3.5 first, it needs one extra change.
3. Press **Run ▶**.

## 3.5 Running on a physical device instead of the emulator

A real phone isn't on your machine's loopback, so `10.0.2.2` won't reach
anything from it. Two things need to change:

1. **Same Wi-Fi.** The phone and your development machine must be on the same
   network.
2. **Point the app at your machine's LAN IP.** In
   `mobile/SmartGridMobile/app/build.gradle.kts`, change:

   ```kotlin
   buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8081/api/\"")
   ```

   to your machine's LAN IP (find it with `ipconfig` on Windows, look for
   "IPv4 Address"):

   ```kotlin
   buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.23:8081/api/\"")
   ```

   Then re-sync Gradle and re-run — this field is read at build time, not
   runtime.

If your router hands out addresses outside `192.168.0.x`/`192.168.1.x`
(common on eduroam/campus Wi-Fi, some home mesh routers), plain HTTP to your
IP may also be blocked by the app's cleartext policy. It's allow-listed in
`app/src/main/res/xml/network_security_config.xml` — add your subnet there if
you hit an unexpected connection failure that only happens on a physical
device, never the emulator.

Remember to change `API_BASE_URL` back to `10.0.2.2` before committing, or
your teammates' emulators will silently stop working.

---

## Test data

`backend/SmartMicrogrid/TESTING.md` documents every endpoint and the expected
responses. To load sample stations, slots and users:

```powershell
cd backend/SmartMicrogrid
.\tools\Seed-TestData.ps1 -AdminPassword "<Seed:AdminPassword>"
```

Run it against an empty database — re-running adds a second set of stations and
slots.

The Backoffice account is seeded at startup with NIC `199000000001` and your
`Seed:AdminPassword`. Use it to sign in to the web console; mobile sign-in is
for Prosumer accounts, which register themselves from within the app.

## Deployment

`backend/SmartMicrogrid/DEPLOYMENT.md` covers publishing the API to IIS, both
via `tools/Setup-IIS.ps1` and by hand. On a server the secrets come from
per-site environment variables rather than user secrets.

Note that **Swagger is only available in Development** — a deployed instance
will 404 on `/swagger`. Use `/api/database/ping` to check that a deployment is
alive.

---

## Troubleshooting

**`InvalidOperationException: MongoDb:ConnectionString is not configured.`**

User secrets are only loaded when `ASPNETCORE_ENVIRONMENT=Development`, which
`dotnet run` and Visual Studio set automatically via `launchSettings.json`. You
get this error when you run the compiled `.exe` directly, or use
`dotnet run --no-launch-profile`. Either use `dotnet run`, or set the variable
first:

```powershell
$env:ASPNETCORE_ENVIRONMENT = 'Development'
```

If you get it from `dotnet run`, your secrets were not saved — re-check 1.4,
and make sure you ran the commands from the folder with the `.csproj`.

**`Could not find the global property 'UserSecretsId'`**

You ran `dotnet user-secrets` from the wrong folder. It must be
`backend/SmartMicrogrid/SmartMicrogrid`.

**The API starts, then hangs or times out on the first database call**

Your IP is not on the Atlas allow-list. See 1.3.

**`Jwt:Key must be at least 32 bytes`**

The key you were given was truncated when you copied it.

**CORS errors in the browser console (frontend)**

The frontend isn't on port 5173/3000, or the backend was started with the
`https` launch profile instead of the default `http` one. See 1.5 and 2.4.

**Mobile app can't reach the backend / connection refused**

- Emulator: make sure the backend is actually running and bound to port 8081
  (the default `http` profile from 1.5) — the emulator's `10.0.2.2` maps
  straight to your machine's `localhost`.
- Physical device: see 3.5. This is almost always either not being on the
  same Wi-Fi, or `API_BASE_URL` still pointing at `10.0.2.2`.

**Map screen shows "no key configured" instead of a map**

Expected without a Maps key — see 2.3 (web) or 3.3 (mobile). Everything else
still works.

---

## Never commit secrets

`appsettings.json` is tracked by git. Anything you paste into it goes into the
repository history permanently — deleting it in a later commit does not remove
it, and the credential has to be rotated. The same goes for `.env` (frontend)
and `local.properties` (mobile) — both are already `.gitignore`d, so don't
force-add them.

Keep connection strings and keys in user secrets / `.env` / `local.properties`,
and share them over a direct message, never in a commit, issue or pull
request.
