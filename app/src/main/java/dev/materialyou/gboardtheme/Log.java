package dev.materialyou.gboardtheme;

import de.robv.android.xposed.XposedBridge;

/** Logs to logcat (tag GboardMaterialYou) and to the Xposed/NPatch log. */
final class Log {
    private static final String TAG = "GboardMaterialYou";

    private Log() { }

    static void i(String message) {
        android.util.Log.i(TAG, message);
        xposed(message, null);
    }

    static void e(String message, Throwable error) {
        android.util.Log.e(TAG, message, error);
        xposed("ERROR " + message, error);
    }

    private static void xposed(String message, Throwable error) {
        try {
            XposedBridge.log(TAG + ": " + message);
            if (error != null) XposedBridge.log(error);
        } catch (Throwable ignored) {
            // Xposed log unavailable; logcat already has it.
        }
    }
}
