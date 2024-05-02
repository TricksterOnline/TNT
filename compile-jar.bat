@ECHO OFF
SET "ReadMe=TNT\README.md"
ATTRIB -r TNT.jar
DEL TNT.jar
CD ..
jar cf TNT\src\TNT.jar TNT\src\*.java TNT\docs TNT\LICENSE TNT\VERSION %ReadMe%
CD TNT\src
javac *.java
jar ufe TNT.jar Main *.class
CD ..
DEL src\*.class
MOVE src\TNT.jar TNT.jar
ATTRIB +r TNT.jar
