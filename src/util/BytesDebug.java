package util;

public class BytesDebug {

    private static boolean isPrintable(byte b) {
        return b >= ' ' && b <= '~';
    }

    // debug function prints debug-like output for bytes in.
    @SuppressWarnings("StringConcatenationInLoop")
    public static String bytesDebugString(byte[] bytesIn, int start, int length, int startIdx) {
        String rv = "";
        StringBuffer sb;
        int end = start + length;
        while (start < end) {
            sb = new StringBuffer(String.format("%4d)", startIdx));
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
            startIdx += 16;
        }
        return rv;
    }

    public static String bytesDebugString(byte[] bytesIn, int start, int end) {
        return bytesDebugString(bytesIn, start, end, 0);
    }
    public static String bytesDebugString(byte[] bytesIn) {
        return bytesDebugString(bytesIn, 0, bytesIn.length, 0);
    }

    private static String byteCmpErrors = "";

    public static String getBytesCmpError() {
        return byteCmpErrors;
    }

    private static boolean cmpByteValues(
            byte[] buf1, byte[] buf2, int start, int length) {
        if (buf1.length < length) length = buf1.length;
        if (buf2.length < length) length = buf2.length;
        int idx = start;
        boolean rv = true;
        while (idx < start + length) {
            if ((buf1[idx] & 0xff) != (buf2[idx] & 0xff)){
                if (!byteCmpErrors.equals("")) byteCmpErrors += "\n";
                byteCmpErrors = String.format(
                        "Strings differ. offset = %d, buf 1 = 0x%02x, buf 2 = 0x%02x"
                        , idx, buf1[idx], buf2[idx]
                );
                rv = false;
                break;
            }
            idx++;
        }
        return rv;
    }

    public static boolean bytesCmp(byte[] buf1, byte[] buf2) {
        byteCmpErrors = "";
        boolean lengthOK = buf1.length == buf2.length;
        if (!lengthOK)
            byteCmpErrors = String.format(
            "array lengths differ: %d vs %d"
            , buf1.length, buf2.length);
        boolean bytesDifferError
            = cmpByteValues(buf1, buf2, 0, buf1.length);
        return lengthOK && bytesDifferError;
    }

    public static boolean bytesCmp(
            byte[] buf1, byte[] buf2, int start, int length) {
        int l = start + length;
        boolean lengthError = buf1.length <= l && buf2.length <= l;
        if (!lengthError) {
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
        boolean bytesDifferError
            = cmpByteValues(buf1, buf2, start, length);
        return lengthError || bytesDifferError;
    }
}