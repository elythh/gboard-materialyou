package dev.materialyou.gboardtheme;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Regenerates theme/src/colors.css from the dark template with Android's default palette, so the
 * theme still looks right before the module has run once. Usage: RenderDefaultColors [themeDir]
 */
public final class RenderDefaultColors {
    public static void main(String[] args) throws Exception {
        File theme = new File(args.length > 0 ? args[0] : "../theme/src");
        String template = new String(Files.readAllBytes(new File(theme, "materialyou_dark.css.in").toPath()),
                StandardCharsets.UTF_8);
        String colors = TemplateRenderer.render(template, new BaselineShades());
        Files.write(new File(theme, "colors.css").toPath(), colors.getBytes(StandardCharsets.UTF_8));
        System.out.print(colors);
    }
}
