package kana_tutor.com;

import util.Log;

import java.io.IOException;
import java.io.InputStream;

import static kana_tutor.com.SelfTest.bytesToString;
import static kana_tutor.com.VimBlowfish.VIM_MAGIC;
import static Cipher.Blowfish.BlowfishECB.BLOCKSIZE;

public class Reader {
    private static final String TAG = "Reader";
    public static int byteCmp(byte[] buf, byte[] cmpTo) {
        int rv = buf.length - cmpTo.length;
        if (rv == 0) {
            for (int i = 0; i < buf.length && rv == 0; i++)
                rv = (buf[i] & 0xff) - (cmpTo[i] & 0xff);
        }
        return rv;
    }
    public static void blockCP(byte[] src, byte[] dest) {
        System.arraycopy(src, 0, dest, 0, BLOCKSIZE);
    }

    private final int bufSize = 1024;
    private final byte[] inBuf = new byte[bufSize];
    private boolean hasVimMagic = false;
    private final byte[] seed = new byte[BLOCKSIZE]
            , salt = new byte[BLOCKSIZE];
    private int start = 0, end = 0;
    private final InputStream inStream;
    int read(byte[] buf, int x) throws IOException {
        int rv = 0;
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
                while (rv < x && start < end)
                    buf[rv++] = inBuf[start++];
            }
        }
        return rv;
    }
    int read(byte[] buf) throws IOException {
        return read(buf, buf.length);
    }
    boolean isEncrypted() {
        return hasVimMagic;
    }
    byte[] getSeed() {return seed;}
    byte[] getSalt() {return salt;}
    void setSeed(byte[] seedIn) {blockCP(seedIn, seed);}
    void setSalt(byte[] saltIn) {blockCP(saltIn, salt);}
    Reader(InputStream inStream) throws IOException {
        this.inStream = inStream;
        byte[] fileHead = new byte[VIM_MAGIC.length];
        if (byteCmp(fileHead, VIM_MAGIC) == 0) {
            hasVimMagic = true;
            read(seed);
            read(salt);
            Log.d(TAG, String.format("seed:\n%s\nsalt:\n%s\n",
                    bytesToString(seed),bytesToString(seed)));
        }
        else
            start = 0;
    }
}
