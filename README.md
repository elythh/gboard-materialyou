# Gboard Material You

Make hndrk's 3D Gboard theme follow your wallpaper colors (Material You) and dark mode, without
root. This uses a Morphe-patched Gboard, NPatch, and a small Xposed module from this repo.

Tested on a Pixel 10 Pro running Android 17. Android 12 or newer is required, since Material You
colors start there.

**Downloads:** [latest release](https://github.com/elythh/gboard-materialyou/releases/latest)
(`GboardMaterialYou-module.apk` and `3D_Template_MaterialYou.zip`).

## What you need

| Item | What it does |
|---|---|
| [Morphe manager](https://github.com/MorpheApp/morphe-manager) | Patches Gboard |
| [Gboard-patches](https://github.com/jasonwu1994/Gboard-patches) (Morphe source) | Gboard patches, including the custom theme importer. Targets Gboard 18.0.3 |
| [Shizuku](https://shizuku.rikka.app/) | Installs the patched apps without root |
| [NPatch](https://github.com/7723mod/NPatch) manager, 1.0.8 or newer | Loads the Xposed module into Gboard without root |
| `GboardMaterialYou-module.apk` (release) | Recolors the theme from your wallpaper colors |
| `3D_Template_MaterialYou.zip` (release) | hndrk's 3D theme, converted to a Material You template |

The module only recolors themes built as Material You templates. The original 3D template zip will
not change color.

## Step 1: Patch Gboard with Morphe

Skip this step if you already use a Morphe-patched Gboard (GboardWU).

1. Install the Morphe manager and Shizuku, and start Shizuku.
2. In Morphe, add the [Gboard-patches](https://github.com/jasonwu1994/Gboard-patches) repository as a
   patch source.
3. Patch Gboard 18.0.3 with the default patches. Keep the custom theme patch on: it is what imports
   theme zips.
4. **Save the patched APK file.** Step 3 needs this exact file, not the installed app.
5. Install it. The patched app is renamed `dev.jason.com.google.android.inputmethod.latin`, so it sits
   next to the Play Store Gboard.

## Step 2: Install the module

Install `GboardMaterialYou-module.apk` like any app. A **Gboard Material You** icon appears in your app
drawer. It only opens a help screen, since the module does its work inside Gboard.

Install it before patching in NPatch, so NPatch can find it.

## Step 3: Patch Gboard again with NPatch

This step decides whether Gboard opens at all: with the wrong two settings it crashes on start.

1. Open the NPatch manager and start a new patch.
2. Select the **Morphe APK file** you saved in step 1. Don't pick the Play Store Gboard, or the Morphe
   patches are lost.
3. Set the options:

   | Option | Set to | Why |
   |---|---|---|
   | Patch Mode | **Local** | Loads the module from its installed app, so module updates need no re-patch |
   | Signature bypass | **None** | Basic and higher load Gboard's code from a cache copy, which Android 14+ refuses (`Writable dex file is not allowed`). Morphe already handles signatures |
   | Inject Loader DEX | **On** | With bypass None, this keeps Gboard's own code in the main APK. Without it Gboard can't find its main class (`ClassNotFoundException: LatinApp`) |

4. Patch, then install the result with Shizuku.

**Your Gboard data:** NPatch signs with its own key, so the first install needs the Morphe Gboard
uninstalled first. That clears learned words, clipboard and settings. To keep them, set **Settings →
Signature keystore → Custom** to the keystore Morphe used, if you can export it. Later re-patches
signed with the same key install as updates.

On a computer, the NPatch CLI does the same. Its built-in keystore needs
[BouncyCastle](https://repo1.maven.org/maven2/org/bouncycastle/bcprov-jdk18on/1.78.1/) on the
classpath, registered as a security provider:

```sh
echo 'security.provider.14=org.bouncycastle.jce.provider.BouncyCastleProvider' > bc.props
java -Djava.security.properties=bc.props \
  -cp jar-v1.0.8-801-release.jar:bcprov-jdk18on-1.78.1.jar \
  top.nkbe.npatch.patch.NPatch gboard-morphe.apk \
  -l 0 --manager --injectdex -npa -o out
```

## Step 4: Enable the module and import the theme

1. In the NPatch manager, open the patched Gboard, then **Module scope**, and enable **Gboard
   Material You**.
2. Use **Force restart** on the same screen, or force stop Gboard from its app info.
3. Copy `3D_Template_MaterialYou.zip` to the phone, for example to Download.
4. In Gboard's settings, open **Theme** and import the zip with the Morphe custom theme importer. If it
   asks which theme inside the pack to use, pick `3dTemplate_hndrk_materialyou.zip`.
5. Apply it, then open the keyboard in any text field.

The keyboard now takes your wallpaper colors. When you change the wallpaper or switch dark mode, the
new colors appear the next time the keyboard opens.

Your theme list holds at most two copies of the theme: the one you imported, and the latest recolored
copy. The module deletes its older copies, never the one you imported.

## Customize the colors

The theme zip holds three files the module reads (sources in [`theme/src`](theme/src)). Edit them,
re-pack with `theme/build_theme.sh`, and import the result.

| File | What it sets |
|---|---|
| `materialyou.properties` | `mode=system` follows Android dark mode; `dark` or `light` forces one |
| `materialyou_dark.css.in` | Colors used in dark mode |
| `materialyou_light.css.in` | Colors used in light mode |

In the two templates, each color is a placeholder that names a Material You palette and a tone from 0
(black) to 100 (white):

```css
@def web_color_bg {{neutral:12}};
@def web_color_accent {{primary:30}};
@def web_color_edge {{neutral:4:66}};
```

Palettes are `primary`, `secondary`, `tertiary`, `neutral` and `neutral_variant` (or the Android names
`accent1`, `accent2`, `accent3`, `neutral1`, `neutral2`). A third part, like `:66`, adds hex
transparency.

The dark template uses the tones of Android 12–15's dark scheme, so the keys look like stock Gboard's
dynamic theme. Any other Rboard theme can be converted the same way, if its style sheets take all
colors from `colors.css`.

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| Gboard closes right after opening; log shows `Writable dex file … is not allowed` | Signature bypass set to Basic or higher | Re-patch with Signature bypass **None** (step 3) |
| Gboard closes right after opening; log shows `ClassNotFoundException: LatinApp` | Inject Loader DEX left off | Re-patch with **Inject Loader DEX** on |
| Morphe features are missing after NPatch | NPatch patched the Play Store Gboard, or the patched app's code didn't load | Re-patch the saved Morphe APK file with the step 3 settings |
| The module isn't listed in NPatch | NPatch can't see all installed apps, or the module was installed after NPatch scanned | Reopen NPatch; grant it Shizuku or its "installed apps" permission |
| Colors never change | The active theme isn't the Material You template, or the module isn't enabled for Gboard | Import `3D_Template_MaterialYou.zip`; check Module scope (step 4) |

To see what the module did, connect a computer with adb and run `adb logcat -s GboardMaterialYou`. The
same lines appear in NPatch's diagnostic logs. A working setup logs `Loaded in …`, then `Hooked …`, then
`Applied Material You colors: …` after each color change.

## How it works

A Gboard theme zip can only hold fixed colors, so the module rebuilds the theme whenever your colors
change:

1. Each time the keyboard opens, the module checks whether the active theme contains
   `materialyou.properties`.
2. It reads Android's Material You colors, the `system_accent1–3` and `system_neutral1–2` shades. Tones
   between Android's 13 shades are interpolated in CIELAB, within 2/255 per channel of Google's HCT.
3. It fills the dark or light template with those colors and writes a new copy of the theme
   (`user_theme_package_<time>_00.zip`). If nothing changed, it stops here.
4. It gives the copy a new theme id and makes it Gboard's active theme, so Gboard redraws the keyboard
   instead of reusing its cached one.

The module hooks only standard Android keyboard methods (`InputMethodService`), not Gboard's internal
code, so Gboard updates shouldn't break it. After a Gboard or Morphe update, you still repeat step 3.

## Building

Requirements: JDK 17, Gradle 8.9+ and the Android SDK (platform 34), with `sdk.dir` set in
`local.properties`.

```sh
gradle :app:testDebugUnitTest :app:assembleRelease   # -> app/build/outputs/apk/release/app-release.apk
theme/build_theme.sh                                 # -> build/3D_Template_MaterialYou.zip
```

GitHub Actions runs the same build on every push and uploads both files as an artifact. CI builds are
signed with a throwaway debug key, so they can't update a module installed from a release: uninstall
the old module first, or add your own `signingConfig`.

## Credits

- 3D theme: hndrk's `3dTemplate_hndrk` for Rboard. The icons and style sheets in `theme/src` are theirs;
  this repo only changes the colors.
- [Gboard-patches](https://github.com/jasonwu1994/Gboard-patches) for the custom theme importer.
- [NPatch](https://github.com/7723mod/NPatch) for rootless Xposed loading.

## License

The module code is under the MIT License (see [LICENSE](LICENSE)). The theme assets in `theme/src`
belong to their original author.
