/*
Extract.java: this file is part of the TNT program.

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
import static java.lang.System.out;
/**
Class Description:
The Extract class contains the functions which do the actual extraction of the
NORI files. They are given their own class due to their importance.

Dev Notes:
Anything regarding extraction output should happen here. Extra manipulation of
the output file should not be necessary after the Extract class has been used.
The buck stops here.

Development Priority: HIGHEST
*/
public class Extract
{
// class variables
private static NORI nf;
private static ByteBuffer bb;
private static int pos=0,w=0,h=0,bmpCount=0;
private static byte x00=(byte)0,xFF=(byte)255,x1F=(byte)31,x7C=(byte)124;
private static byte[] rawBytes,pixels,bmp;

// constructor for Extract class
public Extract(byte[] ba, File nFile)
{
    nf = new NORI();
    nf.setNORI(nFile);
    bb = mkLEBB(ba);
    try
    {
        // Analyze and assign NORI vars
        Analyzer a = new Analyzer(bb,nf,true);

        // Make the directory where we will extract the bmp to
        Files.createDirectories((new File(nf.exdir)).toPath());

        // Initialize Java Bitmap Library
        JBL bl = new JBL();
        bl.setFileVars(nf.exdir,nf.name);
        bl.set16BitFmtIn("RGB555");
        bl.setNumLength(nf.nLen);
        bl.setPalette(nf.palette);

        // Extract the images
        out.println("Extracting Bitmaps...");
        boolean isSub=false;
        for(int i=0; i < nf.bmpStructs; i++)
        {
            // get/set bmp count (if larger than 1, subset exists)
            bmpCount = bb.getInt();
            isSub = (bmpCount >1);
            for(int x=1,dataLength; x <= bmpCount; x++)
            {
                // get/set the standard info about the bmp
                dataLength = bb.getInt();
                w = bb.getInt();
                h = bb.getInt();
                bb.position(bb.position()+12);//skip bParam4,bmp_x,bmp_y
                bl.setBitmapVars(w,h,nf.bpp);
                // Get image data & turn data into proper scanlines
                rawBytes = bl.getImgBytes(bb,dataLength);
                pixels   = bl.toStdRGB(decompressor(rawBytes));
                // Ntree* uses top-down bmp scanlines in the NORI format
                bmp = bl.setBMP(bl.reverseRows(pixels),false);
                // Write the new BMP into existence
                if(isSub)
                    bl.makeBMP(bmp,i,String.format("_%02d",x));
                else
                    bl.makeBMP(bmp,i,"");
            }
        }
        out.println("Extraction Complete.\n");
    }
    catch(Exception ex)
    {
        out.println("Error in (EM):");
        ex.printStackTrace(System.out);
    }
}

// Minor interface for decompress() to make code cleaner
private static byte[] decompressor(byte[] bmpData)
{
    if(nf.compressed==1)
        return decompress(bmpData);
    else
        return bmpData;
}

// Custom Run-length Encoding Decompression function.
// Each scanline is defined by a encodedSize, then a cycle of background and
// foreground pixel data that is repeated until the encodedSize is met.
private static byte[] decompress(byte[] input)
{
    byte[] output = new byte[w*h*nf.Bpp], bg1= {x1F,x7C}, bg2= {xFF,x00,xFF};
    // Create bytebuffers for the input and output arrays
    ByteBuffer bi = mkLEBB(input), bo = mkLEBB(output);

    for(int i=0,encodedSize,bg,fg,fgxBpp; i < h; i++)
    {
        // set the encodedSize, then subtract 2, since it includes itself
        encodedSize = (int)bi.getShort()-2;
        while(encodedSize > 0)
        {
            // Get the encoded scanline internal parameters
            bg =(int)bi.getShort();
            fg =(int)bi.getShort();
            fgxBpp = fg*nf.Bpp;
            // Get foreground pixel data for the scanline
            byte[] fgData = new byte[fgxBpp];
            bi.get(fgData,0,fgxBpp);
            // Set background pixels for scanline
            for(int x=0; x < bg; x++)
            {
                if(nf.Bpp==2)
                    bo.put(bg1,0,2);
                else if(nf.Bpp==3)
                    bo.put(bg2,0,3);
                else
                    bo.put(x00);
            }
            // Set foreground pixels for scanline
            for(int y=0; y < fgxBpp; y++)
            {
                bo.put(fgData[y]);
            }
            // Subtract the bytes for the fg & bg vars, and fgData
            encodedSize -= 4+fgxBpp;
        }
    }
    return output;
}

// Shorthand function to wrap a byte array in a little-endian bytebuffer
private static ByteBuffer mkLEBB(byte[] ba)
{
    return ByteBuffer.wrap(ba).order(ByteOrder.LITTLE_ENDIAN);
}
}
