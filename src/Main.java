/*
The NORI Tool (TNT), is a program designed to extract & create NORI files for
the Libre Trickster project.

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
import java.util.zip.*;
import static java.lang.System.in;
import static java.lang.System.out;
import static java.lang.System.err;
/**
Class Description:
The Main class is the 'main' class of the TNT program (this is obvious).
It's essentially the interface for all functionality of the program.

Dev Notes:
Everything the program does starts and ends here but does not happen here, and
that is what is important to remember. This class should do as little as
possible of the actual computing, but as much of the error checking as it can.
Catching most of the problems with this class allows the complicated code in the
in the other classes to be far cleaner.

Development Priority: HIGH
*/
public class Main
{
    // Functions ordered by importance to TNT (& thus more likely to be edited)
    // class variables
    public static char mode;
    public static boolean argsBool=false;
    public static int argsLen=0;
    public static File noriFile, cfg;
    public static byte[] fba; // fba: file byte array

    // Main function (keep clean)
    public static void main(String[] args)
    {
        NORI nf = new NORI();
        argsLen = args.length;
        argsBool = argCheck(args);
        if(argsBool)
        {
            if(mode!='c')
            {
                for(int i=1; i < argsLen; i++)
                {
                    noriFile = new File(args[i]);
                    if(noriFile.exists())
                    {
                        nf.setNFileVars(noriFile,0);
                        nf.checkDir();
                        fba = byteLoader(noriFile);
                        runMode(inflateIfNeeded(fba),nf);
                    }
                    else
                    {
                        argErrors(3);
                    }
                }
            }
            else
            {
                Create optC = new Create(cfg,args[2],nf);
            }
        }
    }

    // This function loads the file into a byte array, so other functions can
    // access it. Does nothing else. No need to name the file after it ;)
    private static byte[] byteLoader(File file)
    {
        // Max byte array size is ~2GB, which is much longer than any existing
        // NORI file so we don't have to check for a file that is too big.
        byte[] bytes = new byte[(int)file.length()];
        try
        {
            // Loads file into byte array
            bytes = Files.readAllBytes(file.toPath());
        }
        catch(Exception ex)
        {
            out.println("Error in (FBA):\n"+ex);
        }
        return bytes;
    }

    // Checks for and decompresses zlib compression if found
    private static byte[] inflateIfNeeded(byte[] in)
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
            catch(Exception ex)
            {
                out.println("Error in (INFLATE):\n"+ex);
            }
            return tmp;
        }
        else
        {
            return in;
        }
    }

    // Runs the selected mode (side bonus: removes code duplication)
    private static void runMode(byte[] ba, NORI nri)
    {
        switch(mode)
        {
        case 'e':
            Extract opte = new Extract(ba,nri,false);
            break;
        case 'E':
            Extract optE = new Extract(ba,nri,true);
            break;
        case 'A':
            Analyze optA = new Analyze(ba,nri,true);
            break;
        default:
            Analyze opta = new Analyze(ba,nri,false);
            break;
        }
    }

    // Determines validity of cmd-line args & returns the resulting case number
    // This exists as a function because it would make main() ugly if it didn't.
    private static boolean argCheck(String[] args)
    {
        boolean argResult = false;
        // check for existence of mode argument of the correct length
        if(argsLen!=0 && args[0].length()==1)
        {
            // assign first argument to mode
            mode = args[0].charAt(0);
            // This 'if' tree checks for valid mode arg and correct # of args
            // for the given mode. Invokes helpful error messages on failure.
            if((mode =='a'||mode =='A') && argsLen >=2)
                argResult = true;
            else if((mode =='e'||mode =='E') && argsLen >=2)
                argResult = true;
            else if(mode =='c' && argsLen ==3)
                argResult = cmArgCheck(args);
            else if((mode =='a'||mode =='A') && argsLen < 2)
                argErrors(2);
            else if((mode =='e'||mode =='E') && argsLen < 2)
                argErrors(2);
            else if(mode =='c' && argsLen !=3)
                argErrors(2);
            else
                argErrors(1);
        }
        else
        {
            argErrors(0);
        }
        return argResult;
    }

    // Verifies the existence of the Create mode parameters
    private static boolean cmArgCheck(String[] args)
    {
        boolean result = false;
        cfg = new File(args[1]);
        if(cfg.exists())
        {
            if(Files.isDirectory(Paths.get(args[2])))
                result = true;
            else
                argErrors(5);
        }
        else
        {
            argErrors(4);
        }
        return result;
    }

    // Invalid command-line arguments responses
    private static void argErrors(int argErrorNum)
    {
        int errNum = argErrorNum;
        String errMsg0,errMsg1,errMsg2,errMsg3,errMsg4,errMsg5,errMsg6;
        errMsg0 ="Error: Mode argument is too long or missing";
        errMsg1 ="Error: Invalid Mode argument";
        errMsg2 ="Error: Incorrect number of arguments for this Mode";
        errMsg3 ="Error: NORI file does not exist. Nice try.";
        errMsg4 ="Error: Config file does not exist. Tragic.";
        errMsg5 ="Error: Specified BMP directory does not exist.";
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
        case 4:
            out.println(errMsg4);
            break;
        case 5:
            out.println(errMsg5);
            break;
        default:
            out.println("Unknown Error");
            break;
        }
        usage();
    }

    // Standard usage output, explaining available modes and required arguments
    private static void usage()
    {
        String cr, use, col, bdr, opa, opA, ope, opE, opc, ex;
        cr = "The NORI Tool (TNT)\n"+
             "Copyright (C) 2014-2020 Libre Trickster Team\n"+
             "License: GPLv3+\n\n";

        use="Usage: java -jar TNT.jar {mode} {/path/file.nri} {etc}\n";
        col="|Mode|        Arguments         | Description                 |\n";
        bdr="===============================================================\n";
        opa="| a  | [filename(s)]            | Analyze NORI files          |\n";
        opA="| A  | [filename(s)]            | Analyze w/ config output    |\n";
        ope="| e  | [filename(s)]            | Extract NORI files          |\n";
        opE="| E  | [filename(s)]            | Extract w/ img subsets      |\n";
        opc="| c  | [example.cfg] [/imgDir/] | Create NORI file            |\n";

        ex ="Example: java -jar TNT.jar a ../ex/path/ntf/all.nri\n";

        // Actual output function
        out.println("\n"+cr+use+bdr+col+bdr+opa+opA+ope+opE+opc+bdr+ex);
    }
}

