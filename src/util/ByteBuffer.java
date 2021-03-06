/*
 * Copyright 2019 sjs@kana-tutor.com
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package util;
/*
 * ByteBuffer is meant to be similar to StringBuffer.
 */

import java.io.IOException;

@SuppressWarnings("ALL")
public class ByteBuffer {
    private static final String TAG = "ByteBuffer";

    private byte[] buffer;
    private int end = 0, readPointer = 0;
    private static final int blockSize = 256;

    public ByteBuffer(int initialSize) {
        buffer = new byte[initialSize];
    }
    public ByteBuffer() {
        this(blockSize);
    }
    // add bytes to the buffer.
    public int append(byte[] appendBuf, int nBytes) {
        while (buffer.length < end + nBytes) {
            byte[] buf = new byte[buffer.length + blockSize];
            System.arraycopy(buffer, 0, buf, 0, end);
            buffer = buf;
        }
        System.arraycopy(appendBuf, 0, buffer, end, nBytes);
        end += nBytes;
        return end;
    }
    public int append(byte[] appendBuf) {
        return append(appendBuf, appendBuf.length);
    }

    // Return as many bytes as possible.  If the user's start is
    // beyond the end of the buffer, return 0 sized array.
    public byte[] getBytes(int start, int length) {
        int end = start + length;
        if (start > end)
            length = 0;
        else if (start + length > end)
            length = end - start;
        byte[] rv = new byte[length];
        if (length > 0)
            System.arraycopy(buffer, start, rv, 0, length);
        return rv;
    }
    public int length() {return end;}
    // return a copy of the buffer.
    public byte[] getBytes() {
        return getBytes(0, end);
    }
    // sequential byte read from buffer.
    // get the next N bytes starting with readPointer
    public int read(byte[] buf, int readSize) throws IOException {
        if (readPointer < end) {
            if (readPointer + readSize > end)
                readSize = end - readPointer;
        }
        else
            readSize = 0;
        if (readSize > buf.length)
            throw new IOException(String.format(
                    "read %d bytes to buffer size %d FAILS",
                    readSize, buf.length)
            );
        System.arraycopy(buffer, readPointer, buf, 0, readSize);
        readPointer += readSize;
        return readSize;
    }
    // fill the buffer if possible with the bytes starting
    // with readPointer
    public int read(byte[] buf) throws IOException {
        return read(buf, buf.length);
    }

    public static void cpBytesBlock(byte[] src, byte[] dest) {
        if (src.length != dest.length) throw new RuntimeException(
                String.format(
                        "%s: cpBytesBlock buffers should be same size..\n"
                                + "Found %d vs %d."
                        , TAG, src.length, dest.length
                )
        );
        System.arraycopy(src, 0, dest, 0, src.length);
    }

}
