import kana_tutor.com.SelfTest;
import kana_tutor.com.VimBlowfish;

import java.io.*;
import java.util.jar.JarEntry;

import static util.BytesDebug.bytesDebugString;

public class Main {
    private static final String USAGE
    = "USAGE: JavaVimBlowfish [-t -f] inFile outFile [password]\n"
    + "  -t  Run the selftest, print results and exit\n"
    + "  -f  force write to the output file if it exists.  If not set and the\n"
    + "      output file exists, we abort.\n"
    + "  inFile   the input file\n"
    + "  outFile  the output file\n"
    + "  password the encrypt/decrypt password.  Optional unless inFile or outFile\n"
    + "     is stdin opr stdout.\n"
    + "     We examine the input file and if it is a non-encrypted file, we \n"
    + "  encrypt the result and send it to outFile.  Likewise, if the input\n"
    + "  is encrypted we decrypt and send the output to outFile.\n"
    + "     '-' for inFile/outFile means send input/output to stdin/stdout.\n"
    + "     If inFile or outFile is to stdin/stdout, a command line password\n"
    + "  must be supplied.\n"
    + "\n";

    // safe equals.  Test null false and string equals.
    private static boolean sEquals(String s, String val) {
        return (s != null && s.equals(val));
    }
    public static void main(String[] args) {

        boolean doSelfTest = false, force = false;
        String inFileName = null, outFileName = null, password = null;
        try {
            InputStream inStream = null;
            OutputStream outputStream = null;
            if (args.length == 0) throw new Exception(
                "No arguments"
            );
            else for (String arg : args) {
                if (arg.startsWith("-")) {
                    if (arg.equals("-")) {
                        if (inFileName == null) inFileName = arg;
                        else if (outFileName == null) outFileName = arg;
                        else throw new Exception(
                                    "arg \"-\" not valid here.  "
                                            + "  inFileName and outFileName are already "
                                            + "assigned."
                            );
                    }
                    else if (arg.equals("-t")) {
                        doSelfTest = true;
                        if (args.length > 1) throw new Exception(
                                "for self test,  -t is only arg.  \""
                                        + String.join(" ", args) + "\" not valid."
                        );
                    }
                    else if (arg.equals("-f")) force = true;
                    else throw new Exception(
                                "Unrecognized '-' argument:" + arg
                        );
                }
                else if (inFileName == null) inFileName = arg;
                else if (outFileName == null) outFileName = arg;
                else if (password == null) password = arg;
                else throw new Exception(
                        "all arguments already assigned."
                );
            }
            if (doSelfTest) {
                try {
                    new SelfTest();
                } catch (Exception e) {
                    System.err.println("Self test failed:" + e.getLocalizedMessage());
                    System.exit(1);
                }
                System.exit(0);
            }
            if (password == null) {
                if (inFileName == null || outFileName == null) throw new Exception(
                        "Input or output file not supplied."
                );
                if (sEquals(inFileName,"-") || sEquals(outFileName, "-"))
                    throw new Exception("if stdin/stdout is used for input/output,"
                        + " password must be assigned on command line."
                );
                Console cons = System.console();
                if(cons == null) throw new RuntimeException(
                        "Can not getPassword on this system.  If you are running in an IDE\n"
                        + "the IDE uses stdin/stdout and getPassword won't work."
                );
                String verify = "";
                while (password == null
                        || password.length() == 0
                        || !verify.equals(password)) {
                    password = String.valueOf(cons.readPassword("Password:"));
                    if (password.length() == 0) {
                        System.err.println("Please enter a password.");
                        continue;
                    }
                    verify = String.valueOf(cons.readPassword("Verify Password:"));
                    if (!verify.equals(password))
                        System.err.println("Verify failed.");
                }
            }
            if (inFileName != null) {
                if (inFileName.equals(("-"))) {
                    inStream = System.in;
                }
                else {
                    File inFile = new File(inFileName);
                    if (!inFile.exists())
                        throw new IOException(String.format("Input file % does not exist."
                                , inFileName));
                    if (!inFile.canRead())
                        throw new IOException(String.format("Input file % is non=read."
                                , inFileName));
                    if (inFile.length() <= 8)
                        throw new IOException(String.format(
                            "Input file %s must contain at least 8 bytes.  "
                            + "Length is %d bytes."
                            , inFileName, inFile.length()));
                    inStream = new FileInputStream(inFile);
                }
                /*
                byte[] buf = new byte[1024];
                int bytesRead = 0, totalbytes = 0;
                while((bytesRead = inStream.read(buf, 0, buf.length)) > 0) {
                    System.out.println(bytesDebugString(buf, 0, bytesRead, totalbytes));
                    totalbytes += bytesRead;
                }
                System.out.println(String.format("total bytes:%d", totalbytes));
                */
            }
            if (outFileName != null) {
                if (outFileName.endsWith("-")) {
                    outputStream = System.out;
                }
                else {
                    File outFile = new File(outFileName);
                    if (outFile.exists() && !force) throw new IOException(String.format(
                            "Output file %s exists and no -f force option was set.\n"
                                    + "Please either remove the file or use the '-f' option"
                            , outFile.getAbsoluteFile())
                    );
                    outputStream = new FileOutputStream(outFile);
                }
                /*
                outputStream.write("hello world\n".getBytes());
                outputStream.close();
                */
            }
            new VimBlowfish(inStream, outputStream, password);
        }
        catch (Exception e) {
            System.err.print(USAGE + e.getMessage() + "\n");
            System.exit(1);
        }
        // command line parsed ok.

        System.exit(0);
    }
}
