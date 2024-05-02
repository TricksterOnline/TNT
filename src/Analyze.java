/*
Analyze.java: this file is part of the TNT program.

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
import java.nio.file.*;
import java.util.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import static java.lang.System.out;
/**
Class Description:
This class provides a standalone interface for the Analyzer functionality.
More importantly, it can produce config files for pre-existing NORI files.

Dev Notes:
The xml config code here isn't entirely user friendly but that's not really my
fault. It's just the way the standard libs for xml are. Plus, the program is
handling a huge amount of data in single file so I think it is decent.

Development Priority: HIGH
*/
public class Analyze
{
// class variables
private static NORI nf;
private static Document cfg;
private static int specsIdx=0,numFrames,numPlanes;

// constructor for Analyze class
public Analyze(byte[] ba, File nFile, boolean createConfig)
{
    nf = new NORI();
    nf.setNORI(nFile);
    try
    {
        // Wraps byte array in litte-endian bytebuffer
        ByteBuffer bb = ByteBuffer.wrap(ba).order(ByteOrder.LITTLE_ENDIAN);
        // Analyze the file
        Analyzer a = new Analyzer(bb,nf,false);

        // make NORI config file
        if(createConfig)
        {
            nf.fixNORI(false);
            writeCfg();
            if(nf.hasPalette==1)
            {
                File palFile = new File(nf.dir+nf.name+"_pal.bin");
                Files.write(palFile.toPath(),nf.palBytes);
            }
        }
    }
    catch(Exception ex)
    {
        out.println("Error in (OptA):");
        ex.printStackTrace(System.out);
    }
}

// Prepare and write NORI config file
private static void writeCfg()
{
    try
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        // Set xml config document to global static var: cfg
        cfg = docBuilder.newDocument();
        // Root Element
        Element root = cfg.createElement("NORI");
        root.setAttribute("name",nf.name);
        cfg.appendChild(root);
        // NORI Header Elements
        Element noriHdr = cfg.createElement("NORI_HDR");
        root.appendChild(noriHdr);
        // NORI Header SubElements
        setNoriHdrVars(noriHdr);
        // GAWI Elements
        Element gawi = cfg.createElement("GAWI");
        root.appendChild(gawi);
        // GAWI Header Elements
        Element gawiHdr = cfg.createElement("GAWI_HDR");
        gawi.appendChild(gawiHdr);
        // NORI Header SubElements
        setGawiHdrVars(gawiHdr);
        // Palette Elements
        if(nf.hasPalette==1)
        {
            Element pal = cfg.createElement("PAL");
            gawi.appendChild(pal);
            setPaletteVars(pal);
        }
        // BMP Offset Elements
        for(int i=0; i < nf.bmpStructs; i++)
        {
            Element bmpOff = cfg.createElement(nf.xml_tag[31]);
            bmpOff.setAttribute("id",""+i);
            bmpOff.appendChild(cfg.createTextNode(""+nf.bmpOffsets[i]));
            gawi.appendChild(bmpOff);
        }
        // BMP Data Elements
        for(int i=0; i < nf.bmpStructs; i++)
        {
            Element bmp = cfg.createElement("BMP");
            bmp.setAttribute("id",""+i);
            bmp.setAttribute("offset",""+nf.bpos+"+"+nf.bmpOffsets[i]);
            gawi.appendChild(bmp);
            // BMP SubElements
            setBmpSpecs(bmp,i);
        }
        // Animation Offset Elements
        for(int a=0; a < nf.anims; a++)
        {
            Element animOff = cfg.createElement(nf.xml_tag[39]);
            animOff.setAttribute("id",""+a);
            animOff.appendChild(cfg.createTextNode(""+nf.animOffsets[a]));
            root.appendChild(animOff);
        }
        // Animation Data Elements
        for(int a=0; a < nf.anims; a++)
        {
            Element anim = cfg.createElement("ANIM");
            anim.setAttribute("id",""+a);
            anim.setAttribute("offset",""+nf.apos+"+"+nf.animOffsets[a]);
            root.appendChild(anim);
            // Anim SubElements
            Element name = cfg.createElement(nf.xml_tag[40]);
            name.appendChild(cfg.createTextNode(nf.title[a]));
            anim.appendChild(name);
            numFrames = nf.numFrames[a];
            mkSubE(anim, nf.xml_tag[41], numFrames);
            setFrameOffsets(anim,a);
            // Frame Data and SubElements
            setFrames(anim,a);
        }

        // Prep xml data
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = tf.newTransformer();
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        String indentAmount = "{http://xml.apache.org/xslt}indent-amount";
        t.setOutputProperty(indentAmount,"2");
        // Output xml config file
        DOMSource src = new DOMSource(cfg);
        File config = new File(nf.dir+nf.name+".cfg");
        StreamResult file = new StreamResult(config);
        t.transform(src, file);

        }catch(Exception ex)
        {
            out.println("Error in (mkCfg):");
            ex.printStackTrace(System.out);
        }
}

private static void setNoriHdrVars(Element e)
{
    mkSubE(e, nf.xml_tag[0], nf.fsig);
    mkSubE(e, nf.xml_tag[1], nf.noriVer);
    mkSubE(e, nf.xml_tag[2], nf.nParam1);
    mkSubE(e, nf.xml_tag[3], nf.nParam2);
    mkSubE(e, nf.xml_tag[4], nf.nParam3);
    mkSubE(e, nf.xml_tag[5], nf.nParam4);
    mkSubE(e, nf.xml_tag[6], nf.nParam5);
    mkSubE(e, nf.xml_tag[7], nf.anims);
    mkSubE(e, nf.xml_tag[8], nf.woGawi);
    mkSubE(e, nf.xml_tag[9], nf.fsize);
}

private static void setGawiHdrVars(Element e)
{
    mkSubE(e, nf.xml_tag[10], nf.gsig);
    mkSubE(e, nf.xml_tag[11], nf.gawiVer);
    mkSubE(e, nf.xml_tag[12], nf.bpp);
    mkSubE(e, nf.xml_tag[13], nf.compressed);
    mkSubE(e, nf.xml_tag[14], nf.hasPalette);
    mkSubE(e, nf.xml_tag[15], nf.gParam4);
    mkSubE(e, nf.xml_tag[16], nf.gParam5);
    mkSubE(e, nf.xml_tag[17], nf.gParam6);
    mkSubE(e, nf.xml_tag[18], nf.gParam7);
    mkSubE(e, nf.xml_tag[19], nf.bmpStructs);
    mkSubE(e, nf.xml_tag[20], nf.gsize);
}

private static void setPaletteVars(Element e)
{
    mkSubE(e, nf.xml_tag[21], nf.psig);
    mkSubE(e, nf.xml_tag[22], nf.palVer);
    mkSubE(e, nf.xml_tag[23], nf.pParam1);
    mkSubE(e, nf.xml_tag[24], nf.pParam2);
    mkSubE(e, nf.xml_tag[25], nf.pParam3);
    mkSubE(e, nf.xml_tag[26], nf.pParam4);
    mkSubE(e, nf.xml_tag[27], nf.divided);
    mkSubE(e, nf.xml_tag[28], nf.psize);
    mkSubE(e, "RGB24DATA", "");
    if(nf.psize==808)
    {
        mkSubE(e, nf.xml_tag[29], nf.mainS);
        mkSubE(e, nf.xml_tag[30], nf.mainE);
    }
}

private static void setBmpSpecs(Element bmp, int i)
{
    mkSubE(bmp, nf.xml_tag[32], nf.bmpCount[i]);
    boolean subBMP = (nf.bmpCount[i] > 1);
    for(int x=0; x < nf.bmpCount[i]; x++)
    {
        if(subBMP) mkSubE(bmp, String.format("SubBMP_%02d",x+1), "");
        mkSubE(bmp, nf.xml_tag[33], nf.bmpSpecs[specsIdx][0]);
        mkSubE(bmp, nf.xml_tag[34], nf.bmpSpecs[specsIdx][1]);
        mkSubE(bmp, nf.xml_tag[35], nf.bmpSpecs[specsIdx][2]);
        mkSubE(bmp, nf.xml_tag[36], nf.bmpSpecs[specsIdx][3]);
        mkSubE(bmp, nf.xml_tag[37], nf.bmpSpecs[specsIdx][4]);
        mkSubE(bmp, nf.xml_tag[38], nf.bmpSpecs[specsIdx][5]);
        mkSubE(bmp, "RGB"+nf.bpp+"DATA", "");
        specsIdx++;
    }
}

private static void setFrameOffsets(Element e, int a)
{
    // Frame Offset Elements
    for(int f=0; f < numFrames; f++)
    {
        Element frameOff = cfg.createElement(nf.xml_tag[42]);
        frameOff.setAttribute("id",""+f);
        frameOff.appendChild(cfg.createTextNode(""+nf.frameOffsets[a][f]));
        e.appendChild(frameOff);
    }
}

private static void setFrames(Element e, int a)
{
    for(int f=0; f < numFrames; f++)
    {
        // Frame element
        Element frame = cfg.createElement("FRAME");
        frame.setAttribute("id",""+f);
        frame.setAttribute("offset",""+nf.frameOffsets[a][f]);
        e.appendChild(frame);
        // FrameDataTop (duration,numPlanes)
        mkSubE(frame, nf.xml_tag[43], nf.frameDataTop[a][f][0]);
        mkSubE(frame, nf.xml_tag[44], nf.frameDataTop[a][f][1]);
        numPlanes = nf.frameDataTop[a][f][1];
        // PlaneData (bmp_id,x,y,opacity,flip,blend_mode,flag_param)
        setPlanes(frame,a,f);
        // FrameDataBottom
        setFrameDataBottom(frame,a,f);
    }
}

private static void setPlanes(Element e, int a, int f)
{
    for(int p=0; p < numPlanes; p++)
    {
        Element plane = cfg.createElement("PLANE");
        plane.setAttribute("id",""+p);
        e.appendChild(plane);
        mkSubE(plane, nf.xml_tag[45], nf.planeData[a][f][p][0]);
        mkSubE(plane, nf.xml_tag[46], nf.planeData[a][f][p][1]);
        mkSubE(plane, nf.xml_tag[47], nf.planeData[a][f][p][2]);
        mkSubE(plane, nf.xml_tag[48], nf.planeData[a][f][p][3]);
        mkSubE(plane, nf.xml_tag[49], nf.planeData[a][f][p][4]);
        mkSubE(plane, nf.xml_tag[50], nf.planeData[a][f][p][5]);
        mkSubE(plane, nf.xml_tag[51], nf.planeData[a][f][p][6]);
    }
}

// Much of the FrameDataBottom information is unknown. Therefore, as a temporary
// measure, much of it has been encoded & stored as base64 data.
// Once we know more about this section the base64 encoding can be replaced with
// proper data type vars.
private static void setFrameDataBottom(Element frame, int a, int f)
{
    if(nf.notV300)
    {
        mkSubE(frame, nf.xml_tag[52], nf.numCoordSets[a][f]);
        for(int i=0; i < nf.numCoordSets[a][f]; i++)
        {
            mkSubE(frame, nf.xml_tag[53], nf.coordSets[a][f][i][0]);
            mkSubE(frame, nf.xml_tag[54], nf.coordSets[a][f][i][1]);
        }
    }
    mkSubE(frame, nf.xml_tag[55], nf.cdBlockSize);
    if(nf.hasEB)
    {
        mkSubE(frame, nf.xml_tag[56], b64Enc(nf.entryBlocks[a][f][0]));
        mkSubE(frame, nf.xml_tag[56], b64Enc(nf.entryBlocks[a][f][1]));
        mkSubE(frame, nf.xml_tag[56], b64Enc(nf.entryBlocks[a][f][2]));
        mkSubE(frame, nf.xml_tag[56], b64Enc(nf.entryBlocks[a][f][3]));
        mkSubE(frame, nf.xml_tag[56], b64Enc(nf.entryBlocks[a][f][4]));
        mkSubE(frame, nf.xml_tag[56], b64Enc(nf.entryBlocks[a][f][5]));
    }
    mkSubE(frame, nf.xml_tag[57], b64Enc(nf.unknownData1[a][f][0]));
    mkSubE(frame, nf.xml_tag[57], b64Enc(nf.unknownData1[a][f][1]));
    mkSubE(frame, nf.xml_tag[58], nf.soundEffect[a][f]);
    mkSubE(frame, nf.xml_tag[59], b64Enc(nf.unknownData2[a][f]));
    if(nf.maybeMCV)
    {
        mkSubE(frame, nf.xml_tag[60], nf.hasMCValues[a][f]);
        if(nf.hasMCValues[a][f]==1)
        {
            mkSubE(frame, nf.xml_tag[61], nf.mcValues[a][f][0]);
            mkSubE(frame, nf.xml_tag[62], nf.mcValues[a][f][1]);
            mkSubE(frame, nf.xml_tag[63], nf.mcValues[a][f][2]);
            mkSubE(frame, nf.xml_tag[64], nf.mcValues[a][f][3]);
            mkSubE(frame, nf.xml_tag[65], nf.mcValues[a][f][4]);
            mkSubE(frame, nf.xml_tag[66], nf.mcValues[a][f][5]);
            mkSubE(frame, nf.xml_tag[67], nf.mcValues[a][f][6]);
            mkSubE(frame, nf.xml_tag[68], nf.mcParam7[a][f]);
            mkSubE(frame, nf.xml_tag[69], b64Enc(nf.mcParam8[a][f]));
        }
    }
}

// Make Element child (Element's Element)
private static void mkSubE(Element e, String name, String value)
{
    Element subE = cfg.createElement(name);
    subE.appendChild(cfg.createTextNode(""+value));
    e.appendChild(subE);
}

// Make Element child (Element's Element)
private static void mkSubE(Element e, String name, int value)
{
    Element subE = cfg.createElement(name);
    subE.appendChild(cfg.createTextNode(""+value));
    e.appendChild(subE);
}

// Shorten the new base64 encoded string from byte array command
private static String b64Enc(byte[] ba)
{
    return Base64.getEncoder().encodeToString(ba);
}
}
