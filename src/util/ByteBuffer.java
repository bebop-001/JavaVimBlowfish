package util;

public class ByteBuffer {
    private byte[] buffer;
    private int end = 0;
    private static final int blockSize = 256;
    public ByteBuffer(int initialSize) {
        buffer = new byte[initialSize];
    }
    public ByteBuffer() {
        this(blockSize);
    }
    public int append(byte[] appendBuf, int nBytes) {
        while (buffer.length > end + nBytes) {
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
    public byte[] getBytes() {
        byte [] rv = new byte[end];
        System.arraycopy(buffer, 0, rv, 0, end);
        return rv;
    }
}
