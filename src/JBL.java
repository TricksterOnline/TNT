/*
Java Bitmap Library (JBL)
A fairly spontaneous Java library that contains functions to deal with bitmaps
both standard and abnormal. Useful for any bitmap not just BMP images.

Copyright (C) 2018-2021 Sean Stafford (a.k.a. PyroSamurai)

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
proprietary image archive formats to standard RGB bitmaps. It can help with
normal bitmap creation too, since Java lacks proper byte support for bitmaps.

Dev Notes:
Works exclusively with bytes and byte arrays: no numbers, objects, or generics.
No dealing with the RGB like its a short int. We do things the byte way.
All data is little-endian format. Don't think too hard about the actual code.
It is a huge headache to understand these bit formats.

For the sake of versatility all members of the class are public & rely as little
as possible on each other for data, relying mostly on params & class vars.
This means you are responsible for using them in the right order, though.
Just keep that in mind.

For your benefit, the functions are actually in the order you should use them,
with the exception of stripPadding & reverseRows whose location/existence in
your program can vary a lot with your use-case.

Version: 1.1.2
*/
public class JBL
{
// class variables
public int bmpSize,dataStart,dibSize,w,h,planes=1,bpp,compMethod,dataSize;
public int Bpp,bppOut,pixels,pxLen,nLen,bitmaskR,bitmaskG,bitmaskB,bitmaskA;
public String name,dir,bitFmtIn,bitFmtOut,RGB8="RGB8";
public String RGB24="RGB24",RGB555="RGB555",RGB565="RGB565",ARGB16="ARGB16";
public byte[][] palette = new byte[256][3];
public boolean bitFmtOutSet=false;

// constructor for JBL class
public JBL(){}

// ######################## class mutators: begin ########################
// The mutators need to be run before non-mutators, to have any effect

// Sets the file-related vars, required for makeBMP()
public void setFileVars(String fileDir,String bmpRootName)
{
    // File Directory String (should include the File.separator)
    dir = fileDir;
    // If it doesn't have File.separator, add it
    if(!dir.endsWith(File.separator)) dir += File.separator;
    // The name that will serve as the base for all bitmap output
    name = bmpRootName;
}

// Sets the pixel-related vars, must use this or getBitmapVars for makeBMP()
public void setBitmapVars(int width,int height,int bitDepth)
{
    // Bitmap width & height in pixels
    w = width;
    h = height;
    // Bits Per Pixel
    bpp = bitDepth;
    // Set bpp output
    if(bpp==8 && bitFmtOutSet==false) bppOut=24;
    if(bpp!=8 && bitFmtOutSet==false) bppOut=bpp;
    // Calculated # of pixels in bitmap
    pixels = w*h;
    // Pixel Length, calculated size of the input bitmap data
    pxLen = pixels*(bpp/8);
}

// Sets the pixel-related vars, useful for pre-existing BMP
public void getBitmapVars(byte[] bitmap)
{
    ByteBuffer bmp = ByteBuffer.wrap(bitmap).order(ByteOrder.LITTLE_ENDIAN);
    bmp.getChar();//skip file signature
    bmpSize = bmp.getInt();
    bmp.getInt();//skip reserved bytes
    dataStart = bmp.getInt();
    dibSize = bmp.getInt();
    switch(dibSize)
    {
    case 12:
        w = (int)bmp.getChar();
        h = (int)bmp.getChar();
        bmp.getChar();//planes
        bpp = (int)bmp.getChar();
        break;
    case 16:
    case 52:
    case 56:
    case 64:
    case 108:
    case 124:
    default://40
        w = bmp.getInt();
        h = bmp.getInt();
        bmp.getChar();//planes
        bpp = (int)bmp.getChar();
        compMethod = bmp.getInt();
        dataSize = bmp.getInt();
        break;
    }
    // Calculated # of pixels in bitmap
    pixels = w*h;
    // Pixel Length, calculated size of the input bitmap data
    pxLen = pixels*(bpp/8);
}

// Sets the palette array, required for 8-bit conversions
public void setPalette(byte[][] pal)
{
    palette = pal;
}

// Sets the input bit format, required for 16-bit conversions
public void set16BitFmtIn(String bitFormat)
{
    bitFmtIn = bitFormat;
    bitFmtOut = bitFormat;
}

// Sets bit format output, required for a bppOut != bpp
// 8bit input will default to 24bit output if this is not set
public void setBitFmtOut(String bitFormat)
{
    bitFmtOut = bitFormat;
    if(bitFmtOut.equals(RGB24)) bppOut=24;
    if(!bitFmtOut.equals(RGB24)) bppOut=16;
    bitFmtOutSet = true;
}

// Use of 1 of the following is required for makeBMP(byte[],int,String)
//##########################################################################

// Sets nLen to the value of the int parameter
public void setNumLength(int lengthOfLargestNumber)
{
    nLen = lengthOfLargestNumber;
}

// Sets nLen to length of largest number
public void setImgsetSize(int numberOfImages)
{
    nLen = String.valueOf(numberOfImages).length();
}

// ######################### class mutators: end #########################
/*########################################################################*/

// Get bitmap bytes from a bytebuffer-wrapped byte array
public byte[] getImgBytes(ByteBuffer bb, int dataLength)
{
    // dataLength is for grabbing compressed data, just set to 0 if unneeded
    if(dataLength!=0) dataSize = dataLength;
    if(dataSize==0) dataSize = pxLen;
    byte[] rawBMP = new byte[dataSize];
    // Read the raw bitmap data into the array that was just made.
    bb.get(rawBMP,0,dataSize);
    return rawBMP;
}

// An interface to convert pixels to a standard RGB format
public byte[] toStdRGB(byte[] rawPixels)
{
    byte[] temp24 = toRGB24(rawPixels);
    if(bppOut==16)
        return addPadding((toRGB16(temp24)), 2);
    else
        return addPadding(temp24, 3);
}

// Converts other BMP formats to the uncompressed 24-bit format
public byte[] toRGB24(byte[] rawBytes)
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
            // them in reverse order b/c that is the way the format is, ugh
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
public byte[] toRGB16(byte[] rgb24)
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
        // A good location to set rgb555-specific bitmask info
        bitmaskR = 31744;
        bitmaskG = 992;
        bitmaskB = 31;
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
        // A good location to set rgb565-specific bitmask info
        bitmaskR = 63488;
        bitmaskG = 2016;
        bitmaskB = 31;
    }
    return rgb16;
}

// add the necessary scanline byte padding required by bitmaps
public byte[] addPadding(byte[] rgb, int BppOut)
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
public byte[] stripPadding(byte[] scanlines)
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
                    dex2++;//skip through padding
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
public byte[] reverseRows(byte[] topDownLines)
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
    int lastLine = h-1;
    for(int i=0; i < h; i++)
    {
        for(int x=0; x < scanline; x++)
        {
            trueScanlines[dex2++] = scanlines[lastLine-i][x];
        }
    }
    return trueScanlines;
}

// Set bpp-specific dib header info
public void setDibSizeParams()
{
    if(bppOut==16)
    {
        dataStart  = 70;
        dibSize    = 56;
        compMethod = 3;
    }
    else
    {
        dataStart  = 54;
        dibSize    = 40;
        compMethod = 0;
    }
}

// Sets bmpSize, requires padded scanlines size; run setDibSizeParams() first
public void setBitmapSize(int dataLength)
{
    // Set BMP output size
    bmpSize = dataLength+dataStart;
}

// Makes the final BMP image array
public byte[] setBMP(byte[] scanlines, boolean vertFlip)
{
    setDibSizeParams();
    setBitmapSize(scanlines.length);
    // Get the BMP header
    byte[] bmpHeader = setHeader(scanlines.length,vertFlip);
    // Join the header and image data arrays then return
    return joinImgParts(bmpHeader,scanlines);
}

// Make/Set the BMP header array
public byte[] setHeader(int dataLength, boolean hFlip)
{
    // if needed, flip image vertically, the easy way, make height negative
    if(hFlip) h = -h;
    // If you want to know what the values in this function mean, read this
    // wikipedia page: wikipedia.org/wiki/BMP_file_format
    byte[] header = new byte[dataStart];
    ByteBuffer hdr = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
    hdr.put((byte)'B');
    hdr.put((byte)'M');
    hdr.putInt(bmpSize);
    hdr.putInt(0);
    hdr.putInt(dataStart);
    hdr.putInt(dibSize);
    hdr.putInt(w);
    hdr.putInt(h);
    hdr.putShort((short)1);
    hdr.putShort((short)bppOut);
    hdr.putShort((short)compMethod);
    hdr.putShort((short)0);
    hdr.putInt(dataLength);
    hdr.putInt(2835);
    hdr.putInt(2835);
    hdr.putInt(0);
    hdr.putInt(0);
    if(bppOut==16)
    {
        hdr.putInt(bitmaskR);
        hdr.putInt(bitmaskG);
        hdr.putInt(bitmaskB);
        hdr.putInt(bitmaskA);
    }
    return header;
}

// combine the header and scanline arrays & return as single new array
public byte[] joinImgParts(byte[] hdr, byte[] data)
{
    byte[] bitmap = new byte[bmpSize];
    ByteBuffer BMP = ByteBuffer.wrap(bitmap).order(ByteOrder.LITTLE_ENDIAN);
    BMP.put(hdr);
    BMP.put(data);
    return bitmap;
}

// Output single BMP to file
public void makeBMP(byte[] BMP)
{
    try
    {
        // Set BMP name and location, then write BMP to file
        File img = new File(dir+name+".bmp");
        Files.write(img.toPath(),BMP);
    }
    catch(Exception ex)
    {
        out.println("Error in (makeBMP):");
        ex.printStackTrace(System.out);
    }
}

// For use when making a set of BMP (one at a time in a loop)
public void makeBMP(byte[] BMP, int currentNum, String suffix)
{
    try
    {
        // Set BMP name and location, then write BMP to file
        String sNum = String.format("%0"+nLen+"d", currentNum);
        File img = new File(dir+name+"_"+sNum+suffix+".bmp");
        Files.write(img.toPath(),BMP);
    }
    catch(Exception ex)
    {
        out.println("Error in (makeBMPSet):");
        ex.printStackTrace(System.out);
    }
}
}
