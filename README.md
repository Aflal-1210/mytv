# My TV (SEI300SLT / PEO TV GO box V2)

A tiny Android TV launcher with exactly four tiles:
YouTube, YouTube Music, Spotify, Web Browser.

Each tile opens the official Android TV app if installed, otherwise falls back to the web version.

## Get the APK (no Android Studio needed)
1. Create a new GitHub repo and upload this whole folder (keep the .github folder).
2. Open the repo's Actions tab, run "Build APK" (it also runs on every push).
3. Download the `my-tv-apk` artifact and unzip it to get app-debug.apk.

## Install on the box
1. On the box: Settings > Device Preferences > About > tap "Build" 7 times.
2. Settings > Device Preferences > Developer options > enable USB/network debugging.
3. On your computer (same Wi-Fi): 
   adb connect BOX_IP:5555
   adb install app-debug.apk
   (Or put the APK on a USB stick and open it with a file manager app.)

## Customise
Edit the `tiles` list in MainActivity.kt (the "puppet" tiles with dummy = true do nothing, they are just for show).

## Auto-start on boot
The app launches itself when the box boots. Two things are needed on Android 10+:
1. Open "My TV" manually at least once after installing (Android won't deliver boot events to apps never opened).
2. Allow it to start from the background. Run once from your computer:
   adb shell appops set com.vibe.tvlauncher SYSTEM_ALERT_WINDOW allow
   (Or: Settings > Apps > My TV > Display over other apps > Allow.)

Then reboot the box to test: adb reboot

### Optional: make it the real home screen
If boot auto-start is blocked by SLT's firmware, add this inside the MainActivity <intent-filter> in AndroidManifest.xml:
    <category android:name="android.intent.category.HOME" />
    <category android:name="android.intent.category.DEFAULT" />
Android will then offer "My TV" as a home app (press Home and choose Always). Undo by removing those lines or clearing defaults.

## Keeping the box smooth (low-RAM mode)
- Home screen has no endless animation: it only redraws when you move between tiles.
- Browser / web fallbacks run in their own process and are fully shut down when you leave them.
- Keep the number of tiles small; every extra app you open uses some of the box's 1GB of RAM.
