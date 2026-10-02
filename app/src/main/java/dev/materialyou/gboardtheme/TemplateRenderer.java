package dev.materialyou.gboardtheme;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces {{palette:tone}} or {{palette:tone:AA}} placeholders with #RRGGBB / #RRGGBBAA.
 * Palettes: accent1, accent2, accent3, neutral1, neutral2, or the Material names
 * primary, secondary, tertiary, neutral, neutral_variant.
 */
final class TemplateRenderer {
    private static final Pattern PLACEHOLDER = Pattern.compile(
            "\\{\\{\\s*([A-Za-z_0-9]+)\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(?::\\s*([0-9A-Fa-f]{2})\\s*)?\\}\\}");

    private static final Map<String, String> ALIASES = new HashMap<>();

    static {
        for (String name : new String[] {"accent1", "accent2", "accent3", "neutral1", "neutral2"}) {
            ALIASES.put(name, name);
        }
        ALIASES.put("primary", "accent1");
        ALIASES.put("secondary", "accent2");
        ALIASES.put("tertiary", "accent3");
        ALIASES.put("neutral", "neutral1");
        ALIASES.put("neutral_variant", "neutral2");
    }

    private TemplateRenderer() { }

    static String render(String template, Tones.ShadeSource source) {
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String palette = ALIASES.get(matcher.group(1).toLowerCase(Locale.US));
            if (palette == null) {
                throw new IllegalArgumentException("Unknown palette in " + matcher.group());
            }
            int argb = Tones.tone(source, palette, Double.parseDouble(matcher.group(2)));
            String color = String.format(Locale.US, "#%06X", argb & 0xFFFFFF);
            if (matcher.group(3) != null) color += matcher.group(3).toUpperCase(Locale.US);
            matcher.appendReplacement(out, Matcher.quoteReplacement(color));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
