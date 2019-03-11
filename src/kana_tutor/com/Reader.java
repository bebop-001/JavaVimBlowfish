package kana_tutor.com;

import util.Log;

import java.io.IOException;
import java.io.InputStream;

import static kana_tutor.com.VimBlowfish.VIM_MAGIC;
import static Cipher.Blowfish.BlowfishECB.BLOCKSIZE;
import static util.BytesDebug.bytesCmp;
import static util.BytesDebug.bytesDebugString;

public class Reader {
    private static final String TAG = "Reader";
    private final int bufSize = 1024;
    private final byte[] inBuf = new byte[bufSize];
    private boolean hasVimMagic = false;
    private final byte[] seed = new byte[BLOCKSIZE]
            , salt = new byte[BLOCKSIZE];
    private int start = 0, end = 0;
    private final InputStream inStream;
    @SuppressWarnings("WeakerAccess")
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
                Log.d(TAG, String.format(
                    "buffer read start = %d, end = %d, buffer:\n%s\n"
                    , start, end, bytesDebugString(inBuf, start, end))
                );
            }
        }
        if (end > start) {
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
    private void cpBytesBlock(String funcName, byte[] src, byte[] dest) {
        if (src.length != dest.length) throw new RuntimeException(
            String.format(
                "%s: buffers should be same size..\n"
                + "Found %d vs %d."
                , src.length, dest.length
            )
        );
        System.arraycopy(src, 0, dest, 0, src.length);
    }
    void getSeed(byte[] seed) {
        cpBytesBlock("seed", this.seed, seed);
    }
    void getSalt(byte[] salt) {
        cpBytesBlock("salt", this.salt, salt);
    }
    void setSeed(byte[] seed) {
        cpBytesBlock("set seed:",seed, this.seed);
    }
    void setSalt(byte[] salt) {
        cpBytesBlock("setSalt:", salt, this.salt);
    }
    Reader(InputStream inStream) throws IOException {
        this.inStream = inStream;
        byte[] fileHead = new byte[VIM_MAGIC.length];
        read(fileHead);
        if (bytesCmp(fileHead, VIM_MAGIC)) {
            hasVimMagic = true;
            read(salt);
            read(seed);
            Log.d(TAG, String.format("seed:\n%s\nsalt:\n%s\n",
                    bytesDebugString(seed),bytesDebugString(salt)));
        }
        else
            start = 0;
    }
}
