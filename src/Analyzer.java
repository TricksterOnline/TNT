/*
Analyzer.java: this file is part of the TNT program.

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
import java.nio.*;
import java.nio.file.*;
import java.util.*;
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
    public static int pos=0,rem=0,bpos=0,apos=0,fpos=0,bmpNxt=0,animNxt=0;
    // special GAWI variables
    public static int gStart=0,gEnd=0;
    public static boolean compressed=false, hasPalette=false;
    // special BMP data variables
    public static int[] bmpOffsets;
    // special animation variables
    public static int asize=0, numFrames=0, numPlanes=0;
    public static int[] animOffsets;
    public static byte[] animName = new byte[32];
    public static int[][] frameOffsets;

    // constructor for Analyzer class
    public Analyzer(ByteBuffer bb, NORI nf)
    {
        out.println("========================================================");
        out.println("Filename: "+nf.name);
        try
        {
            // Read and Assign info about the noriFile
            setNoriHeader(bb,nf);
            setGawiHeader(bb,nf);
            if(hasPalette) setPaletteData(bb,nf);
            setBmpOffsets(bb,nf);
            dryExtract(bb,nf);// Skip through bmpData, assign bmpSpecs data
            if(nf.gsize==0) gawiSizeFixes(nf);
            prepAnimVars(nf);
            setAnimOffsets(bb,nf);
            setAnimInfo(bb,nf);
            offsetCheck(nf);
            //bbStatus(bb);// rem!=0 if noriVer is wrong (ex: Mini_mapd01a.nri)
            // Reset bytebuffer for extraction
            bb.position(bpos);
        }
        catch(Exception ex)
        {
            out.println("Error in (AM):\n"+ex);
        }
    }

    private static void setNoriHeader(ByteBuffer bb, NORI nf)
    {
        nf.fsig = bb.getInt();
        noriCheck(nf.fsig);
        nf.noriVer = bb.getInt();
        noriVerCheck(nf);
        nf.nParam1 = bb.getInt();
        nf.nParam2 = bb.getInt();
        nf.nParam3 = bb.getInt();
        nf.nParam4 = bb.getInt();
        nf.nParam5 = bb.getInt();
        nf.anims = bb.getInt();
        out.println("# of animations: "+nf.anims);
        nf.woGawi = bb.getInt();
        out.println("fsize w/o GAWI: "+nf.woGawi);
        nf.fsize = bb.getInt();
        out.println("fsize: "+nf.fsize);
        if(nf.fsize==0) nf.fsize = bb.capacity();// Fix Ntree*'s mistake
        out.println();
    }

    private static void setGawiHeader(ByteBuffer bb, NORI nf)
    {
        gStart = bb.position();
        nf.gsig = bb.getInt();
        gawiCheck(nf.gsig);
        nf.gawiVer = bb.getInt();
        gawiVerCheck(nf.gawiVer);
        nf.bpp = bb.getInt();
        out.println("BitsPerPixel: "+nf.bpp);
        nf.compressed = bb.getInt();
        compressed = (nf.compressed==1);
        out.println("Compressed: "+compressed);
        nf.hasPalette = bb.getInt();
        hasPalette = (nf.hasPalette==1);
        out.println("hasPalette: "+hasPalette);
        nf.gParam4 = bb.getInt();
        nf.gParam5 = bb.getInt();
        nf.gParam6 = bb.getInt();
        nf.gParam7 = bb.getInt();
        nf.numBMP = bb.getInt();
        out.println("# of images: "+nf.numBMP);
        nf.gsize = bb.getInt();
        out.println("gsize: "+nf.gsize);
        out.println();
    }

    private static void setPaletteData(ByteBuffer bb, NORI nf)
    {
        nf.psig = bb.getInt();
        palCheck(nf.psig);
        nf.palVer = bb.getInt();
        palVerCheck(nf.palVer);
        nf.pParam1 = bb.getInt();
        nf.pParam2 = bb.getInt();
        nf.pParam3 = bb.getInt();
        nf.pParam4 = bb.getInt();
        nf.divided = bb.getInt();
        nf.psize = bb.getInt();
        out.println("psize: "+nf.psize);
        nf.palette = setPalette(bb,nf);
        if(nf.psize==808)
        {
            nf.mainS = bb.getInt();
            nf.mainE = bb.getInt();
        }
        out.println();
    }

    // Make BMP color palette from raw palette data. Okay, one of the harder to
    // follow parts here. Colors are stored in BGR order. Take it in stride.
    private static byte[][] setPalette(ByteBuffer bb, NORI nf)
    {
        nf.palBytes = new byte[768];
        byte[] newBG = {(byte)255,(byte)0,(byte)255};
        byte[][] colors = new byte[256][3];
        try
        {
            // gets/puts the palette bytes into the palBytes array
            bb.get(nf.palBytes,0,768);
            ByteBuffer pbb = mkLEBB(nf.palBytes);
            // standardize the bg to neon pink
            pbb.put(newBG,0,3);
            // Place the bytes in the dual array 'colors' that groups the rgb
            // bytes according to the color/palette index they represent
            for(int i = 0; i < 256; i++)
            {
                int x = i*3, b=x+0, g=x+1, r=x+2;
                colors[i][0] = nf.palBytes[r];
                colors[i][1] = nf.palBytes[g];
                colors[i][2] = nf.palBytes[b];
            }
        }
        catch(Exception ex)
        {
            out.println("Error in (setPal):\n"+ex);
        }
        return colors;
    }

    // Load bmp offsets into the bmpOffsets array for global use
    private static void setBmpOffsets(ByteBuffer bb, NORI nf)
    {
        bmpOffsets = new int[nf.numBMP+1];
        nf.bmpOffsets = new int[nf.numBMP+1];
        for(int i=0; i < nf.numBMP; i++)
        {
            nf.bmpOffsets[i] = bb.getInt();
            if(compressed) nf.bmpOffsets[i] += i*28;
        }
        // get buffer position at end of offsets
        bpos = bb.position();
        nf.bpos = bpos;
    }

    // Load the bmpSpecs array and simulate extraction for the bytebuffer
    private static void dryExtract(ByteBuffer bb, NORI nf)
    {
        JBL bl = new JBL();
        nf.bmpSpecs = new int[nf.numBMP][7];
        int offsetDiff = nf.bmpOffsets[nf.numBMP-1]-nf.bmpOffsets[0];
        boolean offDiff = (offsetDiff > 0);
        for(int i=0; i < nf.numBMP; i++)
        {
            bmpOffsets[i] = bb.position() - bpos;// Set offsetCheck() value
            if(nf.bmpOffsets[i+1]!=0)
                bmpNxt=nf.bmpOffsets[i+1] + bpos;
            else
                bmpNxt=nf.bmpOffsets[i+1];
            nf.bmpSpecs[i][0] = bb.getInt();
            nf.bmpSpecs[i][1] = bb.getInt();
            nf.bmpSpecs[i][2] = bb.getInt();
            nf.bmpSpecs[i][3] = bb.getInt();
            nf.bmpSpecs[i][4] = bb.getInt();
            nf.bmpSpecs[i][5] = bb.getInt();
            nf.bmpSpecs[i][6] = bb.getInt();
            bl.setBmpVars(nf.bmpSpecs[i][2],nf.bmpSpecs[i][3],nf.bpp);
            byte[] rawBytes = bl.getImgBytes(bb,nf.bmpSpecs[i][1]);
            // Ensure the buffer is in the right position for the next bmp
            if(offDiff && bpos!=bmpNxt && bmpNxt!=0) bb.position(bmpNxt);
        }
        gEnd = bb.position();
        asize = bb.remaining();
    }

    // One of many data fixes I've implemented to prevent Ntree* mistakes from
    // being carried over to the config files. This fixes woGawi and gsize.
    private static void gawiSizeFixes(NORI nf)
    {
        nf.gsize = gEnd - gStart;
        if(nf.woGawi==0) nf.woGawi = 40 + asize;
    }

    // Prepare the animation-related arrays
    private static void prepAnimVars(NORI nf)
    {
        // large array sizes need for dealing with unknown input
        animOffsets = new int[nf.anims+1];
        nf.animOffsets = new int[nf.anims+1];
        nf.animName = new String[nf.anims];
        nf.frames = new int[nf.anims];
        frameOffsets = new int[nf.anims][200];
        nf.frameOffsets = new int[nf.anims][200];
        nf.frameData = new int[nf.anims][200][2];
        nf.planeData = new int[nf.anims][200][100][7];
        nf.xfb = new byte[nf.xtraFrameBytes];
    }

    private static void setAnimOffsets(ByteBuffer bb, NORI nf)
    {
        for(int i=0; i < nf.anims; i++)
        {
            nf.animOffsets[i] = bb.getInt();
        }
        apos = bb.position();
        nf.apos = apos;
    }

    // Set the info for all the animations
    private static void setAnimInfo(ByteBuffer bb, NORI nf)
    {
        try
        {
            int offsetDiff = nf.animOffsets[nf.anims-1] - nf.animOffsets[0];
            boolean offDiff = (offsetDiff > 0);
            for(int i=0; i < nf.anims; i++)
            {
                animOffsets[i] = bb.position() - apos;// Set offsetCheck() value
                if(nf.animOffsets[i+1]!=0)
                    animNxt=nf.animOffsets[i+1] + apos;
                else
                    animNxt=nf.animOffsets[i+1];
                bb.get(animName,0,32);
                nf.animName[i] = (new String(animName,"EUC-KR")).trim();
                nf.frames[i] = bb.getInt();
                numFrames = nf.frames[i];
                setFrameOffsets(bb,i,nf);
                setFrameData(bb,i,nf);
                pos = bb.position();
                if(offDiff && pos!=animNxt && animNxt!=0) bb.position(animNxt);
            }
        }
        catch(Exception ex)
        {
            out.println("Error in (setAnimInfo):\n"+ex);
        }
    }

    private static void setFrameOffsets(ByteBuffer bb, int a, NORI nf)
    {
        for(int i=0; i < numFrames; i++)
        {
            nf.frameOffsets[a][i] = bb.getInt();
        }
        fpos = bb.position();
    }

    // Set actual data for frames (and planes)
    private static void setFrameData(ByteBuffer bb, int a, NORI nf)
    {
        for(int i=0; i < numFrames; i++)
        {
            frameOffsets[a][i] = bb.position() - fpos;// Set offsetCheck() value
            nf.frameData[a][i][0] = bb.getInt();
            nf.frameData[a][i][1] = bb.getInt();
            numPlanes = nf.frameData[a][i][1];
            setPlaneData(bb,a,i,nf);
        }
    }

    private static void setPlaneData(ByteBuffer bb, int a, int f, NORI nf)
    {
        for(int i=0; i < numPlanes; i++)
        {
            nf.planeData[a][f][i][0] = bb.getInt();
            nf.planeData[a][f][i][1] = bb.getInt();
            nf.planeData[a][f][i][2] = bb.getInt();
            nf.planeData[a][f][i][3] = bb.getInt();
            nf.planeData[a][f][i][4] = bb.getInt();
            nf.planeData[a][f][i][5] = bb.getInt();
            nf.planeData[a][f][i][6] = bb.getInt();
        }
        bb.get(nf.xfb,0,nf.xtraFrameBytes);
    }

    // Check if nf offset arrays = local arrays, fix nf arrays if not equal
    private static void offsetCheck(NORI nf)
    {
        if(nf.bmpOffsets!=bmpOffsets) nf.bmpOffsets = bmpOffsets;
        if(nf.animOffsets!=animOffsets) nf.animOffsets = animOffsets;
        if(nf.frameOffsets!=frameOffsets) nf.frameOffsets = frameOffsets;
    }

    private static void noriCheck(int signature)
    {
        out.print("NORI Signature Check: ");
        intCheck(1230131022, signature);
    }

    // Checks NORI version and sets the appropriate # of extra bytes
    private static void noriVerCheck(NORI nf)
    {
        out.print("NORI Version: ");
        switch(nf.noriVer)
        {
        case 300:
            nf.xtraFrameBytes = 224;
            break;
        case 301:
            nf.xtraFrameBytes = 228;
            break;
        case 302:
            nf.xtraFrameBytes = 348;
            break;
        case 303:
            nf.xtraFrameBytes = 352;
            break;
        default:
            out.println("Unknown type! File a bug report.");
            System.exit(1);
            break;
        }
        out.println(nf.noriVer);
    }

    private static void gawiCheck(int signature)
    {
        out.print("GAWI Signature Check: ");
        intCheck(1230455111, signature);
    }

    private static void gawiVerCheck(int verNum)
    {
        out.print("GAWI Version: ");
        intCheck(300,verNum);
    }

    private static void palCheck(int signature)
    {
        out.print("PAL_ Signature Check: ");
        intCheck(1598832976, signature);
    }

    private static void palVerCheck(int verNum)
    {
        out.print("PAL_ Version: ");
        intCheck(100,verNum);
    }

    // Reusable int check, b/c we do this often
    private static void intCheck(int ref, int input)
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
    private static void bbStatus(ByteBuffer bb)
    {
        rem = bb.remaining();
        pos = bb.position();
        out.println("\nRemaining bytes: "+rem);
        out.println("position: "+pos);
        out.println("========================================");
    }

    // Shorthand function to wrap a byte array in a little-endian bytebuffer
    private static ByteBuffer mkLEBB(byte[] ba)
    {
        // this long syntax call is why this function exists ;)
        return ByteBuffer.wrap(ba).order(ByteOrder.LITTLE_ENDIAN);
    }
}
