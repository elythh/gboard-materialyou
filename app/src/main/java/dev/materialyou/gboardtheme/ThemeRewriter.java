package dev.materialyou.gboardtheme;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Re-renders a Material You template theme. A theme opts in by shipping materialyou.properties:
 *
 * <pre>
 * target=colors.css                  # style sheet to regenerate
 * mode=system                        # system | dark | light
 * dark=materialyou_dark.css.in       # template used in dark mode
 * light=materialyou_light.css.in     # template used in light mode
 * </pre>
 */
final class ThemeRewriter {
    static final String MARKER = "materialyou.properties";
    private static final String METADATA = "metadata.json";
    private static final Pattern LIGHT_FLAG = Pattern.compile("\"is_light_theme\"\\s*:\\s*(true|false)");
    private static final Pattern THEME_ID = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]*)\"");
    /** Suffix this module appends to the theme id; matched so it is replaced, not stacked. */
    private static final Pattern ID_SUFFIX = Pattern.compile("_my[0-9a-f]{8}$");

    private ThemeRewriter() { }

    /** Returns the rebuilt theme, or null when the archive is not a template or is already current. */
    static byte[] rewrite(byte[] archive, Tones.ShadeSource source, boolean systemNight) throws IOException {
        Map<String, byte[]> entries = read(archive);
        byte[] marker = entries.get(MARKER);
        if (marker == null) return null;

        Properties config = new Properties();
        config.load(new StringReader(utf8(marker)));
        String mode = config.getProperty("mode", "system").trim();
        boolean dark = "dark".equals(mode) || (!"light".equals(mode) && systemNight);
        String templateName = config.getProperty(dark ? "dark" : "light",
                config.getProperty(dark ? "light" : "dark", "")).trim();
        String target = config.getProperty("target", "colors.css").trim();
        byte[] template = entries.get(templateName);
        if (template == null) throw new IOException("Template " + templateName + " missing from theme");

        boolean changed = false;
        byte[] rendered = TemplateRenderer.render(utf8(template), source).getBytes(StandardCharsets.UTF_8);
        if (!java.util.Arrays.equals(rendered, entries.get(target))) {
            entries.put(target, rendered);
            changed = true;
        }

        byte[] metadata = entries.get(METADATA);
        if (metadata != null) {
            // Gboard caches built keyboards by theme id, so every color set needs its own id or
            // the switch to the re-colored copy shows the old colors until that cache is dropped.
            String updated = setThemeId(setLightFlag(utf8(metadata), !dark), colorTag(rendered, dark));
            if (!updated.equals(utf8(metadata))) {
                entries.put(METADATA, updated.getBytes(StandardCharsets.UTF_8));
                changed = true;
            }
        }
        return changed ? write(entries) : null;
    }

    static String setThemeId(String metadata, String tag) {
        Matcher matcher = THEME_ID.matcher(metadata);
        if (!matcher.find()) return metadata;
        String base = ID_SUFFIX.matcher(matcher.group(1)).replaceFirst("");
        return metadata.substring(0, matcher.start(1)) + base + "_my" + tag + metadata.substring(matcher.end(1));
    }

    private static String colorTag(byte[] colors, boolean dark) {
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(colors);
        crc.update(dark ? 1 : 0);
        return String.format(java.util.Locale.US, "%08x", crc.getValue());
    }

    static String setLightFlag(String metadata, boolean light) {
        Matcher matcher = LIGHT_FLAG.matcher(metadata);
        if (matcher.find()) {
            return metadata.substring(0, matcher.start(1)) + light + metadata.substring(matcher.end(1));
        }
        int brace = metadata.indexOf('{');
        if (brace < 0) return metadata;
        return metadata.substring(0, brace + 1) + "\n    \"is_light_theme\": " + light + ","
                + metadata.substring(brace + 1);
    }

    static Map<String, byte[]> read(byte[] archive) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory()) entries.put(entry.getName(), readAll(zip));
            }
        }
        return entries;
    }

    static byte[] write(Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                ZipEntry out = new ZipEntry(entry.getKey());
                out.setTime(0L);
                zip.putNextEntry(out);
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    static byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[16 * 1024];
        int read;
        while ((read = input.read(buffer)) != -1) out.write(buffer, 0, read);
        return out.toByteArray();
    }

    private static String utf8(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
