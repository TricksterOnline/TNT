/*
Java Bitmap Library (JBL)
A fairly spontaneous Java library that contains functions to deal with bitmaps
both standard and abnormal. Useful for any bitmap not just BMP images.

Copyright (C) 2018-2020 Sean Stafford (a.k.a. PyroSamurai)

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
This class/library was created to deal with the task of extracting bitmaps from
proprietary image archive formats to standard RGB24 bitmaps. It can help with
normal bitmap creation too, since Java lacks proper byte support for bitmaps.

Dev Notes:
Works exclusively with bytes and byte arrays: no numbers, objects, or generics.
No dealing with the RGB like its a short int. We do things the byte way.
All data is little-endian format. Don't think too hard about the actual code.
It is a huge headache to understand these bit formats.

Version: 1.0.0
*/
public class JBL
{
    // class variables
    public static int bpp=0,Bpp=0,w=0,h=0,dataSize=0,bppOut=0,pixels=0,nLen=0;
    public static String name, dir, bitFmtIn, bitFmtOut, RGB24="RGB24";
    public static String RGB555="RGB555",RGB565="RGB565",ARGB16="ARGB16";
    public static byte[][] palette = new byte[256][3];
    public static boolean bitFmtOutSet=false;

    // constructor for JBL class
    public JBL() {}

    // ######################## class mutators: begin ########################
    // The mutators need to be run before getImgBytes(), to have any effect

    // Sets the file-related variables, required for all
    public static void setFileVars(String fileDir,String bmpRootName)
    {
        // File Directory String (should include the File.separator)
        dir = fileDir;
        // The name that will serve as the base for all bitmap output
        name = bmpRootName;
    }

    // Sets the pixel-related variables, required for all
    public static void setBmpVars(int W,int H,int bitDepth)
    {
        // Bitmap width in pixels
        w = W;
        // Bitmap height in pixels
        h = H;
        // Bits Per Pixel
        bpp = bitDepth;
        // Set bpp output
        if(bpp==8 && bitFmtOutSet==false) bppOut=24;
        if(bpp!=8 && bitFmtOutSet==false) bppOut=bpp;
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

    // Sets the input bit format, required for 16-bit conversions
    public static void set16BitFmtIn(String bitFormat)
    {
        bitFmtIn = bitFormat;
        bitFmtOut = bitFormat;
    }

    // Sets bit format output, required for a bppOut != bpp
    // 8bit input will default to 24bit output if this is not set
    public static void setBitFmtOut(String bitFormat)
    {
        bitFmtOut = bitFormat;
        if(bitFmtOut.equals(RGB24)) bppOut=24;
        if(!bitFmtOut.equals(RGB24)) bppOut=16;
        bitFmtOutSet = true;
    }

    // Sets length of largest # & returns it, required for makeBMP(byte[],int)
    public static int setImgSetSize(int numberOfImages)
    {
        return nLen = String.valueOf(numberOfImages).length();
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
            return addPadding((toRGB16(temp24)), 2);
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
        else if(bpp==16 && bitFmtIn.equals(RGB555))
        {
            // RGB555 (5 bits per color) stored in 2 bytes
            for(int i = 0; i < pixels; i++)
            {
                // A shoutout to OrigamiGuy for insight on this conversion
                int x=i*2, y=i*3;
                byte b1=rawBytes[x], b2=rawBytes[x+1];
                // assign the bits inside the 2 bytes to r, g, b vars
                int b = (b1 & 0x1F) << 3;
                int g = ((b2 & 0x03) << 6) | ((b1 & 0xE0) >> 2);
                int r = (b2 & 0x7C) << 1;
                // mirror the 5 bits to 3 empty ones to get the right 8bit vals
                r = r | r >> 5;
                g = g | g >> 5;
                b = b | b >> 5;
                // change the int vars to bytes vars & add them to px array add
                // them in reverse order b/c that is the way format is, ugh
                px[y+0] = (byte)b;
                px[y+1] = (byte)g;
                px[y+2] = (byte)r;
            }
        }
        else if(bpp==16 && bitFmtIn.equals(RGB565))
        {
            // RGB565 stored in 2 bytes (as bgr)
            for(int i = 0; i < pixels; i++)
            {
                int x=i*2, y=i*3;
                byte b1=rawBytes[x], b2=rawBytes[x+1];
                // assign the bits inside the 2 bytes to r, g, b vars
                int b = (b1 & 0x1F) << 3;
                int g = ((b2 & 0x07) << 5) | ((b1 & 0xE0) >> 3);
                int r = (b2 & 0xF8);
                // mirror the color bits to the empty bits for correct 8bit vals
                r = r | r >> 5;
                g = g | g >> 6;
                b = b | b >> 5;
                // change the int vars to bytes vars & add them to px array add
                // them in reverse order b/c that is the way format is, ugh
                px[y+0] = (byte)b;
                px[y+1] = (byte)g;
                px[y+2] = (byte)r;
            }
        }
        else if(bpp==16 && bitFmtIn.equals(ARGB16))
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
                // change the int vars to bytes vars & add them to px array add
                // them in reverse order b/c that is the way format is, ugh
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

    // Converts standard 24-bit BMP pixels to 16-bit
    public static byte[] toRGB16(byte[] rgb24)
    {
        byte[] rgb16 = new byte[pixels*2];
        if(bitFmtOut.equals(RGB555))
        {
            // RGB24 to RGB555 (5 bits per color) stored in 2 bytes
            for(int i=0; i < pixels; i++)
            {
                int x=i*2, y=i*3;
                byte b=rgb24[y],g=rgb24[y+1],r=rgb24[y+2];
                int b1 = ((g<<2) & 0xE0) | ((b>>3) & 0x1F);
                int b2 = ((r>>1) & 0x7C) | ((g>>6) & 0x03);
                // change the ints to byte vars and add them to rgb16 array
                rgb16[x+0] = (byte)b1;
                rgb16[x+1] = (byte)b2;
            }
        }
        else if(bitFmtOut.equals(ARGB16))
        {
            // RGB24 to ARGB16 stored in 2 bytes
            for(int i = 0; i < pixels; i++)
            {
                int x=i*2, y=i*3;
                byte b=rgb24[y],g=rgb24[y+1],r=rgb24[y+2];
                // assign the a, r, g, b vars to 2 bytes
                int b1 = (g & 0xF0) | (b & 0x0F);
                int b2 = (r & 0x0F);
                // change the ints to byte vars and add them to rgb16 array
                rgb16[x+0] = (byte)b1;
                rgb16[x+1] = (byte)b2;
            }
        }
        else
        {
            // RGB24 to RGB565 Standard 16bit Format for bitmaps
            for(int i=0; i < pixels; i++)
            {
                int x=i*2, y=i*3;
                byte b=rgb24[y],g=rgb24[y+1],r=rgb24[y+2];
                int b1 = ((g<<3) & 0xE0) | ((b>>3) & 0x1F);
                int b2 = (r & 0xF8) | ((g>>5) & 0x07);
                // change the ints to byte vars and add them to rgb16 array
                rgb16[x+0] = (byte)b1;
                rgb16[x+1] = (byte)b2;
            }
        }
        return rgb16;
    }

    // add the necessary scanline byte padding required by bitmaps
    public static byte[] addPadding(byte[] rgb, int BppOut)
    {
        int colorBytes=w*BppOut, padBytes=(4-(w*BppOut%4))%4;
        int scanline=colorBytes+padBytes, size=scanline*h, dex1=0, dex2=0;
        byte[] scanlines = new byte[size];
        byte padByte = 0x00;
        if(padBytes!=0)
        {
            for(int i=0; i < h; i++)
            {
                for(int x=0; x < scanline; x++)
                {
                    if(x < colorBytes)
                        scanlines[dex1++] = rgb[dex2++];
                    else
                        scanlines[dex1++] = padByte;
                }
            }
        }
        else
        {
            scanlines = rgb;
        }
        return scanlines;
    }

    // remove the scanline byte padding required by bitmaps
    public static byte[] stripPadding(byte[] scanlines)
    {
        int colorBytes=w*(bpp/8), padBytes=(4-(w*(bpp/8)%4))%4;
        int scanline=colorBytes+padBytes, size=colorBytes*h, dex1=0, dex2=0;
        byte[] rgb = new byte[size];
        if(padBytes!=0)
        {
            for(int i=0; i < h; i++)
            {
                for(int x=0; x < scanline; x++)
                {
                    if(x<colorBytes)
                        rgb[dex1++] = scanlines[dex2++];
                    else
                        dex2++;
                }
            }
        }
        else
        {
            rgb = scanlines;
        }
        return rgb;
    }

    // For BMP's convoluted format to work well the data needs to written
    // bottom-up, with the last scanline at the top and vice versa.
    public static byte[] reverseRows(byte[] topDownLines)
    {
        int scanline=(topDownLines.length / h), dex1=0, dex2=0;
        byte[] trueScanlines = new byte[topDownLines.length];
        byte[][] scanlines = new byte[h][scanline];
        for(int i=0; i < h; i++)
        {
            for(int x=0; x < scanline; x++)
            {
                scanlines[i][x] = topDownLines[dex1++];
            }
        }
        int lastLine = h - 1;
        for(int i=0; i < h; i++)
        {
            for(int x=0; x < scanline; x++)
            {
                trueScanlines[dex2++] = scanlines[lastLine-i][x];
            }
        }
        return trueScanlines;
    }

    // Makes the final BMP image array
    public static byte[] setBMP(byte[] scanlines, boolean vertFlip)
    {
        // Set BMP output size
        int imgSize = scanlines.length+54;
        byte[] bmp = new byte[imgSize];
        // Get the BMP header
        byte[] bmpHeader = setHeader(imgSize,scanlines,vertFlip);
        // Join the header and image data arrays then return
        return joinImgParts(imgSize,bmpHeader,scanlines);
    }

    // Make/Set the BMP header array
    public static byte[] setHeader(int bsize, byte[] data, boolean hFlip)
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
        addInt2Arr4(34,hdr,data.length);
        addInt2Arr4(38,hdr,2835);
        addInt2Arr4(42,hdr,2835);
        addInt2Arr4(46,hdr,0);
        addInt2Arr4(50,hdr,0);
        return hdr;
    }

    // combine the header and scanline arrays & return as single new array
    public static byte[] joinImgParts(int bsize, byte[] hdr, byte[] data)
    {
        byte[] bitmap = new byte[bsize];
        for(int i=0; i < hdr.length; i++)
        {
            bitmap[i] = hdr[i];
        }
        for(int i=0; i < data.length; i++)
        {
            bitmap[i+54] = data[i];
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
            out.println("Error in (makeBMP):\n"+ex);
        }
    }

    // For use when making a set of BMP (one at a time in a loop)
    public static void makeBMP(byte[] bmp, int currentNum, String suffix)
    {
        try
        {
            // Set BMP name and location, then write BMP to file
            String sNum = String.format("%0"+nLen+"d", currentNum);
            File img = new File(dir+name+"_"+sNum+suffix+".bmp");
            Files.write(img.toPath(),bmp);
        }
        catch(Exception ex)
        {
            out.println("Error in (makeBMPSet):\n"+ex);
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
