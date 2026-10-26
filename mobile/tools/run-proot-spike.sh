#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
MOBILE_DIR="$ROOT_DIR/mobile"
ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
AVDMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager"
EMULATOR="$ANDROID_HOME/emulator/emulator"
APK="$MOBILE_DIR/app/build/outputs/apk/debug/app-debug.apk"
RESULTS_DIR="$MOBILE_DIR/spike-results"
SERIAL="emulator-5554"
PACKAGE="dev.prootcode.app"
ACTIVITY="$PACKAGE/.MainActivity"
REPORT_TIMEOUT_SECONDS="${REPORT_TIMEOUT_SECONDS:-900}"

APIS=("$@")
if [ ${#APIS[@]} -eq 0 ]; then
    APIS=(26 29 33 35 36)
fi

if [ ! -f "$APK" ]; then
    echo "missing $APK, run ./gradlew assembleDebug first" >&2
    exit 1
fi

mkdir -p "$RESULTS_DIR"

kill_emulator() {
    adb -s "$SERIAL" emu kill >/dev/null 2>&1 || true
    sleep 2
}

wait_for_boot() {
    timeout 600 adb -s "$SERIAL" wait-for-device
    local waited=0
    until [ "$(adb -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
        sleep 5
        waited=$((waited + 5))
        if [ "$waited" -ge 300 ]; then
            echo "emulator did not boot in $waited seconds" >&2
            return 1
        fi
    done
    sleep 5
}

run_api() {
    local api="$1"
    local image="system-images;android-$api;default;x86_64"
    local avd="spike-$api"
    local out="$RESULTS_DIR/api-$api"
    mkdir -p "$out"

    echo "=== API $api ==="
    if [ ! -d "$ANDROID_HOME/system-images/android-$api/default/x86_64" ]; then
        yes | "$SDKMANAGER" "$image" >/dev/null
    fi
    if ! "$AVDMANAGER" list avd 2>/dev/null | grep -q "Name: $avd"; then
        echo no | "$AVDMANAGER" create avd -n "$avd" -k "$image" -d pixel_5 --force >/dev/null
    fi

    kill_emulator
    "$EMULATOR" -avd "$avd" -no-window -no-audio -no-boot-anim -no-snapshot -wipe-data \
        -gpu angle_indirect -memory 4096 -cores 4 -accel on -no-metrics \
        -port 5554 >"$out/emulator.log" 2>&1 &
    local emulator_pid=$!

    if ! wait_for_boot; then
        kill "$emulator_pid" >/dev/null 2>&1 || true
        return 1
    fi

    adb -s "$SERIAL" shell getprop >"$out/device.txt"
    adb -s "$SERIAL" install -r -g "$APK" >"$out/install.txt" 2>&1
    adb -s "$SERIAL" logcat -c

    adb -s "$SERIAL" shell am start -W -n "$ACTIVITY" --ez autorun true >"$out/launch.txt" 2>&1

    local waited=0
    local report=""
    while [ "$waited" -lt "$REPORT_TIMEOUT_SECONDS" ]; do
        report="$(adb -s "$SERIAL" shell run-as "$PACKAGE" cat files/diagnostics/report.json 2>/dev/null | tr -d '\r')"
        if [ -n "$report" ]; then
            break
        fi
        sleep 10
        waited=$((waited + 10))
    done

    if [ -z "$report" ]; then
        echo "no report produced for API $api after $waited seconds" >&2
    else
        printf '%s\n' "$report" >"$out/report.json"
        adb -s "$SERIAL" shell run-as "$PACKAGE" cat files/diagnostics/report.txt \
            | tr -d '\r' >"$out/report.txt"
    fi

    adb -s "$SERIAL" logcat -d -v threadtime >"$out/logcat.txt"
    adb -s "$SERIAL" logcat -b all -d -v brief | grep -i 'avc:' >"$out/avc.txt" || true
    adb -s "$SERIAL" logcat -d -s "ProotSpike:*" >"$out/proot-spike-log.txt" || true
    adb -s "$SERIAL" shell "run-as $PACKAGE ls -la files/spike" >"$out/app-files.txt" 2>&1 || true

    adb -s "$SERIAL" emu kill >/dev/null 2>&1 || true
    kill "$emulator_pid" >/dev/null 2>&1 || true
    sleep 3
}

for api in "${APIS[@]}"; do
    if run_api "$api"; then
        echo "API $api done"
    else
        echo "API $api failed" >&2
    fi
done
