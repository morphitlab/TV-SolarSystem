# Sideloading Solar System Live onto Fire TV

## 1. Enable Developer Options on Fire TV
1. Go to **Settings > My Fire TV > About**.
2. Highlight your Fire TV device name and press **OK** **7 times** until *"No need, you are already a developer"* appears.
3. Open **Developer Options** and turn ON **ADB Debugging** and **Apps from Unknown Sources**.

## 2. Build, Sideload & Profile
```bash
./gradlew assembleDebug test
adb connect <tv-ip>:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p com.solarsystemlive -c android.intent.category.LEANBACK_LAUNCHER 1
```

## 3. Capture Screenshot & Frame Profiling
```bash
adb exec-out screencap -p > shot.png
adb shell dumpsys gfxinfo com.solarsystemlive framestats
```
