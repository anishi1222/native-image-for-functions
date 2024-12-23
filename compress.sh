#!/usr/bin/env bash

echo "Extracting the original ZIP"
cd target
unzip -o demofunc.zip
rm demofunc.zip

echo "Compressing the folder with ZIP"
cd app
zip -r -0 ../app.zip .
cd ../..
rm -rf target/app
