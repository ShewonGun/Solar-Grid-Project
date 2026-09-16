# Solar Grid Project

Smart Solar Microgrid — an ASP.NET Core 8 web service backed by MongoDB Atlas,
with a React web app and an Android mobile app.

## Repository layout

```
Solar-Grid-Project/
├── backend/SmartMicrogrid/     ASP.NET Core 8 Web API (the service)
├── frontend/                   React 19 + Vite + Tailwind web app
└── mobile/                     Android app
```

---

## 1. Prerequisites

| Tool | Notes |
|---|---|
| .NET SDK 8 or 9 | The project targets `net8.0`; either SDK builds it. [Download](https://dotnet.microsoft.com/download) |
| Node.js 20+ | For the React frontend |
| Git | |
| Visual Studio 2022 *or* VS Code | Optional — `dotnet run` works from any terminal |

Check what you have:

```bash
dotnet --version
node --version
```

## 2. Clone

```bash
git clone https://github.com/ShewonGun/Solar-Grid-Project.git
cd Solar-Grid-Project
```

## 3. Get access to the database

**Ask the project owner for two things:**

1. The three secret values (see step 4).
2. Your public IP added to the **MongoDB Atlas allow-list**.

The second one catches everybody. Atlas rejects connections from unknown IP
addresses, and the failure looks like a connection timeout rather than a
permissions error. If your API starts but hangs on the first database call,
this is why. Your IP also changes when you switch networks — campus WiFi, home,
mobile hotspot are all different.

> **Owner:** Atlas dashboard → **Network Access** → **Add IP Address**.

## 4. Set the secrets

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

## 5. Run the backend

```bash
cd backend/SmartMicrogrid/SmartMicrogrid
dotnet run
```

Or open `backend/SmartMicrogrid/SmartMicrogrid.sln` in Visual Studio and press F5.

| | URL |
|---|---|
| API | `https://localhost:7068` |
| Swagger UI | `https://localhost:7068/swagger` |
| Health check | `https://localhost:7068/api/database/ping` |

A working setup returns:

```json
{ "status": "connected" }
```

**One-time step** — trust the local HTTPS certificate, or the browser will
reject `https://localhost:7068`:

```bash
dotnet dev-certs https --trust
```

## 6. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Opens on `http://localhost:5173`, which is already in the API's allowed CORS
origins, so no extra configuration is needed.

Point API calls at the HTTPS port:

```js
const BASE_URL = import.meta.env.VITE_API_URL ?? 'https://localhost:7068'
```

> **Do not use `http://localhost:5062`.** It is bound, but HTTPS redirection
> runs before the CORS middleware, so the browser sees a redirect on the
> preflight request and reports an unhelpful CORS error.

If Vite starts on 5174 because 5173 was taken, CORS **will** fail — that origin
is not in the allow-list. Pin the port in `vite.config.js`:

```js
server: { port: 5173, strictPort: true }
```

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
`Seed:AdminPassword`.

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

If you get it from `dotnet run`, your secrets were not saved — re-check step 4,
and make sure you ran the commands from the folder with the `.csproj`.

**`Could not find the global property 'UserSecretsId'`**

You ran `dotnet user-secrets` from the wrong folder. It must be
`backend/SmartMicrogrid/SmartMicrogrid`.

**The API starts, then hangs or times out on the first database call**

Your IP is not on the Atlas allow-list. See step 3.

**CORS errors in the browser console**

Either the frontend is not on port 5173/3000, or you are calling the HTTP port
5062 instead of HTTPS 7068. See step 6.

**`Jwt:Key must be at least 32 bytes`**

The key you were given was truncated when you copied it.

---

## Never commit secrets

`appsettings.json` is tracked by git. Anything you paste into it goes into the
repository history permanently — deleting it in a later commit does not remove
it, and the credential has to be rotated.

Keep connection strings and keys in user secrets, and share them over a direct
message, never in a commit, issue or pull request.
