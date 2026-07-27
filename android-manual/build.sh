#!/bin/bash
set -e

ANDROID_HOME=/opt/android-sdk
BUILD_TOOLS=$ANDROID_HOME/build-tools/34.0.0
PLATFORM=$ANDROID_HOME/platforms/android-34
AAPT2=$BUILD_TOOLS/aapt2
D8=$BUILD_TOOLS/d8
ZIPALIGN=$BUILD_TOOLS/zipalign
APKSIGNER=$BUILD_TOOLS/apksigner

PROJECT_DIR=$(pwd)
BUILD_DIR=$PROJECT_DIR/build
OUT_DIR=$PROJECT_DIR/out
ASSETS_DIR=$PROJECT_DIR/assets
WWW_DIR=$PROJECT_DIR/../www

rm -rf $BUILD_DIR $OUT_DIR $ASSETS_DIR
mkdir -p $BUILD_DIR $OUT_DIR $ASSETS_DIR

# Copy plain web assets
if [ -d "$WWW_DIR" ]; then
    cp -r $WWW_DIR/* $ASSETS_DIR/
else
    echo "WWW directory not found: $WWW_DIR"
    exit 1
fi

# Step 1: Compile resources with aapt2
echo "Compiling resources..."
$AAPT2 compile \
    --dir $PROJECT_DIR/res \
    -o $BUILD_DIR/res.zip

# Step 2: Link resources and generate R.java / base APK
echo "Linking resources..."
$AAPT2 link \
    -I $PLATFORM/android.jar \
    --manifest $PROJECT_DIR/AndroidManifest.xml \
    $BUILD_DIR/res.zip \
    -o $BUILD_DIR/base.apk \
    --java $BUILD_DIR \
    --auto-add-overlay

# Step 3: Compile Java source
echo "Compiling Java..."
mkdir -p $BUILD_DIR/classes
javac \
    -source 1.8 -target 1.8 \
    -parameters \
    -bootclasspath $PLATFORM/android.jar \
    -d $BUILD_DIR/classes \
    $PROJECT_DIR/src/com/example/cryptoqr/MainActivity.java \
    $BUILD_DIR/com/example/cryptoqr/R.java

# Step 4: Convert class files to DEX
echo "Converting to DEX..."
$D8 \
    --release \
    --min-api 21 \
    --lib $PLATFORM/android.jar \
    --output $BUILD_DIR \
    $BUILD_DIR/classes/com/example/cryptoqr/*.class

# Step 5: Add DEX and assets to base APK
echo "Packaging APK..."
cd $BUILD_DIR
unzip -q -o base.apk -d apk_extracted
mv classes.dex apk_extracted/
cp -r $ASSETS_DIR apk_extracted/

# Re-zip with store compression for APK compatibility
rm -f $OUT_DIR/app-unsigned.apk
cd apk_extracted
zip -q -r -0 $OUT_DIR/app-unsigned.apk .

# Step 6: Zipalign
echo "Zipaligning..."
$ZIPALIGN -f -v 4 $OUT_DIR/app-unsigned.apk $OUT_DIR/app-aligned.apk

# Step 7: Generate debug key if not exists
KEYSTORE=$PROJECT_DIR/debug.keystore
if [ ! -f "$KEYSTORE" ]; then
    echo "Generating debug keystore..."
    keytool -genkey -v \
        -keystore $KEYSTORE \
        -alias androiddebugkey \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass android -keypass android \
        -dname "CN=Android Debug,O=Android,C=US"
fi

# Step 8: Sign APK
echo "Signing APK..."
$APKSIGNER sign \
    --ks $KEYSTORE \
    --ks-pass pass:android \
    --key-pass pass:android \
    --in $OUT_DIR/app-aligned.apk \
    --out $OUT_DIR/crypto-qr.apk

echo "APK built successfully: $OUT_DIR/crypto-qr.apk"
ls -lh $OUT_DIR/crypto-qr.apk
