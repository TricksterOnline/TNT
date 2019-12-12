/*
Create.java: this file is part of the TNT program.

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
import javax.xml.parsers.*;
import org.w3c.dom.*;
import static java.lang.System.out;
/**
Class Description:
For all intents and purposes, the Create class is meant to be the interface for
creating new NORI files from suitable bitmap images. It will rely heavily on
config files which will be specified by user input.

Dev Notes:
Creating new NORI files is honestly of little use to the Libre Trickster project
and this part of the program is little more than a curiosity.

Development Priority: LOW
*/
public class Create
{
    // class variables
    public static int maxNoF=0,maxNoP=0,pos=0;
    public static int pdex0=0,pdex1=0,pdex2=0,pdex3=0,pdex4=0,pdex5=0,pdex6=0;
    public static byte[] nfba, palette, imgData;
    public static int[] bmp_id, point_x, point_y, opacity, flip_axis;
    public static int[] blend_mode, flag_param;
    // constructor for Create class
    public Create(File config, String bmpDir, NORI nf)
    {
        try
        {
            out.println("\nGathering data from config file...");
            // Set the NORI file vars
            nf.setNFileVars(config,1);
            nf.checkDir();
            out.println("NORI filename: "+nf.name);
            // Get XML Data
            getConfigData(config,nf);
            // Setup NORI file byte array and bytebuffer
            nfba = new byte[nf.fsize];
            ByteBuffer nbb = mkLEBB(nfba);
            // Add NORI header to the nfba
            addNoriHdr(nbb,nf);
            // Add GAWI header to the nfba
            addGawiHdr(nbb,nf);
            // Add Palette section if it exists; it won't, future-proofing a bit
            if(nf.hasPalette==1) addPalSection(nbb,nf);
            // Add BMP Offsets
            addBmpOffsets(nbb,nf);
            // Get image data from BMP files
            imgData = getImgData(bmpDir,nf);
            ByteBuffer dbb = mkLEBB(imgData);
            // Add BMP specs and data
            addBmpSection(nbb,dbb,nf);
            // Add Animation Offsets
            addAnimOffsets(nbb,nf);
            // Get XFB
            getXFB(nf);
            // Add Anims, Frames, Plane Data, & xfb
            addAnims(nbb,nf);
            out.println("Finalizing file...");

            // Set BMP name and location, then write BMP to file
            File nori = new File(nf.dir+nf.name);
            Files.write(nori.toPath(),nfba);
            out.println("NORI File Creation Complete.\n");
        }
        catch(Exception ex)
        {
            out.println("Error in (CM):\n"+ex);
        }
    }

    private static void addNoriHdr(ByteBuffer bb, NORI nf)
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

    private static void addGawiHdr(ByteBuffer bb, NORI nf)
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
        bb.putInt(nf.numBMP);
        bb.putInt(nf.gsize);
    }

    private static void getPal(NORI nf)
    {
        try
        {
            String filename = nf.dir+nf.dname+"_pal.bin";
            nf.palBytes = Files.readAllBytes((new File(filename)).toPath());
        }
        catch(Exception ex)
        {
            out.println("Error in (getPal):\n"+ex);
        }
    }

    private static void addPalSection(ByteBuffer bb, NORI nf)
    {
        bb.putInt(nf.psig);
        bb.putInt(nf.palVer);
        bb.putInt(nf.pParam1);
        bb.putInt(nf.pParam2);
        bb.putInt(nf.pParam3);
        bb.putInt(nf.pParam4);
        bb.putInt(nf.divided);
        bb.putInt(nf.psize);
        bb.put(nf.palBytes);
        if(nf.psize==808)
        {
            bb.putInt(nf.mainS);
            bb.putInt(nf.mainE);
        }
    }

    private static void addBmpOffsets(ByteBuffer bb, NORI nf)
    {
        for(int i=0; i < nf.numBMP; i++)
        {
            bb.putInt(nf.bmpOffsets[i]);
        }
    }

    private static void addBmpSection(ByteBuffer bb, ByteBuffer dbb, NORI nf)
    {
        String dcErr,manualFix;
        dcErr="Error: dcount not 1, space was added for BMP id: ";
        manualFix="To solve, manually fix: fsize, gsize, bmpOffsets, & dcount";
        for(int i=0; i < nf.numBMP; i++)
        {
            int addSpace=0;
            bb.putInt(nf.bmpSpecs[i][0]);
            bb.putInt(nf.bmpSpecs[i][1]);
            bb.putInt(nf.bmpSpecs[i][2]);
            bb.putInt(nf.bmpSpecs[i][3]);
            bb.putInt(nf.bmpSpecs[i][4]);
            bb.putInt(nf.bmpSpecs[i][5]);
            bb.putInt(nf.bmpSpecs[i][6]);
            byte[] data = new byte[nf.bmpSpecs[i][1]];
            dbb.get(data,0,nf.bmpSpecs[i][1]);
            bb.put(data);
            pos = bb.position();
            if(nf.bmpSpecs[i][0]!=1)
            {
                if(i!=(nf.numBMP-1))
                    addSpace = nf.bmpOffsets[i+1]-nf.bmpOffsets[i]-data.length;
                else
                    addSpace = (nf.gsize+40) - pos;
                out.println(dcErr+i+"\n"+manualFix);
                bb.position(pos+addSpace);
            }
        }
    }

    private static void addAnimOffsets(ByteBuffer bb, NORI nf)
    {
        for(int i=0; i < nf.anims; i++)
        {
            bb.putInt(nf.animOffsets[i]);
        }
    }

    private static void addAnims(ByteBuffer bb, NORI nf)
    {
        for(int i=0; i < nf.anims; i++)
        {
            byte[] animName = new byte[32];
            animName = (nf.animName[i]).getBytes();
            pos = bb.position();
            bb.put(animName);
            bb.position(pos+32);
            bb.putInt(nf.frames[i]);
            addFrameOffsets(bb,i,nf);
            addFrameData(bb,i,nf);
        }
    }

    private static void addFrameOffsets(ByteBuffer bb, int a, NORI nf)
    {
        for(int i=0; i < nf.frames[a]; i++)
        {
            bb.putInt(nf.frameOffsets[a][i]);
        }
    }

    // Set actual data for frames (and planes)
    private static void addFrameData(ByteBuffer bb, int a, NORI nf)
    {
        for(int i=0; i < nf.frames[a]; i++)
        {
            bb.putInt(nf.frameData[a][i][0]);
            bb.putInt(nf.frameData[a][i][1]);
            addPlaneData(bb,a,i,nf);
        }
    }

    private static void addPlaneData(ByteBuffer bb, int a, int f, NORI nf)
    {
        for(int i=0; i < nf.frameData[a][f][1]; i++)
        {
            bb.putInt(nf.planeData[a][f][i][0]);
            bb.putInt(nf.planeData[a][f][i][1]);
            bb.putInt(nf.planeData[a][f][i][2]);
            bb.putInt(nf.planeData[a][f][i][3]);
            bb.putInt(nf.planeData[a][f][i][4]);
            bb.putInt(nf.planeData[a][f][i][5]);
            bb.putInt(nf.planeData[a][f][i][6]);
        }
        // Skip through xtraFrameBytes
        bb.put(nf.xfb);
    }

    private static void getXFB(NORI nf)
    {
        try
        {
            String filename = nf.dir+"xfb"+nf.noriVer+".bin";
            nf.xfb = Files.readAllBytes((new File(filename)).toPath());
        }
        catch(Exception ex)
        {
            out.println("Error in (getXFB):\n"+ex);
        }
    }

    private static byte[] getImgData(String bmpDir, NORI nf)
    {
        JBL bl = new JBL();
        // Setup byte array where pixel data will go
        int ids=0;
        for(int i=0; i < nf.numBMP; i++)
        {
            ids += nf.bmpSpecs[i][1];
        }
        byte[] ba = new byte[ids];
        ByteBuffer bb = mkLEBB(ba);
        try
        {
            // Gather the list of bmp files
            File dataDir = new File(bmpDir);
            String[] tmpFL = dataDir.list();
            String[] fl = cleanFL(tmpFL,nf);
            // Alphabetic ordering
            Arrays.sort(fl);
            // Pull the file contents into a byte array for later use
            out.println("Absorbing BMP files:");
            for(int i=0; i < fl.length; i++)
            {
                // Make the string into a file & read its bytes into the array
                String file = bmpDir+fl[i];
                out.println(file);
                byte[] bmp = Files.readAllBytes((new File(file)).toPath());
                ByteBuffer bbb = mkLEBB(bmp);
                // Strip the header off the image
                bbb.position(10);
                int pxStart = bbb.getInt();
                //out.println("pxStart: "+pxStart);
                int pxLen = bbb.capacity() - pxStart;
                //out.println("pxLength: "+pxLen);
                bbb.position(pxStart);
                byte[] hdrless = new byte[pxLen];
                bbb.get(hdrless,0,pxLen);
                // Strip any padding on the pixels
                bl.setBmpVars(nf.bmpSpecs[i][2],nf.bmpSpecs[i][3],nf.bpp);
                // NORI format uses top-down scanlines
                byte[] revData = bl.reverseRows(hdrless);
                byte[] rawData = bl.stripPadding(revData);
                // Add raw data to ba byte array
                bb.put(rawData);
            }
        }
        catch(Exception ex)
        {
            out.println("Error in (getImgData):\n"+ex);
        }
        return ba;
    }

    // Cleans the file list, if user is stupid, to make sure only bmp get in
    private static String[] cleanFL(String[] tmp, NORI nf)
    {
        String[] cfl = new String[nf.numBMP];
        int x=0;
        for(int i=0; i < tmp.length; i++)
        {
            if((tmp[i].toLowerCase()).endsWith(".bmp")) cfl[x++]=tmp[i];
        }
        return cfl;
    }

    private static void getConfigData(File config, NORI nf)
    {
        try
        {
            // Make document object from config file
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbf.newDocumentBuilder();
            Document cfg = dBuilder.parse(config);
            cfg.getDocumentElement().normalize();
            // Set NORI Header and GAWI Header Elements
            Element noriHdr = getElementByTagName(cfg,"NORI_HDR");
            Element gawiHdr = getElementByTagName(cfg,"GAWI_HDR");
            // Get NORI Header Data
            nf.fsig    = getIntVal(noriHdr,"fsig");
            nf.noriVer = getIntVal(noriHdr,"noriver");
            nf.nParam1 = getIntVal(noriHdr,"nparam1");
            nf.nParam2 = getIntVal(noriHdr,"nparam2");
            nf.nParam3 = getIntVal(noriHdr,"nparam3");
            nf.nParam4 = getIntVal(noriHdr,"nparam4");
            nf.nParam5 = getIntVal(noriHdr,"nparam5");
            nf.anims   = getIntVal(noriHdr,"anims");
            nf.woGawi  = getIntVal(noriHdr,"woGawi");
            nf.fsize   = getIntVal(noriHdr,"fsize");
            // Get GAWI Header Data
            nf.gsig       = getIntVal(gawiHdr,"gsig");
            nf.gawiVer    = getIntVal(gawiHdr,"gawiver");
            nf.bpp        = getIntVal(gawiHdr,"bpp");
            nf.compressed = getIntVal(gawiHdr,"compressed");
            nf.hasPalette = getIntVal(gawiHdr,"hasPalette");
            nf.gParam4    = getIntVal(gawiHdr,"gparam4");
            nf.gParam5    = getIntVal(gawiHdr,"gparam5");
            nf.gParam6    = getIntVal(gawiHdr,"gparam6");
            nf.gParam7    = getIntVal(gawiHdr,"gparam7");
            nf.numBMP     = getIntVal(gawiHdr,"numBMP");
            nf.gsize      = getIntVal(gawiHdr,"gsize");
            // Get BMP Offsets
            nf.bmpOffsets = getIntArrByTag(cfg,"bmpOffset");
            // Get BMP Specs
            nf.bmpSpecs = new int[nf.numBMP][7];
            int[] dcount  = getIntArrByTag(cfg,"dcount");
            int[] dlen    = getIntArrByTag(cfg,"dlen");
            int[] w       = getIntArrByTag(cfg,"w");
            int[] h       = getIntArrByTag(cfg,"h");
            int[] bparam4 = getIntArrByTag(cfg,"bparam4");
            int[] pos_x   = getIntArrByTag(cfg,"pos_x");
            int[] pos_y   = getIntArrByTag(cfg,"pos_y");
            for(int bmp=0; bmp < nf.numBMP; bmp++)
            {
                nf.bmpSpecs[bmp][0] = dcount[bmp];
                nf.bmpSpecs[bmp][1] = dlen[bmp];
                nf.bmpSpecs[bmp][2] = w[bmp];
                nf.bmpSpecs[bmp][3] = h[bmp];
                nf.bmpSpecs[bmp][4] = bparam4[bmp];
                nf.bmpSpecs[bmp][5] = pos_x[bmp];
                nf.bmpSpecs[bmp][6] = pos_y[bmp];
            }
            // Get Animation Offsets
            nf.animOffsets = getIntArrByTag(cfg,"animOffset");
            // Get Animation Data
            nf.animName = getStrArrByTag(cfg,"name");
            nf.frames   = getIntArrByTag(cfg,"frames");
            // Prep Frame Data arrays
            maxNoF = getMax(nf.frames);
            nf.frameOffsets = new int[nf.anims][maxNoF];
            nf.frameData = new int[nf.anims][maxNoF][2];
            // Get Frame Data Arrays
            int[] frameOff = getIntArrByTag(cfg,"frameOffset");
            int[] delays   = getIntArrByTag(cfg,"delay");
            int[] planes   = getIntArrByTag(cfg,"planes");
            // Prep Plane Data arrays
            maxNoP = getMax(planes);
            nf.planeData = new int[nf.anims][maxNoF][maxNoP][7];
            // Get Plane Data Arrays
            bmp_id     = getIntArrByTag(cfg,"bmp_id");
            point_x    = getIntArrByTag(cfg,"point_x");
            point_y    = getIntArrByTag(cfg,"point_y");
            opacity    = getIntArrByTag(cfg,"opacity");
            flip_axis  = getIntArrByTag(cfg,"flip_axis");
            blend_mode = getIntArrByTag(cfg,"blend_mode");
            flag_param = getIntArrByTag(cfg,"flag_param");
            // Get Frame Data
            int dex0=0,dex1=0,dex2=0;
            for(int a=0; a < nf.anims; a++)
            {
                for(int f=0; f < nf.frames[a]; f++)
                {
                    nf.frameOffsets[a][f] = frameOff[dex0++];
                    nf.frameData[a][f][0] = delays[dex1++];
                    nf.frameData[a][f][1] = planes[dex2++];
                    // Get Plane Data
                    getPlaneData(cfg,a,f,nf);
                }
            }
        }
        catch(Exception ex)
        {
            out.println("Error in (getConfigData):\n"+ex);
        }
    }

    private static void getPlaneData(Document cfg, int a, int f, NORI nf)
    {
        for(int p=0; p < nf.frameData[a][f][1]; p++)
        {
            nf.planeData[a][f][p][0] = bmp_id[pdex0++];
            nf.planeData[a][f][p][1] = point_x[pdex1++];
            nf.planeData[a][f][p][2] = point_y[pdex2++];
            nf.planeData[a][f][p][3] = opacity[pdex3++];
            nf.planeData[a][f][p][4] = flip_axis[pdex4++];
            nf.planeData[a][f][p][5] = blend_mode[pdex5++];
            nf.planeData[a][f][p][6] = flag_param[pdex6++];
        }
    }

    // Another function that makes the code look better & have less duplication
    private static int getMax(int[] array)
    {
        int max=0;
        for(int x : array)
        {
            if(x > max) max=x;
        }
        return max;
    }

    // Get single int array (int[]) by tagName
    private static int[] getIntArrByTag(Document cfg, String tag)
    {
        NodeList nl = cfg.getElementsByTagName(tag);
        int max = nl.getLength();
        int[] tmp = new int[max];
        for(int i = 0; i < max; i++)
        {
            Node n = nl.item(i);
            tmp[i] = toInt((n.getTextContent()).trim());
        }
        return tmp;
    }

    // Get single string array (String[]) by tagName
    private static String[] getStrArrByTag(Document cfg, String tag)
    {
        NodeList nl = cfg.getElementsByTagName(tag);
        int max = nl.getLength();
        String[] tmp = new String[max];
        for(int i = 0; i < max; i++)
        {
            Node n = nl.item(i);
            tmp[i] = (n.getTextContent()).trim();
        }
        return tmp;
    }

    private static Element getElementByTagName(Document cfg,String tagName)
    {
        Node node0 = cfg.getElementsByTagName(tagName).item(0);
        return (Element) node0;
    }

    private static int getIntVal(Element parentE,String tagName)
    {
        Node node0 = parentE.getElementsByTagName(tagName).item(0);
        return toInt((node0.getTextContent()).trim());
    }

    // Shorthand function to wrap a byte array in a little-endian bytebuffer
    private static ByteBuffer mkLEBB(byte[] ba)
    {
        // this long syntax call is why this function exists ;)
        return ByteBuffer.wrap(ba).order(ByteOrder.LITTLE_ENDIAN);
    }

    // Lets make my life easier and save some code space
    private static int toInt(String str)
    {
        int i=0;
        try
        {
            i = Integer.parseInt(str);
        }
        catch(NumberFormatException e)
        {
            i = 0;
        }
        return i;
    }
}
