#!/bin/bash

echo "- Cleaning up before start the build process..."
./gradlew fetch2:clean
./gradlew fetch2core:clean
./gradlew fetch2fileserver:clean
./gradlew fetch2okhttp:clean
./gradlew fetch2rx:clean
./gradlew fetchmigrator:clean

echo "- Building fetch2..."
./gradlew fetch2:assembleRelease

echo "- Building fetch2core..."
./gradlew fetch2core:assembleRelease

echo "- Building fetch2fileserver..."
./gradlew fetch2fileserver:assembleRelease

echo "- Building fetch2okhttp..."
./gradlew fetch2okhttp:assembleRelease

echo "- Building fetch2rx..."
./gradlew fetch2rx:assembleRelease

echo "- Building fetchmigrator..."
./gradlew fetchmigrator:assembleRelease

echo "- Publishing to maven..."
./gradlew fetch2:publish
./gradlew fetch2core:publish
./gradlew fetch2fileserver:publish
./gradlew fetch2okhttp:publish
./gradlew fetch2rx:publish
./gradlew fetchmigrator:publish

echo "Finished!"
