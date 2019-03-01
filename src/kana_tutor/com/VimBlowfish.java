package kana_tutor.com;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class VimBlowfish {

    private static boolean isPrintable(byte b) {
        return b >= ' ' && b <= '~';
    }

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

    /*
        from Python vimBlowfish
        import hashlib
        def pwToKey(pw, salt):
          for i in range(1000):
            pw = hashlib.sha256(pw.encode() + salt).hexdigest()
        return hashlib.sha256(pw.encode() + salt).digest()

        For salt = 09 5b 17 da dc d0 b7, pw = 'hello',
        result should be 32 bytes:
          f0 28 69 c9 2c 50 c3 | c2 d7 2c 4e 41 12 57 2b
          64 5e 44 f1 36 51 30 | 6e 68 3c 95 d3 84 41 f1

        First pass, bytes for 'hello' + seed is:
          68 65 6c 6c 6f 09 5b | da dc d0 b7 16
        hashlib.sha256(x).hexdigest() gives 64 byte string:
        'd7dc49413209238b057a8613deff90a0c1d8b0ffb34e484e7d85b31d0d7fce15'

    */
    // calculate the hex-digest of the bytes in.  Hex-digest is just
    // a hex con-concatonization string of the bytes.
    private static String hexdigest(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes)
            sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }
    // after first pass, byte digest will always be the same size.  Use
    // the same array as much as possible since it's accessed in a 1000
    // loop for.
    private static byte[] byteDigest = null;
    private static byte[] vimDigest(String hexdigest, byte[] salt) {
        byte[] hd = hexdigest.getBytes();
        int l = hd.length + salt.length;
        if (byteDigest == null || byteDigest.length  != l)
            byteDigest = new byte[l];
        for (int i = 0; i < l; i++) {
            if (i < hd.length)
                byteDigest[i] = hd[i];
            else
                byteDigest[i] = salt[i - hd.length];
        }
        return byteDigest;
    }
    private static byte[] passwordToKey(String pw, byte[] salt)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(vimDigest(pw, salt));
        String hd;
        for(int i = 0; i < 1000; i++) {
            hd = hexdigest(md.digest());
            md.update(vimDigest(hd, salt));
        }
        byteDigest = null;
        return md.digest();
    }
    private static byte[] xor(byte[] a, byte[] b) {
        int min = (a.length < b.length) ? a.length : b.length;
        byte[] c = new byte[min];
        for (int i = 0; i < min; i++)
            c[i] = (byte) ((a[i] ^ b[i]) & 0xff);
        return c;
    }

    boolean passwordTest() {
        String password = "hello";
        byte[] seed = {
                (byte)0x09, (byte)0x5b, (byte)0x17, (byte)0xda
                , (byte)0xdc, (byte)0xd0, (byte)0xb7, (byte)0x16
        };
        byte[] expectedResult = {
            (byte)0xf0, (byte)0x28, (byte)0x69, (byte)0xc9
            , (byte)0x2c, (byte)0x50, (byte)0xc3, (byte)0x5a
            , (byte)0xc2, (byte)0xd7, (byte)0x2c, (byte)0x4e
            , (byte)0x41, (byte)0x12, (byte)0x57, (byte)0x2b
            , (byte)0x64, (byte)0x5e, (byte)0x44, (byte)0xf1
            , (byte)0x36, (byte)0x51, (byte)0x30, (byte)0xea
            , (byte)0x6e, (byte)0x68, (byte)0x3c, (byte)0x95
            , (byte)0xd3, (byte)0x84, (byte)0x41, (byte)0xf1
        };

        byte[] key;
        boolean passed = true;
        try {
            key = passwordToKey(password, seed);
            // System.out.println("key:\n" + bytesToString(key));
            if (key.length == expectedResult.length) {
                for(int i = 0; i < expectedResult.length && passed; i++) {
                    passed = expectedResult[i] == key[i];
                }
            }
        } catch (NoSuchAlgorithmException e) {
            passed = false;
            e.printStackTrace();
        }
        return passed;
    }

    VimBlowfish() {
    }


}



