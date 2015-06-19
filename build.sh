#!/bin/bash

BASEPATH="$(dirname "$0")"
BASEPATH="$(cd "$BASEPATH" && pwd)"

cd $BASEPATH

cordova plugin rm test-plugin
#cordova plugin rm com.nuvyyo.tabloplayer
#rm -rf $BASEPATH/platforms/android/TabloPlayer
rm -rf $BASEPATH/plugins/com.nuvyyo.tabloplayer
cordova plugin add $BASEPATH/plugins-src/test-plugin
#cordova plugin add $BASEPATH/plugins-src/TabloPlayer
export ANDROID_BUILD="gradle"; cordova build android
