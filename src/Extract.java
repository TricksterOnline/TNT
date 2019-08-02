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
    // class variables
    public static int nBMP=0, bpp=0, pos=0, dlen=0, w=0, h=0, nlen=0, dcount=0;
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
                out.printf("#%0"+nlen+"d, offset: %d\n",n,bmpNow);
                // get data count (determines if subset exists)
                dcount = fbb.getInt();
                for(int x=1; x <= dcount; x++)
                {
                    // get/set the standard info about the bmp
                    setBitmapData(fbb);
                    bl.setJBLVars(dir,name,w,h,bpp);
                    // Next 4 lines: get img data, convert to BMP24, & prep BMP
                    byte[] rawBytes = bl.getImgBytes(fbb,dlen);
                    byte[] bytes = decompressor(rawBytes);
                    byte[] pixels = bl.toStdRGB(bytes);
                    byte[] bmp = bl.setBMP(pixels,true);
                    // Write the new BMP into existence
                    if(dcount >1)
                    {
                        String subName = String.format("_%02d",x);
                        bl.makeBMP(bmp,n,subName);
                    }
                    else
                    {
                        bl.makeBMP(bmp,n,"");
                    }
                }
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
        // assign: data length, width, height; then skip unknowns
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
        // Initialize vars: encodedSize, bg pixels, fg pixels, Bytes/px, fg*Bpp
        int encodedSize=0, bg=0, fg=0, Bpp=(bpp/8), fgxBpp=0;
        byte x00=(byte)0, x1F=(byte)31, x7C=(byte)124, xFF=(byte)255;
        byte[] result = new byte[w*h*Bpp], bg1= {x1F,x7C}, bg2= {xFF,x00,xFF};
        // Create bytebuffers for the input and output arrays
        ByteBuffer bi = ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer bo = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
        for(int i=0; i < h; i++)
        {
            // set the encodedSize, then subtract 2, since it includes itself
            encodedSize = (int)bi.getShort()-2;
            while(encodedSize > 0)
            {
                // Get the encoded scanline internal parameters
                bg =(int)bi.getShort();
                fg =(int)bi.getShort();
                fgxBpp = fg*Bpp;
                // Get foreground pixel data for the scanline
                byte[] fgData = new byte[fgxBpp];
                bi.get(fgData,0,fgxBpp);
                // Set background pixels for scanline
                for(int x=0; x < bg; x++)
                {
                    if(Bpp==2)
                        bo.put(bg1,0,2);
                    else if(Bpp==3)
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
        return result;
    }

    // Runs Analyzer & passes vars to local globals.
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
