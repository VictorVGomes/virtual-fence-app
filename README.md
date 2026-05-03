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

**Why this is useful:** You decide which places deserve attention. Set a fence around your gym, your office, a relative's house, or a neighborhood block. The app answers questions like *"How many hours did I actually spend at the gym this month?"* or *"How often do I visit the park?"* — without any cloud account or subscription.

---

## Features

### Core
- **Create geofences** — draw a circle (50 m – 5 000 m radius) around any GPS coordinate with a custom name and icon
- **Live inside/outside status** — see which fences you are currently inside, with a running timer
- **Event log** — every fence crossing (ENTER / EXIT) is stored locally with its GPS coordinates and timestamp
- **CSV export** — export your entire event history for analysis in a spreadsheet

### Map
- Interactive OpenStreetMap view (offline-capable) showing all your fences and current position
- Fence boundaries rendered as circles; user position shown with a real-time marker
- Long-press anywhere to create a new fence; tap an existing fence to edit or delete it
- Built-in location search

### Statistics
Three chart visualizations on the Stats screen help you understand your patterns over time:

- **Entry/Exit histogram** — distribution of fence crossings by 5-minute slot across the full day (pinch to zoom); select which fence to inspect via the dropdown
- **Average daily time per fence** — bar chart showing how many hours per day you spend inside each fence on average; filter by All / Weekdays / Weekends / No Holidays / Workdays
- **Daily timeline** — swimlane/Gantt chart of every interval inside each fence for the selected day; tap a bar to see start time, end time, and duration; navigate with the day arrows or the date picker

### Notifications & Background Monitoring
- Optional push notifications on every confirmed fence crossing
- A foreground service keeps location updates alive so crossings are never missed
- Activity Recognition integration: the service backs off to 5-minute geofence responsiveness when you are stationary and ramps back to 1-minute responsiveness when you start moving — saving battery without missing crossings
- Fences are automatically restored after a device reboot

### Personalization
- Choose an avatar character (male, female, cat, dog)
- Choose a marker icon for each fence (pin, home, flag, star, bell, heart)

### Debug / Testing
- Built-in mock location injection — test any fence crossing without leaving your desk

---

## How fence crossings are confirmed

Raw geofence events from Google Play Services can fire spuriously — brief GPS jitter or a ghost trigger from fence re-registration can create false events. Virtual Fence runs every trigger through a **45-second confirmation window** before writing anything to the database:

```
Trigger received (ENTER or EXIT)
        │
        ▼
Enqueue TransitionConfirmationWorker (WorkManager, 45 s initial delay)
        │
        ▼
After 45 s: check that the transition was not immediately reversed
        │
        ▼
Persist event → send notification
```

The 45-second delay filters out brief boundary crossings where the device quickly re-crosses in the opposite direction. Only transitions that are still valid after the window has elapsed are written to the database.

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
    │   ├── GeofenceBroadcastReceiver    # Receives raw transition intents; enqueues worker
    │   ├── TransitionConfirmationWorker # 45-second confirmation delay (WorkManager)
    │   ├── ActivityRecognitionManager   # Registers for STILL_ENTER/STILL_EXIT transitions
    │   ├── ActivityTransitionReceiver   # Adapts geofence responsiveness to motion state
    │   └── BootReceiver                 # Restores active fences after reboot
    ├── service/
    │   └── GeofenceMonitorService       # Foreground service; passive-first GPS + watchdog
    ├── ui/
    │   ├── fences/        # Fence list, toggle active/inactive, delete, edit
    │   ├── map/           # OSMDroid map, fence creation, character/icon selection, search
    │   ├── log/           # Event log, CSV export
    │   ├── stats/         # Histogram, avg-time chart, swimlane timeline; MPAndroidChart
    │   ├── personalization/ # Avatar, icons, notification toggle
    │   └── debug/         # Mock location injection
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
| Location / Geofencing | Google Play Services Location 21.2 |
| Activity Recognition | Google Play Services Location 21.2 (Activity Transitions API) |
| Map | OSMDroid 6.1 (OpenStreetMap, offline tiles) |
| Charts | MPAndroidChart v3.1.0 (via JitPack) |
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

| Permission | Why |
|---|---|
| `ACCESS_FINE_LOCATION` | Precise GPS required for accurate fence boundary detection |
| `ACCESS_BACKGROUND_LOCATION` | Detect crossings while the screen is off |
| `FOREGROUND_SERVICE_LOCATION` | Keep the monitor service alive in the background |
| `ACTIVITY_RECOGNITION` | Detect when the device is stationary to adapt battery usage |
| `POST_NOTIFICATIONS` | Show crossing alerts (Android 13+) |
| `RECEIVE_BOOT_COMPLETED` | Re-register fences after the device restarts |
| `WAKE_LOCK` | Ensure the confirmation worker completes when the device dozes |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prevent the OS from killing the monitor service |
| `INTERNET` / `ACCESS_NETWORK_STATE` | OSMDroid map tile downloads |

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
