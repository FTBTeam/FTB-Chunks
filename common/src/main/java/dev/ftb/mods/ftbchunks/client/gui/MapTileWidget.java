package dev.ftb.mods.ftbchunks.client.gui;

import dev.ftb.mods.ftbchunks.client.map.MapRegion;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import net.minecraft.client.gui.GuiGraphics;

public class MapTileWidget extends Widget {
	public final MapRegion region;

	public MapTileWidget(RegionMapPanel panel, MapRegion region) {
		super(panel);
		this.region = region;
	}

	@Override
	public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
		if (region.isMapImageLoaded()) {
			graphics.blit(region.getRenderedTextureId(), x, y, x + w, x + h, 0f, 0f, 1f, 1f);
		}

//		int id = region.getRenderedMapImageTextureId();
//
//		if (region.isMapImageLoaded()) {
//			RenderSystem.bindTextureForSetup(id);
//			int filter = w * Minecraft.getInstance().getWindow().getGuiScale() < 512D ? GL11.GL_LINEAR : GL11.GL_NEAREST;
//			RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
//			RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
//
//			RenderSystem.setShaderTexture(0, id);
//			GuiHelper.drawTexturedRect(graphics, x, y, w, h, Color4I.WHITE, 0F, 0F, 1F, 1F);
//		}
	}
}