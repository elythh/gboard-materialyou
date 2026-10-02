package dev.materialyou.gboardtheme;

import java.util.HashMap;
import java.util.Map;

/** Android's default palette (seed #6750A4, Tonal Spot), shades 0..1000, for tests. */
final class BaselineShades implements Tones.ShadeSource {
    private static final Map<String, int[]> SHADES = new HashMap<>();

    private static void put(String palette, int[] shades) {
        SHADES.put(palette, shades);
    }

    static {
        put("accent1", new int[] {0xFFFFFFFF, 0xFFFFFBFF, 0xFFF6EEFF, 0xFFE9DDFF, 0xFFCFBDFE, 0xFFB3A2E1, 0xFF9887C5, 0xFF7E6EA9, 0xFF65558F, 0xFF4D3D75, 0xFF36275D, 0xFF201047, 0xFF000000});
        put("accent2", new int[] {0xFFFFFFFF, 0xFFFFFBFF, 0xFFF6EEFF, 0xFFE8DEF8, 0xFFCBC2DB, 0xFFB0A7C0, 0xFF958DA4, 0xFF7B748A, 0xFF625B71, 0xFF4A4458, 0xFF332D41, 0xFF1E192B, 0xFF000000});
        put("accent3", new int[] {0xFFFFFFFF, 0xFFFFFBFF, 0xFFFFECF0, 0xFFFFD9E3, 0xFFEFB8C8, 0xFFD29DAD, 0xFFB58392, 0xFF996A79, 0xFF7E5260, 0xFF633B48, 0xFF4A2532, 0xFF31101D, 0xFF000000});
        put("neutral1", new int[] {0xFFFFFFFF, 0xFFFFFBFF, 0xFFF5EFF7, 0xFFE6E0E9, 0xFFCAC5CC, 0xFFAEA9B1, 0xFF938F96, 0xFF79767D, 0xFF605D64, 0xFF48464C, 0xFF322F35, 0xFF1D1B20, 0xFF000000});
        put("neutral2", new int[] {0xFFFFFFFF, 0xFFFFFBFF, 0xFFF5EEFA, 0xFFE7E0EB, 0xFFCAC4CF, 0xFFAFA9B4, 0xFF948F99, 0xFF7A757F, 0xFF615D66, 0xFF49454E, 0xFF322F38, 0xFF1D1A22, 0xFF000000});
    }

        // neutral1:12 exact HCT = #211F24
        // neutral1:22 exact HCT = #36343A
        // neutral1:4 exact HCT = #0F0D13
        // neutral1:94 exact HCT = #F2ECF4
        // neutral1:99 exact HCT = #FFFBFF
        // accent1:30 exact HCT = #4D3D75
        // neutral2:80 exact HCT = #CAC4CF

    @Override
    public int shade(String palette, int shade) {
        int[] values = SHADES.get(palette);
        for (int i = 0; i < Tones.SHADES.length; i++) {
            if (Tones.SHADES[i] == shade) return values[i];
        }
        throw new IllegalArgumentException("No shade " + shade);
    }
}
