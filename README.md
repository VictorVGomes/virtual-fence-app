<p align="center">
  <img src="ic_launcher/play_store_512.png" width="120" alt="Virtual Fence App Icon"/>
</p>

<h1 align="center">Virtual Fence</h1>

<p align="center">
  A personal geofencing app for Android that tracks how you spend your time across the places that matter to you.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" alt="Android"/>
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-informational" alt="Min SDK 26"/>
  <img src="https://img.shields.io/badge/License-MIT-blue" alt="MIT License"/>
</p>

---

## What it does

Virtual Fence lets you draw circular "fences" around any place in the world. The moment you step into or out of a fenced area your device records the event — timestamp, GPS coordinates, and event type. Over time you build up a personal log that shows exactly when and how long you were at each location.

**Why this is useful:** You decide which places deserve attention. Set a fence around your gym, your office, a relative's house, your car, or a neighborhood block. The app answers questions like *"How many hours did I actually spend at the gym this month?"* or *"How often do I visit the park?"* — without any cloud account or subscription.

---

## Features

### Core
- **Create geofences** — draw a circle (50 m – 5 000 m radius) around any GPS coordinate with a custom name and icon
- **Live inside/outside status** — see which fences you are currently inside, with a running timer
- **Event log** — every fence crossing (ENTER / EXIT) is stored locally with its GPS coordinates and timestamp
- **Observations** — attach free-text notes to any logged event (weather, what you were doing, etc.)
- **CSV export** — export your entire event history for analysis in a spreadsheet

### Map
- Interactive OpenStreetMap view (offline-capable) showing all your fences and current position
- Fence boundaries rendered as circles; user position shown with a real-time marker

### Notifications & Background Monitoring
- Optional push notifications on every fence crossing
- A foreground service keeps location updates alive so crossings are never missed
- Fences are automatically restored after a device reboot

### Personalization
- Choose an avatar character (male, female, cat, dog)
- Choose a marker icon for each fence (pin, home, flag, star, bell, heart)

### Debug / Testing
- Built-in mock location injection — test any fence crossing without leaving your desk

---

## How fence crossings are confirmed

Raw geofence events from Google Play Services can fire spuriously — a brief GPS jitter while you are standing still, or a "ghost" trigger from fence re-registration, can create false events. Virtual Fence runs every trigger through a **45-second confirmation window** before writing anything to the database:

```
Trigger received (ENTER or EXIT)
        │
        ▼
State-machine check ──► discard if transition is impossible
        │                (e.g. EXIT when no ENTER was ever recorded)
        ▼
Collect GPS samples for 45 s at 3-second intervals
        │
        ▼
Filter out samples with accuracy > 1.5 × fence radius
        │
        ▼
Require ≥ 3 confirming samples that agree on inside/outside
        │
        ▼
Persist event + send notification
```

The timestamp and coordinates stored with the event come from the **first sample of the final confirming run**, not from the original Google trigger. This means the logged position is always a real GPS fix, not an interpolated estimate.

Distance calculations use the **Haversine formula** on the WGS-84 sphere (radius 6 371 000 m), so fence boundaries are accurate anywhere on Earth.

---

## Architecture

```
app/
└── src/main/java/com/victorgomes/geofenceapp/
    ├── data/
    │   ├── database/          # Room entities, DAOs, migrations (schema v4)
    │   └── repository/        # GeofenceRepository — single data-access point
    ├── geofence/
    │   ├── GeofenceManager              # Registers/unregisters fences with Play Services
    │   ├── GeofenceBroadcastReceiver    # Receives raw transition intents
    │   ├── TransitionConfirmationWorker # 45-second GPS confirmation (WorkManager)
    │   └── BootReceiver                 # Restores active fences after reboot
    ├── service/
    │   └── GeofenceMonitorService       # Foreground service; keeps location updates alive
    ├── ui/
    │   ├── fences/        # Fence list, creation dialog, live status
    │   ├── map/           # OSMDroid map fragment
    │   ├── log/           # Event log, filter, CSV export
    │   ├── debug/         # Mock location, real-time fence status
    │   └── personalization/ # Avatar, icons, notification toggle
    └── utils/             # Notifications, permissions, shared prefs
```

**Pattern:** MVVM with a Repository layer. ViewModels expose LiveData; Fragments observe and update the UI reactively. Room handles all persistence; WorkManager runs the confirmation worker off the main thread.

---

## Tech stack

| Layer | Library / API |
|---|---|
| Language | Kotlin 1.9 |
| UI | Material Design 3, View Binding, Navigation Component |
| Architecture | ViewModel, LiveData, Kotlin Coroutines & Flow |
| Database | Room 2.6 (SQLite, schema version 4) |
| Location | Google Play Services Location 21.2 (Geofencing + Fused Location) |
| Map | OSMDroid 6.1 (OpenStreetMap, offline tiles) |
| Background work | WorkManager 2.9 |
| Min / Target SDK | 26 / 34 |

---

## Building from source

**Prerequisites**
- Android Studio Hedgehog or later (or the Android command-line tools)
- Java 17
- A device or emulator running Android 8.0+ with Google Play Services

```bash
# Clone
git clone https://github.com/VictorVGomes/virtual_fence.git
cd virtual_fence

# Debug build (installs on connected device)
./gradlew installDebug

# Or just build the APK
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

> **Note:** The first build downloads Gradle dependencies and may take a few minutes.

---

## Permissions

The app requests the following permissions and explains why:

| Permission | Why |
|---|---|
| `ACCESS_FINE_LOCATION` | Precise GPS required for accurate fence boundary checks |
| `ACCESS_BACKGROUND_LOCATION` | Detect crossings while the screen is off |
| `FOREGROUND_SERVICE_LOCATION` | Keep the monitor service alive in the background |
| `POST_NOTIFICATIONS` | Show crossing alerts (Android 13+) |
| `RECEIVE_BOOT_COMPLETED` | Re-register fences after the device restarts |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prevent the OS from killing the monitor service |

No data ever leaves the device. Everything is stored in a local SQLite database.

---

## Database schema

The Room database (`geofence_db`, version 4) has two tables:

**`geofence_configs`** — one row per fence
```
id (UUID PK) | name | latitude | longitude | radiusMeters | isActive | createdAt | markerIcon
```

**`geofence_events`** — one row per confirmed crossing
```
id (auto PK) | timestamp | eventType (ENTER/EXIT) | geofenceId | latitude | longitude | obs
```

Migration history: v1 → initial schema · v2 → configs table · v3 → `obs` column · v4 → `markerIcon` column

---

## License

[MIT](LICENSE) © 2026 Victor Gomes
