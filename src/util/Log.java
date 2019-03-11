package util;

// emulate Android's log functions to simplify moving code to Android.

public class Log {
    public static void d(String tag, String mess) {
        System.out.print(tag + ": " + mess + "\n");
    }
    public static void i(String tag, String mess) {
        System.out.print(tag + ": " + mess + "\n");
    }
}
