package dev.materialyou.gboardtheme;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs inside Gboard. When the active theme is a Material You template and the system palette
 * (or dark mode) changed, writes a re-colored copy next to it and points Gboard at the copy.
 */
final class ThemeUpdater {
    private static final String THEME_KEY = "additional_keyboard_theme";
    private static final String FILES_PREFIX = "files:";
    private static final String STATE_PREFS = "materialyou_module_state";
    private static final String GENERATED_KEY = "generated";
    private static final long OLD_THEME_DELETE_DELAY_MS = 5000L;

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final AtomicBoolean RUNNING = new AtomicBoolean();
    private static volatile String lastFingerprint;

    private ThemeUpdater() { }

    static void requestUpdate(Context context) {
        if (!RUNNING.compareAndSet(false, true)) return;
        WORKER.execute(() -> {
            try {
                update(context);
            } catch (Throwable t) {
                Log.e("Theme update failed", t);
            } finally {
                RUNNING.set(false);
            }
        });
    }

    private static void update(Context context) throws IOException {
        SharedPreferences prefs = gboardPrefs(context);
        String value = prefs.getString(THEME_KEY, null);
        if (value == null || !value.startsWith(FILES_PREFIX)) return;

        File filesDir = context.getFilesDir();
        File current = new File(filesDir, value.substring(FILES_PREFIX.length()));
        if (!current.isFile()) return;

        boolean night = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        SystemShades shades = new SystemShades(context.getResources());
        String fingerprint = value + '|' + night + '|' + current.length() + '|'
                + current.lastModified() + '|' + shades.fingerprint();
        if (fingerprint.equals(lastFingerprint)) return;

        byte[] rebuilt = ThemeRewriter.rewrite(readFile(current), shades, night);
        lastFingerprint = fingerprint;
        if (rebuilt == null) return;

        File parent = current.getParentFile();
        File output = newThemeFile(parent);
        File staging = new File(parent, "." + output.getName() + ".part");
        try (FileOutputStream out = new FileOutputStream(staging)) {
            out.write(rebuilt);
            out.getFD().sync();
        }
        if (!staging.renameTo(output)) {
            staging.delete();
            throw new IOException("Cannot move rebuilt theme to " + output);
        }
        String newValue = FILES_PREFIX + filesDir.toURI().relativize(output.toURI()).getPath();
        MAIN.post(() -> activate(context, prefs, value, newValue, current, output));
    }

    private static void activate(Context context, SharedPreferences prefs, String expected,
            String newValue, File previous, File output) {
        if (!expected.equals(prefs.getString(THEME_KEY, null))) {
            output.delete(); // the user picked another theme meanwhile
            return;
        }
        if (!prefs.edit().putString(THEME_KEY, newValue).commit()) {
            output.delete();
            Log.e("Could not switch Gboard to " + newValue, null);
            return;
        }
        Log.i("Applied Material You colors: " + newValue);

        // Only ever delete copies this module wrote, never the theme the user imported.
        SharedPreferences state = context.createDeviceProtectedStorageContext()
                .getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE);
        Set<String> generated = new HashSet<>(state.getStringSet(GENERATED_KEY, new HashSet<>()));
        if (generated.remove(previous.getAbsolutePath())) {
            MAIN.postDelayed(previous::delete, OLD_THEME_DELETE_DELAY_MS);
        }
        generated.add(output.getAbsolutePath());
        state.edit().putStringSet(GENERATED_KEY, generated).apply();
    }

    private static SharedPreferences gboardPrefs(Context context) {
        return context.createDeviceProtectedStorageContext().getSharedPreferences(
                context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
    }

    /** Same naming as Gboard's own imported themes (user_theme_package_<millis>_<nn>.zip). */
    private static File newThemeFile(File directory) throws IOException {
        long now = System.currentTimeMillis();
        for (int suffix = 0; suffix < 100; suffix++) {
            File candidate = new File(directory,
                    String.format(Locale.US, "user_theme_package_%015d_%02d.zip", now, suffix));
            if (!candidate.exists()) return candidate;
        }
        throw new IOException("No free theme file name in " + directory);
    }

    private static byte[] readFile(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            return ThemeRewriter.readAll(in);
        }
    }

    /** Reads android:color/system_<palette>_<shade>, which follow the wallpaper on Android 12+. */
    static final class SystemShades implements Tones.ShadeSource {
        private static final String[] PALETTES = {"accent1", "accent2", "accent3", "neutral1", "neutral2"};
        private final Resources resources;
        private final Map<String, Integer> cache = new HashMap<>();

        SystemShades(Resources resources) {
            this.resources = resources;
        }

        @Override
        public int shade(String palette, int shade) {
            String name = "system_" + palette + "_" + shade;
            Integer color = cache.get(name);
            if (color == null) {
                int id = resources.getIdentifier(name, "color", "android");
                if (id == 0) throw new IllegalStateException("Missing system color " + name);
                color = resources.getColor(id, null);
                cache.put(name, color);
            }
            return color;
        }

        String fingerprint() {
            StringBuilder out = new StringBuilder();
            for (String palette : PALETTES) {
                out.append(Integer.toHexString(shade(palette, 300)))
                        .append(Integer.toHexString(shade(palette, 700)));
            }
            return out.toString();
        }
    }
}
