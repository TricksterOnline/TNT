/*
Extract.java: this file is part of the TNT program.

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
    public static int pos=0,dlen=0,w=0,h=0,nlen=0,dcount=0,bmpNxt=0;
    public static boolean compressed=false, dcBool=false;
    public static byte x00=(byte)0,xFF=(byte)255,x1F=(byte)31,x7C=(byte)124;

    // constructor for Extract class
    public Extract(byte[] ba, NORI nf, boolean subs)
    {
        try
        {
            ByteBuffer bb = mkLEBB(ba);
            // Analyze and assign class vars
            Analyzer a = new Analyzer(bb,nf);
            compressed = a.compressed;

            // Make the directory where we will extract the bmp to
            File d = new File(nf.exdir);
            Files.createDirectories(d.toPath());

            // Initialize Java Bitmap Library
            JBL bl = new JBL();
            bl.setFileVars(nf.exdir,nf.name);
            bl.set16BitFmtIn("RGB555");
            nlen = bl.setImgSetSize(nf.numBMP);
            bl.setPalette(nf.palette);

            // Extract the images
            out.println("Extracting Bitmaps...");
            for(int i=0; i < nf.numBMP; i++)
            {
                if(nf.bmpOffsets[i+1]!=0)
                    bmpNxt=nf.bmpOffsets[i+1] + nf.bpos;
                else
                    bmpNxt=nf.bmpOffsets[i+1];
                // get data count (if larger than 1, subset exists)
                dcount = bb.getInt();
                dcBool = (dcount >1);
                for(int x=1; x <= dcount; x++)
                {
                    // get/set the standard info about the bmp
                    setBitmapData(bb);
                    bl.setBmpVars(w,h,nf.bpp);
                    // Next 4 lines: get img data, prep pixel data, & prep BMP
                    byte[] rawBytes = bl.getImgBytes(bb,dlen);
                    byte[] bytes = decompressor(rawBytes,nf);
                    byte[] pixels = bl.toStdRGB(bytes);
                    // Ntree* uses top-down bmp scanlines in the NORI format
                    byte[] bmp = bl.setBMP(bl.reverseRows(pixels),false);
                    // Write the new BMP into existence
                    if(dcBool && subs)
                        bl.makeBMP(bmp,i+1,String.format("_%02d",x));
                    else
                        bl.makeBMP(bmp,i+1,"");
                }
                pos = bb.position();
                // Ensure the buffer is in the right position for the next bmp
                if(pos!=bmpNxt && bmpNxt!=0) bb.position(bmpNxt);
            }
            out.println("Extraction Complete.\n");
        }
        catch(Exception ex)
        {
            out.println("Error in (EM):\n"+ex);
        }
    }

    // Assign the BitmapData header info
    private static void setBitmapData(ByteBuffer bb)
    {
        // assign: data length, width, height; then temporarily assign unknowns
        dlen = bb.getInt();
        w = bb.getInt();
        h = bb.getInt();
        int bParam4 = bb.getInt();
        int pos_x = bb.getInt();
        int pos_y = bb.getInt();
    }

    // Minor interface for decompress() to make code cleaner
    private static byte[] decompressor(byte[] rawBytes, NORI nf)
    {
        if(compressed)
            return decompress(rawBytes,nf);
        else
            return rawBytes;
    }

    // Custom Run-length Encoding Decompression function.
    // Each scanline is defined by a encodedSize, then a cycle of background and
    // foreground pixel data that is repeated until the encodedSize is met.
    private static byte[] decompress(byte[] input, NORI nf)
    {
        // Initialize vars: encodedSize, bg pixels, fg pixels, Bytes/px, fg*Bpp
        int encodedSize=0, bg=0, fg=0, Bpp=(nf.bpp/8), fgxBpp=0;
        byte[] output = new byte[w*h*Bpp], bg1= {x1F,x7C}, bg2= {xFF,x00,xFF};
        // Create bytebuffers for the input and output arrays
        ByteBuffer bi = mkLEBB(input), bo = mkLEBB(output);

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
        return output;
    }

    // Shorthand function to wrap a byte array in a little-endian bytebuffer
    private static ByteBuffer mkLEBB(byte[] ba)
    {
        return ByteBuffer.wrap(ba).order(ByteOrder.LITTLE_ENDIAN);
    }
}
