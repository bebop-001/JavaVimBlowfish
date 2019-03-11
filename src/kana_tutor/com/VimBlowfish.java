package kana_tutor.com;

import Cipher.Blowfish.BlowfishECB;
import util.ByteBuffer;
import util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static util.ByteBuffer.cpBytesBlock;
import static util.BytesDebug.bytesDebugString;

public class VimBlowfish {
    private static final String TAG = "VimBlowfish";

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
        byte[] key = md.digest();
        Log.d(TAG, String.format("key: len = %d: bytes\n%s"
                , key.length, bytesDebugString(key)));
        return key;
    }
    public static class Cipher {
        final BlowfishECB bf;
        private void swapEnd(byte[] in) {
            byte tmp;
            int[] swo = {0,3,1,2,4,7,5,6};
            for(int i = 0; i < swo.length; i += 2) {
                tmp = in[swo[i]]; in[swo[i]] = in[swo[i+1]]; in[swo[i+1]] = tmp;
            }
        }
        public Cipher(byte[] key) {
            bf = new BlowfishECB(key);
        }
        final byte[] cpIn = new byte[BLOCKSIZE];
        public void encrypt(byte[] in, byte[] out) {
            cpBytesBlock(in, cpIn); // cp so we don't step on in with swap.
            swapEnd(cpIn);
            bf.encrypt(cpIn, out);
            swapEnd(out);
        }
    }
    public static final byte[] VIM_MAGIC = "VimCrypt~03!".getBytes();
    // xor count bytes of block a with block b and put the result in block result.
    private static void xor(byte[] a, byte[] b, byte[] result, int count) {
        for (int i = 0; i < count; i++)
            result[i] = (byte) ((a[i] ^ b[i]) & 0xff);
    }
    private static final int BLOCKSIZE = BlowfishECB.BLOCKSIZE;

    /*
     *             ciphertext[0]    /        ciphertext[N+1]   /
     *                  |           /              |           /
     *                  +-----+     /              +-----+     /
     *                  |     |     /              |     |     /
     *                  v     |     /              v     |     /
     * IV--->D(key)-->(xor)   +-IV->/-->D(key)-->(xor)   +-IV->/  ETC...
     *                  |           /              |           /
     *                  v           /              v           /
     *             plaintext[0]     /         plaintext[N+1]   /
     */
    private void decrypt(Reader reader, OutputStream plaintextOut, String password)
            throws IOException, NoSuchAlgorithmException {
        byte[] ciphertext = new byte[BLOCKSIZE]
            , plaintext = new byte[BLOCKSIZE]
            , c0 = new byte[BLOCKSIZE]
            , seed = new byte[BLOCKSIZE]
            , salt = new byte[BLOCKSIZE]
            , iv,key;
        reader.getSalt(salt);
        reader.getSeed(seed);
        key = passwordToKey(password, salt);
        Cipher bf = new Cipher(key);
        iv = seed;
        int bytesRead = reader.read(ciphertext);
        while(bytesRead > 0) {
            bf.encrypt(iv, c0);  // iv & c0 are length BLOCKSIZE.
            xor(c0, ciphertext, plaintext, bytesRead);
            Log.d(TAG, String.format(
                "decrypt:\niv:\n%s\nc0:\n%s\nciphertext:\n%s\nplaintext:\n%s\n"
                    , bytesDebugString(iv)
                    , bytesDebugString(c0)
                    , bytesDebugString(ciphertext)
                    , bytesDebugString(plaintext)));
            plaintextOut.write(plaintext, 0, bytesRead);
            cpBytesBlock(ciphertext, iv);
            bytesRead = reader.read(ciphertext);
        }
    }
    /*
     *             plaintext[0]     /         plaintext[N+1]   /
     *                  |           /              |           /
     *                  v           /              v           /
     * IV--->D(key)-->(xor)   +-IV->/-->D(key)-->(xor)   +-IV->/  ETC...
     *                  |     |     /              |     |     /
     *                  +-----+     /              +-----+     /
     *                  |           /              |           /
     *                  v           /              v           /
     *             ciphertext[0]    /        ciphertext[N+1]   /
     */
    private void encrypt(Reader reader, OutputStream cipherOut, String password)
        throws IOException, NoSuchAlgorithmException  {
        byte[] ciphertext = new byte[BLOCKSIZE]
            , c0 = new byte[BLOCKSIZE]
            , salt = new byte[BLOCKSIZE]
            , seed = new byte[BLOCKSIZE]
            , plaintext = new byte[BLOCKSIZE]
            , iv = new byte[BLOCKSIZE]
            , key;
        reader.getSalt(salt); reader.getSeed(seed);
        key = passwordToKey(password, salt);
        // Write the header.
        cipherOut.write(VIM_MAGIC);
        cipherOut.write(salt);
        cipherOut.write(seed);

        Cipher bf = new Cipher(key);
        iv = seed;
        int bytesRead = reader.read(plaintext);
        while(bytesRead > 0) {
            bf.encrypt(iv, c0);  // iv & c0 are length BLOCKSIZE.
            xor(c0, plaintext, ciphertext, bytesRead); // before final read, ciphertext <= 8.
            Log.d(TAG, String.format(
                    "encrypt:\niv:\n%s\nc0:\n%s\nciphertext:\n%s\nplaintext:\n%s\n"
                    , bytesDebugString(iv)
                    , bytesDebugString(c0)
                    , bytesDebugString(ciphertext)
                    , bytesDebugString(plaintext)));
            cpBytesBlock(ciphertext, iv);
            cipherOut.write(ciphertext, 0, bytesRead);
            bytesRead = reader.read(plaintext);
        }
    }

    VimBlowfish(InputStream inStream, OutputStream outStream, String passwd)
            throws IOException {
        try {
            Reader reader = new Reader(inStream);
            if (reader.isEncrypted()) {
                    decrypt(reader, outStream, passwd);
            }
            else
                encrypt(reader, outStream, passwd);
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
    // for testing only, allow caller to set seed & salt.
    VimBlowfish(InputStream inStream, OutputStream outStream, String passwd
                , byte[] seed, byte[] salt)
            throws IOException {
        Reader reader = new Reader(inStream);
        try {
            if (reader.isEncrypted()) {
                throw new RuntimeException("this instance createor is for testing encrypt only.");
            }
            else {
                reader.setSeed(seed);
                reader.setSalt(salt);
                encrypt(reader, outStream, passwd);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(
                    "failed to create key from password:" + e.getMessage());
        }
    }
}
