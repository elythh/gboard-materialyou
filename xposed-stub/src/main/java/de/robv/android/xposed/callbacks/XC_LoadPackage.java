package de.robv.android.xposed.callbacks;

/** Compile-only stub; the real class is provided by the Xposed framework at runtime. */
public abstract class XC_LoadPackage {
    public static final class LoadPackageParam {
        public String packageName;
        public String processName;
        public ClassLoader classLoader;
        public boolean isFirstApplication;
    }
}
