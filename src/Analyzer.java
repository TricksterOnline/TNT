/*
Analyzer.java: this file is part of the TNT program.

Copyright (C) 2014-2024 Libre Trickster Team

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
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import static java.lang.System.out;
/**
Class Description:
The Analyzer class contains all functions that TNT uses to find the information
that it needs to know about a NORI file in order to perform its duties.

Dev Notes:
It is assumed that you have read the NORI Format Specification first, so most of
the variables and variable assignments are not commented. Descriptive names are
used for most things regardless. Fair warning has been given.

Development Priority: HIGHEST
*/
public class Analyzer
{
// class variables
private static NORI nf;
private static ByteBuffer bb;
static Charset UTF8=StandardCharsets.UTF_8,EUC_KR=Charset.forName("EUC-KR");
private static int pos,rem,animNxt;
static String bxc ="[^\u0020-\uD7FF\uE000-\uFFFD\ud800\udbff-\udc00\udfff]";
private static String badXmlChars = bxc;
// OffsetCheck arrays
private static int[] bmpOffsets,animOffsets;
private static int[][] frameOffsets;
// Special animation variables
private static int numFrames,fpos,numPlanes,subtractNum,areaSize;

// constructor for Analyzer class
public Analyzer(ByteBuffer BB, NORI NF, boolean extract_mode)
{
    // Save coding space by making BB and NF static global vars
    bb = BB;
    nf = NF;
    // Start Analyzer output
    out.println("Filename: "+nf.name);
    try
    {
        // Read and Assign info about the noriFile
        setNoriHeaderData();
        setGawiHeaderData();
        if(nf.hasPalette==1) setPaletteData();
        setBmpOffsets();
        if(!extract_mode)
        {
            dryExtract();// Skip through bmpData, assign bmpSpecs data
            prepAnimVars();
            setAnimOffsets();
            // Set Animation Data
            for(int a=0; a < nf.anims; a++)
            {
                setAnimData(a);
            }
        }
    }
    catch(Exception ex)
    {
        out.println("Error in (AM):");
        ex.printStackTrace(System.out);
    }
}

private static void setNoriHeaderData()
{
    nf.fsig = bb.getInt();
    noriCheck();
    nf.noriVer = bb.getInt();
    noriVerCheck();
    nf.nParam1 = bb.getInt();
    nf.nParam2 = bb.getInt();
    nf.nParam3 = bb.getInt();
    nf.nParam4 = bb.getInt();
    nf.nParam5 = bb.getInt();
    nf.anims   = bb.getInt();
    out.println("# of animations: "+nf.anims);
    nf.woGawi  = bb.getInt();
    out.println("File size w/o GAWI: "+nf.woGawi);
    nf.fsize   = bb.getInt();
    out.println("File size: "+nf.fsize);
    // Fix fsize here, even if not needed, b/c it's the best place to do so
    nf.fsize   = bb.capacity();
    out.println();
}

private static void setGawiHeaderData()
{
    nf.gsig = bb.getInt();
    gawiCheck();
    nf.gawiVer = bb.getInt();
    gawiVerCheck();
    nf.bpp = bb.getInt();
    nf.Bpp = nf.bpp/8;
    out.println("BitsPerPixel: "+nf.bpp);
    nf.compressed = bb.getInt();
    out.println("Compressed: "+(nf.compressed==1));
    nf.hasPalette = bb.getInt();
    out.println("hasPalette: "+(nf.hasPalette==1));
    nf.gParam4 = bb.getInt();
    nf.gParam5 = bb.getInt();
    nf.gParam6 = bb.getInt();
    nf.gParam7 = bb.getInt();
    nf.bmpStructs  = bb.getInt();
    out.println("# of BMP Structures: "+nf.bmpStructs);
    nf.nLen  = String.valueOf(nf.bmpStructs).length();
    nf.gsize = bb.getInt();
    out.println("gsize: "+nf.gsize);
    out.println();
}

private static void setPaletteData() throws Exception
{
    nf.psig = bb.getInt();
    palCheck();
    nf.palVer = bb.getInt();
    palVerCheck();
    nf.pParam1 = bb.getInt();
    nf.pParam2 = bb.getInt();
    nf.pParam3 = bb.getInt();
    nf.pParam4 = bb.getInt();
    nf.divided = bb.getInt();
    nf.psize = bb.getInt();
    out.println("psize: "+nf.psize);
    nf.palette = setPalette();
    if(nf.psize==808)
    {
        nf.mainS = bb.getInt();
        nf.mainE = bb.getInt();
    }
    out.println();
}

// Make BMP color palette from raw palette data. Okay, one of the harder to
// follow parts here. Colors are stored in BGR order. Take it in stride.
private static byte[][] setPalette() throws Exception
{
    nf.palBytes = new byte[768];
    byte[][] colors = new byte[256][3];
    // gets/puts the palette bytes into the pb array
    bb.get(nf.palBytes,0,768);
    // standardize the bg to neon pink
    nf.palBytes[0] = (byte)255;
    nf.palBytes[1] = (byte)0;
    nf.palBytes[2] = (byte)255;
    // Place the bytes in the dual array 'colors' that groups the rgb
    // bytes according to the color/palette index they represent
    for(int i=0; i < 256; i++)
    {
        int x=i*3, b=x+0, g=x+1, r=x+2;
        colors[i][0] = nf.palBytes[r];
        colors[i][1] = nf.palBytes[g];
        colors[i][2] = nf.palBytes[b];
    }
    return colors;
}

// Load bmp offsets into the bmpOffsets array for global use
private static void setBmpOffsets()
{
    bmpOffsets = new int[nf.bmpStructs];
    nf.bmpOffsets = new int[nf.bmpStructs];
    switch(nf.compressed)
    {
    case 1:
        for(int i=0; i < nf.bmpStructs; i++)
        {
            nf.bmpOffsets[i] = bb.getInt();
            nf.bmpOffsets[i] += i*28;
        }
        break;
    default:
        for(int i=0; i < nf.bmpStructs; i++)
        {
            nf.bmpOffsets[i] = bb.getInt();
        }
        break;
    }
    // get buffer position at end of offsets
    nf.bpos = bb.position();
}

// Load the bmpCount & bmpSpecs array and simulate extraction for the bytebuffer
private static void dryExtract()
{
    nf.bmpCount = new int[nf.bmpStructs];
    for(int i=0,dataLength=0; i < nf.bmpStructs; i++)
    {
        bmpOffsets[i] = bb.position()-nf.bpos;// Set bmpOffsetCheck() value
        // Get & Set bmpCount data
        nf.bmpCount[i] = bb.getInt();
        for(int x=0; x < nf.bmpCount[i]; x++)
        {
            dataLength = bb.getInt();
            movePosFwd(20+dataLength);
        }
        nf.totalBMP += nf.bmpCount[i];// Update total BMP
    }
    bmpOffsetCheck();// Test the BMP offsets before we go any further (for dbg)
    bb.position(nf.bpos);
    nf.bmpSpecs = new int[nf.totalBMP][6];
    for(int i=0,specsIdx=0; i < nf.bmpStructs; i++)
    {
        // Get & Set BMP Specs
        if(nf.bmpCount[i]!=0) bb.getInt();// Skip bmpCount, it is already known
        for(int x=0; x < nf.bmpCount[i]; x++)
        {
            nf.bmpSpecs[specsIdx][0] = bb.getInt();//dataLength
            nf.bmpSpecs[specsIdx][1] = bb.getInt();//w
            nf.bmpSpecs[specsIdx][2] = bb.getInt();//h
            nf.bmpSpecs[specsIdx][3] = bb.getInt();//bParam4
            nf.bmpSpecs[specsIdx][4] = bb.getInt();//bmp_x
            nf.bmpSpecs[specsIdx][5] = bb.getInt();//bmp_y
            movePosFwd(nf.bmpSpecs[specsIdx][0]);
            specsIdx++;
        }
    }
    nf.asize = bb.remaining();
}

// Prepare the animation-related arrays
private static void prepAnimVars()
{
    // large array sizes needed for dealing with unknown input
    int frames = 220;//largest found:216 (pet_cm_387.nri)
    int planes = 120;//largest found:114 (map_sq07.bac)
    nf.animOffsets  = new int[nf.anims+1];
    nf.titleBytes   = new byte[nf.anims][32];//stored as an array for debugging
    nf.title        = new String[nf.anims];
    nf.numFrames    = new int[nf.anims];
    frameOffsets    = new int[nf.anims][frames];
    nf.frameOffsets = new int[nf.anims][frames];
    nf.frameDataTop = new int[nf.anims][frames][2];
    // planeData is the largest array, consuming approx 472MB for itm_cm_shop000
    nf.planeData    = new int[nf.anims][frames][planes][7];
    if(nf.notV300)
    {
        nf.numCoordSets = new int[nf.anims][frames];//record:14 (map_uw03.bac)
        nf.coordSets    = new int[nf.anims][frames][20][2];
    }
    if(nf.hasEB) nf.entryBlocks = new byte[nf.anims][frames][6][28];
    nf.unknownData1 = new byte[nf.anims][frames][2][22];
    nf.soundEffect  = new String[nf.anims][frames];
    nf.unknownData2 = new byte[nf.anims][frames][18];
    if(nf.maybeMCV)
    {
        nf.hasMCValues = new int[nf.anims][frames];
        nf.mcValues    = new int[nf.anims][frames][7];
        nf.mcParam7    = new String[nf.anims][frames];
        nf.mcParam8    = new byte[nf.anims][frames][20];
    }
}

private static void setAnimOffsets()
{
    for(int a=0; a < nf.anims; a++)
    {
        nf.animOffsets[a] = bb.getInt();
    }
    nf.apos = bb.position();
}

// Set the data for all the animations
private static void setAnimData(int a)
{
    if(nf.animOffsets[a+1]!=0)
        animNxt=nf.animOffsets[a+1]+nf.apos;
    else
        animNxt=nf.animOffsets[a+1];
    // Get & Set ANIM data
    bb.get(nf.titleBytes[a],0,32);
    nf.title[a] = newXmlStr(nf.titleBytes[a],EUC_KR);
    nf.numFrames[a] = bb.getInt();
    numFrames = nf.numFrames[a];
    //if(numFrames>220) out.printf("Ole!numFrames:%d @%d\n",numFrames,getPos());
    nf.totalFrames += numFrames;//Fixer var
    // Set Frame Offsets
    for(int f=0; f < numFrames; f++)
    {
        nf.frameOffsets[a][f] = bb.getInt();
    }
    fpos = bb.position();//End of frame offsets
    // Set Frame Data
    for(int f=0; f < numFrames; f++)
    {
        frameOffsets[a][f] = bb.position()-fpos;// Set frameOffsetCheck() value
        setFrameDataTop(a,f);
        setPlaneData(a,f);
        setFrameDataBottom(a,f);
    }
    frameOffsetCheck(a,numFrames,fpos);// Test the frameOffsets (for dbg)
    pos = bb.position();
    if(pos!=animNxt && animNxt!=0)
    {
        out.printf("Ole! Anim:%d @%d, animNxt:%d\n",a,pos,animNxt);
        bb.position(animNxt);
    }
}

private static void setFrameDataTop(int a, int f)
{
    nf.frameDataTop[a][f][0] = bb.getInt();//duration
    nf.frameDataTop[a][f][1] = bb.getInt();//numPlanes
    numPlanes = nf.frameDataTop[a][f][1];
    //if(numPlanes>120) out.printf("Ole!numPlanes:%d @%d\n",numPlanes,getPos());
    nf.totalPlanes += numPlanes;//Fixer var
}

private static void setPlaneData(int a, int f)
{
    for(int p=0; p < numPlanes; p++)
    {
        nf.planeData[a][f][p][0] = bb.getInt();//bmp_id
        nf.planeData[a][f][p][1] = bb.getInt();//plane_x
        nf.planeData[a][f][p][2] = bb.getInt();//plane_y
        nf.planeData[a][f][p][3] = bb.getInt();//opacity
        nf.planeData[a][f][p][4] = bb.getInt();//flip
        nf.planeData[a][f][p][5] = bb.getInt();//blend_mode
        nf.planeData[a][f][p][6] = bb.getInt();//flag_param
    }
}

private static void setFrameDataBottom(int a, int f)
{
    try
    {
        if(nf.notV300)
        {
            nf.numCoordSets[a][f] = bb.getInt();
            for(int i=0; i < nf.numCoordSets[a][f]; i++)
            {
                nf.coordSets[a][f][i][0] = bb.getInt();
                nf.coordSets[a][f][i][1] = bb.getInt();
            }
        }
        movePosFwd(nf.cdBlockSize);
        if(nf.hasEB)
        {
            bb.get(nf.entryBlocks[a][f][0]);
            bb.get(nf.entryBlocks[a][f][1]);
            bb.get(nf.entryBlocks[a][f][2]);
            bb.get(nf.entryBlocks[a][f][3]);
            bb.get(nf.entryBlocks[a][f][4]);
            bb.get(nf.entryBlocks[a][f][5]);
        }
        bb.get(nf.unknownData1[a][f][0]);
        bb.get(nf.unknownData1[a][f][1]);
        bb.get(nf.sfx);
        nf.soundEffect[a][f] = newXmlStr(nf.sfx,UTF8);
        bb.get(nf.unknownData2[a][f]);
        if(nf.maybeMCV)
        {
            nf.hasMCValues[a][f] = bb.getInt();
            if(nf.hasMCValues[a][f]==1)
            {
                nf.mcValues[a][f][0] = bb.getInt();
                nf.mcValues[a][f][1] = bb.getInt();
                nf.mcValues[a][f][2] = bb.getInt();
                nf.mcValues[a][f][3] = bb.getInt();
                nf.mcValues[a][f][4] = bb.getInt();
                nf.mcValues[a][f][5] = bb.getInt();
                nf.mcValues[a][f][6] = bb.getInt();
                areaSize = nf.mcValues[a][f][1]*nf.mcValues[a][f][2];
                byte[] area = new byte[areaSize];
                bb.get(area);
                //nf.mcParam7[a][f] = toBase64RLE(b64Enc(area));
                nf.mcParam7[a][f] = b64Enc(area);
                bb.get(nf.mcParam8[a][f]);
            }
        }
    }
    catch(Exception ex)
    {
        out.println("Error in (FrameDataBottom):");
        ex.printStackTrace(System.out);
    }
}

// Check if nf offset arrays = local arrays, fix nf arrays if not equal
private static void bmpOffsetCheck()
{
    if(!Arrays.equals(nf.bmpOffsets,bmpOffsets))
    {
        out.println("BMP Offset Check Failed!");
        out.println("Original BMP Offsets:");
        printIntArr(nf.bmpOffsets,""+nf.bpos+"+","",nf.bmpStructs);
        out.println("New BMP Offsets:");
        printIntArr(bmpOffsets,""+nf.bpos+"+","",nf.bmpStructs);
        nf.bmpOffsets = bmpOffsets;
    }
}

private static void frameOffsetCheck(int a, int frames, int frameOffsetOrigin)
{
    if(!Arrays.equals(nf.frameOffsets[a],frameOffsets[a]))
    {
        out.println("Frame Offset Check Failed!");
        out.println("Original Frame Offsets for AnimID["+a+"]:");
        printIntArr(nf.frameOffsets[a],""+frameOffsetOrigin+"+","",frames);
        out.println("New Frame Offsets for AnimID["+a+"]:");
        printIntArr(frameOffsets[a],""+frameOffsetOrigin+"+","",frames);
        nf.frameOffsets[a] = frameOffsets[a];
    }
}

private static void noriCheck()
{
    out.print("NORI Signature Check: ");
    intCheck(1230131022, nf.fsig);
}

// Checks NORI version and sets the version-specific variables
private static void noriVerCheck()
{
    out.print("NORI Version: ");
    nf.setVerSpecific();
    out.println(nf.noriVer);
}

private static void gawiCheck()
{
    out.print("GAWI Signature Check: ");
    intCheck(1230455111, nf.gsig);
}

private static void gawiVerCheck()
{
    out.print("GAWI Version: ");
    intCheck(300, nf.gawiVer);
}

private static void palCheck()
{
    out.print("PAL_ Signature Check: ");
    intCheck(1598832976, nf.psig);
}

private static void palVerCheck()
{
    out.print("PAL_ Version: ");
    intCheck(100, nf.palVer);
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

// Moves the ByteBuffer position forward by int param value
private static void movePosFwd(int incrementNum)
{
    pos = bb.position()+incrementNum;
    bb.position(pos);
}

// A space saver + better readability function
private static int getPos()
{
    return bb.position();
}

// Shorten the new base64 encoded string from byte array command
private static String b64Enc(byte[] ba)
{
    return Base64.getEncoder().encodeToString(ba);
}

// Exception catching for new XML String creation
private static String newXmlStr(byte[] ba, Charset charSet)
{
    String newXmlStr="";
    try
    {
        newXmlStr = new String(ba,charSet);
        newXmlStr = newXmlStr.replaceAll(badXmlChars," ").trim();
    }
    catch(Exception ex)
    {
        out.println("Error in (newXmlStr):");
        ex.printStackTrace(System.out);
    }
    return newXmlStr;
}

private static void printIntArr(int[] arr, String prfx, String sffx, int limit)
{
    for(int x=0; x < limit; x++)
    {
        out.println(prfx+arr[x]+sffx);
    }
}

// Shorthand function to wrap a byte array in a little-endian bytebuffer
private static ByteBuffer mkLEBB(byte[] ba)
{
    return ByteBuffer.wrap(ba).order(ByteOrder.LITTLE_ENDIAN);
}
}
