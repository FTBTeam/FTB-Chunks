package dev.ftb.mods.ftbchunks.client.map.color;

import com.mojang.blaze3d.platform.NativeImage;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import java.io.InputStream;
import java.util.HashMap;

public class ColorUtils {
	private static Color4I[] reducedColorPalette = null;
	private static Color4I[] topographyPalette = null;
	private static Color4I[][] lightMapPalette = null;
	private static final HashMap<Color4I, Color4I> reducedColorMap = new HashMap<>();

	public static int convertToNative(int c) {
		return ARGB.color(
				(c >> 24) & 0xFF,
				(c >> 16) & 0xFF,
				(c >> 8) & 0xFF,
				c & 0xFF
		);
	}

	public static int convertFromNative(int c) {
		return (ARGB.alpha(c) << 24) | (ARGB.red(c) << 16) | (ARGB.green(c) << 8) | ARGB.blue(c);
	}

	public static Color4I addBrightness(Color4I c, float f) {
		float r = c.redf() + f;
		float g = c.greenf() + f;
		float b = c.bluef() + f;

		int ri = Mth.clamp((int) (r * 255F), 0, 255);
		int gi = Mth.clamp((int) (g * 255F), 0, 255);
		int bi = Mth.clamp((int) (b * 255F), 0, 255);

		return Color4I.rgb(ri, gi, bi);
	}

	public static Color4I reduce(Color4I c) {
		if (reducedColorPalette == null) {
			reducedColorPalette = new Color4I[0];

			try (InputStream stream = Minecraft.getInstance().getResourceManager().getResource(FTBChunksAPI.id("textures/reduced_color_palette.png")).orElseThrow().open()) {
				NativeImage image = NativeImage.read(stream);
				int w = image.getWidth();
				int h = image.getHeight();

				reducedColorPalette = new Color4I[w * h];

                // TODO: [21.8] Validate this works.
                int pixelIndex = 0;
                int[] pixelsABGR = image.getPixelsABGR();
				for (int x = 0; x < w; x++) {
					for (int y = 0; y < h; y++) {
                        int col = pixelsABGR[pixelIndex];
						reducedColorPalette[x + y * w] = Color4I.rgb((ARGB.red(col) << 16) | (ARGB.green(col) << 8) | ARGB.blue(col));
                        pixelIndex++;
					}
				}

				image.close();
			} catch (Exception ignored) {
			}
		}

		if (reducedColorPalette.length == 0) {
			return c;
		}

		return reducedColorMap.computeIfAbsent(c, col -> {
			int r = col.redi();
			int g = col.greeni();
			int b = col.bluei();
			long prevDist = Long.MAX_VALUE;
			Color4I colr = Color4I.BLACK;

			for (Color4I rcol : reducedColorPalette) {
				long dr = r - rcol.redi();
				long dg = g - rcol.greeni();
				long db = b - rcol.bluei();
				long d = dr * dr + dg * dg + db * db;

				if (d < prevDist) {
					prevDist = d;
					colr = rcol;
				}
			}

			return colr;
		});
	}

	public static Color4I[] getTopographyPalette() {
		if (topographyPalette == null) {
			topographyPalette = new Color4I[0];

			try (InputStream stream = Minecraft.getInstance().getResourceManager().getResource(FTBChunksAPI.id("textures/topography_palette.png")).orElseThrow().open()) {
				NativeImage image = NativeImage.read(stream);
				int w = image.getWidth();
				int h = image.getHeight();

				topographyPalette = new Color4I[w * h];

                // TODO: [21.8] Validate this works.
                int pixelIndex = 0;
                int[] pixelsABGR = image.getPixelsABGR();
				for (int x = 0; x < w; x++) {
					for (int y = 0; y < h; y++) {
                        int col = pixelsABGR[pixelIndex];
						topographyPalette[x + y * w] = Color4I.rgb(convertFromNative(col)).withAlpha(255);
                        pixelIndex++;
					}
				}

				image.close();
			} catch (Exception ignored) {
			}
		}

		return topographyPalette;
	}

	public static Color4I[][] getLightMapPalette() {
		if (lightMapPalette == null) {
			lightMapPalette = new Color4I[0][0];

			try (InputStream stream = Minecraft.getInstance().getResourceManager().getResource(FTBChunksAPI.id("textures/lightmap_palette.png")).orElseThrow().open()) {
				NativeImage image = NativeImage.read(stream);
				int w = image.getWidth();
				int h = image.getHeight();

				lightMapPalette = new Color4I[w][h];

                // TODO: [21.8] Validate this works.
                int pixelIndex = 0;
                int[] pixelsABGR = image.getPixelsABGR();
				for (int x = 0; x < w; x++) {
					for (int y = 0; y < h; y++) {
                        int col = pixelsABGR[pixelIndex];
						lightMapPalette[x][y] = Color4I.rgb(convertFromNative(col)).withAlpha(255);
                        pixelIndex++;
					}
				}

				image.close();
			} catch (Exception ignored) {
			}
		}

		return lightMapPalette;
	}
}
