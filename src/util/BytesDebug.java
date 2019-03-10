package util;

public class BytesDebug {

    private static boolean isPrintable(byte b) {
        return b >= ' ' && b <= '~';
    }

    // debug function prints debug-like output for bytes in.
    @SuppressWarnings("StringConcatenationInLoop")
    public static String bytesDebugString(byte[] bytesIn, int start, int length) {
        String rv = "";
        StringBuffer sb;
        int end = start + length;
        while (start < end) {
            sb = new StringBuffer(String.format("%4d)", start));
            for (int j = 0; j < 16 && start + j < end; j++) {
                sb.append(String.format(" %02x", bytesIn[start + j] & 0xff));
                if (j == 7 && bytesIn.length > 7) sb.append(" |");
            }
            while (sb.length() < 58) sb.append(" ");
            for (int j = 0; j < 16 && start + j < end; j++) {
                byte b = bytesIn[start + j];
                sb.append(String.format("%c", (isPrintable(b)) ? (char) b : 'ø'));
                if (j == 7) sb.append(" | ");
            }
            if (end > start + 16)
                sb.append("\n");
            start += 16;
            rv += sb.toString();
        }
        return rv;
    }

    public static String bytesDebugString(byte[] bytesIn) {
        return bytesDebugString(bytesIn, 0, bytesIn.length);
    }

    private static String byteCmpErrors = "";

    public static String getBytesCmpError() {
        return byteCmpErrors;
    }

    private static boolean cmpByteValues(
            byte[] buf1, byte[] buf2, int start, int length) {
        int rv = 0;
        int idx = start;
        while (idx < start + length && rv == 0) {
            rv = (buf1[idx] & 0xff) - (buf2[idx] & 0xff);
            idx++;
        }
        if (rv != 0)
            byteCmpErrors = String.format(
                    "Strings differ. index = %d, buf 1 = 0x%02x, buf 2 = 0x%02x"
                    , idx - 1, buf1[idx - 1], buf2[idx - 1]
            );
        return rv == 0;
    }

    public static boolean bytesCmp(byte[] buf1, byte[] buf2) {
        byteCmpErrors = "";
        boolean rv = buf1.length == buf2.length;
        if (!rv)
            byteCmpErrors = String.format(
            "array lengths differ: %d vs %d"
            , buf1.length, buf2.length);
        else rv = cmpByteValues(buf1, buf2, 0, buf1.length);
        return rv;
    }

    public static boolean bytesCmp(
            byte[] buf1, byte[] buf2, int start, int length) {
        int l = start + length;
        boolean rv = buf1.length <= l && buf2.length <= l;
        if (!rv) {
            String err = "";
            if (buf1.length > l) err += String.format(
                "Buffer 1 length < start + length: "
                + "buffer 1 length = %d, start + length = %d"
                , buf1.length, l);
            if (buf2.length > l) {
                if (!err.equals("")) err += "\n";
                err += String.format(
                        "Buffer 2 length < start + length: "
                                + "buffer 2 length = %d, start + length = %d"
                        , buf2.length, l);
            }
        }
        else rv = cmpByteValues(buf1, buf2, start, length);
        return rv;
    }
}