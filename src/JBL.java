/*
Java Bitmap Library (JBL)
A fairly spontaneous Java library that contains functions to deal with bitmaps
both standard and abnormal. Useful for any bitmap not just BMP images.

Copyright (C) 2018 Sean Stafford (a.k.a. PyroSamurai)

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
This class/library was created to deal with the task of extracting bitmaps from
proprietary image archive formats to standard RGB24 bitmaps. It can help with
normal bitmap creation too, since Java lacks proper byte support for bitmaps.

Dev Notes:
Works exclusively with bytes and byte arrays: no numbers, objects, or generics.
No dealing with the RGB like its a short int. We do things the byte way.
All data is little-endian format.

Version: 0.9.0
*/
public class JBL
{
    // class variables
    public static int bpp=0,Bpp=0,w=0,h=0,dataSize=0,bppOut=24,pixels=0,nLen=0;
    public static String name, dir, bitFmt;
    public static final String RGB555="RGB555",RGB565="RGB565",ARGB16="ARGB16";
    public static byte[][] palette = new byte[256][3];

    // constructor for JBL class
    public JBL() {}

    // ######################## class mutators: begin ########################
    // The mutators need to be run before getImgBytes(), to have any effect

    // This function is very important, it must be run first before any of the
    // other functions in this library, param names shortened to fit 80 cols
    // Full names: directory path, filename, width, height, bits per pixel
    public static void setJBLVars(String D,String Name,int W,int H,int bitDepth)
    {
        dir = D;
        name = Name;
        // Bitmap width in pixels
        w = W;
        // Bitmap height in pixels
        h = H;
        // Bits Per Pixel
        bpp = bitDepth;
        // Calculated # of pixels in bitmap
        pixels = w*h;
        // Data Size, calculated size of the input bitmap data
        dataSize = pixels*(bpp/8);
    }

    // Sets the palette array, required for 8-bit conversions
    public static void setPalette(byte[][] pal)
    {
        palette = pal;
    }

    // Sets the bit format, required for 16-bit conversions
    public static void setBitFormat(String format)
    {
        bitFmt = format;
    }

    // Sets length of largest # & returns it, required for makeBMP(byte[],int)
    public static int setImgSetSize(int numberOfImages)
    {
        return nLen = String.valueOf(numberOfImages).length();
    }

    // Sets BMP output to 16-bit, required for toRGB16() (not implemented yet)
    public static void set16BitOutput()
    {
        bppOut = 16;
    }

    // ######################### class mutators: end #########################
    // #######################################################################

    // Place bitmap bytes in a byte array
    public static byte[] getImgBytes(ByteBuffer bb, int dataLength)
    {
        // dataLength is for grabbing compressed data, just set to 0 if unneeded
        if(dataLength!=0) dataSize = dataLength;
        byte[] rawBitmap = new byte[dataSize];
        // Read the raw bitmap data into the array that was just made.
        bb.get(rawBitmap,0,dataSize);
        return rawBitmap;
    }

    // An interface to convert pixels to a standard RGB format
    public static byte[] toStdRGB(byte[] rawPixels)
    {
        byte[] temp24 = toRGB24(rawPixels);
        if(bppOut==16)
            return addPadding(toRGB16(temp24), 2);
        else
            return addPadding(temp24, 3);
    }

    // Converts other BMP formats to the uncompressed 24-bit format
    public static byte[] toRGB24(byte[] rawBytes)
    {
        byte[] px = new byte[pixels*3];
        // convert 8-bit bmp data to 24-bit data
        if(bpp==8)
        {
            for(int i = 0; i < pixels; i++)
            {
                int x = i*3, r=0, g=1, b=2;
                // get the color index from the 8bit bmp array
                int c = rawBytes[i];
                // bytes are always signed, deal with the negative half
                if(c < 0) c = (c & 0xFF);
                // add/assign the rgb bytes from the palette based on the index
                px[x+0] = palette[c][r];
                px[x+1] = palette[c][g];
                px[x+2] = palette[c][b];
            }
        }
        // ######################## 16-bit Conversions ########################
        else if(bpp==16 && bitFmt.equals(RGB555))
        {
            // RGB555 (5 bits per color) stored in 2 bytes
            for(int i = 0; i < pixels; i++)
            {
                // A shoutout to OrigamiGuy for insight on this conversion
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
        }
        else if(bpp==16 && bitFmt.equals(RGB565))
        {
            // RGB565 stored in 2 bytes
            for(int i = 0; i < pixels; i++)
            {
                int x=i*2, y=i*3;
                byte b1=rawBytes[x], b2=rawBytes[x+1];
                // assign the bits inside the 2 bytes to r, g, b vars
                int r = (b2 & 0xF8);
                int g = ((b2 & 0x07) << 5) | ((b1 & 0xE0) >> 3);
                int b = (b1 & 0x1F) << 3;
                // mirror the color bits to the empty bits for correct 8bit vals
                r = r | r >> 5;
                g = g | g >> 6;
                b = b | b >> 5;
                // change the int vars to bytes vars and add them to px array
                px[y+0] = (byte)b;
                px[y+1] = (byte)g;
                px[y+2] = (byte)r;
            }
        }
        else if(bpp==16 && bitFmt.equals(ARGB16))
        {
            // ARGB16 (ARGB4444) (4 bits per color) stored in 2 bytes
            // bitmaps don't support transparency, so even though this format is
            // mainly used to support it in 16-bit, I'm going to ignore it, ftb.
            for(int i = 0; i < pixels; i++)
            {
                int x=i*2, y=i*3;
                byte b1=rawBytes[x], b2=rawBytes[x+1];
                // assign the bits inside the 2 bytes to a, r, g, b vars
                int a = (b2 & 0xF0);
                int r = (b2 & 0x0F) << 4;
                int g = (b1 & 0xF0);
                int b = (b1 & 0x0F) << 4;
                // mirror the 4 bits to 4 empty ones to get the right 8bit vals
                a = a | a >> 4;
                r = r | r >> 4;
                g = g | g >> 4;
                b = b | b >> 4;
                // change the int vars to bytes vars and add them to px array
                px[y+0] = (byte)b;
                px[y+1] = (byte)g;
                px[y+2] = (byte)r;
            }
        }
        // pass 24-bit bitmap data right through untouched
        else
        {
            px = rawBytes;
        }
        return px;
    }

    // Converts standard 24-bit BMP pixels to 16-bit (RGB565) (TODO 1.0.0)
    public static byte[] toRGB16(byte[] bgr24)
    {
        byte[] bgr16 = new byte[pixels*2];
        for(int i = 0; i < pixels; i++)
        {
            int x=i*3, y=i*2;
            int b = bgr24[x], g = bgr24[x+1], r = bgr24[x+2];
        }
        return bgr16;
    }

    // add the necessary scanline byte padding required by bitmaps
    public static byte[] addPadding(byte[] bgr, int BppOut)
    {
        int colorBytes = w*BppOut, padBytes = (4-(w*BppOut%4))%4;
        int scanline = colorBytes+padBytes, size = scanline*h, dex1=0, dex2=0;
        byte[] pixelBytes = new byte[size];
        byte padByte = 0x00;
        for(int i=0; i < h; i++)
        {
            for(int x=0; x < scanline; x++)
            {
                if(x < colorBytes)
                    pixelBytes[dex1++] = bgr[dex2++];
                else
                    pixelBytes[dex1++] = padByte;
            }
        }
        return pixelBytes;
    }

    // Makes the final BMP image array
    public static byte[] setBMP(byte[] pixels, boolean vertFlip)
    {
        // Set BMP output size
        int imgSize = pixels.length+54;
        byte[] bmp = new byte[imgSize];
        // Get the BMP header
        byte[] bmpHeader = setHeader(imgSize,pixels,vertFlip);
        // Join the header and image data arrays then return
        return joinImgParts(imgSize,bmpHeader,pixels);
    }

    // Make/Set the BMP header array
    public static byte[] setHeader(int bsize, byte[] px, boolean hFlip)
    {
        // if needed, flip image vertically, the easy way, make height negative
        if(hFlip) h = -h;
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
        addInt2Arr4(22,hdr,h);
        addInt2Arr2(26,hdr,1);
        addInt2Arr2(28,hdr,bppOut);
        addInt2Arr4(30,hdr,0);
        addInt2Arr4(34,hdr,px.length);
        addInt2Arr4(38,hdr,2835);
        addInt2Arr4(42,hdr,2835);
        addInt2Arr4(46,hdr,0);
        addInt2Arr4(50,hdr,0);
        return hdr;
    }

    // combine the header and pixel arrays & return as single new array
    public static byte[] joinImgParts(int bsize, byte[] hdr, byte[] px)
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

    // Output single BMP to file
    public static void makeBMP(byte[] bmp)
    {
        try
        {
            // Set BMP name and location, then write BMP to file
            File img = new File(dir+name+".bmp");
            Files.write(img.toPath(),bmp);
        }
        catch(Exception ex)
        {
            out.println("Something donked up (makeBMP):\n"+ex);
        }
    }

    // For use when making a set of BMP (one at a time in a loop)
    public static void makeBMP(byte[] bmp, int currentNum)
    {
        try
        {
            // Set BMP name and location, then write BMP to file
            String sNum = String.format("%0"+nLen+"d", currentNum);
            File img = new File(dir+name+"_"+sNum+".bmp");
            Files.write(img.toPath(),bmp);
        }
        catch(Exception ex)
        {
            out.println("Something donked up (makeBMPSet):\n"+ex);
        }
    }


    // ########################## Utility Functions ############################
    // Utility functions mostly for internal use, but feel free to use them

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
    public static final byte[] int2ba(int v)
    {
        byte[] ba = {(byte)v, (byte)(v>>>8), (byte)(v>>>16), (byte)(v>>>24)};
        return ba;
    }
}
