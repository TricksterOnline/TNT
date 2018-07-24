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

How to compile and/or package TNT
----------------------------------

Assuming you have Java installed, all you have to do to compile TNT is to 
access the `src` directory from the command prompt or terminal.

Then run the following command:
```bash
javac *.java
```

If you want the program the to be a little more mobile, you can package TNT 
into a `.jar` file.

To do this you need to compile TNT, then run this command in the same directory:
```bash
jar cfe TNT.jar TNT *.class
```

Now you can copy & paste TNT.jar anywhere you like and use it from there.

To find out the available commands for TNT, you can run it from compile in `src`
like so:
```bash
java TNT mode /path/to/file.nri
```

Or with the `.jar` file anywhere:
```bash
java -jar TNT.jar mode /path/to/file.nri
```
