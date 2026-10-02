package de.robv.android.xposed;

import java.lang.reflect.Member;

/** Compile-only stub; the real class is provided by the Xposed framework at runtime. */
public final class XposedBridge {
    private XposedBridge() { }

    public static XC_MethodHook.Unhook hookMethod(Member hookMethod, XC_MethodHook callback) {
        throw new UnsupportedOperationException();
    }

    public static void log(String text) { throw new UnsupportedOperationException(); }

    public static void log(Throwable t) { throw new UnsupportedOperationException(); }
}
