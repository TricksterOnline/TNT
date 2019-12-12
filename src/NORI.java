/*
NORI.java: this file is part of the TNT program.

Copyright (C) 2014-2020 Libre Trickster Team

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
import static java.lang.System.out;
import static java.lang.System.getProperty;
/**
Class Description:
A structure-like class for storing a NORI file's data neatly.

Dev Notes:
Since this class is essentially a structure, I'm going to attempt to keep it as
simple and clean as possible.

Development Priority: HIGH
*/
public class NORI
{
    // class variables
    public static File nf;
    public static String name,dname,dir,exdir;
    // special NORI variables
    public static int fsig = 0;
    public static int noriVer = 0;
    public static int nParam1 = 0;
    public static int nParam2 = 0;
    public static int nParam3 = 0;
    public static int nParam4 = 0;
    public static int nParam5 = 0;
    public static int anims = 0;
    public static int woGawi = 0;
    public static int fsize = 0;
    // special GAWI variables
    public static int gsig = 0;
    public static int gawiVer = 0;
    public static int bpp = 0;
    public static int compressed = 0;
    public static int hasPalette = 0;
    public static int gParam4 = 0;
    public static int gParam5 = 0;
    public static int gParam6 = 0;
    public static int gParam7 = 0;
    public static int numBMP = 0;
    public static int gsize = 0;
    // special palette variables
    public static int psig = 0;
    public static int palVer = 0;
    public static int pParam1 = 0;
    public static int pParam2 = 0;
    public static int pParam3 = 0;
    public static int pParam4 = 0;
    public static int divided = 0;
    public static int psize = 0;
    public static byte[] palBytes;
    public static byte[][] palette;
    public static int mainS = 111;
    public static int mainE = 254;
    // special BMP data variables
    public static int[] bmpOffsets;
    public static int bpos = 0;
    public static int[][] bmpSpecs;
    public static byte[] bmpData;
    // special animation variables
    public static int[] animOffsets;
    public static int apos = 0;
    public static String[] animName;
    public static int[] frames;
    public static int[][] frameOffsets;
    public static int[][][] frameData;
    public static int[][][][] planeData;
    public static int xtraFrameBytes = 0;
    public static byte[] xfb;

    // constructor for NORI class
    public NORI() {}

    public static void setNFileVars(File nf, int src)
    {
        name = nf.getName();// Plain file name
        // Using cfg file, which is the same name & dir, just 4 extra characters
        if(src!=0) name = name.substring(0,name.length()-4);
        if(!(name.endsWith(".bac"))||!(name.endsWith(".nri"))) 
        {
            out.println("Error: Config file named incorrectly!");
            System.exit(1);
        }
        dname = name.replace('.','_');// Name without dots (useful)
        dir = nf.getParent()+File.separator;// Directory the file is in
        exdir = dir+dname+File.separator;// Extraction directory (where bmp go)
    }

    public static void checkDir()
    {
        String badDir = "null"+File.separator;
        if(dir.equals(badDir)) dir = getProperty("user.dir")+File.separator;
        exdir = dir+dname+File.separator;
    }
}
