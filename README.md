# Dual Island Manager — Android app (v1.0)

**Two independent, user-configurable Dynamic-Island-style floating pills in one native Android app.** No root required. Minimum Android 8.0 (API 26), targets Android 16 (API 36), Java 17.

## What works in this first version

- Two separate always-on-top overlay windows, managed by one foreground service.
- Individually enable/disable each island. Choose **Media**, **Notifications**, **Shortcuts**, or **Timer** for either island.
- Drag each compact island to a new position; your phone remembers each position separately. A simple collision snap prevents the two compact pills resting directly on top of each other.
- Tap a pill to expand/collapse its controls.
- Set each island's accent colour, width, and opacity independently; reset its saved position.
- Media play/pause, previous, next, open music app. Now-playing titles rely on optional Android Notification Access; playback depends on the music app's media session.
- Latest notification (optional Notification Access), with private previews **off** by default. Tap to open the original notification when available.
- Shortcuts for Android Settings, camera, browser. Recent app launchers appear with optional Usage Access.
- Five-minute countdown timer with +1 minute and Cancel.
- Persistent foreground-service notification with **Stop islands** action; no automatic boot starts.
- Everything is local on-device. No analytics or external libraries.

## Important limitations

- This is a **source project, not a compiled APK**. The environment used to author the app does not include Android SDK/Gradle downloads and cannot test it on your phone. Compilation and runtime compatibility remain unverified until a build/test run succeeds.
- Overlay placement around a hole-punch camera and overlay survival differ between Pixel, Poco/HyperOS, and OnePlus/OxygenOS; battery restrictions may require changes on your device.
- This does not manipulate other developers' Dynamic Island apps. It creates its own two floating overlays.
- Android may temporarily hide overlays on sensitive permission/security screens and may block some controls in certain apps.
- Notification permission and usage access are opt-in; the app does not scrape other apps' private content.
- On Android 15+, foreground service launches from the background have additional restrictions. The user manually starts the service from the app. Google Play publication also requires reviewing `specialUse` foreground-service declarations.
- Shortcuts offer three preset actions; custom app picker and advanced animation aren't part of v1.

## Build on Android without a Windows PC (via GitHub Actions)

This project is hosted at [Wayudiesa24/DualIslandManager](https://github.com/Wayudiesa24/DualIslandManager).

1. Open the repository's **Actions** tab and select **Build Dual Island Manager APK**. On the initial upload, a build starts automatically; you can also choose **Run workflow**.
2. When the workflow has a green check, open the run → **Artifacts** → **dual-island-manager-debug-apk**. Download the artifact ZIP and extract `app-debug.apk`.
3. On Android, open the extracted APK and allow installation from that download source if prompted. Launch **Dual Island Manager** → allow **Display over other apps** → **Start islands**.
4. Optional: allow **Notification access** for media/notification information and **Usage access** for recent-app shortcuts. On some phones, background/battery optimisation settings may be needed.

The initial GitHub commit includes `dual-island-source.tar.xz` as a complete source snapshot. The GitHub Actions workflow extracts the source before building and attempts to publish the Java/XML/Gradle source files to the repository for convenient browsing. This publishing step is best effort and does not block the APK build if branch permissions prevent it.

The workflow uses Java 17, Gradle 8.13 and Android API 36; it produces a debug-signed APK. For long-term distribution, set up your own secure signing key rather than distributing a debug APK publicly.

## Build with Android Studio

Open this project folder in a recent Android Studio that supports AGP 8.13.2, ensure Android SDK Platform 36 and JDK 17 are installed, then run `:app:assembleDebug`. The resulting APK should be in `app/build/outputs/apk/debug/app-debug.apk`.

## Source map

- `MainActivity.java`: local settings, island mode/colour/size/opacity, permission shortcuts and start/stop.
- `IslandService.java`: foreground service, two independent overlay windows, drag/tap interaction, media/shortcuts/timer.
- `IslandNotificationListener.java`: optional Android notifications and current media-session permission.
- `IslandConfig.java`: preferences.
- `AndroidManifest.xml`: permissions and required service types.
- `.github/workflows/build-apk.yml`: cloud APK build.