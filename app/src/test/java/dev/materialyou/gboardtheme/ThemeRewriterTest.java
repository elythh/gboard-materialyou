package dev.materialyou.gboardtheme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public class ThemeRewriterTest {
    private static final File THEME = new File("../theme/src");
    private final BaselineShades shades = new BaselineShades();

    @Test
    public void exactShadesPassThrough() {
        assertEquals(shades.shade("accent1", 800), Tones.tone(shades, "accent1", 20));
        assertEquals(0xFFFFFFFF, Tones.tone(shades, "neutral1", 100));
    }

    @Test
    public void interpolatedTonesMatchMaterialColorUtilities() {
        // Expected values computed with HCT (material-color-utilities) from the same seed.
        assertClose(0x211F24, Tones.tone(shades, "neutral1", 12));
        assertClose(0x36343A, Tones.tone(shades, "neutral1", 22));
        assertClose(0x0F0D13, Tones.tone(shades, "neutral1", 4));
        assertClose(0xF2ECF4, Tones.tone(shades, "neutral1", 94));
        assertClose(0xFFFBFF, Tones.tone(shades, "neutral1", 99));
    }

    @Test
    public void rendersAliasesAndAlpha() {
        String out = TemplateRenderer.render("a {{primary:30}}; b {{neutral:0:66}};", shades);
        assertEquals("a #4D3D75; b #00000066;", out);
    }

    @Test
    public void rewritesTemplateOnceThenStaysCurrent() throws Exception {
        byte[] dark = ThemeRewriter.rewrite(ThemeRewriter.write(themeEntries(null)), shades, true);
        if (dark == null) dark = ThemeRewriter.write(themeEntries(null)); // shipped colors.css is the dark baseline
        Map<String, byte[]> entries = ThemeRewriter.read(dark);
        String colors = new String(entries.get("colors.css"), StandardCharsets.UTF_8);
        assertTrue(colors, colors.contains("@def web_color_bg #211F24;"));
        assertTrue(colors, colors.contains("@def web_color_edge #"));
        assertTrue(colors, !colors.contains("{{"));
        assertNull("second pass must be a no-op", ThemeRewriter.rewrite(dark, shades, true));

        byte[] light = ThemeRewriter.rewrite(dark, shades, false);
        assertNotNull(light);
        Map<String, byte[]> lightEntries = ThemeRewriter.read(light);
        String metadata = new String(lightEntries.get("metadata.json"), StandardCharsets.UTF_8);
        assertTrue(metadata, metadata.contains("\"is_light_theme\": true"));
        assertTrue(new String(lightEntries.get("colors.css"), StandardCharsets.UTF_8)
                .contains("@def web_color_label #1D1B20;"));
        assertEquals(entries.keySet(), lightEntries.keySet());

        String darkId = themeId(entries);
        String lightId = themeId(lightEntries);
        assertTrue(darkId, darkId.matches("rboardtemplatebyhndrk_materialyou_v1\\.6_my[0-9a-f]{8}"));
        assertTrue(lightId, lightId.matches("rboardtemplatebyhndrk_materialyou_v1\\.6_my[0-9a-f]{8}"));
        assertTrue("each color set needs its own id", !darkId.equals(lightId));

        byte[] darkAgain = ThemeRewriter.rewrite(light, shades, true);
        assertEquals("ids are replaced, not stacked", darkId, themeId(ThemeRewriter.read(darkAgain)));
    }

    private static String themeId(Map<String, byte[]> entries) {
        String metadata = new String(entries.get("metadata.json"), StandardCharsets.UTF_8);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*\"([^\"]*)\"").matcher(metadata);
        assertTrue(metadata, m.find());
        return m.group(1);
    }

    @Test
    public void forcedModeIgnoresSystemNight() throws Exception {
        byte[] archive = ThemeRewriter.write(themeEntries("mode=dark"));
        byte[] rebuilt = ThemeRewriter.rewrite(archive, shades, false);
        byte[] current = rebuilt != null ? rebuilt : archive;
        String metadata = new String(ThemeRewriter.read(current).get("metadata.json"), StandardCharsets.UTF_8);
        assertTrue(metadata, metadata.contains("\"is_light_theme\": false"));
    }

    @Test
    public void ignoresThemesWithoutMarker() throws Exception {
        Map<String, byte[]> entries = themeEntries(null);
        entries.remove(ThemeRewriter.MARKER);
        assertNull(ThemeRewriter.rewrite(ThemeRewriter.write(entries), shades, true));
    }

    private static Map<String, byte[]> themeEntries(String modeOverride) throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        File[] files = THEME.listFiles();
        assertNotNull("run tests from the app module", files);
        java.util.Arrays.sort(files);
        for (File file : files) entries.put(file.getName(), Files.readAllBytes(file.toPath()));
        if (modeOverride != null) {
            String marker = new String(entries.get(ThemeRewriter.MARKER), StandardCharsets.UTF_8)
                    .replace("mode=system", modeOverride);
            entries.put(ThemeRewriter.MARKER, marker.getBytes(StandardCharsets.UTF_8));
        }
        return entries;
    }

    private static void assertClose(int expectedRgb, int actualArgb) {
        for (int shift = 0; shift <= 16; shift += 8) {
            int expected = (expectedRgb >> shift) & 0xFF;
            int actual = (actualArgb >> shift) & 0xFF;
            assertTrue(String.format("expected #%06X, got #%06X", expectedRgb, actualArgb & 0xFFFFFF),
                    Math.abs(expected - actual) <= 2);
        }
    }
}
