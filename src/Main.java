import kana_tutor.com.SelfTest;

public class Main {
    private static final String USAGE
    = "USAGE: JavaVimBlowfish [-t -s -f] inFile outFile [password]\n"
    + "  -t  Run the selftest, print results and exit\n"
    + "  -s  Run the selftest, print results and and save to files, then\n"
    + "      exit\n"
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


    public static void main(String[] args) {

        boolean doSelfTest = false;
        try {
            for (int argc = 0; argc < args.length; argc++) {
                String arg = args[argc];
                if (arg.startsWith("-")) {
                    if (arg.equals("-t")) doSelfTest = true;
                    else
                        throw new Exception("Unrecognized '-' argument:" + arg);
                }
            }
        }
        catch (Exception e) {
            System.err.print(USAGE + e.getMessage() + "\n");
            System.exit(1);
        }
        // command line parsed ok.
        SelfTest selfTest = new SelfTest();
        System.exit(0);
    }
}
