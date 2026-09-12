<img width="1280" height="960" alt="image" src="https://github.com/user-attachments/assets/67eef54a-6aea-486c-9a43-550387652aa8" />

# B-Barq ⚡

> [!NOTE]
> <div dir="ltr" style="text-align: left;">
> 
> [برای مشاهده توضیحات به فارسی کلیک کنید](README.md)
> 
> </div>
B-Barq is an Android app that monitors power outage schedules for multiple places from the official BargheMan.com source and reminds you ahead of time — so your phone never runs out of charge and your work stays safe.

## How It Works
1. **Sign in with your phone number** (OTP verification via BargheMan.com).
2. **Add one or more places**, each with its own Bill ID (شناسه قبض).
3. **Grant the required permissions.**
4. **Start the foreground service** to begin monitoring.

The service checks the outage source every hour, displays the latest schedules for all your places in a persistent notification, and sends reminders before each outage based on the offsets you configure per place.

## Features
* Monitor **multiple places** at once, each with its own outage schedule.
* Hourly monitoring of official outage schedules.
* Persistent, expandable notification showing outages sorted by urgency, with failed places listed separately.
* **Configurable reminder offsets** per place (e.g. 15 min, 30 min, 1 hour before).
* Phone-based sign-in with OTP.
* Material Design 3 UI built with Jetpack Compose.

## Requirements
* Android 7.0+
* Internet access
* A valid phone number for sign-in

## Installation
1. Download the latest APK from [Releases](https://github.com/alijafari-gd/B-Barq/releases).
2. Download `SHA256SUMS.txt` from the same release and check the download is intact:

   ```bash
   sha256sum -c SHA256SUMS.txt
   ```

3. Install the APK on your device.
4. Sign in, add your place(s), grant permissions, and start the service.

> [!IMPORTANT]
> Every release published before now was signed with a debug key by mistake.
> Releases are signed with the real upload key from now on, and Android refuses
> to update an app whose signing key changed. If you installed an earlier APK,
> **uninstall it first, then install the new one** (uninstalling clears the
> app's saved data).

## Building from source

```bash
git clone https://github.com/MrMatin0/B-Barq.git
cd B-Barq
./gradlew assembleDebug
```

You need JDK 17 and Android SDK 36; Gradle provisions the JDK for you if it is
missing. [docs/BUILDING.md](docs/BUILDING.md) covers the full build, how to
generate a signing key, and how to cut a release.

## Privacy
B-Barq does not collect or share personal information such as your name, phone number, or bill ID.

The app stores your saved data locally on your device. Requests for electricity outage information are made directly from the app to the relevant service.

The app bundles no analytics or tracking SDK. (Older versions used Yandex AppMetrica; that dependency has been removed.)

## Contribution
Contributions are welcome! If you have ideas, improvements, or bug fixes, feel free to:
* Fork the repository
* Create a new branch
* Submit a pull request

Run `./gradlew assembleDebug test lint` before opening a pull request — the same
three tasks run in CI on every PR.

## TODO (Upcoming Features)
* Automatic Bill ID detection based on the user's location
* Widget support for at-a-glance outage status

## License
MIT License

### Made with love (and frequent power cuts) by [AJ](https://github.com/alijafari-gd/). Inspired by his autistic friend.
