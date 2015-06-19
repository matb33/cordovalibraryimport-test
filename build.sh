#!/bin/bash

BASEPATH="$(dirname "$0")"
BASEPATH="$(cd "$BASEPATH" && pwd)"

$BASEPATH/cordova plugin rm test-plugin
rm -rf $BASEPATH/platforms/android/test-library-plugin
$BASEPATH/cordova plugin add $BASEPATH/plugins-src/test-plugin
$BASEPATH/cordova build android