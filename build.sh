#!/bin/bash

BASEPATH="$(dirname "$0")"
BASEPATH="$(cd "$BASEPATH" && pwd)"

cordova plugin rm test-plugin
rm -rf $BASEPATH/platforms/android/test-library-plugin
cordova plugin add $BASEPATH/plugins-src/test-plugin
cordova build android