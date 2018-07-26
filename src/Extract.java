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
import java.nio.file.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
    public static boolean compressed=false, hasPal=false;
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

            // Extract the images
            out.println("Extracting Bitmaps:");
            for(int i=0; i < nBMP; i++)
            {
                int n=i+1, bmpNow=offsets[i], bmpNxt=offsets[i+1];
                // get the standard info about the bmp
                setBitmapData(fbb);
                out.printf("#%0"+nlen+"d, offset: %d\n",n,bmpNow);
                // Next 2 lines: get the image/pixel data & convert to BMP24 fmt
                byte[] bgr24 = getImgBytes(fbb);
                byte[] pixels = addPadding(bgr24);
                // Write the new BMP into existence
                makeBMP(pixels,n);
                // Ensure the buffer is in the right position for the next bmp
                if(pos!=bmpNxt && bmpNxt!=0) fbb.position(bmpNxt);
            }
        }
        catch (Exception ex)
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

    // Place BMP bytes in byte array
    public static byte[] getImgBytes(ByteBuffer bb)
    {
        byte[] rawBMP = new byte[dlen];
        // Read the raw bmp data into the array that was just made.
        bb.get(rawBMP,0,dlen);
        byte[] tmp = new byte[w*h*(bpp/8)];
        if(compressed)
        {
            decompress(rawBMP, tmp);
            return toRGB24(tmp);
        }
        else
        {
            return toRGB24(rawBMP);
        }
    }

    // Makes the actual 24-bit BMP image
    public static void makeBMP(byte[] bgr, int num)
    {
        try
        {
            // Set BMP output size
            int bsize = bgr.length+54;
            byte[] bmp = new byte[bsize];
            // Get the BMP header and BMP data, then combine
            byte[] bmpHeader = setHeader(bsize,bgr);
            bmp = setBMP(bmpHeader,bgr,bsize);
            // Set BMP name and location, then write BMP to file
            String sNum = String.format("%0"+nlen+"d", num);
            File img = new File(dir+name+"_"+sNum+".bmp");
            Files.write(img.toPath(),bmp);
        }
        catch (Exception ex)
        {
            out.println("Something donked up (makeBMP):\n"+ex);
        }
    }

    // Make/Set the BMP header array
    public static byte[] setHeader(int bsize, byte[] px)
    {
        // If you want to know what the values in this function mean, read this
        // wikipedia page: wikipedia.org/wiki/BMP_file_format
        byte[] hdr = new byte[54];
        hdr[0] = (byte)'B';
        hdr[1] = (byte)'M';
        // key: hdr[index], hdr, integer input
        addInt2Arr4(2, hdr,bsize);
        addInt2Arr4(6, hdr,0);
        addInt2Arr4(10,hdr,54);
        addInt2Arr4(14,hdr,40);
        addInt2Arr4(18,hdr,w);
        // height is negative to flip the image (the easy way)
        addInt2Arr4(22,hdr,-h);
        addInt2Arr2(26,hdr,1);
        addInt2Arr2(28,hdr,24);
        addInt2Arr4(30,hdr,0);
        addInt2Arr4(34,hdr,px.length);
        addInt2Arr4(38,hdr,2835);
        addInt2Arr4(42,hdr,2835);
        addInt2Arr4(46,hdr,0);
        addInt2Arr4(50,hdr,0);
        return hdr;
    }

    // combine the header and pixel arrays & return as single new array
    public static byte[] setBMP(byte[] hdr, byte[] px, int bsize)
    {
        byte[] bitmap = new byte[bsize];
        for(int i=0; i < hdr.length; i++)
        {
            bitmap[i] = hdr[i];
        }
        for(int i=0; i < px.length; i++)
        {
            bitmap[i+54] = px[i];
        }
        return bitmap;
    }

    // Converts other BMP formats to the uncompress 24-bit format
    public static byte[] toRGB24(byte[] rawBytes)
    {
        // convert 8-bit bmp data to 24-bit data
        if(bpp==8 && hasPal)
        {
            byte[] px = new byte[dlen*3];
            for(int i = 0; i < dlen; i++)
            {
                int x = i*3, r=0, g=1, b=2;
                // get the color index from the 8bit bmp array
                int c = rawBytes[i];
                // bytes are always signed, deal with the negative half
                if(c < 0) c = (c & 0xFF);
                // add/assign the rgb bytes from the palette based on the index
                px[x+0] = pal[c][r];
                px[x+1] = pal[c][g];
                px[x+2] = pal[c][b];
            }
            return px;
        }
        // convert 16-bit bmp data to 24-bit data
        else if(bpp==16)
        {
            // I've decided not to bother with using any palettes for 16-bit
            // simply b/c it is unneeded. No need for unnecessary code.
            int pixels = w*h;
            byte[] px = new byte[pixels*3];
            // 16-bit is RGB555 (5bit per color) stored in 2 bytes
            // so we gotta extract those bits to 3 full bytes, to 3 full colors
            for(int i = 0; i < pixels; i++)
            {
                // A shoutout to OrigamiGuy for insight/code on this conversion
                int x=i*2, y=i*3;
                byte b1=rawBytes[x], b2=rawBytes[x+1];
                // assign the bits inside the 2 bytes to r, g, b vars
                int r = (b2 & 0x7C) << 1;
                int g = ((b2 & 0x03) << 6) | ((b1 & 0xE0) >> 2);
                int b = (b1 & 0x1F) << 3;
                // mirror the 5 bits to 3 empty ones to get the right 8bit vals
                r = r | r >> 5;
                g = g | g >> 5;
                b = b | b >> 5;
                // change the int vars to bytes vars and add them to px array
                px[y+0] = (byte)b;
                px[y+1] = (byte)g;
                px[y+2] = (byte)r;
            }
            return px;
        }
        // pass 24-bit bmp data right through untouched
        else
        {
            return rawBytes;
        }
    }

    // add the necessary scanline byte padding required by bitmaps
    public static byte[] addPadding(byte[] bgr)
    {
        int colorBytes = w*3, padBytes =(4-(w*3%4))%4;
        int scanline = colorBytes+padBytes;
        int size = scanline*h, index1=0, index2=0;
        byte[] pixelBytes = new byte[size];
        for(int i=0; i < h; i++)
        {
            for(int x=0; x < scanline; x++)
            {
                if(x < colorBytes)
                {
                    // add a color byte to the array
                    pixelBytes[index1] = bgr[index2];
                    index1++;
                    index2++;
                }
                else
                {
                    // add padding byte to the array
                    pixelBytes[index1] = 0;
                    index1++;
                }
            }
        }
        return pixelBytes;
    }

    // Custom Run-length Encoding Decompression function.
    // Each scanline is defined by a encodedSize, and a cycle of background and
    // foreground pixel data that is repeated until the encodedSize is met.
    public static void decompress(byte[] in, byte[] result)
    {
        // Create bytebuffers for the input and output arrays
        ByteBuffer bi = ByteBuffer.wrap(in).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer bo = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
        // Initialize the vars: encodedSize, bg pixels, fg pixels, Bytes / pixel
        int encodedSize=0, bg=0, fg=0, Bpp=(bpp/8);
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
                    if(Bpp==1) bo.put((byte)0x00);
                    if(Bpp==2)
                    {
                        bo.put((byte)0x1F);
                        bo.put((byte)0x7C);
                    }
                    if(Bpp==3)
                    {
                        bo.put((byte)0xFF);
                        bo.put((byte)0x00);
                        bo.put((byte)0xFF);
                    }
                }
                for(int y=0; y < fg*Bpp; y++)
                {
                    bo.put(fgData[y]);
                }
                // Subtract the bytes for the fg & bg vars, and fgData
                encodedSize -= 4+(fg*Bpp);
            }
        }
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
        hasPal = a.hasPalette;
        pal = a.palette;
        offsets = a.bmpOffsets;
        nlen = String.valueOf(nBMP).length();
    }

    // auto add ints to a byte array
    public static void addInt2Arr4(int index, byte[] array, int num)
    {
        byte[] tmp = int2ba(num);
        array[index+0] = tmp[0];
        array[index+1] = tmp[1];
        array[index+2] = tmp[2];
        array[index+3] = tmp[3];
    }

    // auto add ints to a byte array
    public static void addInt2Arr2(int index, byte[] array, int num)
    {
        byte[] tmp = int2ba(num);
        array[index+0] = tmp[0];
        array[index+1] = tmp[1];
    }

    // int(4bytes) to byte array
    public static final byte[] int2ba(int val)
    {
        byte[] ba={(byte)val,(byte)(val>>>8),(byte)(val>>>16),(byte)(val>>>24)};
        return ba;
    }
}
