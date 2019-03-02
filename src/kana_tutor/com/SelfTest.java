package kana_tutor.com;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.NoSuchAlgorithmException;

public class SelfTest {

    // info gathered by use of vim for use in testing.
    private static final byte[] VIM_MAGIC = "VimCrypt~03!'".getBytes();
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

    // debug function prints debug-like output for bytes in.
    @SuppressWarnings("StringConcatenationInLoop")
    private static String bytesToString(byte[] bytesIn) {
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
    private boolean testDecrypt() {
        boolean rv = true;
        VimBlowfish bf = new VimBlowfish(encryptedFile);
        if (! bf.isVimEncrypted()) {
            System.err.println(
                "Failed to detect encrypted file as vim encrypted.");
            rv = false;
        }
        ByteArrayInputStream encrypted = new ByteArrayInputStream(encryptedFile);
        ByteArrayOutputStream plaintext = new ByteArrayOutputStream(256);
        bf.decrypt(encrypted, plaintext, testPassword);
        return rv;
    }
    private boolean testEncrypt() {
        boolean rv = true;
        VimBlowfish bf = new VimBlowfish(plaintextFile);
        if (bf.isVimEncrypted()) {
            System.err.println("Failed to detect plaintext file as plaintext");
            rv = false;
        }
        ByteArrayInputStream plaintext
            = new ByteArrayInputStream(plaintextFile);
        ByteArrayOutputStream encrypted
            = new ByteArrayOutputStream(256);
        bf.__encrypt(plaintext, encrypted, testPassword
            , testSeed, testSalt);
        return rv;
    }
    
    SelfTest() {
        System.out.println("Password test:"
                + ((passwordTest()) ? "Passed" : "FAILED"));
        System.out.println("Decrypt test:"
                + ((testDecrypt()) ? "Passed" : "FAILED"));
        System.out.println("Encrypt test:"
                + ((testEncrypt()) ? "Passed" : "FAILED"));

    }
}
