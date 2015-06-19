#!/bin/bash
COMMIT_HASH=6c99edb

# Delete existing companion library
rm -rf CastCompanionLibrary-android

# Clone Repo
git clone git@github.com:googlecast/CastCompanionLibrary-android.git

# Checkout required commit
cd CastCompanionLibrary-android
git checkout $COMMIT_HASH

