/*
Create.java: this file is part of the TNT program.

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
import java.util.zip.*;
import static java.lang.System.out;
/**
Class Description:
The Create class is the interface for creating new NORI files from suitable BMP
images. It relies heavily on a config file that is specified by user input.

Dev Notes:
Creating new NORI files is honestly of little use to the Libre Trickster project
so this part of the program is little more than a curiosity. Nonetheless, it has
a high demand among the userbase.

Development Priority: LOW
*/
public class Create
{
// class variables
private static NORI nf;
private static ByteBuffer bb;
static Charset UTF8=StandardCharsets.UTF_8,EUC_KR=Charset.forName("EUC-KR");
private static byte xCD = (byte)0xCD;
private static int pos, zlibSize;
private static byte[] nfba, palette, tmpData, finalBA;
// constructor for Create class
public Create(File config, String bmpDir, boolean zlibCompress)
{
    nf = new NORI();
    if(!bmpDir.endsWith(nf.fs)) bmpDir += nf.fs;
    try
    {
        out.println("\nGathering data from config file...");
        // Get XML Data
        GetCfgData gcd = new GetCfgData(config,nf);
        out.println("NORI filename: "+nf.name);
        // Get image data from BMP files
        getImgData(bmpDir);
        out.println("Total image data bytes: "+nf.bmpData.length);
        // Run size fixes & disable uncouth features
        nf.fixNORI(true);
        // Setup NORI file byte array and bytebuffer
        nfba = new byte[nf.fsize];
        out.println("NORI file size: "+nfba.length);
        bb = mkLEBB(nfba);
        // Add NORI header to the nfba
        add_NORI_HDR();
        // Add GAWI header to the nfba
        add_GAWI_HDR();
        // Add Palette section if it exists & bpp=8
        if(nf.hasPalette==1 && nf.bpp==8) add_PAL();
        // Add BMP Offsets
        add_bmpOffsets();
        // Add BMP specs and data
        add_BMP();
        // Add Animation Offsets
        add_animOffsets();
        // Add Animations, FrameDataTops, Plane Data, & FrameDataBottoms
        add_ANIM();
        out.println("Finalizing file...");
        if(zlibCompress)
        {
            Deflater zlib = new Deflater();
            zlib.setInput(nfba);
            zlib.finish();
            byte[] tmpBA = new byte[nf.fsize];
            zlibSize = zlib.deflate(tmpBA);
            out.println("Compressed Size: "+(zlibSize+12));
            zlib.end();
            finalBA = new byte[12+zlibSize];
            ByteBuffer finalBB = mkLEBB(finalBA);
            finalBB.putInt(41136);// xB0A00000
            finalBB.putInt(nfba.length);// Actual Size
            finalBB.putInt(zlibSize);// Compressed Size
            finalBB.put(tmpBA,0,zlibSize);// Compressed Data w/ zlib header
        }
        else
        {
            finalBA = nfba;
        }
        // Set NORI file location and name
        File nori = new File(nf.dir+nf.name);
        File nori_orig = new File(nf.dir+nf.name+".orig");
        // Make backup file if it doesn't already exist
        if(nori.exists() && !nori_orig.exists()) nori.renameTo(nori_orig);
        // Write NORI to file
        Files.write(nori.toPath(),finalBA);
        out.println("NORI File Creation Complete.\n");
    }
    catch(Exception ex)
    {
        out.println("Error in (CM):");
        ex.printStackTrace(System.out);
    }
}

private static void getImgData(String bmpDir)
{
    try
    {
        JBL bl = new JBL();
        // Gather the list of bmp files
        File dataDir = new File(bmpDir);
        String[] tmpFL = dataDir.list();
        String[] fl = cleanFL(tmpFL);
        if(fl.length!=nf.totalBMP)
        {
            out.println("Check BMPs and the config file.");
            out.println("The sum of all "+nf.xml_tag[32]+" values is wrong!");
            System.exit(1);
        }
        // Alphabetic ordering
        Arrays.sort(fl);
        // Pull the file contents into a byte array for later use
        out.println("Absorbing BMP files:");
        // Prep bmpData array
        for(int i=0; i < nf.totalBMP; i++)
        {
            nf.bmpDataSize += nf.bmpSpecs[i][0];
        }
        nf.bmpData = new byte[nf.bmpDataSize];
        ByteBuffer bmpData = mkLEBB(nf.bmpData);
        String end;
        // Fill bmpData array
        for(int i=0; i < fl.length; i++)
        {
            // output full file name
            out.printf(bmpDir+fl[i]);
            end = "";
            // Read BMP into a byte array & wrap in ByteBuffer
            byte[] bmp = file2BA(bmpDir+fl[i]);
            ByteBuffer img = mkLEBB(bmp);
            // Set JBL BMP variables
            bl.getBitmapVars(bmp);
            // Strip the header off the image
            img.position(bl.dataStart);
            byte[] hdrless = bl.getImgBytes(img,0);
            // NORI format uses top-down scanlines
            byte[] revData = bl.reverseRows(hdrless);
            // Strip any padding off the pixels
            byte[] rawData = bl.stripPadding(revData);
            // PhotoSh*p BMP fix
            if(bl.w!=2 && nf.bmpSpecs[i][0]==(rawData.length-2))
            {
                tmpData = new byte[nf.bmpSpecs[i][0]];
                for(int x=0; x < nf.bmpSpecs[i][0]; x++)
                {
                    tmpData[x] = rawData[x];
                }
                rawData = tmpData;
                end = " (PS BMP Fixed)";
            }
            out.println(end);
            // Crash if image size doesn't match w*h*(bpp/8) calculation
            if(nf.bmpSpecs[i][0]!=rawData.length)
            {
                out.println("BMP #"+i+"'s pixel data size does not match!");
                out.println("Expected: "+nf.bmpSpecs[i][0]);
                out.println("Received: "+rawData.length);
                out.println("Causes: incorrect bpp, w, h, &/or input BMP");
                System.exit(1);
            }
            // Add raw data to bmpData array
            bmpData.put(rawData);
        }
    }catch(Exception ex)
    {
        out.println("Error in (getImgData):");
        ex.printStackTrace(System.out);
    }
}

private static void add_NORI_HDR()
{
    bb.putInt(nf.fsig);
    bb.putInt(nf.noriVer);
    bb.putInt(nf.nParam1);
    bb.putInt(nf.nParam2);
    bb.putInt(nf.nParam3);
    bb.putInt(nf.nParam4);
    bb.putInt(nf.nParam5);
    bb.putInt(nf.anims);
    bb.putInt(nf.woGawi);
    bb.putInt(nf.fsize);
}

private static void add_GAWI_HDR()
{
    bb.putInt(nf.gsig);
    bb.putInt(nf.gawiVer);
    bb.putInt(nf.bpp);
    bb.putInt(nf.compressed);
    bb.putInt(nf.hasPalette);
    bb.putInt(nf.gParam4);
    bb.putInt(nf.gParam5);
    bb.putInt(nf.gParam6);
    bb.putInt(nf.gParam7);
    bb.putInt(nf.bmpStructs);
    bb.putInt(nf.gsize);
}

private static void add_PAL()
{
    bb.putInt(nf.psig);
    bb.putInt(nf.palVer);
    bb.putInt(nf.pParam1);
    bb.putInt(nf.pParam2);
    bb.putInt(nf.pParam3);
    bb.putInt(nf.pParam4);
    bb.putInt(nf.divided);
    bb.putInt(nf.psize);
    nf.palBytes = file2BA(nf.dir+nf.name+"_pal.bin");
    bb.put(nf.palBytes);
    if(nf.psize==808)
    {
        bb.putInt(nf.mainS);
        bb.putInt(nf.mainE);
    }
}

private static void add_bmpOffsets()
{
    for(int i=0; i < nf.bmpStructs; i++)
    {
        bb.putInt(nf.bmpOffsets[i]);
    }
}

private static void add_BMP()
{
    for(int i=0,offset=0,bmpIdx=0; i < nf.bmpStructs; i++)
    {
        bb.putInt(nf.bmpCount[i]);
        for(int x=0; x < nf.bmpCount[i]; x++)
        {
            bb.putInt(nf.bmpSpecs[bmpIdx][0]);
            bb.putInt(nf.bmpSpecs[bmpIdx][1]);
            bb.putInt(nf.bmpSpecs[bmpIdx][2]);
            bb.putInt(nf.bmpSpecs[bmpIdx][3]);
            bb.putInt(nf.bmpSpecs[bmpIdx][4]);
            bb.putInt(nf.bmpSpecs[bmpIdx][5]);
            bb.put(nf.bmpData, offset, nf.bmpSpecs[bmpIdx][0]);
            offset += nf.bmpSpecs[bmpIdx][0];
            bmpIdx++;
        }
    }
}

private static void add_animOffsets()
{
    for(int a=0; a < nf.anims; a++)
    {
        bb.putInt(nf.animOffsets[a]);
        out.println("AnimOffset["+a+"]: "+nf.animOffsets[a]);
    }
}

private static void add_ANIM() throws UnsupportedEncodingException
{
    for(int a=0; a < nf.anims; a++)
    {
        pos = bb.position();
        bb.put(nf.title[a].getBytes(EUC_KR));
        bb.position(pos+32);//ensure title uses only 32 bytes
        bb.putInt(nf.numFrames[a]);
        // Add Frame Offsets
        for(int f=0; f < nf.numFrames[a]; f++)
        {
            bb.putInt(nf.frameOffsets[a][f]);
        }
        // Add Frame Data
        for(int f=0; f < nf.numFrames[a]; f++)
        {
            add_FrameDataTop(a,f);
            add_PlaneData(a,f);
            add_FrameDataBottom(a,f);
        }
    }
}

private static void add_FrameDataTop(int a, int f)
{
    bb.putInt(nf.frameDataTop[a][f][0]);
    bb.putInt(nf.frameDataTop[a][f][1]);
}

private static void add_PlaneData(int a, int f)
{
    for(int p=0; p < nf.frameDataTop[a][f][1]; p++)
    {
        bb.putInt(nf.planeData[a][f][p][0]);
        bb.putInt(nf.planeData[a][f][p][1]);
        bb.putInt(nf.planeData[a][f][p][2]);
        bb.putInt(nf.planeData[a][f][p][3]);
        bb.putInt(nf.planeData[a][f][p][4]);
        bb.putInt(nf.planeData[a][f][p][5]);
        bb.putInt(nf.planeData[a][f][p][6]);
    }
}

private static void add_FrameDataBottom(int a, int f)
{
    if(nf.notV300)
    {
        bb.putInt(nf.numCoordSets[a][f]);
        for(int i=0; i < nf.numCoordSets[a][f]; i++)
        {
            bb.putInt(nf.coordSets[a][f][i][0]);
            bb.putInt(nf.coordSets[a][f][i][1]);
        }
    }
    for(int i=0; i < nf.cdBlockSize; i++)
    {
        bb.put(xCD);
    }
    if(nf.hasEB)
    {
        bb.put(nf.entryBlocks[a][f][0]);
        bb.put(nf.entryBlocks[a][f][1]);
        bb.put(nf.entryBlocks[a][f][2]);
        bb.put(nf.entryBlocks[a][f][3]);
        bb.put(nf.entryBlocks[a][f][4]);
        bb.put(nf.entryBlocks[a][f][5]);
    }
    bb.put(nf.unknownData1[a][f][0]);
    bb.put(nf.unknownData1[a][f][1]);
    pos = bb.position();
    bb.put(nf.soundEffect[a][f].getBytes(UTF8));
    bb.position(pos+18);//ensure soundEffect uses 18 bytes
    bb.put(nf.unknownData2[a][f]);
    if(nf.maybeMCV)
    {
        bb.putInt(nf.hasMCValues[a][f]);
        if(nf.hasMCValues[a][f]==1)
        {
            bb.putInt(nf.mcValues[a][f][0]);
            bb.putInt(nf.mcValues[a][f][1]);
            bb.putInt(nf.mcValues[a][f][2]);
            bb.putInt(nf.mcValues[a][f][3]);
            bb.putInt(nf.mcValues[a][f][4]);
            bb.putInt(nf.mcValues[a][f][5]);
            bb.putInt(nf.mcValues[a][f][6]);
            bb.put(b64Dec(nf.mcParam7[a][f]));
            bb.put(nf.mcParam8[a][f]);
        }
    }
}

// Shorten the byte array from base64 encoded string command
private static byte[] b64Dec(String s)
{
    return Base64.getDecoder().decode(s.getBytes());
}

// Cleans the file list to make sure only bmp get in, b/c users are careless
private static String[] cleanFL(String[] tmp)
{
    String[] cfl = new String[nf.totalBMP];
    for(int i=0,x=0; i < tmp.length; i++)
    {
        if((tmp[i].toLowerCase()).endsWith(".bmp"))
            cfl[x++]=tmp[i];
        else
            out.println(tmp[i]+" is not a BMP file. Remove from BMP folder.");
    }
    return cfl;
}

// Shorthand function to wrap a byte array in a little-endian bytebuffer
private static ByteBuffer mkLEBB(byte[] ba)
{
    return ByteBuffer.wrap(ba).order(ByteOrder.LITTLE_ENDIAN);
}

// An anti-duplication + better readability function
private static byte[] file2BA(String fStr)
{
    File file = new File(fStr);
    byte[] ba = new byte[(int)file.length()];
    try
    {
        ba = Files.readAllBytes(file.toPath());
    }
    catch(Exception ex)
    {
        out.println("Error in (file2BA):");
        ex.printStackTrace(System.out);
    }
    return ba;
}
}
