#!/bin/bash

originalAarName=LibBlinkInput.aar
libName=blinkinput
libVersion=$(grep --max-count=1 '<version>' pom.xml | cut -d ">" -f 2 | cut -d '<' -f 1)
echo $libName version: $libVersion

tmpDir=./tmp-$libName
tmpDest=$tmpDir/com/microblink/$libName/$libVersion

mkdir -p $tmpDest
cp $originalAarName $tmpDest/$libName-$libVersion.aar

# Create destination pom file and add SCM section after description line
sed '/<description>.*<\/description>/a\
  <scm>\
       <url>https://github.com/blinkinput/blinkinput-android</url>\
       <connection>scm:git:https://github.com/blinkinput/blinkinput-android.git</connection>\
       <developerConnection>scm:git:https://github.com/blinkinput/blinkinput-android.git</developerConnection>\
  </scm>\
' pom.xml > $tmpDest/$libName-$libVersion.pom



pushd $tmpDest > /dev/null
md5 -q $libName-$libVersion.pom > $libName-$libVersion.pom.md5
md5 -q $libName-$libVersion.aar > $libName-$libVersion.aar.md5
shasum $libName-$libVersion.pom | cut -d ' ' -f 1 > $libName-$libVersion.pom.sha1
shasum $libName-$libVersion.aar | cut -d ' ' -f 1 > $libName-$libVersion.aar.sha1
gpg -ab $libName-$libVersion.pom
gpg -ab $libName-$libVersion.aar
popd > /dev/null

(cd $tmpDir && zip -r ../$libName-$libVersion-maven-bundle.zip .)
rm -r $tmpDir