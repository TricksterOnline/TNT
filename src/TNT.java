/*
The NORI Tool (TNT), is a program designed to extract and possibly create NORI
files for the Libre Trickster project.

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
import java.util.zip.*;
import static java.lang.System.in;
import static java.lang.System.out;
import static java.lang.System.err;
/**
Class Description:
The TNT class is the 'main' class of the TNT program (this is obvious).
It essentially is the interface for all functionality of the program.

Dev Notes:
Everything the program does starts and ends here but does not happen here, and
that is what is important to remember. This class should do as little as
possible of the actual computing, but as much of the error checking as it can.
Catching most of the problems with this class allows the complicated code in the
in the other classes to be far cleaner.

Development Priority: HIGH
*/
public class TNT
{
    // Functions ordered by importance to TNT (& thus more likely to be edited)
    // class variables
    public static char mode;
    public static File noriFile;
    public static byte[] fba; // fba: file byte array

    // Main function (keep clean)
    public static void main(String[] args)
    {
        switch(argCheck(args))
        {
        case 1:
            // In order for Analyzer to have its own mode, some code duplication
            // between case 1 and 2 was necessary
            noriFile = new File(args[1]);
            if(noriFile.exists()==true)
            {
                fba = byteLoader(noriFile);
                byte[] ba = inflateIfNeeded(fba);
                Analyze optA = new Analyze(ba,noriFile);
            }
            else
            {
                argErrors(3);
            }
            break;
        case 2:
            noriFile = new File(args[1]);
            if(noriFile.exists()==true)
            {
                fba = byteLoader(noriFile);
                byte[] ba = inflateIfNeeded(fba);
                Extract e = new Extract(ba,noriFile);
            }
            else
            {
                argErrors(3);
            }
            break;
        case 3:
            lackFeature("Create");
            break;
        case 4:
            argErrors(2);
            break;
        case 5:
            argErrors(1);
            break;
        default:
            argErrors(0);
            break;
        }
    }

    // This function loads the file into a byte array, so other functions can
    // access it. Does nothing else. No need to name the file after it ;)
    public static byte[] byteLoader(File file)
    {
        long numBytes = file.length();
        // Max byte array size is ~2GB, which is much longer than any existing
        // NORI file so we don't have to check for a file that is too big.
        byte[] bytes = new byte[(int)numBytes];
        try
        {
            // Loads file into byte array
            bytes = Files.readAllBytes(file.toPath());
        }
        catch (Exception ex)
        {
            out.println("Something donked up (FBA):\n"+ex);
        }
        return bytes;
    }

    // While I would prefer to have this function in the another class but that
    // would necessitate the duplication of this code in both Analyze & Extract
    // or the gathering of data with two bytebuffers if I place it in Analyzer.
    // Thus this is the optimal class for this funct with least code duplication
    // Checks for and decompresses zlib compression if found
    public static byte[] inflateIfNeeded(byte[] in)
    {
        int fsig=0, decompsz=0, cmpdatasz=0;
        boolean fsb = false;
        ByteBuffer bb = ByteBuffer.wrap(in).order(ByteOrder.LITTLE_ENDIAN);
        fsig = bb.getInt();
        fsb = (fsig!=1230131022);
        decompsz = bb.getInt();
        cmpdatasz = bb.getInt();
        if(fsb && in[12]==0x78 && (in[13]==0x01||in[13]==0x9C||in[13]==0xDA))
        {
            byte[] tmp = new byte[decompsz];
            try
            {
                // Inflater class expects the zlib header to be included unless
                // initialized to Inflater(true) instead of Inflater()
                Inflater dcmp = new Inflater();
                // Loads input byte array, start offset, compressed data size
                dcmp.setInput(in,12,cmpdatasz);
                // inflate funct takes in the recipient array and loads it with
                // the result. Also has a return value: the decompressed size
                int realsz = dcmp.inflate(tmp);
                dcmp.end();
                if(decompsz==realsz) out.println("Decompression successful!");
            }
            catch (Exception ex)
            {
                out.println("Something donked up (INFLATE):\n"+ex);
            }
            return tmp;
        }
        else
        {
            return in;
        }
    }

    // Determines validity of cmd-line args & returns the resulting case number
    // This exists as a function because it would make main() ugly if it didn't.
    public static int argCheck(String[] args)
    {
        int argResult = 0;
        // check for existence of mode argument of the correct length
        if (args.length !=0 && args[0].length()==1)
        {
            // assign first argument to mode
            mode = args[0].charAt(0);
            char m = mode; // shorten for boolean checks

            // This 'if' tree checks for valid mode arg and correct # of args
            // for the given mode. Invokes helpful error messages on failure.
            if(m =='a' && args.length ==2)
                argResult = 1;
            else if(m =='e' && args.length ==2)
                argResult = 2;
            else if(m =='c' && args.length ==4)
                argResult = 3;
            else if((m =='e'|| m =='a') && args.length!=2)
                argResult = 4;
            else if(m =='c' && args.length!=4)
                argResult = 4;
            else
                argResult = 5;
        }
        return argResult;
    }

    // Invalid command-line arguments responses
    public static void argErrors(int argErrorNum)
    {
        int errNum = argErrorNum;
        String errMsg0,errMsg1,errMsg2,errMsg3,errMsg4,errMsg5,errMsg6;
        errMsg0 ="Error: Mode argument is too long or missing";
        errMsg1 ="Error: Invalid Mode argument";
        errMsg2 ="Error: Incorrect number of arguments for this Mode";
        errMsg3 ="Error: File does not exist. Nice try.";
        switch(errNum)
        {
        case 0:
            out.println(errMsg0);
            break;
        case 1:
            out.println(errMsg1);
            break;
        case 2:
            out.println(errMsg2);
            break;
        case 3:
            out.println(errMsg3);
            break;
        default:
            out.println("Unknown Error");
            break;
        }
        usage();
    }

    // Standard usage output, explaining available modes and required arguments
    public static void usage()
    {
        String cr, use, cols, bord, optA, optE, optC;
        cr = "The NORI Tool (TNT)\n"+
             "Copyright (C) 2014-2018 Libre Trickster Team\n"+
             "License: GPLv3+\n\n";

        use ="Usage: java {-jar} TNT{.jar} {mode} {/path/file.nri} {etc}\n";
        cols="| Mode |           Arguments           | Description      |\n";
        bord="|=========================================================|\n";
        optA="|  a   | [filename]                    | Analyze          |\n";
        optE="|  e   | [filename]                    | Extraction       |\n";
        optC="|  c   | [filename] [srcFileDir] [fmt] | Create           |\n";

        // Actual output function
        out.println("\n"+cr+use+bord+cols+bord+optA+optE+optC+bord);
    }

    // Standardized 'missing feature' response
    public static void lackFeature(String feature)
    {
        String errMsg, ftrName = feature;
        errMsg ="\nSorry, the '"+ftrName+"' feature is not currently available";
        out.println(errMsg);
    }
}

