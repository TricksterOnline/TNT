/*
Analyze.java: this file is part of the TNT program.

Copyright (C) 2014-2018 Libre Trickster Team

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
*/
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import static java.lang.System.in;
import static java.lang.System.out;
import static java.lang.System.err;
/**
Class Description:
The Analyze class contains the functions which call the Analyzer class. This
class provides a standalone interface for the Analyzer functionality.

Dev Notes:
This class is a stickman construct. I hope to find a better way to include a 
standalone Analyze option inside Main.java, but for the sake of time this will 
have to do for now. I will admit that my limited understanding of Java and
usage of bytebuffers across classes is the cause of this file's existence.

Development Priority: HIGH
*/
public class Analyze
{
    // class variables

    // constructor for Analyze class
    public Analyze(byte[] ba, File file)
    {
        try
        {
            String name = file.getName();
            // Wraps byte array in litte-endian bytebuffer
            ByteBuffer fbb = ByteBuffer.wrap(ba).order(ByteOrder.LITTLE_ENDIAN);

            // Analyze the file
            Analyzer a = new Analyzer(fbb,name);
        }
        catch (Exception ex)
        {
            out.println("Something donked up (OptA):\n"+ex);
        }
    }
}
