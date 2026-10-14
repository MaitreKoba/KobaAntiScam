#!/bin/bash
# Script pour reconstruire et installer l'application avec le nouveau nom et la nouvelle icône

echo "=== Cleaning project ==="
./gradlew clean

echo ""
echo "=== Building APK ==="
./gradlew assembleDebug

echo ""
echo "=== Uninstalling old version ==="
D:/Kobayashi/Developpement/Android/sdk/platform-tools/adb.exe uninstall fr.kobayashi.shield.debug

echo ""
echo "=== Installing new version ==="
D:/Kobayashi/Developpement/Android/sdk/platform-tools/adb.exe install -r app/build/outputs/apk/debug/app-debug.apk

echo ""
echo "=== Verifying app name ==="
D:/Kobayashi/Developpement/Android/sdk/platform-tools/adb.exe shell dumpsys package fr.kobayashi.shield.debug | grep "label"

echo ""
echo "=== Done! ==="
echo "The app should now show as 'Kobayashi AntiScan' with the new Tohru icon."
