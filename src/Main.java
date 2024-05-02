/*
The NORI Tool (TNT), is a program designed to extract & create NORI files for
the Libre Trickster project.

Copyright (C) 2014-2024 Libre Trickster Team

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
import static java.lang.System.out;
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
// class variables
private static int argsLen=0;
private static char mode;
private static boolean noError,create_mode;
private static String aAe="aAe",cC="cC",RTFM="",dLn="";
private static File nFile, cfg;

// Main function (keep clean)
public static void main(String[] args)
{
    RTFM="See 'NORI_format.md' for information on the XML tags!";
    dLn="=====================================================================";
    argsLen = args.length;
    argCheck(args);
    if(create_mode)
    {
        out.println(dLn);
        switch(mode)
        {
        case 'C':
            Create opt_C = new Create(cfg,args[2],true);
            break;
        default:
            Create opt_c = new Create(cfg,args[2],false);
            break;
        }
    }
    else
    {
        for(int i=1; i < argsLen; i++)
        {
            nFile = new File(args[i]);
            if(nFile.exists()==false) argErrors(3);
            out.println(dLn);
            byte[] nFileBA = zInflate(file2BA(nFile));
            switch(mode)
            {
            case 'e':
                Extract opt_e = new Extract(nFileBA,nFile);
                break;
            case 'A':
                Analyze opt_A = new Analyze(nFileBA,nFile,true);
                break;
            default:
                Analyze opt_a = new Analyze(nFileBA,nFile,false);
                break;
            }
        }
        if(mode=='A') out.println(RTFM);
    }
}

// Checks for and decompresses zlib compression if found
private static byte[] zInflate(byte[] in)
{
    ByteBuffer bb = ByteBuffer.wrap(in).order(ByteOrder.LITTLE_ENDIAN);
    int sig = bb.getInt();
    int sizeExpected = bb.getInt();
    if(sig!=1230131022 && in[12]==0x78)
    {
        byte[] tmp = new byte[sizeExpected];
        try
        {
            // Inflater() expects the zlib header to be included
            Inflater dcmp = new Inflater();
            // Loads input byte array, start offset, compressed data size
            dcmp.setInput(in,12,bb.getInt());
            // Takes in a byte array & loads it with the decompressed result
            int size = dcmp.inflate(tmp);// returns decompressed size
            dcmp.end();
            if(sizeExpected==size) out.println("Decompression successful!\n");
        }
        catch(Exception ex)
        {
            out.println("Error in (INFLATE):");
            ex.printStackTrace(System.out);
        }
        return tmp;
    }
    else
    {
        return in;
    }
}

// Determines validity of cmd-line args & prevents main() from being ugly
private static void argCheck(String[] args)
{
    // check for existence of mode argument of the correct length
    if(args.length!=0 && args[0].length()==1)
    {
        // assign the first argument's first character to mode
        mode = args[0].charAt(0);
        // This 'if' tree checks for valid mode arg and correct # of args
        // for the given mode. Invokes helpful error messages on failure.
        if(aAe.indexOf(mode)>=0)
        {
            if(argsLen < 2) argErrors(2);
        }
        else if(cC.indexOf(mode)>=0)
        {
            if(argsLen !=3) argErrors(2);
            cmCheck(args);
        }
        else
        {
            argErrors(1);
        }
    }
    else
    {
        argErrors(0);
    }
}

// Verifies the existence of the Create mode parameters
private static void cmCheck(String[] args)
{
    create_mode = true;
    cfg = new File(args[1]);
    if(cfg.exists()==false) argErrors(4);
    if(Files.isDirectory(Paths.get(args[2]))==false) argErrors(5);
}

// Invalid command-line arguments responses
private static void argErrors(int errorNum)
{
    String errMsg0,errMsg1,errMsg2,errMsg3,errMsg4,errMsg5,errMsg6;
    errMsg0 ="Error: Mode argument is too long or missing";
    errMsg1 ="Error: Invalid Mode argument";
    errMsg2 ="Error: Incorrect number of arguments for this MODE";
    errMsg3 ="Error: NORI file does not exist. Nice try.";
    errMsg4 ="Error: Config file does not exist. Tragic.";
    errMsg5 ="Error: Specified BMP directory does not exist";
    switch(errorNum)
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
    System.exit(1);
}

// Standard usage output, explaining available modes & required arguments
private static void usage()
{
    String cr, use, col, bdr, opa, opA, ope, opc, opC, ex;
    // You are not allowed to remove this copyright notice or its output
    cr ="The NORI Tool (TNT) - https://github.com/TricksterOnline/TNT\n"+
        "Copyright (C) 2014-2024 Libre Trickster Team\n"+
        "License: GPLv3+\n\n";

    use="Usage: java -jar TNT.jar {mode} {/path/file.nri} {etc}\n";
    col="|Mode|        Arguments         | Description                     |\n";
    bdr="===================================================================\n";
    opa="| a  | [filename(s)]            | Analyze NORI files              |\n";
    opA="| A  | [filename(s)]            | Analyze w/ config file output   |\n";
    ope="| e  | [filename(s)]            | Extract BMPs from NORI files    |\n";
    opc="| c  | [example.cfg] [/imgDir/] | Create NORI file                |\n";
    opC="| C  | [example.cfg] [/imgDir/] | Create w/ zlib-compression      |\n";

    ex ="Example: java -jar TNT.jar a ../ex/path/ntf/all.bac\n";

    // Actual output function
    out.println("\n"+cr+use+bdr+col+bdr+opa+opA+ope+opc+opC+bdr+ex);
}

// An anti-duplication + better readability function
private static byte[] file2BA(File file)
{
    byte[] ba = new byte[(int)file.length()];
    try
    {
        ba = Files.readAllBytes(file.toPath());
    }
    catch(Exception ex)
    {
        out.println("Error in (file2BA):");
        ex.printStackTrace(System.out);
    }
    return ba;
}
}
