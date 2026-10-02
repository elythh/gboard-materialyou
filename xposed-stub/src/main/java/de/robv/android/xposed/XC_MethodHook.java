package de.robv.android.xposed;

import java.lang.reflect.Member;

/** Compile-only stub; the real class is provided by the Xposed framework at runtime. */
public abstract class XC_MethodHook {
    public XC_MethodHook() { }

    protected void beforeHookedMethod(MethodHookParam param) throws Throwable { }

    protected void afterHookedMethod(MethodHookParam param) throws Throwable { }

    public static final class MethodHookParam {
        public Member method;
        public Object thisObject;
        public Object[] args;

        public Object getResult() { throw new UnsupportedOperationException(); }

        public void setResult(Object result) { throw new UnsupportedOperationException(); }
    }

    public class Unhook {
        public void unhook() { throw new UnsupportedOperationException(); }
    }
}
