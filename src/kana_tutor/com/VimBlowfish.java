package kana_tutor.com;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class VimBlowfish {


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
    protected static byte[] passwordToKey(String pw, byte[] salt)
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
    private static final byte[] VIM_MAGIC = "VimCrypt~03!'".getBytes();
    private static byte[] xor(byte[] a, byte[] b) {
        int min = (a.length < b.length) ? a.length : b.length;
        byte[] c = new byte[min];
        for (int i = 0; i < min; i++)
            c[i] = (byte) ((a[i] ^ b[i]) & 0xff);
        return c;
    }
    // should be called from encrypt or SelfTest only.
    protected void __encrypt(
        InputStream plaintextIn, OutputStream cryptedOut, String password
        , byte[] seed, byte[] salt) {
    }
    void encrypt(
        InputStream plaintextIn, OutputStream cryptedOut, String password) {
    }
    void decrypt(
        InputStream cryptedIn, OutputStream plaintextOut, String password) {
    }

    boolean isEncrypted = true;
    boolean isVimEncrypted() {return isEncrypted;}
    VimBlowfish(byte[] bytesIn) {
        for(int i= 0; i < VIM_MAGIC.length && isEncrypted && i < bytesIn.length; i++)
            isEncrypted = bytesIn[i] == VIM_MAGIC[i];
    }
}
