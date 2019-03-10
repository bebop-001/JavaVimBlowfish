package kana_tutor.com;

import Cipher.Blowfish.BlowfishECB;
import util.ByteBuffer;
import util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static kana_tutor.com.VimBlowfish.*;
import static Cipher.Blowfish.BlowfishECB.BLOCKSIZE;
import static util.BytesDebug.*;


public class SelfTest {
    private static final String TAG = "SelfTest";

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
    private void passwordTest() throws Exception {
        byte[] key;
        boolean passed = true;
        key = VimBlowfish.passwordToKey(testPassword, testSalt);
        // System.out.println("key:\n" + bytesDebugString(key));
        if (key.length == debugKey.length) {
            for(int i = 0; i < debugKey.length && passed; i++) {
                passed = debugKey[i] == key[i];
            }
        }
        if (!passed) throw new Exception(
            "Password test failed. expected:\n"
                + bytesDebugString(debugKey) + "\ngot:\n"
                + bytesDebugString(debugKey) + "\n"
        );
    }

    private void testReader() throws Exception {
        // build a long byte array for testing.
        byte[] expected = new byte[1024 + 18];
        char ch = 'A';
        for (int i = 0; i < expected.length; i++) {
            expected[i] = (i > 0 && (i % 1024) == 0) ? (byte)'-' : (byte) ch;
            if (++ch > 'Z') ch = 'A';
        }
        ByteArrayInputStream plaintext = new ByteArrayInputStream(expected);
        Reader reader = new Reader(plaintext);
        ByteBuffer bb = new ByteBuffer(1024);
        byte[] buf = new byte[BLOCKSIZE];
        int bytesRead = reader.read(buf);
        int i = 0;
        while (bytesRead > 0) {
            Log.d(TAG, String.format("%d:buf:\n%s\n", i, bytesDebugString(buf)));
            bb.append(buf, bytesRead);
            bytesRead = reader.read(buf);
        }
        byte[] received = bb.getBytes();
        if (!bytesCmp(expected, received)) throw new Exception(
            "testReader FAILED:\n" + getBytesCmpError() + "\n");
    }

    @SuppressWarnings({"UnusedAssignment", "ConstantConditions"})
    public SelfTest() throws Exception {
        testReader();
        Log.i(TAG,"test reader class passed.");
        passwordTest();
        Log.i(TAG, "password test passed.");

        // test to make sure blowfish is working properly.
        testBlowfish();
        Log.i(TAG, "BlowfishECB selftest passed");

        // test decrypt of known file.
        {
            ByteArrayInputStream encrypted
                = new ByteArrayInputStream(encryptedFile);
            ByteArrayOutputStream plaintext
                = new ByteArrayOutputStream(256);
            new VimBlowfish(encrypted, plaintext, testPassword);
            String result = plaintext.toString();
            if (!result.equals(new String(plaintextFile)))
                throw new Exception(
                    "Decrypt test passed.\nExpected:\n"
                    + bytesDebugString(plaintextFile) + "got:\n"
                    + bytesDebugString(result.getBytes()) + "\n"
            );
        }

        // test encryption with known salt/seed/file.
        {
            ByteArrayInputStream plaintext
                = new ByteArrayInputStream(plaintextFile);
            ByteArrayOutputStream encrypted
                = new ByteArrayOutputStream(256);
            new VimBlowfish(plaintext, encrypted, testPassword
                , testSeed, testSalt);
            byte[] result = encrypted.toByteArray();
            if (!result.equals(new String(plaintextFile)))
                throw new Exception(
                        "Decrypt test passed.\nExpected:\n"
                                + bytesDebugString(encryptedFile) + "got:\n"
                                + bytesDebugString(result) + "\n"
                );
            Log.i(TAG, "test plaintext -> encrypted passed.");
        }

        /*
         * Create a long string to test Reader overflow.
         */
        //noinspection unused
    }
}
