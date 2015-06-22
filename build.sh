#!/bin/bash

BASEPATH="$(dirname "$0")"
BASEPATH="$(cd "$BASEPATH" && pwd)"

cd $BASEPATH

cordova plugin rm test-plugin

# rm -rf $BASEPATH/platforms/android/com.google.android.libraries.cast.companionlibrary
rm -rf $BASEPATH/platforms/android/com.nuvyyo.tabloplayer

# rm -rf $BASEPATH/plugins/com.google.android.libraries.cast.companionlibrary
rm -rf $BASEPATH/plugins/com.nuvyyo.tabloplayer

cordova plugin add $BASEPATH/plugins-src/test-plugin

export ANDROID_BUILD="gradle"; cordova build android
