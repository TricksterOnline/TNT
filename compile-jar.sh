#!/bin/bash
src="TNT/src"
rm -f TNT.jar
cd ..
jar cf $src/TNT.jar $src/*.java TNT/docs TNT/LICENSE TNT/VERSION TNT/README.md
cd $src
javac *.java
jar ufe TNT.jar Main *.class
cd ..
rm src/*.class
mv src/TNT.jar TNT.jar
chmod 705 TNT.jar
