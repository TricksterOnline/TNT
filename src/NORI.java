/*
NORI.java: this file is part of the TNT program.

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
import static java.lang.System.out;
/**
Class Description:
A structure-like class that represents and stores a NORI file's data neatly.

Dev Notes:
Since this class is essentially a structure, I'm going to attempt to keep it as
simple and clean as possible.

Development Priority: HIGH
*/
public class NORI
{
// class variables
public String name,dname,dir,exdir;
public static String fs=File.separator;
public static String[] xml_tag;

public static byte[] sfx = new byte[18];
// Special variables for modifying NORI data
public int totalBMP,totalFrames,totalPlanes;
public int asize;//# of animOffsets+animations bytes
public int fdbSizeSum;
public int ebSize;//# of entryblocks bytes per frame
public int totalCoordSetsBytes;
public int mcSizeSum;
// Special NORI variables
public int fsig=1230131022;//file signature
public int noriVer;
public int nParam1;
public int nParam2;
public int nParam3;
public int nParam4;
public int nParam5;
public int anims;
public int woGawi;
public int fsize;//file size
// Special GAWI variables
public int gsig=1230455111;//GAWI section signature
public int gawiVer=300;
public int bpp,Bpp;
public int compressed;
public int hasPalette;
public int gParam4;
public int gParam5;
public int gParam6;
public int gParam7;
public int bmpStructs,nLen;
public int gsize;//GAWI section size
// Special PAL variables
public int psig=1598832976;//palette section signature
public int palVer=100;
public int pParam1;
public int pParam2;
public int pParam3;
public int pParam4;
public int divided;
public int psize;//palette section size
public byte[] palBytes;
public byte[][] palette;
public int mainS = 111;
public int mainE = 254;
// Special BMP data variables
public int[] bmpOffsets;
public int bpos;
public int[] bmpCount;
public int[][] bmpSpecs;
public byte[] bmpData;
public int bmpDataSize;
// Special animation variables
public int[] animOffsets;
public int apos;
public byte[][] titleBytes;//[anims][32];
public String[] title;
public int[] numFrames;
public int[][] frameOffsets;
public int[][][] frameDataTop;
public int[][][][] planeData;
// Special FrameDataBottom variables
public boolean notV300=false;
public int[][] numCoordSets;
public int[][][][] coordSets;
public int cdBlockSize;
public boolean hasEB=false;
public byte[][][][] entryBlocks;
public byte[][][][] unknownData1;
public String[][] soundEffect;
public byte[][][] unknownData2;
public boolean maybeMCV=false;
public int[][] hasMCValues;
public int[][][] mcValues;//mcParam[0-6]
public String[][] mcParam7;
public byte[][][] mcParam8;

// constructor for NORI class
public NORI()
{
    setXmlTags();
}
public static void setXmlTags()
{
    xml_tag = new String[70];
    xml_tag[0]  = "fsig";
    xml_tag[1]  = "noriVer";
    xml_tag[2]  = "nParam1";
    xml_tag[3]  = "nParam2";
    xml_tag[4]  = "nParam3";
    xml_tag[5]  = "nParam4";
    xml_tag[6]  = "nParam5";
    xml_tag[7]  = "anims";
    xml_tag[8]  = "woGawi";
    xml_tag[9]  = "fsize";
    xml_tag[10] = "gsig";
    xml_tag[11] = "gawiVer";
    xml_tag[12] = "bpp";
    xml_tag[13] = "compressed";
    xml_tag[14] = "hasPalette";
    xml_tag[15] = "gParam4";
    xml_tag[16] = "gParam5";
    xml_tag[17] = "gParam6";
    xml_tag[18] = "gParam7";
    xml_tag[19] = "bmpStructs";
    xml_tag[20] = "gsize";
    xml_tag[21] = "psig";
    xml_tag[22] = "palVer";
    xml_tag[23] = "pParam1";
    xml_tag[24] = "pParam2";
    xml_tag[25] = "pParam3";
    xml_tag[26] = "pParam4";
    xml_tag[27] = "divided";
    xml_tag[28] = "psize";
    xml_tag[29] = "mainS";
    xml_tag[30] = "mainE";
    xml_tag[31] = "bmpOffset";
    xml_tag[32] = "bmp_count";
    xml_tag[33] = "bmp_size";
    xml_tag[34] = "w";
    xml_tag[35] = "h";
    xml_tag[36] = "bParam4";
    xml_tag[37] = "bmp_x";
    xml_tag[38] = "bmp_y";
    xml_tag[39] = "animOffset";
    xml_tag[40] = "title";
    xml_tag[41] = "numFrames";
    xml_tag[42] = "frameOffset";
    xml_tag[43] = "duration";
    xml_tag[44] = "numPlanes";
    xml_tag[45] = "bmp_id";
    xml_tag[46] = "plane_x";
    xml_tag[47] = "plane_y";
    xml_tag[48] = "opacity";
    xml_tag[49] = "flip";
    xml_tag[50] = "blend_mode";
    xml_tag[51] = "flag_param";
    xml_tag[52] = "coordSets";
    xml_tag[53] = "coord_x";
    xml_tag[54] = "coord_y";
    xml_tag[55] = "cdBlockSize";
    xml_tag[56] = "entryBlock";
    xml_tag[57] = "UnknownData1";
    xml_tag[58] = "soundEffect";
    xml_tag[59] = "UnknownData2";
    xml_tag[60] = "hasMCValues";
    xml_tag[61] = "mcParam0";
    xml_tag[62] = "mcParam1";
    xml_tag[63] = "mcParam2";
    xml_tag[64] = "mcParam3";
    xml_tag[65] = "mcParam4";
    xml_tag[66] = "mcParam5";
    xml_tag[67] = "mcParam6";
    xml_tag[68] = "mcParam7";
    xml_tag[69] = "mcParam8";
}

public void setNORI(File nFile)
{
    name = nFile.getName();// Plain file name
    setDir(nFile);
    setExDir();
}
public void setDir(File nFile)
{
    dir = nFile.getParent()+fs;// Directory the file is in
    if(dir.equals("null"+fs)) dir=System.getProperty("user.dir")+fs;
}
public void setExDir()
{
    dname = name.replace('.','_');// Name without dots (useful)
    exdir = dir+dname+fs;// Extraction directory (where bmp go)
}

// Sets noriVer-specific variables
public void setVerSpecific()
{
    int nv = noriVer-300;
    if(nv==0||nv==1||nv==2||nv==3)
    {
        notV300 = (nv!=0);
        switch(nv)
        {
        case 0:
        case 1:
            cdBlockSize = 144;
            break;
        case 3:
            maybeMCV = true;
        case 2:
            cdBlockSize = 96;
            hasEB = true;
            ebSize = 168;
            break;
        }
    }
    else
    {
        out.println("Unknown NORI Version!");
        System.exit(1);
    }
}

/*########################################################################*/
/*######################## FIXER FUNCTIONS BELOW #########################*/

// Size and Flag fixes
public void fixNORI(boolean create_mode)
{
    setVerSpecific();
    if(create_mode) fixAnimSize();
    if(bpp==8) fixPAL();
    fixGawiHeader(create_mode);
    fixNoriHeader(create_mode);
}

private void fixAnimSize()
{
    int sfxLen = 18;
    int fdbStaticSizes = (cdBlockSize+ebSize+44+sfxLen+18);
    fdbSizeSum = (totalFrames*fdbStaticSizes)+totalCoordSetsBytes;
    if(maybeMCV) fdbSizeSum += (totalFrames*4)+mcSizeSum;
    asize = (40*anims)+(12*totalFrames)+(28*totalPlanes)+fdbSizeSum;
}
private void fixPAL()
{
    if(psize==808) divided=1;
}
private void fixGawiHeader(boolean create_mode)
{
    if(create_mode)
    {
        gsize = 44;
        compressed = 0;
        hasPalette = 0;
        if(bpp==8)
        {
            hasPalette = 1;
            gsize += psize;
        }

        gsize += (8*bmpStructs)+(24*totalBMP)+bmpDataSize;
    }
    else if(gsize==0)
    {
        gsize = fsize-40-asize;
    }
}
private void fixNoriHeader(boolean create_mode)
{
    woGawi = 40+asize;
    if(create_mode) fsize = gsize+woGawi;
}
}
