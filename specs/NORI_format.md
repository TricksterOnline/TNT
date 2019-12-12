NORI Format Specification
=========================

<pre>
Copyright (C) 2014-2020 Libre Trickster Team

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
+--------------+-----+---------------------------------------------------------+
| Name         |Bytes| Description                                             |
+--------------+-----+---------------------------------------------------------+
| signature    |  4  | NORI file signature                                     |
+--------------+-----+---------------------------------------------------------+
| version/type |  4  | NORI format version (300, 301, 302, 303)                |
+--------------+-----+---------------------------------------------------------+
| nParam_01-05 | 20  | Unidentified data                                       |
+--------------+-----+---------------------------------------------------------+
| anims        |  4  | Number of included animations                           |
+--------------+-----+---------------------------------------------------------+
| withoutGawi  |  4  | fsize - GAWI Section size                               |
+--------------+-----+---------------------------------------------------------+
| fsize        |  4  | NORI file size                                          |
+--------------+-----+---------------------------------------------------------+
+------------------------------------------------------------------------------+
| END OF NORI HEADER                                                           |
+------------------------------------------------------------------------------+
+------------------------------------------------------------------------------+
| GAWI SECTION                                                                 |
+------------------------------------------------------------------------------+
+------------------------------------------------------------------------------+
| GAWI Header: Imageset Section Header                                         |
+--------------+-----+---------------------------------------------------------+
| Name         |Bytes| Description                                             |
+--------------+-----+---------------------------------------------------------+
| signature    |  4  | 'GAWI' Identifier of the GAWI section                   |
+--------------+-----+---------------------------------------------------------+
| version      |  4  | Version of the GAWI section format (always 300)         |
+--------------+-----+---------------------------------------------------------+
| bpp          |  4  | Bit depth of the image (8, 16, or 24)                   |
+--------------+-----+---------------------------------------------------------+
| compressed   |  4  | Image compression flag (1 is yes) (RLE compression)     |
+--------------+-----+---------------------------------------------------------+
| hasPalette   |  4  | Palette usage flag (set to 1, if palette exists)        |
+--------------+-----+---------------------------------------------------------+
| gParam_04-07 | 16  | Unidentified data                                       |
+--------------+-----+---------------------------------------------------------+
| numBMP       |  4  | Number of images                                        |
+--------------+-----+---------------------------------------------------------+
| gsize        |  4  | Size of all image data combined                         |
+--------------+-----+---------------------------------------------------------+
+------------------------------------------------------------------------------+
| END OF GAWI HEADER                                                           |
+------------------------------------------------------------------------------+
+------------------------------------------------------------------------------+
| Palette Data:  Palette Section (may or may not exist)                        |
+--------------+-----+---------------------------------------------------------+
| Name         |Bytes| Description                                             |
+--------------+-----+---------------------------------------------------------+
| signature    |  4  | 'PAL_' Identifier of palette structure                  |
+--------------+-----+---------------------------------------------------------+
| version      |  4  | Version of the PAL_ section format (always 100)         |
+--------------+-----+---------------------------------------------------------+
| pParam_01-04 | 16  | Unidentified data                                       |
+--------------+-----+---------------------------------------------------------+
| divided      |  4  | T/F flag for whether pal is divided into 2 sections.    |
|              |     | They are:[base]&[main]; if =1, pal sect has 2 extra vars|
+--------------+-----+---------------------------------------------------------+
| psize        |  4  | size of entire palette section                          |
+--------------+-----+---------------------------------------------------------+
| [RGB24DATA]  | 768 | raw palette data; see BitmapData                        |
+--------------+-----+---------------------------------------------------------+
| If psize=800, this is End of Palette Section. Otherwise, 2 more variables.   |
+--------------+-----+---------------------------------------------------------+
| main_start   |  4  | Palette index # that is the start of [main] section.    |
|              |     | Standard value is 111. palette[111]                     |
+--------------+-----+---------------------------------------------------------+
| main_end     |  4  | Palette index # that is the end of [main] section.      |
|              |     | Standard value is 254. palette[254]                     |
+--------------+-----+---------------------------------------------------------+
+------------------------------------------------------------------------------+
| END OF PALETTE DATA                                                          |
+------------------------------------------------------------------------------+
+------------------------------------------------------------------------------+
| BMP Offsets: Bitmap Offset Address Info Section                              |
+--------------+-----+---------------------------------------------------------+
| Name         |Bytes| Description                                             |
+--------------+-----+---------------------------------------------------------+
| bmpOffsets   | bos | BMP 1st byte locations; bos=(4)(numBMP); If compressed  |
|              |     | by RLE, add (28)(n) to each location; n=0,n++ each time |
+--------------+-----+---------------------------------------------------------+
+------------------------------------------------------------------------------+
| END OF BMP OFFSETS                                                           |
+------------------------------------------------------------------------------+
+------------------------------------------------------------------------------+
| BitmapData: BMP Data For Each Image                                          |
+--------------+-----+---------------------------------------------------------+
| Name         |Bytes| Description                                             |
+--------------+-----+---------------------------------------------------------+
| data_count   |  4  | When >1, img subset exists, subs lack a bmpOffset value |
+--------------+-----+---------------------------------------------------------+
+--------------+-----+---------------------------------------------------------+
| data_length  |  4  | data size(=sod)                                         |
+--------------+-----+---------------------------------------------------------+
| pic_width    |  4  | Image width (in pixels)                                 |
+--------------+-----+---------------------------------------------------------+
| pic_height   |  4  | Image height (in pixels)                                |
+--------------+-----+---------------------------------------------------------+
| bParam_04    |  4  | Unidentified data (possibly a delay #)                  |
+--------------+-----+---------------------------------------------------------+
| position_x   |  4  | The image's X position, usually 0                       |
+--------------+-----+---------------------------------------------------------+
| position_y   |  4  | The image's Y position, usually 0                       |
+--------------+-----+---------------------------------------------------------+
|[RGB24Data]   | sod | 24bit img; bit fmt: B8G8R8 (3Bytes)                     |
|[RGB16Data]   | sod | 16bit img; bit fmt: R5G5B5 [nRRRRRGG GGGBBBBB] (2Bytes) |
|[PalIndexData]| sod |  8bit img; bit fmt: 1 index # = 1 unsigned byte         |
+--------------+-----+---------------------------------------------------------+
+------------------------------------------------------------------------------+
| END OF BMP DATA                                                              |
+------------------------------------------------------------------------------+
</pre><pre>
+------------------------------------------------------------------------------+
| AnimOffsetData: Animation Offset Address Info                                |
+--------------+-----+---------------------------------------------------------+
| Name         |Bytes| Description                                             |
+--------------+-----+---------------------------------------------------------+
| animOffsets  | aos | Animation first byte locations, aos = (4)(anims)        |
+--------------+-----+---------------------------------------------------------+
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
+--------------+-----+---------------------------------------------------------+
| Name         |Bytes| Description                                             |
+--------------+-----+---------------------------------------------------------+
| animName     | 32  | Animation name (uses EUC-KR encoding)                   |
+--------------+-----+---------------------------------------------------------+
| frames       |  4  | Number of frames(=nof)                                  |
+--------------+-----+---------------------------------------------------------+
| frameOffsets | fos | Frame first byte locations, fos = (4)(nof)              |
+--------------+-----+---------------------------------------------------------+
+------------------------------------------------------------------------------+
| FrameData: Basic Data For Each Frame in Every Animation                      |
+--------------+-----+---------------------------------------------------------+
| Name         |Bytes| Description                                             |
+--------------+-----+---------------------------------------------------------+
| delay        |  4  | Duration of this frame (milliseconds)                   |
+--------------+-----+---------------------------------------------------------+
| plane_count  |  4  | Number of planes(=nop)                                  |
+--------------+-----+---------------------------------------------------------+
+------------------------------------------------------------------------------+
| PlaneData: Basic Data For Each Plane in Every Frame                          |
+--------------+-----+---------------------------------------------------------+
| Name         |Bytes| Description                                             |
+--------------+-----+---------------------------------------------------------+
| bitmap_id    |  4  | BMP ID number for plane (# from 0 to (numBMP-1))        |
+--------------+-----+---------------------------------------------------------+
| point_x      |  4  | Location coordinates x                                  |
+--------------+-----+---------------------------------------------------------+
| point_y      |  4  | Location coordinates y                                  |
+--------------+-----+---------------------------------------------------------+
| opacity      |  4  | Always 100, as in 100%, from what I've seen             |
+--------------+-----+---------------------------------------------------------+
| flip_axis    |  4  | Defines the flippable image axis; 0 = horiz, 1 = vert   |
+--------------+-----+---------------------------------------------------------+
| blend_mode   |  4  | ADD, MULTY, INVMULTY, etc                               |
+--------------+-----+---------------------------------------------------------+
| flag_param   |  4  | Seen 14,15,16,& 32; a flag for something                |
+--------------+-----+---------------------------------------------------------+
+------------------------------------------------------------------------------+
| ExtraData: Extra Data After Every Frame                                      |
+--------------+-----+---------------------------------------------------------+
| NORI Version |Bytes| Description                                             |
+--------------+-----+---------------------------------------------------------+
| v300         | 224 | Possibly Sound Effect Specification, otherwise unknown  |
+--------------+-----+---------------------------------------------------------+
| v301         | 228 | Possibly Sound Effect Specification, otherwise unknown  |
+--------------+-----+---------------------------------------------------------+
| v302         | 348 | Possibly Sound Effect Specification, otherwise unknown  |
+--------------+-----+---------------------------------------------------------+
| v303         | 352 | Possibly Sound Effect Specification, otherwise unknown  |
+--------------+-----+---------------------------------------------------------+
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
+--------------+-----+---------------------------------------------------------+
| Name         |Bytes| Description                                             |
+--------------+-----+---------------------------------------------------------+
| encodedSize  |  2  | # of bytes to read for this scanline (includes itself)  |
+--------------+-----+---------------------------------------------------------+
+--------------+-----+---------------------------------------------------------+
| bg_pixels    |  2  | # of bg pixels; no color specified, you choose it       |
+--------------+-----+---------------------------------------------------------+
| fg_pixels    |  2  | # of actual foreground pixels that follow  (=fg)        |
+--------------+-----+---------------------------------------------------------+
| fg_data      | fgs | Actual foreground pixels.  fgs = (fg)(bpp/8)            |
+--------------+-----+---------------------------------------------------------+
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
+--------------+-----+---------------------------------------------------------+
| Name         |Bytes| Description                                             |
+--------------+-----+---------------------------------------------------------+
| eParam_01    |  4  | Value usually = 0xCDCDCDCD (probably unassigned data)   |
+--------------+-----+---------------------------------------------------------+
| eParam_02    |  4  | Unknown (usually null)                                  |
+--------------+-----+---------------------------------------------------------+
| eParam_x     |  4  | Probably X-value                                        |
+--------------+-----+---------------------------------------------------------+
| eParam_y     |  4  | Probably Y-value                                        |
+--------------+-----+---------------------------------------------------------+
| eParam_03-05 | 12  | Unknown (usually null)                                  |
+--------------+-----+---------------------------------------------------------+
+------------------------------------------------------------------------------+
Some map files such as "map_sq00.bac" (Megalopolis Square) are found to have
non-zero eParam_x and eParam_y for certain frames.
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
