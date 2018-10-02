The NORI Tool (TNT)
===================

A program designed to Extract and possibly create NORI
files for the Libre Trickster project.

Test it on any .nri or .bac file you want. If it isn't extracted correctly, 
open a GitHub issue immediately and it will get fixed.

<pre>
Please avoid forking this repo unless you plan to make pull request.
Download the repo or a release if you want a local copy.
Non-updated forks are annoying.
</pre>

------------------------------------

How to compile and package TNT
----------------------------------

Assuming you have [Java JDK](http://jdk.java.net) installed, all you have to do
to compile TNT is to access the `src` folder from the command prompt or terminal

Then run the following command:
```bash
javac *.java
```

Then to package TNT into a `.jar` file, run this command in the same directory:
```bash
jar cfe TNT.jar Main *.class
```

Now you can copy & paste TNT.jar anywhere you like and use it from there.

To use TNT or find out the available commands for it, you can run it like so:
```bash
java -jar TNT.jar mode /path/to/file.nri
```
