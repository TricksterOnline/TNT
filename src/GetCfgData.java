/*
GetCfgData.java: this file is part of the TNT program.

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
import javax.xml.parsers.*;
import org.w3c.dom.*;
import static java.lang.System.out;
/**
Class Description:

Dev Notes:

Development Priority: MEDIUM
*/
public class GetCfgData
{
// class variables
private static NORI nf;
private static Document cfg;
static Charset UTF8=StandardCharsets.UTF_8,EUC_KR=Charset.forName("EUC-KR");
private static int pos,max,maxNoF,NoF,maxNoP,NoP;
private static int animOffDiff,frameOffDiff,frameOffTotal;
private static int[] ftDex,pdex,fbDex,coordSets,coordX,coordY,hasMCV;
private static int[][] mcVals;
private static String[] eBlocks,uData1,sfx,uData2,mcParam7,mcParam8;

// constructor for GetCfgData class
public GetCfgData(File config, NORI NF)
{
    nf = NF;
    try
    {
        // Set NORI file directory
        nf.setDir(config);
        // Make document object from config file
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbf.newDocumentBuilder();
        // Set xml config document to global var: cfg
        cfg = dBuilder.parse(config);
        cfg.getDocumentElement().normalize();
        // Set NORI element, get & set NORI file name
        Element root = cfg.getDocumentElement();
        nf.name = root.getAttribute("name");

        // Set NORI Header and GAWI Header Elements
        Element noriHdr = getElementByTagName("NORI_HDR");
        Element gawiHdr = getElementByTagName("GAWI_HDR");
        // Get & Set NORI Header Data
        nf.noriVer = getIntVal(noriHdr,nf.xml_tag[1]);
        nf.nParam1 = getIntVal(noriHdr,nf.xml_tag[2]);
        nf.nParam2 = getIntVal(noriHdr,nf.xml_tag[3]);
        nf.nParam3 = getIntVal(noriHdr,nf.xml_tag[4]);
        nf.nParam4 = getIntVal(noriHdr,nf.xml_tag[5]);
        nf.nParam5 = getIntVal(noriHdr,nf.xml_tag[6]);
        nf.anims   = getIntVal(noriHdr,nf.xml_tag[7]);
        // Set NORI version-specific variables
        nf.setVerSpecific();
        // Get & Set GAWI Header Data
        nf.bpp        = getIntVal(gawiHdr,nf.xml_tag[12]);
        nf.hasPalette = getIntVal(gawiHdr,nf.xml_tag[14]);
        nf.gParam4    = getIntVal(gawiHdr,nf.xml_tag[15]);
        nf.gParam5    = getIntVal(gawiHdr,nf.xml_tag[16]);
        nf.gParam6    = getIntVal(gawiHdr,nf.xml_tag[17]);
        nf.gParam7    = getIntVal(gawiHdr,nf.xml_tag[18]);
        nf.bmpStructs = getIntVal(gawiHdr,nf.xml_tag[19]);
        // Get & Set Palette Header Data, if palette exists
        if(nf.bpp==8)
        {
            Element pal = getElementByTagName("PAL");
            nf.pParam1 = getIntVal(pal,nf.xml_tag[23]);
            nf.pParam2 = getIntVal(pal,nf.xml_tag[24]);
            nf.pParam3 = getIntVal(pal,nf.xml_tag[25]);
            nf.pParam4 = getIntVal(pal,nf.xml_tag[26]);
            nf.psize   = getIntVal(pal,nf.xml_tag[28]);
            if(nf.psize==808)
            {
                nf.mainS = getIntVal(pal,nf.xml_tag[29]);
                nf.mainE = getIntVal(pal,nf.xml_tag[30]);
            }
        }
        // Prep BMP Offsets & BMP Specs
        nf.bmpOffsets = new int[nf.bmpStructs];
        nf.bmpOffsets = getIntArrByTag(nf.xml_tag[31]);
        nf.bmpCount   = new int[nf.bmpStructs];
        nf.bmpCount   = getIntArrByTag(nf.xml_tag[32]);
        nf.totalBMP   = getIntArrSum(nf.bmpCount);
        nf.bmpSpecs   = new int[nf.totalBMP][6];
        // Get & Set BMP Specs data arrays
        int[] width   = getIntArrByTag(nf.xml_tag[34]);
        int[] height  = getIntArrByTag(nf.xml_tag[35]);
        int[] bParam4 = getIntArrByTag(nf.xml_tag[36]);
        int[] bmpX    = getIntArrByTag(nf.xml_tag[37]);
        int[] bmpY    = getIntArrByTag(nf.xml_tag[38]);
        // Set BMP Specs
        for(int bmp=0; bmp < nf.totalBMP; bmp++)
        {
            nf.bmpSpecs[bmp][0] = width[bmp]*height[bmp]*(nf.bpp/8);
            nf.bmpSpecs[bmp][1] = width[bmp];
            nf.bmpSpecs[bmp][2] = height[bmp];
            nf.bmpSpecs[bmp][3] = bParam4[bmp];
            nf.bmpSpecs[bmp][4] = bmpX[bmp];
            nf.bmpSpecs[bmp][5] = bmpY[bmp];
        }
        // Prep Animation Offsets
        nf.animOffsets = new int[nf.anims];
        // Get & Set Animation Data
        nf.title  = getStrArrByTag(nf.xml_tag[40]);
        nf.numFrames = getIntArrByTag(nf.xml_tag[41]);
        nf.totalFrames = getIntArrSum(nf.numFrames);
        // Prep FrameDataTop arrays
        maxNoF = getMax(nf.numFrames);
        nf.frameOffsets = new int[nf.anims][maxNoF];
        nf.frameDataTop = new int[nf.anims][maxNoF][2];
        ftDex = new int[2];
        // Get & Set FrameDataTop Data Arrays
        int[] durations = getIntArrByTag(nf.xml_tag[43]);
        int[] numPlanes = getIntArrByTag(nf.xml_tag[44]);
        nf.totalPlanes = getIntArrSum(numPlanes);
        // Prep PlaneData array
        maxNoP = getMax(numPlanes);
        nf.planeData = new int[nf.anims][maxNoF][maxNoP][7];
        pdex = new int[7];
        // Get & Set PlaneData Data Arrays
        int[] bmpID      = getIntArrByTag(nf.xml_tag[45]);
        int[] planeX     = getIntArrByTag(nf.xml_tag[46]);
        int[] planeY     = getIntArrByTag(nf.xml_tag[47]);
        int[] opacity    = getIntArrByTag(nf.xml_tag[48]);
        int[] flip       = getIntArrByTag(nf.xml_tag[49]);
        int[] blend_mode = getIntArrByTag(nf.xml_tag[50]);
        int[] flag_param = getIntArrByTag(nf.xml_tag[51]);
        // Prep FrameDataBottom + Get & Set FDB Data arrays
        fbDex = new int[11];
        if(nf.notV300)
        {
            // Get & Set Coordinate Data Arrays
            coordSets = getIntArrByTag(nf.xml_tag[52]);
            coordX    = getIntArrByTag(nf.xml_tag[53]);
            coordY    = getIntArrByTag(nf.xml_tag[54]);
            nf.totalCoordSetsBytes = (coordSets.length+(coordX.length*2))*4;
            // Prep Coordinate arrays
            int maxNoCS = getMax(coordSets);
            nf.numCoordSets = new int[nf.anims][maxNoF];
            nf.coordSets    = new int[nf.anims][maxNoF][maxNoCS][2];
        }
        if(nf.hasEB)
        {
            nf.entryBlocks = new byte[nf.anims][maxNoF][6][28];
            eBlocks = getStrArrByTag(nf.xml_tag[56]);
        }
        nf.unknownData1 = new byte[nf.anims][maxNoF][2][22];
        nf.soundEffect  = new String[nf.anims][maxNoF];
        nf.unknownData2 = new byte[nf.anims][maxNoF][18];
        uData1 = getStrArrByTag(nf.xml_tag[57]);
        sfx    = getStrArrByTag(nf.xml_tag[58]);
        uData2 = getStrArrByTag(nf.xml_tag[59]);
        if(nf.maybeMCV)
        {
            nf.hasMCValues = new int[nf.anims][maxNoF];
            nf.mcValues    = new int[nf.anims][maxNoF][7];
            nf.mcParam7    = new String[nf.anims][maxNoF];
            nf.mcParam8    = new byte[nf.anims][maxNoF][20];
            hasMCV = getIntArrByTag(nf.xml_tag[60]);
            mcVals = new int[7][nf.totalFrames];
            mcVals[0] = getIntArrByTag(nf.xml_tag[61]);
            mcVals[1] = getIntArrByTag(nf.xml_tag[62]);
            mcVals[2] = getIntArrByTag(nf.xml_tag[63]);
            mcVals[3] = getIntArrByTag(nf.xml_tag[64]);
            mcVals[4] = getIntArrByTag(nf.xml_tag[65]);
            mcVals[5] = getIntArrByTag(nf.xml_tag[66]);
            mcVals[6] = getIntArrByTag(nf.xml_tag[67]);
            mcParam7  = getStrArrByTag(nf.xml_tag[68]);
            mcParam8  = getStrArrByTag(nf.xml_tag[69]);
        }
        // Set Frame Data & animOffsets & frameOffsets
        for(int a=0,animOffTotal=0; a < nf.anims; a++)
        {
            nf.animOffsets[a] = animOffTotal;
            animOffDiff=0;
            frameOffTotal=0;
            NoF = nf.numFrames[a];
            animOffDiff = 36+(4*NoF);
            for(int f=0; f < NoF; f++)
            {
                nf.frameOffsets[a][f] = frameOffTotal;
                frameOffDiff=0;
                // Set FrameDataTop values
                nf.frameDataTop[a][f][0] = durations[ftDex[0]++];
                nf.frameDataTop[a][f][1] = numPlanes[ftDex[1]++];
                NoP = nf.frameDataTop[a][f][1];
                frameOffDiff = 8+(28*NoP);
                // Set PlaneData array
                for(int p=0; p < NoP; p++)
                {
                    nf.planeData[a][f][p][0] = bmpID[pdex[0]++];
                    nf.planeData[a][f][p][1] = planeX[pdex[1]++];
                    nf.planeData[a][f][p][2] = planeY[pdex[2]++];
                    nf.planeData[a][f][p][3] = opacity[pdex[3]++];
                    nf.planeData[a][f][p][4] = flip[pdex[4]++];
                    nf.planeData[a][f][p][5] = blend_mode[pdex[5]++];
                    nf.planeData[a][f][p][6] = flag_param[pdex[6]++];
                }
                // Set FrameDataBottom values
                fillFrameDataBottom(a,f);
                frameOffTotal += frameOffDiff;
            }
            animOffDiff += frameOffTotal;
            animOffTotal += animOffDiff;
        }
    }catch(Exception ex)
    {
        out.println("Error in (getConfigData):");
        ex.printStackTrace(System.out);
    }
}

private static void fillFrameDataBottom(int a, int f)
{
    if(nf.notV300)
    {
        nf.numCoordSets[a][f] = coordSets[fbDex[0]++];
        for(int i=0; i < nf.numCoordSets[a][f]; i++)
        {
            nf.coordSets[a][f][i][0] = coordX[fbDex[1]++];
            nf.coordSets[a][f][i][1] = coordY[fbDex[2]++];
        }
        frameOffDiff += 4+(nf.numCoordSets[a][f]*8);
    }
    frameOffDiff += nf.cdBlockSize;
    if(nf.hasEB)
    {
        nf.entryBlocks[a][f][0] = b64Dec(eBlocks[fbDex[3]++]);
        nf.entryBlocks[a][f][1] = b64Dec(eBlocks[fbDex[3]++]);
        nf.entryBlocks[a][f][2] = b64Dec(eBlocks[fbDex[3]++]);
        nf.entryBlocks[a][f][3] = b64Dec(eBlocks[fbDex[3]++]);
        nf.entryBlocks[a][f][4] = b64Dec(eBlocks[fbDex[3]++]);
        nf.entryBlocks[a][f][5] = b64Dec(eBlocks[fbDex[3]++]);
        frameOffDiff += 168;
    }
    nf.unknownData1[a][f][0] = b64Dec(uData1[fbDex[4]++]);
    nf.unknownData1[a][f][1] = b64Dec(uData1[fbDex[4]++]);
    nf.soundEffect[a][f]     = sfx[fbDex[5]++];
    nf.unknownData2[a][f]    = b64Dec(uData2[fbDex[6]++]);
    frameOffDiff += 44+18+18;
    if(nf.maybeMCV)
    {
        nf.hasMCValues[a][f] = hasMCV[fbDex[7]++];
        frameOffDiff += 4;
        if(nf.hasMCValues[a][f]==1)
        {
            nf.mcSizeSum += 28;
            frameOffDiff += 28;
            nf.mcValues[a][f][0] = mcVals[0][fbDex[8]];
            nf.mcValues[a][f][1] = mcVals[1][fbDex[8]];
            nf.mcValues[a][f][2] = mcVals[2][fbDex[8]];
            nf.mcValues[a][f][3] = mcVals[3][fbDex[8]];
            nf.mcValues[a][f][4] = mcVals[4][fbDex[8]];
            nf.mcValues[a][f][5] = mcVals[5][fbDex[8]];
            nf.mcValues[a][f][6] = mcVals[6][fbDex[8]++];
            int A = nf.mcValues[a][f][1]*nf.mcValues[a][f][2];
            nf.mcSizeSum += A;
            frameOffDiff += A;
            nf.mcParam7[a][f] = mcParam7[fbDex[9]++];
            nf.mcParam8[a][f] = b64Dec(mcParam8[fbDex[10]++]);
            nf.mcSizeSum += 20;
            frameOffDiff += 20;
        }
    }
}

// An anti-duplication + better readability function
private static int getMax(int[] array)
{
    max=0;
    for(int x : array)
    {
        if(x > max) max=x;
    }
    return max;
}

private static int getIntArrSum(int[] array)
{
    int sum=0;
    for(int x : array)
    {
        sum += x;
    }
    return sum;
}

// Shorten the byte array from base64 encoded string command
private static byte[] b64Dec(String s)
{
    return Base64.getDecoder().decode(s.getBytes());
}

// Get single int array (int[]) by tagName
private static int[] getIntArrByTag(String tag)
{
    NodeList nl = cfg.getElementsByTagName(tag);
    max = nl.getLength();
    int[] tmp = new int[max];
    for(int i = 0; i < max; i++)
    {
        Node n = nl.item(i);
        tmp[i] = toInt((n.getTextContent()).trim());
    }
    return tmp;
}

// Get single string array (String[]) by tagName
private static String[] getStrArrByTag(String tag)
{
    NodeList nl = cfg.getElementsByTagName(tag);
    max = nl.getLength();
    String[] tmp = new String[max];
    for(int i = 0; i < max; i++)
    {
        Node n = nl.item(i);
        tmp[i] = (n.getTextContent()).trim();
    }
    return tmp;
}

// An anti-duplication + better readability function
private static Element getElementByTagName(String tagName)
{
    Node node0 = cfg.getElementsByTagName(tagName).item(0);
    return (Element) node0;
}

// An anti-duplication + better readability function
private static int getIntVal(Element parentE,String tagName)
{
    Node node0 = parentE.getElementsByTagName(tagName).item(0);
    return toInt((node0.getTextContent()).trim());
}

// An anti-duplication + better readability function
private static int toInt(String str)
{
    try
    {
        return Integer.parseInt(str);
    }
    catch(NumberFormatException e)
    {
        return 0;
    }
}
}
