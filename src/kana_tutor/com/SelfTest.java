package kana_tutor.com;

import Cipher.Blowfish.BlowfishECB;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import static kana_tutor.com.VimBlowfish.*;

import static java.lang.System.exit;

public class SelfTest {

    // info gathered by use of vim for use in testing.
    private static final String testPassword = "hello";
    private static final byte[] testSalt = {
        (byte)0x09, (byte)0x5b, (byte)0x17, (byte)0xda
        , (byte)0xdc, (byte)0xd0, (byte)0xb7, (byte)0x16
    };
    private static final byte[] testSeed = {
        (byte)0x52, (byte)0x01, (byte)0xb1, (byte)0x60
        , (byte)0x85, (byte)0x30, (byte)0x9a, (byte)0x7a
    };
    private static final byte[] encrypted = {
        (byte)0xfd, (byte)0xa2, (byte)0x9e, (byte)0x94, (byte)0x7f
        , (byte)0xd4, (byte)0x5f, (byte)0xa6, (byte)0x4d, (byte)0xaa
        , (byte)0x5e, (byte)0x39, (byte)0x6a, (byte)0x14, (byte)0x12
        , (byte)0xc0, (byte)0x7b, (byte)0x59, (byte)0x90, (byte)0xc5
        , (byte)0x54, (byte)0x5b, (byte)0x6e, (byte)0x38, (byte)0xe9
        , (byte)0x6f
    };
    private static final byte[] encryptedFile = new byte[
        VIM_MAGIC.length + testSalt.length + testSalt.length + encrypted.length
    ];
    static {
        int i = 0;
        for (byte b : VIM_MAGIC)    encryptedFile[i++] = b;
        for (byte b: testSalt)      encryptedFile[i++] = b;
        for (byte b: testSeed)      encryptedFile[i++] = b;
        for (byte b: encrypted)     encryptedFile[i++] = b;
    }
    private static final byte[]
        plaintextFile = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes();
    private static final byte[] debugKey = {
        (byte)0xf0, (byte)0x28, (byte)0x69, (byte)0xc9, (byte)0x2c
        , (byte)0x50, (byte)0xc3, (byte)0x5a, (byte)0xc2, (byte)0xd7
        , (byte)0x2c, (byte)0x4e, (byte)0x41, (byte)0x12, (byte)0x57
        , (byte)0x2b, (byte)0x64, (byte)0x5e, (byte)0x44, (byte)0xf1
        , (byte)0x36, (byte)0x51, (byte)0x30, (byte)0xea, (byte)0x6e
        , (byte)0x68, (byte)0x3c, (byte)0x95, (byte)0xd3, (byte)0x84
        , (byte)0x41, (byte)0xf1
    };


    private static boolean isPrintable(byte b) {
        return b >= ' ' && b <= '~';
    }

    /*
     * From experience, I know that the bf encrypt routine from
     * vim must encrypt 0 bytes with this bogus password and
     * give encryptTest as a result.
     */
    private static final byte[] encryptTest = new byte[]{
        (byte)0xdd, (byte)0x0c, (byte)0x25, (byte)0xf9
        , (byte)0x8d, (byte)0x73, (byte)0xc9, (byte)0x37};
    private static void testBlowfish() throws IOException {
        if(! BlowfishECB.selfTest())
            throw new IOException("BlowfishECB selftest Failed!");
        byte[] key = "abcdefghijklmnopqrstuvwxyzABCDEF".getBytes();
        VimBlowfish.Cipher bf = new VimBlowfish.Cipher(key);
        byte[] test = new byte[] {0,0,0,0,0,0,0,0};
        bf.encrypt(test, test);
        boolean pass = true;
        for(int i = 0; i < test.length && pass; i++)
            pass = test[i] == encryptTest[i];
        if (!pass)
            throw new IOException("testBlowfish Failed!");
    }

    // debug function prints debug-like output for bytes in.
    @SuppressWarnings("StringConcatenationInLoop")
    public static String bytesToString(byte[] bytesIn) {
        String rv = "";
        StringBuffer sb;

        for(int i = 0; i < bytesIn.length; i += 16) {
            sb = new StringBuffer(String.format("%3d)", i));
            for (int j = 0; j < 16 && i + j < bytesIn.length; j++) {
                sb.append(String.format(" %02x", bytesIn[i + j] & 0xff));
                if (j == 7 && bytesIn.length > 7) sb.append(" |");
            }
            while (sb.length() < 58) sb.append(" ");
            for (int j = 0; j < 16 && i + j < bytesIn.length; j++) {
                byte b = bytesIn[i + j];
                sb.append(String.format("%c", (isPrintable(b)) ? (char) b : 'ø'));
                if (j == 7) sb.append(" | ");
            }
            if (bytesIn.length > i + 16)
                sb.append("\n");
            rv += sb.toString();
        }
        return rv;
    }

    private boolean passwordTest() {
        byte[] key;
        boolean passed = true;
        try {
            key = VimBlowfish.passwordToKey(testPassword, testSalt);
            // System.out.println("key:\n" + bytesToString(key));
            if (key.length == debugKey.length) {
                for(int i = 0; i < debugKey.length && passed; i++) {
                    passed = debugKey[i] == key[i];
                }
            }
        } catch (NoSuchAlgorithmException e) {
            passed = false;
            e.printStackTrace();
        }
        return passed;
    }

    @SuppressWarnings({"UnusedAssignment", "ConstantConditions"})
    public SelfTest() {
        System.out.println("Password test:"
                + ((passwordTest()) ? "Passed" : "FAILED"));

        try {
            // test to make sure blowfish is working properly.
            testBlowfish();
            System.out.println("BlowfishECB selftest passed");

            // test decrypt of known file.
            ByteArrayInputStream encrypted = new ByteArrayInputStream(encryptedFile);
            ByteArrayOutputStream plaintext = new ByteArrayOutputStream(256);
            new VimBlowfish(encrypted, plaintext, testPassword);
            String result = plaintext.toString();
            if (result.endsWith(new String(plaintextFile)))
                Log.i("Decrypt test passed.\n");
            else
                Log.i("Decrypt test FAILED.\n");
            Log.i("Result = " + result);
            exit(0);

        }
        catch (IOException e) {
            throw new RuntimeException("Selftest FAILED:" + e.getMessage());
        }

        /*
         * Create a long string to test Reader overflow.
         */
        //noinspection unused
        ByteArrayInputStream plaintext = null;
        if (false) {
            char ch = 'A';
            byte[] longString = new byte[0x1024 + 18];
            for (int i = 0; i < longString.length; i++) {
                longString[i] = ((i % 0x1024) == 0) ? (byte)'-' : (byte) ch;
                if (++ch > 'Z') ch = 'A';
            }
            plaintext = new ByteArrayInputStream(longString);
        }
        else {
             plaintext = new ByteArrayInputStream(plaintextFile);
        }
    }
}
