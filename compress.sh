#!/usr/bin/env bash
set -euo pipefail

artifact_name="demofunc"

echo "Extracting the original ZIP"
cd target
rm -rf app app.zip
unzip -o demofunc.zip
rm demofunc.zip
find app -type f ! -name "${artifact_name}" -exec chmod 0644 {} +
chmod 0755 "app/${artifact_name}"

echo "Compressing the folder with ZIP"
cd app
zip -r -0 ../app.zip .
cd ../..
rm -rf target/app
