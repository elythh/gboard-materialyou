package dev.materialyou.gboardtheme;

/**
 * Resolves any Material You tone (0-100) from the 13 shades Android exposes per palette
 * (system_accent1_0 ... system_accent1_1000). Shade N is tone 100 - N / 10; tones in between
 * are interpolated in CIELAB, whose L* axis is the Material tone axis.
 */
final class Tones {
    /** Supplies one system shade as an ARGB int, e.g. ("accent1", 600). */
    interface ShadeSource {
        int shade(String palette, int shade);
    }

    static final int[] SHADES = {0, 10, 50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000};

    private Tones() { }

    static int tone(ShadeSource source, String palette, double tone) {
        double t = Math.max(0, Math.min(100, tone));
        for (int i = 0; i < SHADES.length - 1; i++) {
            double upper = toneOf(SHADES[i]);
            double lower = toneOf(SHADES[i + 1]);
            if (t > upper || t < lower) continue;
            if (t == upper) return source.shade(palette, SHADES[i]);
            if (t == lower) return source.shade(palette, SHADES[i + 1]);
            double fraction = (upper - t) / (upper - lower);
            return lerpLab(source.shade(palette, SHADES[i]), source.shade(palette, SHADES[i + 1]), fraction);
        }
        throw new IllegalStateException("Tone out of range: " + tone);
    }

    static double toneOf(int shade) {
        return 100.0 - shade / 10.0;
    }

    static int lerpLab(int from, int to, double fraction) {
        double[] a = toLab(from);
        double[] b = toLab(to);
        return fromLab(
                a[0] + (b[0] - a[0]) * fraction,
                a[1] + (b[1] - a[1]) * fraction,
                a[2] + (b[2] - a[2]) * fraction);
    }

    // sRGB <-> CIELAB (D65)

    private static final double[] WHITE = {95.047, 100.0, 108.883};

    static double[] toLab(int argb) {
        double r = linearize((argb >> 16) & 0xFF);
        double g = linearize((argb >> 8) & 0xFF);
        double b = linearize(argb & 0xFF);
        double x = 0.41233895 * r + 0.35762064 * g + 0.18051042 * b;
        double y = 0.2126 * r + 0.7152 * g + 0.0722 * b;
        double z = 0.01932141 * r + 0.11916382 * g + 0.95034478 * b;
        double fx = labF(x / WHITE[0]);
        double fy = labF(y / WHITE[1]);
        double fz = labF(z / WHITE[2]);
        return new double[] {116.0 * fy - 16.0, 500.0 * (fx - fy), 200.0 * (fy - fz)};
    }

    static int fromLab(double l, double a, double bStar) {
        double fy = (l + 16.0) / 116.0;
        double fx = a / 500.0 + fy;
        double fz = fy - bStar / 200.0;
        double x = labInvF(fx) * WHITE[0];
        double y = labInvF(fy) * WHITE[1];
        double z = labInvF(fz) * WHITE[2];
        double r = 3.2413774792388685 * x - 1.5376652402851851 * y - 0.49885366846268053 * z;
        double g = -0.9691452513005321 * x + 1.8758853451067872 * y + 0.04156585616912061 * z;
        double b = 0.05562093689691305 * x - 0.20395524564742123 * y + 1.0571799111220335 * z;
        return 0xFF000000 | (delinearize(r) << 16) | (delinearize(g) << 8) | delinearize(b);
    }

    private static double linearize(int channel) {
        double c = channel / 255.0;
        return (c <= 0.040449936 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4)) * 100.0;
    }

    private static int delinearize(double linear) {
        double c = linear / 100.0;
        double v = c <= 0.0031308 ? c * 12.92 : 1.055 * Math.pow(c, 1.0 / 2.4) - 0.055;
        return (int) Math.max(0, Math.min(255, Math.round(v * 255.0)));
    }

    private static double labF(double t) {
        double e = 216.0 / 24389.0;
        double k = 24389.0 / 27.0;
        return t > e ? Math.cbrt(t) : (k * t + 16.0) / 116.0;
    }

    private static double labInvF(double ft) {
        double e = 216.0 / 24389.0;
        double k = 24389.0 / 27.0;
        double cube = ft * ft * ft;
        return cube > e ? cube : (116.0 * ft - 16.0) / k;
    }
}
