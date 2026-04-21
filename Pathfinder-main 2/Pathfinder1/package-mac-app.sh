#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

mvn package

rm -rf target/app-input target/dist
mkdir -p target/app-input target/dist

cp target/Pathfinder.jar target/app-input/Pathfinder.jar
cp -R wallsets target/app-input/wallsets

jpackage \
  --type app-image \
  --name Pathfinder \
  --input target/app-input \
  --main-jar Pathfinder.jar \
  --main-class Pathfinder \
  --dest target/dist \
  --app-version 1.0 \
  --mac-package-identifier com.pathfinder.app \
  --java-options "-Dapple.awt.application.name=Pathfinder"

echo "Built target/dist/Pathfinder.app"
