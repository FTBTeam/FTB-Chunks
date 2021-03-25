package dev.ftb.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import com.feed_the_beast.mods.ftbguilibrary.widget.GuiHelper;
import com.feed_the_beast.mods.ftbguilibrary.widget.Theme;
import com.feed_the_beast.mods.ftbguilibrary.widget.Widget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ftb.mods.ftbchunks.client.map.MapRegion;

/**
 * @author LatvianModder
 */
public class RegionMapButton extends Widget {
	public final MapRegion region;

	public RegionMapButton(RegionMapPanel pa, MapRegion r) {
		super(pa);
		region = r;
	}

	@Override
	public void draw(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
		int id = region.getRenderedMapImageTextureId();

		if (region.mapImageLoaded) {
			RenderSystem.bindTexture(id);
			GuiHelper.drawTexturedRect(matrixStack, x, y, w, h, Color4I.WHITE, 0F, 0F, 1F, 1F);
		}
	}
}