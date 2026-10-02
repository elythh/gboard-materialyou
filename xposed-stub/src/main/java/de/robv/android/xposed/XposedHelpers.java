package de.robv.android.xposed;

/** Compile-only stub; the real class is provided by the Xposed framework at runtime. */
public final class XposedHelpers {
    private XposedHelpers() { }

    public static XC_MethodHook.Unhook findAndHookMethod(
            Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        throw new UnsupportedOperationException();
    }
}
