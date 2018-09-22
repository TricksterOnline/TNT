/*
Analyzer.java: this file is part of the TNT program.

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
import static java.lang.System.in;
import static java.lang.System.out;
import static java.lang.System.err;
/**
Class Description:
The Analyzer class contains all functions that TNT uses to find the information
that it needs to know about a NORI file in order to perform its duties.

Dev Notes:
It is assumed that you have read the NORI Format Specification first, so most of
the variables and variable assignments are not commented. Descriptive names are
used for most things regardless. Fair warning has been given.

Development Priority: HIGH
*/
public class Analyzer
{
    // class variables
    public static int total=0, length=0, pos=0, rem=0;
    // special NORI file variables
    public static int fsig = 0;
    public static int noriVer = 0;
    public static int anims = 0;
    public static int withoutGawi = 0;
    public static int fsize = 0;
    public static int xtraFrameBytes = 0;
    // special GAWI variables
    public static int gsig = 0;
    public static int gawiVer = 0;
    public static int bmpBitDepth = 0;
    public static boolean compressed = false;
    public static boolean hasPalette = false;
    public static int numBMP = 0;
    public static int gsize = 0;
    // special palette variables
    public static int psig = 0;
    public static int palVer = 0;
    public static int psize = 0;
    public static byte[][] palette;
    // special BMP data variables
    public static int[] bmpOffsets;

    // constructor for Analyzer class
    public Analyzer(ByteBuffer bb, String filename)
    {
        try
        {
            out.println("Filename: "+filename);
            // Read and Assign info about the noriFile
            setNoriHeader(bb);
            setGawiData(bb);
        }
        catch (Exception ex)
        {
            out.println("Something donked up (AM):\n"+ex);
        }
    }

    public static void setNoriHeader(ByteBuffer bb)
    {
        fsig = bb.getInt();
        noriCheck(fsig);
        noriVer = bb.getInt();
        noriVerCheck(noriVer);
        bb.position(28);// jump over unidentified data
        anims = bb.getInt();
        out.println("# of animations: "+anims);
        withoutGawi = bb.getInt();
        out.println("fsize w/o gawi: "+withoutGawi);
        fsize = bb.getInt();
        out.println("fsize: "+fsize);
        //bbStatus(bb);
    }

    public static void setGawiData(ByteBuffer bb)
    {
        out.println();
        gsig = bb.getInt();
        gawiCheck(gsig);
        gawiVer = bb.getInt();
        gawiVerCheck(gawiVer);
        bmpBitDepth = bb.getInt();
        out.println("bmpBitDepth: "+bmpBitDepth);
        compressed = bool(bb.getInt());
        out.println("Compressed: "+compressed);
        hasPalette = bool(bb.getInt());
        out.println("hasPalette: "+hasPalette);
        bb.position(76);// jump over unidentified data
        numBMP = bb.getInt();
        out.println("# of images: "+numBMP);
        gsize = bb.getInt();
        out.println("gsize: "+gsize);
        //bbStatus(bb);

        // Palette Section (if exists)
        if(hasPalette) setPaletteData(bb);

        // Offsets
        setBmpOffsets(bb,numBMP);
        //bbStatus(bb);
    }

    public static void setPaletteData(ByteBuffer bb)
    {
        out.println();
        psig = bb.getInt();
        palCheck(psig);
        palVer = bb.getInt();
        palVerCheck(palVer);
        bb.position(112);// jump over unidentified data
        psize = bb.getInt();
        out.println("psize: "+psize);
        palette = setPalette(bb,psize-40);
        int num1 = bb.getInt();
        int num2 = bb.getInt();
        //bbStatus(bb);
    }

    // Make BMP color palette from raw palette data. Okay, one of the harder to
    // follow parts here. Colors are stored in BGR order. Take it in stride.
    public static byte[][] setPalette(ByteBuffer bb, int size)
    {
        byte[] palBytes = new byte[size];
        int numColors = size/3;
        byte[][] colors = new byte[numColors][3];
        try
        {
            // gets/puts the palette bytes into the palBytes array
            bb.get(palBytes,0,size);
            // Place the bytes in the dual array 'colors' that groups the rgb
            // bytes according to the color/palette index they represent
            for(int i = 0; i < numColors; i++)
            {
                int x = i*3, b=x+0, g=x+1, r=x+2;
                colors[i][0] = palBytes[r];
                colors[i][1] = palBytes[g];
                colors[i][2] = palBytes[b];
                // I hate the neon green bg, so lets switch that with the pink
                if(colors[i][0]==(byte)0x00 && colors[i][1]==(byte)0xFF &&
                   colors[i][2]==(byte)0x00 || colors[i][0]==(byte)0x15 &&
                   colors[i][1]==(byte)0xFF && colors[i][2]==(byte)0x00)
                {
                    colors[i][0]=(byte)0xFF;
                    colors[i][1]=(byte)0x00;
                    colors[i][2]=(byte)0xFF;
                }
            }
        }
        catch (Exception ex)
        {
            out.println("Something donked up (SP):\n"+ex);
        }
        return colors;
    }

    // Load bmp offsets into the bmpOffsets array for global use
    public static void setBmpOffsets(ByteBuffer bb, int bmpCount)
    {
        out.println("\nSetting BMP offsets.....");
        bmpOffsets = new int[bmpCount+1];
        for(int i=0; i < bmpCount; i++)
        {
            bmpOffsets[i] = bb.getInt();
            if(compressed) bmpOffsets[i] += i*28;
        }
        // get buffer position at end of offsets
        pos = bb.position();
        // change offsets to represent [from start of file] instead of
        // [from start of bmp section]
        for(int i=0; i < bmpCount; i++)
        {
            bmpOffsets[i] = bmpOffsets[i] + pos;
        }
    }

    public static void noriCheck(int signature)
    {
        out.print("NORI Signature Check: ");
        intCheck(1230131022, signature);
    }

    // Checks NORI version and sets the appropriate # of extra bytes
    public static void noriVerCheck(int verNum)
    {
        out.print("NORI Version: ");
        switch(verNum)
        {
        case 300:
            xtraFrameBytes = 224;
            break;
        case 301:
            xtraFrameBytes = 228;
            break;
        case 302:
            xtraFrameBytes = 348;
            break;
        case 303:
            xtraFrameBytes = 352;
            break;
        default:
            out.println("Unknown type! File a bug report.");
            System.exit(1);
            break;
        }
        out.println(verNum);
    }

    public static void gawiCheck(int signature)
    {
        out.print("GAWI Signature Check: ");
        intCheck(1230455111, signature);
    }

    public static void gawiVerCheck(int verNum)
    {
        out.print("GAWI Version: ");
        intCheck(300,verNum);
    }

    public static void palCheck(int signature)
    {
        out.print("PAL_ Signature Check: ");
        intCheck(1598832976, signature);
    }

    public static void palVerCheck(int verNum)
    {
        out.print("PAL_ Version: ");
        intCheck(100,verNum);
    }

    // Reusable int check, b/c we do this often
    public static void intCheck(int ref, int input)
    {
        if(input == ref)
        {
            out.println("Passed.");
        }
        else
        {
            out.println("Failed!");
            System.exit(1);
        }
    }

    // (Dbg) Prints the buffer's current status info
    public static void bbStatus(ByteBuffer bb)
    {
        rem = bb.remaining();
        pos = bb.position();
        out.println("\nRemaining bytes: "+rem);
        out.println("position: "+pos);
        out.println("========================================");
    }

    // Bool converter for binary flags (b/c Java is inept)
    public static boolean bool(int binary)
    {
        return binary ==1;
    }
}
