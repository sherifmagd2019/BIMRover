# 🗺️ BIMRover Pro: Revit-to-Field Geodetic GPS Stakeout Engine

A professional-grade, high-precision construction layout and geodetic coordinate transformation utility designed to translate digital Building Information Modeling (BIM) structural schedules from the office directly onto raw site dirt. Built natively for Android using Kotlin and Jetpack Compose.

---

## 🛠️ SPECIAL INSTRUCTIONS FOR HACKATHON JUDGES

To facilitate seamless technical evaluation without requiring real cellular RTK connections or live billing loops, we have built-in explicit reviewer shortcuts:

1. **Download & Install:** Download our pre-compiled release artifact (`app-debug.apk`) directly from the GitHub Releases tab or drag-and-drop it into any active Android Studio Emulator running API 31+.
2. **Instant Paywall Bypass:** On first launch, the **RevenueCat Entitlement Gate** will intercept the application dashboard. Scroll to the absolute bottom of the paywall and tap **"Instant Pro Unlock"**. This forces a simulated reviewer VIP tier override (`rc_surveyor_judge_vip`), instantly granting full enterprise feature access.
3. **Run a Field Simulation:** Open the **Stakeout** tab and click the **Tune/Slidewheel Icon** in the top action bar to expand the **Field Simulation Controls**. Click **"Snap to Target (0.00m)"** to instantly simulate single-centimeter alignment locks, or use the integrated D-Pad button arrays (+0.5m N, -0.5m W, etc.) to watch the guidance vectors, compass needle, and Cut/Fill grade counters adapt on the fly from your desk.

---

## 📐 Core Architectural Capabilities

### 1. Geodetic Coordinate Transformation Engine (`GeodeticCoordinateTransformer.kt`)
Autodesk Revit coordinates are generated as localized, flat linear Cartesian grid offsets from an arbitrary project base point. Real-world GNSS telemetry uses global ellipsoidal structures. 
Our mathematical engine performs rigid runtime conversions by factoring in:
* **WGS-84 Reference Ellipsoid Parameters** (Semi-major axis a = 6,378,137.0 m, Flattening f = 1 / 298.257223563).
* Dynamic calculation of the **Meridional Curvature Radius (M)** and **Prime Vertical Curvature Radius (N)** at the specific calibration anchor point.
* Rigid transformation matrix rotation utilizing clockwise **True North Offset Angles**.

### 2. High-Contrast "Sun-Glaze" Ergonomic Mode
Standard mobile dark aesthetics render efficiently indoors but completely wash out under intense glare on raw dirt sites. `Theme.kt` implements a custom `LocalSunGlazeMode` CompositionLocal framework that instantaneously forces a clean, ultra-high-contrast monochromatic light profile readable through heavily scratched safety eyewear in direct midday sunlight.

### 3. Dynamic Split-Metric Guidance & Diagnostics
* **Directional Compass View:** Fuses the device’s hardware `Sensor.TYPE_ROTATION_VECTOR` matrix arrays with live geodetic bearing values to display true direction indicators.
* **Sub-Meter Bullseye Reticle:** An interactive radar mesh that auto-zooms when the user gets within 0.5 meters of a structural coordinate, triggering a localized haptic vibration warning loop when entering a 20mm horizontal survey tolerance window.
* **Vertical Grade Control:** Tracks real-time elevation differences against design baselines to visually isolate a **RED "CUT"** diagnostic status (excavation needed) or a **BLUE "FILL"** diagnostic status (embankment building needed).

### 4. Zero-Connectivity Local Persistence
Remote construction excavations regularly lack reliable cellular networks. `StakeoutDatabase.kt` and `StakeoutDao.kt` deploy an offline-first **SQLite Room Database** pipeline to stamp, track, and save precise as-built surveying observations locally for subsequent raw CSV ledger distribution.

---

## 📦 Ingestion Schema Guide

The engine supports immediate parsing of custom Revit point charts (`RevitPointParser.kt`) using standard table matrices:

### Standard CSV Layout
```csv
Point_ID,Local_X,Local_Y,Local_Z,Category,Description
C1-NW,0.000,0.000,-2.450,Column Grid,NW Corner Main Core Pier
PILE-101,-3.200,-2.800,-8.600,Foundation Pile,1200mm Dia Cast-in-place Pile
```

---

## 🏗️ Local Compilation & Build Instructions

### Prerequisites
* **Android Studio Ladybug (2024.2.1)** or higher.
* **Android SDK 36** dependencies.
* Gradle Wrapper distribution version **9.3.1**.

### Setup Steps
1. Clone the project locally or open the unzipped root project folder inside Android Studio.
2. Allow Gradle to securely sync and index all dependencies defined inside `gradle/libs.versions.toml`.
3. Create a local environment parameters document named `.env` in the root project directory to declare your active configuration values (see `.env.example` as a structural reference layout):
   ```env
   GEMINI_API_KEY=YOUR_SECURE_API_SECRET_KEY
   ```
4. Build the executable binary directly via your terminal application window:
   ```bash
   ./gradlew assembleDebug
   ```
5. The compiled output installer asset will populate inside:
   `app/build/outputs/apk/debug/app-debug.apk`

---

## ⚖️ License
Built independently as a single-developer enterprise module submission for the **RevenueCat Ship-a-thon 2026** Challenge.
