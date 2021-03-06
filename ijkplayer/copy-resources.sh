#!/bin/sh

VERSION=0.4.4.5-SNAPSHOT
OUTPUT=release
ARCHES="arm64 armv7a x86 java exo"

if [ "$ANDROID_SDK" = "" ] ; then
    echo "Set ANDROID_SDK to be the location of your Sdk, USING DEFAULT"
    export ANDROID_SDK=/home/sls/Android/Sdk/
fi


echo "BUILDING NATIVES..."
cd ijkplayer/android/
./compile-ijk.sh all
cd -

export ANDROID_HOME=$ANDROID_SDK

echo "PACKAGING..."
cd ijkplayer/android/ijkplayer/
cp build.gradle build.gradle.orig
cat build.gradle.orig | sed 's/23.0.0/23.0.1/g' > build.gradle
cp build.gradle build.gradle.orig
cat build.gradle.orig | sed "s/.*versionName.*/    versionName = \"${VERSION}\"/g" > build.gradle
./gradlew assemble
cd -

echo "COPYING..."
for ARCH in $ARCHES; do
   echo "COPY ARCH: $ARCH"
   mkdir -p ../android/mavenlocal/sagetv/ijkplayer/ijkplayer-${ARCH}/${VERSION}/
   cp -fv ijkplayer/android/ijkplayer/ijkplayer-${ARCH}/build/outputs/aar/ijkplayer-${ARCH}-${OUTPUT}.aar ../android/mavenlocal/sagetv/ijkplayer/ijkplayer-${ARCH}/${VERSION}/ijkplayer-${ARCH}-${VERSION}.aar
done

#echo "REMOVING CACHES..."
#find ~/.gradle/caches/ -iname 'ijk*' -exec rm -rfv {} \;


