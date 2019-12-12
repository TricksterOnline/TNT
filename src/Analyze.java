/*
Analyze.java: this file is part of the TNT program.

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

    // constructor for Analyze class
    public Analyze(byte[] ba, NORI nf, boolean createConfig)
    {
        try
        {
            // Wraps byte array in litte-endian bytebuffer
            ByteBuffer bb = ByteBuffer.wrap(ba).order(ByteOrder.LITTLE_ENDIAN);
            // Analyze the file
            Analyzer a = new Analyzer(bb,nf);

            // make NORI config file
            if(createConfig)
            {
                writeCfg(nf);
                File xfbFile = new File(nf.dir+"xfb"+nf.noriVer+".bin");
                Files.write(xfbFile.toPath(),nf.xfb);
                if(nf.hasPalette==1)
                {
                    File palFile = new File(nf.dir+nf.name+"_pal.bin");
                    Files.write(palFile.toPath(),nf.palBytes);
                }
            }
        }
        catch(Exception ex)
        {
            out.println("Error in (OptA):\n"+ex);
        }
    }

    // Prepare and write NORI config file
    private static void writeCfg(NORI nf)
    {
        try
        {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbf.newDocumentBuilder();
            Document cfg = docBuilder.newDocument();
            // Root Element
            Element root = cfg.createElement("NORI");
            root.setAttribute("name",nf.name);
            cfg.appendChild(root);
            // NORI Header Elements
            Element noriHdr = cfg.createElement("NORI_HDR");
            root.appendChild(noriHdr);
            // NORI Header SubElements
            setNoriHdrVars(cfg,noriHdr,nf);
            // GAWI Elements
            Element gawi = cfg.createElement("GAWI");
            root.appendChild(gawi);
            // GAWI Header Elements
            Element gawiHdr = cfg.createElement("GAWI_HDR");
            gawi.appendChild(gawiHdr);
            // NORI Header SubElements
            setGawiHdrVars(cfg,gawiHdr,nf);
            // Palette Elements
            if(nf.hasPalette==1)
            {
                Element pal = cfg.createElement("PAL");
                gawi.appendChild(pal);
                setPaletteVars(cfg,pal,nf);
            }
            // BMP Offset Elements
            for(int i=0; i < nf.numBMP; i++)
            {
                Element bmpOff = cfg.createElement("bmpOffset");
                bmpOff.setAttribute("id",""+i);
                bmpOff.appendChild(cfg.createTextNode(""+nf.bmpOffsets[i]));
                gawi.appendChild(bmpOff);
            }
            // BMP Data Elements
            for(int i=0; i < nf.numBMP; i++)
            {
                Element bmp = cfg.createElement("BMP");
                bmp.setAttribute("id",""+i);
                bmp.setAttribute("offset",""+nf.bmpOffsets[i]+"+"+nf.bpos);
                gawi.appendChild(bmp);
                // BMP SubElements
                setBmpSpecs(cfg,bmp,i,nf);
                Element bmpData = cfg.createElement("RGB"+nf.bpp+"DATA");
                bmp.appendChild(bmpData);
            }
            // Animation Offset Elements
            for(int i=0; i < nf.anims; i++)
            {
                Element animOff = cfg.createElement("animOffset");
                animOff.setAttribute("id",""+i);
                animOff.appendChild(cfg.createTextNode(""+nf.animOffsets[i]));
                root.appendChild(animOff);
            }
            // Animation Data Elements
            for(int i=0; i < nf.anims; i++)
            {
                Element anim = cfg.createElement("ANIM");
                anim.setAttribute("id",""+i);
                anim.setAttribute("offset",""+nf.animOffsets[i]+"+"+nf.apos);
                root.appendChild(anim);
                // Anim SubElements
                Element name = cfg.createElement("name");
                name.appendChild(cfg.createTextNode(nf.animName[i]));
                anim.appendChild(name);
                mkSubElement(cfg,anim,"frames",nf.frames[i]);
                setFrameOffsets(cfg,anim,i,nf);
                // Frame Data and SubElements
                setFrames(cfg,anim,i,nf);
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
        }
        catch(Exception ex)
        {
            out.println("Error in (mkCfg):\n"+ex);
        }
    }

    private static void setNoriHdrVars(Document cfg, Element e, NORI nf)
    {
        mkSubElement(cfg, e, "fsig", nf.fsig);
        mkSubElement(cfg, e, "noriver", nf.noriVer);
        mkSubElement(cfg, e, "nparam1", nf.nParam1);
        mkSubElement(cfg, e, "nparam2", nf.nParam2);
        mkSubElement(cfg, e, "nparam3", nf.nParam3);
        mkSubElement(cfg, e, "nparam4", nf.nParam4);
        mkSubElement(cfg, e, "nparam5", nf.nParam5);
        mkSubElement(cfg, e, "anims", nf.anims);
        mkSubElement(cfg, e, "woGawi", nf.woGawi);
        mkSubElement(cfg, e, "fsize", nf.fsize);
    }

    private static void setGawiHdrVars(Document cfg, Element e, NORI nf)
    {
        mkSubElement(cfg, e, "gsig", nf.gsig);
        mkSubElement(cfg, e, "gawiver", nf.gawiVer);
        mkSubElement(cfg, e, "bpp", nf.bpp);
        mkSubElement(cfg, e, "compressed", nf.compressed);
        mkSubElement(cfg, e, "hasPalette", nf.hasPalette);
        mkSubElement(cfg, e, "gparam4", nf.gParam4);
        mkSubElement(cfg, e, "gparam5", nf.gParam5);
        mkSubElement(cfg, e, "gparam6", nf.gParam6);
        mkSubElement(cfg, e, "gparam7", nf.gParam7);
        mkSubElement(cfg, e, "numBMP", nf.numBMP);
        mkSubElement(cfg, e, "gsize", nf.gsize);
    }

    private static void setPaletteVars(Document cfg, Element e, NORI nf)
    {
        mkSubElement(cfg, e, "psig", nf.psig);
        mkSubElement(cfg, e, "palver", nf.palVer);
        mkSubElement(cfg, e, "pparam1", nf.pParam1);
        mkSubElement(cfg, e, "pparam2", nf.pParam2);
        mkSubElement(cfg, e, "pparam3", nf.pParam3);
        mkSubElement(cfg, e, "pparam4", nf.pParam4);
        mkSubElement(cfg, e, "divided", nf.divided);
        mkSubElement(cfg, e, "psize", nf.psize);
        Element palData = cfg.createElement("RGB24DATA");
        e.appendChild(palData);
        if(nf.psize==808)
        {
            mkSubElement(cfg,e,"mainS",nf.mainS);
            mkSubElement(cfg,e,"mainE",nf.mainE);
        }
    }

    private static void setBmpSpecs(Document cfg, Element e, int i, NORI nf)
    {
        mkSubElement(cfg, e, "dcount", nf.bmpSpecs[i][0]);
        mkSubElement(cfg, e, "dlen", nf.bmpSpecs[i][1]);
        mkSubElement(cfg, e, "w", nf.bmpSpecs[i][2]);
        mkSubElement(cfg, e, "h", nf.bmpSpecs[i][3]);
        mkSubElement(cfg, e, "bparam4", nf.bmpSpecs[i][4]);
        mkSubElement(cfg, e, "pos_x", nf.bmpSpecs[i][5]);
        mkSubElement(cfg, e, "pos_y", nf.bmpSpecs[i][6]);
    }

    private static void setFrameOffsets(Document cfg,Element e,int a,NORI nf)
    {
        // Frame Offset Elements
        for(int i=0; i < nf.frames[a]; i++)
        {
            Element frameOff = cfg.createElement("frameOffset");
            frameOff.setAttribute("id",""+i);
            frameOff.appendChild(cfg.createTextNode(""+nf.frameOffsets[a][i]));
            e.appendChild(frameOff);
        }
    }

    private static void setFrames(Document cfg,Element e,int a,NORI nf)
    {
        // Frame Offset Elements
        for(int i=0; i < nf.frames[a]; i++)
        {
            Element frame = cfg.createElement("frame");
            frame.setAttribute("id",""+i);
            frame.setAttribute("offset",""+nf.frameOffsets[a][i]);
            e.appendChild(frame);
            mkSubElement(cfg,frame,"delay",nf.frameData[a][i][0]);
            mkSubElement(cfg,frame,"planes",nf.frameData[a][i][1]);
            setPlanes(cfg,frame,a,i,nf);
            mkSubElement(cfg,frame,"xfb",nf.xtraFrameBytes);
        }
    }

    private static void setPlanes(Document cfg,Element e,int a,int f, NORI nf)
    {
        for(int i=0; i < nf.frameData[a][f][1]; i++)
        {
            Element plane = cfg.createElement("plane");
            plane.setAttribute("id",""+i);
            e.appendChild(plane);
            mkSubElement(cfg,plane,"bmp_id", nf.planeData[a][f][i][0]);
            mkSubElement(cfg,plane,"point_x", nf.planeData[a][f][i][1]);
            mkSubElement(cfg,plane,"point_y", nf.planeData[a][f][i][2]);
            mkSubElement(cfg,plane,"opacity", nf.planeData[a][f][i][3]);
            mkSubElement(cfg,plane,"flip_axis", nf.planeData[a][f][i][4]);
            mkSubElement(cfg,plane,"blend_mode", nf.planeData[a][f][i][5]);
            mkSubElement(cfg,plane,"flag_param", nf.planeData[a][f][i][6]);
        }
    }

    // Make Element child (Element's Element)
    private static void mkSubElement(Document cfg,Element e,String name,int val)
    {
        Element subE = cfg.createElement(name);
        subE.appendChild(cfg.createTextNode(""+val));
        e.appendChild(subE);
    }
}
