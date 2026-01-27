package dev.ftb.mods.ftbchunks.client.gui;

import dev.ftb.mods.ftbchunks.client.map.MapRegion;
import dev.ftb.mods.ftblibrary.client.gui.theme.Theme;
import dev.ftb.mods.ftblibrary.client.gui.widget.Widget;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;

public class MapTileWidget extends Widget {
	public final MapRegion region;

	public MapTileWidget(RegionMapPanel panel, MapRegion region) {
		super(panel);
		this.region = region;
	}

	@Override
	public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
		var regionTexture = region.regionTexture().getTextureID();
		if (regionTexture == null) {
			return;
		}

		graphics.blit(RenderPipelines.GUI_TEXTURED,
			regionTexture,
			x, y,
			0f, 0f,
			w, h,
			w, h,
			w, h,
			Color4I.WHITE.rgba()
		);
	}
}
