# Fake GPS (Android)

A simple mock-location app for Android. It feeds a fake latitude/longitude to
the system location providers via Android's official mock-location API.

## Build on GitHub
1. Push these files to a repo (branch: `main`).
2. Go to the **Actions** tab -> **Build APK** -> wait for the run to finish.
3. Download the artifact **FakeGPS-debug-apk** -> install the APK on your phone.

## How to use
1. Enable Developer Options: Settings -> About phone -> tap "Build number" 7 times.
2. Developer Options -> **Select mock location app** -> choose **Fake GPS**.
3. Open the app, grant location permission, enter coordinates, press **Start**.
4. Verify in Google Maps. Press **Stop** to return to real GPS.

## Requirements
- Android 6.0+ (mock locations enabled)
- Location services turned on

## Disclaimer
For testing and development only. Misusing fake locations may violate the
terms of service of some apps/games and can get accounts banned.