package kana_tutor.com;

import Cipher.Blowfish.BlowfishECB;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.exit;
import static kana_tutor.com.SelfTest.bytesToString;

public class VimBlowfish {
    private static final String TAG = "VimBlowfish";

    static class Log {
        static void d(String tag, String mess) {
            System.out.print(tag + "\n" + mess);
        }
        static void i(String tag, String mess) {
            System.out.print(tag + "\n" + mess);
        }
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
            Log.d(TAG, String.format("key:len =%d:\n%s\n"
                    , key.length, bytesToString(key)));
            bf = new BlowfishECB(key);
        }
        public void encrypt(byte[] in, byte[] out) {
            swapEnd(in);
            bf.encrypt(in, out);
            swapEnd(out);
        }
    }
    public static final byte[] VIM_MAGIC = "VimCrypt~03!".getBytes();
    private static byte[] xor(byte[] a, byte[] b) {
        int min = (a.length < b.length) ? a.length : b.length;
        byte[] c = new byte[min];
        for (int i = 0; i < min; i++)
            c[i] = (byte) ((a[i] ^ b[i]) & 0xff);
        return c;
    }
    public static int byteCmp(byte[] buf, byte[] cmpTo) {
        int rv = buf.length - cmpTo.length;
        if (rv == 0) {
            for (int i = 0; i < buf.length && rv == 0; i++)
                rv = (buf[i] & 0xff) - (cmpTo[i] & 0xff);
        }
        return rv;
    }
    private int byteCmp(byte[] buf, String cmpTo) {
        return byteCmp(buf, cmpTo.getBytes());
    }
    private static final int BLOCKSIZE = BlowfishECB.BLOCKSIZE;
    class Reader {
        final int bufSize = 1024;
        private final byte[] inBuf = new byte[bufSize];
        boolean hasVimMagic = false;
        byte[] seed = null; byte[] salt = null;
        int start = 0, end = 0;
        final InputStream inStream;
        byte[] read(int x) throws IOException {
            byte[] rv = new byte[0]; // return 0 lenght on EOF.
            if (x > end - start) {
                // not enough bytes
                if (x > (end - start)) {
                    // first read
                    if (end == 0) {
                        end = inStream.read(inBuf);
                        if (end == -1)
                            throw new IOException("First read failed.");
                    }
                    else {
                        int newEnd = 0;
                        while (start < end)
                            inBuf[newEnd++] = inBuf[start++];
                        end = newEnd;
                        start = 0;
                        int result
                                = inStream.read(inBuf, start, bufSize - start);
                        if (result > 0)
                            end = start + result;
                    }
                    Log.d(TAG, String.format("start = %d, end = %d, buffer:\n%s\n"
                        , start, end, bytesToString(inBuf)));
                }
            }
            if (end - start > 0) {
                // if we're here and x > end - start, we read and
                // probably got nothing.  Return the last few bytes
                // and call it good.  This or next read will return 0
                // bytes which indicates there ain't no more.
                if (x > end - start)
                    x = end - start;
                if (x > 0) {
                    rv = new byte[x];
                    for (int i = 0; i < x && start < end; i++)
                        rv[i] = inBuf[start++];
                }
            }
            return rv;
        }
        boolean isEncrypted() {
            return hasVimMagic;
        }
        byte[] getSeed() {return seed;}
        byte[] getSalt() {return salt;}
        Reader(InputStream inStream) throws IOException {
            this.inStream = inStream;
            if (byteCmp(this.read(VIM_MAGIC.length), VIM_MAGIC) == 0) {
                hasVimMagic = true;
                seed = this.read(BLOCKSIZE);
                salt = this.read(BLOCKSIZE);
                Log.d(TAG, String.format("seed:\n%s\nsalt:\n%s\n",
                    bytesToString(seed),bytesToString(seed)));
           }
           else
               start = 0;
        }
    }
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
        byte[] ciphertext, plaintext, iv, key;
        key = passwordToKey(password, reader.getSeed());
        Cipher bf = new Cipher(key);
        byte[] c0 = new byte[BLOCKSIZE];
        iv = reader.getSalt();
        ciphertext = reader.read(BLOCKSIZE);
        while(ciphertext.length > 0) {
            bf.encrypt(iv, c0);  // iv & c0 are length BLOCKSIZE.
            plaintext = xor(c0, ciphertext); // before final read, ciphertext <= 8.
            /* System.out.println("====\n"
             *  + bytesToString(iv) + "\n"
             *   + bytesToString(c0) + "\n"
             *   + bytesToString(plaintext));
             */
            plaintextOut.write(plaintext);
            iv = ciphertext;
            ciphertext = reader.read(BLOCKSIZE);
            Log.d(TAG, bytesToString(ciphertext) + "\n");
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
        byte[] ciphertext, plaintext, iv, key;
        key = passwordToKey(password, reader.getSalt());
        // Write the header.
        cipherOut.write(VIM_MAGIC);
        cipherOut.write(reader.getSalt());
        cipherOut.write(reader.getSeed());

        Cipher bf = new Cipher(key);
        byte[] c0 = new byte[BLOCKSIZE];
        iv = reader.getSeed();
        plaintext = reader.read(BLOCKSIZE);
        while(plaintext.length > 0) {
            bf.encrypt(iv, c0);  // iv & c0 are length BLOCKSIZE.
            ciphertext = xor(c0, plaintext); // before final read, ciphertext <= 8.
            cipherOut.write(ciphertext);
            iv = ciphertext;
            plaintext = reader.read(BLOCKSIZE);
            Log.d(TAG, bytesToString(ciphertext) + "\n");
        }
    }
    ArrayList<String[]> stackTrace() {
        // class, method, file, line number
        ArrayList<String[]> rv = new ArrayList<>();
        StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
        for (int i = 0; i < stElements.length; i++) {
            StackTraceElement ste = stElements[i];
            rv.add(new String[] {
                    ste.getClassName() + "." + ste.getMethodName()
                    , ste.getFileName()
                    , String.valueOf(ste.getLineNumber())
            });
        }
        return rv;
    }

    VimBlowfish(InputStream inStream, OutputStream outStream, String passwd)
            throws IOException {
        Reader reader = new Reader(inStream);
        try {
            if (reader.isEncrypted()) {
                decrypt(reader, outStream, passwd);
            }
            else
                encrypt(reader, outStream, passwd);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(
                    "failed to create key from password:" + e.getMessage());
        }
    }
    // for testing only, allow caller to set seed & salt.
    VimBlowfish(InputStream inStream, OutputStream outStream, String passwd
                , byte[] seed, byte[] salt)
            throws IOException {
        Reader reader = new Reader(inStream);
        try {
            // make sure we're only called from SelfTest.
            String thisMethod = "kana_tutor.com.VimBlowfish.<init>";
            String testMethod = "kana_tutor.com.SelfTest.<init>";
            ArrayList<String[]> stack = stackTrace();
            for(int i = 0; i < stack.size() - 1; i++) {
                String method = stack.get(i)[0];
                String nextMethod = stack.get(i + 1)[0];
                if(method.equals(thisMethod)) {
                    if (nextMethod.equals(testMethod))
                        break;
                    else
                        throw new RuntimeException(
                            "VimBlowfish with seed and salt should only be called from " + testMethod);
                }
            }
            if (reader.isEncrypted()) {
                throw new RuntimeException("this instance createor is for testing encrypt only.");
            }
            else {
                reader.seed = seed;
                reader.salt = salt;
                encrypt(reader, outStream, passwd);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(
                    "failed to create key from password:" + e.getMessage());
        }
    }
}
