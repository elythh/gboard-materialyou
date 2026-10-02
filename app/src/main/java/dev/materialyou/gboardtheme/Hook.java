package dev.materialyou.gboardtheme;

import android.content.Context;
import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.view.inputmethod.EditorInfo;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Entry point (assets/xposed_init). Uses only public framework methods, so it does not depend
 * on Gboard's obfuscated classes and survives Gboard updates and Morphe package renames.
 */
public final class Hook implements IXposedHookLoadPackage {
    private static final Set<Class<?>> HOOKED_SERVICES = new HashSet<>();
    private static boolean installed;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam param) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return; // no Material You below Android 12
        synchronized (Hook.class) {
            if (installed) return;
            installed = true;
        }
        // Every IME must call super.onCreate(), so this fires reliably and hands us Gboard's
        // real service class without knowing its (obfuscated) name.
        XposedHelpers.findAndHookMethod(InputMethodService.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam hook) {
                InputMethodService service = (InputMethodService) hook.thisObject;
                hookService(service.getClass());
                ThemeUpdater.requestUpdate(service);
            }
        });
        Log.i("Loaded in " + param.packageName);
    }

    private static void hookService(Class<?> service) {
        synchronized (HOOKED_SERVICES) {
            if (!HOOKED_SERVICES.add(service)) return;
        }
        XC_MethodHook refresh = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam hook) {
                ThemeUpdater.requestUpdate((Context) hook.thisObject);
            }
        };
        try {
            // getMethod returns the most-derived override, which the framework always calls.
            Method startInputView = service.getMethod("onStartInputView", EditorInfo.class, boolean.class);
            XposedBridge.hookMethod(startInputView, refresh);
            Method configurationChanged = service.getMethod("onConfigurationChanged", Configuration.class);
            XposedBridge.hookMethod(configurationChanged, refresh);
            Log.i("Hooked " + service.getName());
        } catch (Throwable t) {
            Log.e("Could not hook " + service.getName(), t);
        }
    }
}
