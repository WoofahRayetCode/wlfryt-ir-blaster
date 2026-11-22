# Wlfryt IR Blaster

An offline Android remote for the **Vizio D24h-J09**, tested with the **POCO X8 Pro Max** on Android 16. It uses the phone’s built-in infrared emitter, so there’s no TV pairing or network setup for the remote itself.

The app has Power, Input, a directional pad with OK, Back, Home, Mute, and volume controls. Tap to change the volume one step or hold to repeat. On the POCO X8 Pro Max, aim the back of the phone toward the TV, with the area around the rear cameras clear.

The app requires Android 6.0 or newer and a phone that exposes a compatible 38 kHz emitter to Android. The D24h-J09 is the only TV model tested so far.

## Build and install

Install JDK 17 or a newer JDK supported by Gradle 9.1, Android SDK Platform 36, Build Tools 36.0.0, and Android SDK Platform Tools. Set `JAVA_HOME` to your JDK and `ANDROID_HOME` to your SDK. Put the SDK’s `platform-tools` directory on your `PATH` for `adb`. JDK 25 was used for the tested Windows build. [Android build requirements](https://developer.android.com/build/releases/agp-9-0-0-release-notes) · [SDK Platform Tools](https://developer.android.com/tools/releases/platform-tools)

Clone the repo and connect your phone over USB with USB debugging enabled. Unlock the phone and approve the USB install prompt if HyperOS shows one.

```sh
git clone https://github.com/WoofahRayetCode/wlfryt-ir-blaster.git
cd wlfryt-ir-blaster
```

**Windows PowerShell**

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

**macOS or Linux**

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Open **Wlfryt IR Blaster** and try **Volume Up** and **Input** first to confirm the IR link.

## TV verification

| Control or flow | Observed result |
| --- | --- |
| Volume Up | Volume increased with the two-button build |
| Input | Worked with the two-button build. The full remote switched the TV to HDMI 1 for a PS5 |
| Arrows and OK | Navigation was sufficient to complete Wi-Fi setup and select HDMI 1. Each direction still needs a separate check |
| Power | Standby and wake await separate TV confirmation |
| Volume Down | Awaits separate TV confirmation |
| Mute | Awaits separate TV confirmation |
| Back | Awaits separate TV confirmation |
| Home | Awaits separate TV confirmation |
| Volume hold and release | Timing and cancellation passed unit tests. TV response awaits separate confirmation |

The remote profile lives in [`VizioProfile.java`](app/src/main/java/com/wlfryt/irblaster/VizioProfile.java). Its commands come from an XRT140R capture in Flipper-IRDB. [IR capture](https://github.com/Lucaslhm/Flipper-IRDB/blob/39c3290b7177f5badb144215897d630531fefb10/TVs/Vizio/Vizio_XRT140R.ir)
