#!/bin/bash

rm -rf ./plugins/video-plugin/libs/video-plugin/*
cp -rf ./app ./build ./CastCompanionLibrary-android ./gradle ./.gradle ./android-video-player.iml ./build.gradle ./gradle.properties ./gradlew ./gradlew.bat ./local.properties ./settings.gradle ./plugins/video-plugin/libs/video-plugin

echo "" > ./plugins/video-plugin/libs/video-plugin/local.properties
