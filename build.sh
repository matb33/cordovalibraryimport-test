#!/bin/bash

BASEPATH="$(dirname "$0")"
BASEPATH="$(cd "$BASEPATH" && pwd)"

cordova plugin rm test-plugin
rm -rf $BASEPATH/platforms/android/TabloPlayer
rm -rf $BASEPATH/plugins/com.nuvyyo.tabloplayer
cordova plugin add $BASEPATH/plugins-src/test-plugin
export ANDROID_BUILD="gradle"; cordova build android
