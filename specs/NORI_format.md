NORI Format Specification
=========================

<pre>
Copyright (C) 2014-2018 Libre Trickster Team

Note: All sizes throughout the specification are measured in bytes.

Okay, before we get to covering the NORI file format you have to deal with the 
fact that some NORI files come compressed with the zlib DEFLATE algorithm. So 
you have to first check for and INFLATE any files that come this way before you 
can move on to reading the normal NORI info.(ex: Taiwan's map_sq00_event01.bac)

The file will look this if compressed by zlib:
</pre><pre>
+------------------------------------------------------------------------------+
| Compression: zlib Compression (DEFLATE Algorithm)                            |
+-----------------+-----+------------------------------------------------------+
| Name            |Bytes| Description                                          |
+-----------------+-----+------------------------------------------------------+
| Fake header     |  4  | Fake file hdr; bit of joke: 0xB0A0 (BA = Byte Array) |
+-----------------+-----+------------------------------------------------------+
| Actual size     |  4  | Data size after decompression                        |
+-----------------+-----+------------------------------------------------------+
| Data size       |  4  | Data size before decompression (includes zlib header)|
+-----------------+-----+------------------------------------------------------+
| zlib header     |  2  | Magic bytes: 0x78XX (XX can be 01, 9C, or DA)        |
+-----------------+-----+------------------------------------------------------+
| Compressed data |  *  | Decompress using the INFLATE algorithm               |
+-----------------+-----+------------------------------------------------------+
+------------------------------------------------------------------------------+
</pre><pre>
After zlib INFLATE or if uncompressed to begin with, you can start reading the 
NORI file as it is suppose to look:
</pre><pre>
+------------------------------------------------------------------------------+
| NORI Header                                                                  |
+---------------+-----+--------------------------------------------------------+
| Name          |Bytes| Description                                            |
+---------------+-----+--------------------------------------------------------+
| signature     |  4  | NORI file signature                                    |
+---------------+-----+--------------------------------------------------------+
| version/type  |  4  | NORI format version (300, 301, 302, 303)               |
+---------------+-----+--------------------------------------------------------+
| param01-05    | 20  | Unidentified data                                      |
+---------------+-----+--------------------------------------------------------+
| anims         |  4  | Number of included animations                          |
+---------------+-----+--------------------------------------------------------+
| param07       |  4  | fsize - GAWI Section size                              |
+---------------+-----+--------------------------------------------------------+
| fsize         |  4  | NORI file size                                         |
+---------------+-----+--------------------------------------------------------+
+------------------------------------------------------------------------------+
| END OF NORI HEADER                                                           |
+------------------------------------------------------------------------------+
+------------------------------------------------------------------------------+
| GAWI_Data: Imageset Section                                                  |
+---------------+-----+--------------------------------------------------------+
| Name          |Bytes| Description                                            |
+---------------+-----+--------------------------------------------------------+
| signature     |  4  | 'GAWI' Identifier of the GAWI section                  |
+---------------+-----+--------------------------------------------------------+
| version/type  |  4  | Version of the GAWI section format (always 300)        |
+---------------+-----+--------------------------------------------------------+
| bpp           |  4  | Bit depth of the image (8, 16, or 24)                  |
+---------------+-----+--------------------------------------------------------+
| flag_compress |  4  | Image compression flag (1 is yes) (RLE compression)    |
+---------------+-----+--------------------------------------------------------+
| flag_palette  |  4  | Palette usage flag (set to 1, if palette exists)       |
+---------------+-----+--------------------------------------------------------+
| param_04-07   | 16  | Unidentified data                                      |
+---------------+-----+--------------------------------------------------------+
| numBMP        |  4  | Number of images                                       |
+---------------+-----+--------------------------------------------------------+
| gsize         |  4  | Size of all image data combined                        |
+---------------+-----+--------------------------------------------------------+
+------------------------------------------------------------------------------+
| Palette_Data:  Palette Section (may or may not exist)                        |
+---------------+-----+--------------------------------------------------------+
| Name          |Bytes| Description                                            |
+---------------+-----+--------------------------------------------------------+
| signature     |  4  | 'PAL_' Identifier of palette structure                 |
+---------------+-----+--------------------------------------------------------+
| version/type  |  4  | Version of the PAL_ section format (always 100)        |
+---------------+-----+--------------------------------------------------------+
| param_01-04   | 20  | Unidentified data                                      |
+---------------+-----+--------------------------------------------------------+
| param_05      |  4  | A true/false flag for something                        |
+---------------+-----+--------------------------------------------------------+
| pal_length    |  4  | length = size of entire palette section                |
+---------------+-----+--------------------------------------------------------+
| [RGB24DATA]   | 768 | raw palette data; see BitmapData                       |
+---------------+-----+--------------------------------------------------------+
| twoNums       |  8  | Only exists if palette length=808; always 111 & 254    |
+---------------+-----+--------------------------------------------------------+
+------------------------------------------------------------------------------+
| BitmapOffsetData: BMP Offset Address Info                                    |
+---------------+-----+--------------------------------------------------------+
| Name          |Bytes| Description                                            |
+---------------+-----+--------------------------------------------------------+
| bmpOffsets    | bos | BMP 1st byte locations; bos=(4)(numBMP); If compressed |
|               |     | by RLE, add (28)(n) to each location; n=0,n++ each time|
+---------------+-----+--------------------------------------------------------+
+------------------------------------------------------------------------------+
| BitmapData: BMP Data For Each Image                                          |
+---------------+-----+--------------------------------------------------------+
| Name          |Bytes| Description                                            |
+---------------+-----+--------------------------------------------------------+
| data_count    |  4  | When >1, imgs are extracted as a subset of a single img|
+---------------+-----+--------------------------------------------------------+
+---------------+-----+--------------------------------------------------------+
| data_length   |  4  | data size(=sdata)                                      |
+---------------+-----+--------------------------------------------------------+
| pic_width     |  4  | Image width (in pixels)                                |
+---------------+-----+--------------------------------------------------------+
| pic_height    |  4  | Image height (in pixels)                               |
+---------------+-----+--------------------------------------------------------+
| param_04      |  4  | Unidentified data                                      |
+---------------+-----+--------------------------------------------------------+
| position_x    |  4  | The image's X position, usually 0                      |
+---------------+-----+--------------------------------------------------------+
| position_y    |  4  | The image's Y position, usually 0                      |
+---------------+-----+--------------------------------------------------------+
|[RGB24Data]    |sdata| 24bit img; bit fmt: B8G8R8 (3Bytes)                    |
|[RGB16Data]    |sdata| 16bit img; bit fmt: R5G5B5 [nRRRRRGG GGGBBBBB] (2Bytes)|
|[PalIndexData] |sdata|  8bit img; bit fmt: 1 index # = 1 unsigned byte        |
+---------------+-----+--------------------------------------------------------+
+------------------------------------------------------------------------------+
| END OF IMAGE DATA                                                            |
+------------------------------------------------------------------------------+
</pre><pre>
The following animation sections are not actually used in TNT. So while I have 
not implemented these in code myself, they were used successfully in the 
multi-tool prototype: TO-Toolbox. Therefore, they should be correct.
</pre><pre>
+------------------------------------------------------------------------------+
| AnimOffsetData: Animation Offset Address Info                                |
+---------------+-----+--------------------------------------------------------+
| Name          |Bytes| Description                                            |
+---------------+-----+--------------------------------------------------------+
| animOffsets   | aos | Animation first byte locations, aos = (4)(anims)       |
+---------------+-----+--------------------------------------------------------+
</pre><pre>
Like the BitmapData section, the next 4 sections are cyclical and can exist in
multiples asymmetrically. The AnimData, FrameData, PlaneData, and ExtraData
section. They are structured visually like this:
</pre>

* AnimData
    * FrameData
        * PlaneData
        * ExtraData

<pre>
+------------------------------------------------------------------------------+
| AnimData: Basic Data For Each Animation                                      |
+---------------+-----+--------------------------------------------------------+
| Name          |Bytes| Description                                            |
+---------------+-----+--------------------------------------------------------+
| animName      | 32  | Animation name (uses EUC-KR encoding)                  |
+---------------+-----+--------------------------------------------------------+
| frames        |  4  | Number of frames(=nof)                                 |
+---------------+-----+--------------------------------------------------------+
| frameOffsets  | fos | Frame first byte locations, fos = (4)(nof)             |
+---------------+-----+--------------------------------------------------------+
+------------------------------------------------------------------------------+
| FrameData: Basic Data For Each Frame in Every Animation                      |
+---------------+-----+--------------------------------------------------------+
| Name          |Bytes| Description                                            |
+---------------+-----+--------------------------------------------------------+
| delay         |  4  | Duration of this frame (milliseconds)                  |
+---------------+-----+--------------------------------------------------------+
| plane_count   |  4  | Number of planes(=nop)                                 |
+---------------+-----+--------------------------------------------------------+
+------------------------------------------------------------------------------+
| PlaneData: Basic Data For Each Plane in Every Frame                          |
+---------------+-----+--------------------------------------------------------+
| Name          |Bytes| Description                                            |
+---------------+-----+--------------------------------------------------------+
| bitmap_id     |  4  | BMP ID number for plane (# from 0 to (numBMP-1))       |
+---------------+-----+--------------------------------------------------------+
| point_x       |  4  | Location coordinates x                                 |
+---------------+-----+--------------------------------------------------------+
| point_y       |  4  | Location coordinates y                                 |
+---------------+-----+--------------------------------------------------------+
| opacity       |  4  | Always 100, as in 100%, from what I've seen            |
+---------------+-----+--------------------------------------------------------+
| flag_reverse  |  4  | img inversion flag ([bit1]vert flip, [bit0]horiz flip) |
+---------------+-----+--------------------------------------------------------+
| blend_mode    |  4  | ADD, MULTY, INVMULTY, etc                              |
+---------------+-----+--------------------------------------------------------+
| param_06      |  4  | Usually 15 or 16, (artificial flag?)                   |
+---------------+-----+--------------------------------------------------------+
+------------------------------------------------------------------------------+
| ExtraData: Extra Data After Every Frame                                      |
+---------------+-----+--------------------------------------------------------+
| NORI Version  |Bytes| Description                                            |
+---------------+-----+--------------------------------------------------------+
| v300          | 224 | Possibly Sound Effect Specification, otherwise unknown |
+---------------+-----+--------------------------------------------------------+
| v301          | 228 | Possibly Sound Effect Specification, otherwise unknown |
+---------------+-----+--------------------------------------------------------+
| v302          | 348 | Possibly Sound Effect Specification, otherwise unknown |
+---------------+-----+--------------------------------------------------------+
| v303          | 352 | Possibly Sound Effect Specification, otherwise unknown |
+---------------+-----+--------------------------------------------------------+
+------------------------------------------------------------------------------+
| END OF ANIMATION DATA                                                        |
+------------------------------------------------------------------------------+
+------------------------------------------------------------------------------+
| END OF FILE FORMAT                                                           |
+------------------------------------------------------------------------------+
</pre>

================Additional Info=================
================================================

<pre>
Custom Run-length Encoding:
Each scanline is defined by a encodedSize, and a cycle of background and
foreground pixel data that is repeated until the encodedSize is met.
</pre><pre>
+------------------------------------------------------------------------------+
| Compression: Custom RLE Per Scanline (wikipedia.org/wiki/Run-length_encoding)|
+---------------+-----+--------------------------------------------------------+
| Name          |Bytes| Description                                            |
+---------------+-----+--------------------------------------------------------+
| encodedSize   |  2  | # of bytes to read for this scanline (includes itself) |
+---------------+-----+--------------------------------------------------------+
+---------------+-----+--------------------------------------------------------+
| bg_pixels     |  2  | # of bg pixels; no color specified, you choose it      |
+---------------+-----+--------------------------------------------------------+
| fg_pixels     |  2  | # of actual foreground pixels that follow  (=fg)       |
+---------------+-----+--------------------------------------------------------+
| fg_data       | fp  | Actual foreground pixels.  fp = (fg)(bpp/8)            |
+---------------+-----+--------------------------------------------------------+
+------------------------------------------------------------------------------+
</pre>

The Extra Data Part
------------------------------------------------

<pre>
We believe that for 0, there is no special meaning or that it can be deduced
for the most part.

We define 3 different structures in the extra data part, which occur in
the following sequence:

1. CD block structure: Just a series of bytes with 0xCD value.
It is probably unused (i.e. malloc-ed but not assigned any value)

2. Entry block structure: Follows the following structure (total size: 28 bytes)
+------------------------------------------------------------------------------+
| Entry block structure                                                        |
+--------------+-------+-------------------------------------------------------+
| Name         | Bytes | Description                                           |
+--------------+-------+-------------------------------------------------------+
| bparam_01    |   4   | Value usually = 0xCDCDCDCD (probably unassigned data) |
+--------------+-------+-------------------------------------------------------+
| bparam_02    |   4   | Unknown (usually null)                                |
+--------------+-------+-------------------------------------------------------+
| bparam_x     |   4   | Probably X-value                                      |
+--------------+-------+-------------------------------------------------------+
| bparam_y     |   4   | Probably Y-value                                      |
+--------------+-------+-------------------------------------------------------+
| bparam_03~05 |   12  | Unknown (usually null)                                |
+--------------+-------+-------------------------------------------------------+
+------------------------------------------------------------------------------+
Some map files such as "map_sq00.bac" (Megalopolis Square) are found to have
non-zero bparam_x and bparam_y for certain frames.
Examples:
* map_sq00.bac, eTO version (latest) - 0x8EA466
* map_sq00_event01.bac, Taiwan TO client (file is zlib compressed) - 0x8F939A

Only files with version 302 and 303 are found to have 5 of these per frame.

3. Null block structure: Just a series of null bytes. Purpose unknown.

    Version 300: Size = 224
    1. CD block: Size is 0x90
    2. No entry blocks
    3. Null block: Size is 0x50

    Version 301: Size = 228
    1. 1 int32 value
    2. CD block: Size is 0x90
    3. No entry blocks
    4. Null block: Size is 0x50

    Version 302: Size = 348
    1. 1 int32 value
    2. CD block: Size is 0x60
    3. 6 entry blocks
    4. Null block: Size is 0x50

    Version 303: Size = 352
    1. 1 int32 value
    2. CD block: Size is 0x60
    3. 6 entry blocks
    4. Null block: Size is 0x54
</pre>
