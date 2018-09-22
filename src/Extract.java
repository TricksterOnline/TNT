/*
Extract.java: this file is part of the TNT program.

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
import java.nio.file.*;
import static java.lang.System.in;
import static java.lang.System.out;
import static java.lang.System.err;
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
    // class variables (some of which I don't need, others I wish I didn't)
    public static int nBMP=0, bpp=0, pos=0, dlen=0, w=0, h=0, nlen=0;
    public static boolean compressed=false;
    public static byte[][] pal;
    public static int[] offsets;
    public static String name, dir, dirname;

    // constructor for Extract class
    public Extract(byte[] ba, File file)
    {
        try
        {
            name = file.getName();
            dirname = name.replace('.','_');
            // Wraps byte array in little-endian bytebuffer
            ByteBuffer fbb = ByteBuffer.wrap(ba).order(ByteOrder.LITTLE_ENDIAN);

            // Analyze and assign class vars
            runAnalyzer(fbb);

            // Make the directory where we will extract the bmp to
            dir = file.getParent()+File.separator+dirname+File.separator;
            File d = new File(dir);
            Files.createDirectories(d.toPath());

            // Initialize Java Bitmap Library
            JBL bl = new JBL();
            bl.setBitFormat("RGB555");
            nlen = bl.setImgSetSize(nBMP);
            bl.setPalette(pal);

            // Extract the images
            out.println("Extracting Bitmaps:");
            for(int i=0; i < nBMP; i++)
            {
                int n=i+1, bmpNow=offsets[i], bmpNxt=offsets[i+1];
                // get/set the standard info about the bmp
                setBitmapData(fbb);
                bl.setJBLVars(dir,name,w,h,bpp);
                out.printf("#%0"+nlen+"d, offset: %d\n",n,bmpNow);
                // Next 4 lines: get img data, convert to BMP24, & prepare BMP
                byte[] rawBytes = bl.getImgBytes(fbb,dlen);
                byte[] bytes = decompressor(rawBytes);
                byte[] pixels = bl.toStdRGB(bytes);
                byte[] bmp = bl.setBMP(pixels,true);
                // Write the new BMP into existence
                bl.makeBMP(bmp,n);
                // Ensure the buffer is in the right position for the next bmp
                if(pos!=bmpNxt && bmpNxt!=0) fbb.position(bmpNxt);
            }
        }
        catch(Exception ex)
        {
            out.println("Something donked up (EM):\n"+ex);
        }
    }

    // Assign the BitmapData header info
    public static void setBitmapData(ByteBuffer bb)
    {
        // assign: flag, data length, width, height; then skip unknowns
        int flag = bb.getInt();
        dlen = bb.getInt();
        w = bb.getInt();
        h = bb.getInt();
        pos = bb.position()+12;
        bb.position(pos);
    }

    // Minor interface for decompress() to make code cleaner
    public static byte[] decompressor(byte[] rawBytes)
    {
        if(compressed)
            return decompress(rawBytes);
        else
            return rawBytes;
    }

    // Custom Run-length Encoding Decompression function.
    // Each scanline is defined by a encodedSize, and a cycle of background and
    // foreground pixel data that is repeated until the encodedSize is met.
    public static byte[] decompress(byte[] input)
    {
        // Initialize the vars: encodedSize, bg pixels, fg pixels, Bytes / pixel
        int encodedSize=0, bg=0, fg=0, Bpp=(bpp/8);
        byte[] result = new byte[w*h*Bpp];
        // Create bytebuffers for the input and output arrays
        ByteBuffer bi = ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer bo = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
        for(int i=0; i < h; i++)
        {
            // set the encodedSize, then subtract 2, since it includes itself
            encodedSize = (int)bi.getShort()-2;
            while(encodedSize > 0)
            {
                bg = (int)bi.getShort();
                fg = (int)bi.getShort();
                byte[] fgData = new byte[fg*Bpp];
                bi.get(fgData,0,fg*Bpp);
                for(int x=0; x < bg; x++)
                {
                    if(Bpp==3)
                    {
                        bo.put((byte)0xFF);
                        bo.put((byte)0x00);
                        bo.put((byte)0xFF);
                    }
                    else if(Bpp==2)
                    {
                        bo.put((byte)0x1F);
                        bo.put((byte)0x7C);
                    }
                    else
                        bo.put((byte)0x00);
                }
                for(int y=0; y < fg*Bpp; y++)
                {
                    bo.put(fgData[y]);
                }
                // Subtract the bytes for the fg & bg vars, and fgData
                encodedSize -= 4+(fg*Bpp);
            }
        }
        return result;
    }

    // Dirty little function I created b/c I didn't have time to waste figuring
    // out how to make Analyzer's instance vars available to all the functions
    // in this class without handing them out one by one as parameters inside
    // the Extract constructor. Runs Analyzer & passes vars to local globals.
    public static void runAnalyzer(ByteBuffer bb)
    {
        // Analyze the file
        Analyzer a = new Analyzer(bb, name);

        // Load the necessary vars
        nBMP = a.numBMP;
        bpp = a.bmpBitDepth;
        pos = a.pos;
        compressed = a.compressed;
        pal = a.palette;
        offsets = a.bmpOffsets;
    }
}
