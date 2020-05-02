package com.feed_the_beast.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbchunks.impl.map.XZ;
import com.feed_the_beast.mods.ftbguilibrary.utils.MouseButton;
import com.feed_the_beast.mods.ftbguilibrary.widget.Panel;
import com.feed_the_beast.mods.ftbguilibrary.widget.Theme;
import com.feed_the_beast.mods.ftbguilibrary.widget.Widget;
import net.minecraft.util.math.MathHelper;

import java.nio.file.Files;

/**
 * @author LatvianModder
 */
public class RegionMapPanel extends Panel
{
	public final LargeMapScreen largeMap;
	public double regionX = 0;
	public double regionZ = 0;
	public int regionMinX, regionMinZ, regionMaxX, regionMaxZ;
	public int blockX = 0;
	public int blockZ = 0;

	public RegionMapPanel(LargeMapScreen panel)
	{
		super(panel);
		largeMap = panel;
	}

	public void updateMinMax()
	{
		regionMinX = Integer.MAX_VALUE;
		regionMinZ = Integer.MAX_VALUE;
		regionMaxX = Integer.MIN_VALUE;
		regionMaxZ = Integer.MIN_VALUE;

		for (Widget w : widgets)
		{
			if (w instanceof RegionMapButton)
			{
				int qx = ((RegionMapButton) w).pos.x;
				int qy = ((RegionMapButton) w).pos.z;

				regionMinX = Math.min(regionMinX, qx);
				regionMinZ = Math.min(regionMinZ, qy);
				regionMaxX = Math.max(regionMaxX, qx);
				regionMaxZ = Math.max(regionMaxZ, qy);
			}
		}

		if (regionMinX == Integer.MAX_VALUE)
		{
			regionMinX = regionMinZ = regionMaxX = regionMaxZ = 0;
		}

		regionMinX -= 1;
		regionMinZ -= 1;
		regionMaxX += 1;
		regionMaxZ += 1;
	}

	public void scrollTo(double x, double y)
	{
		updateMinMax();

		double dx = (regionMaxX - regionMinX);
		double dy = (regionMaxZ - regionMinZ);

		setScrollX((x - regionMinX) / dx * largeMap.scrollWidth - width / 2D);
		setScrollY((y - regionMinZ) / dy * largeMap.scrollHeight - height / 2D);
	}

	public void resetScroll()
	{
		alignWidgets();
		setScrollX((largeMap.scrollWidth - width) / 2D);
		setScrollY((largeMap.scrollHeight - height) / 2D);
	}

	@Override
	public void addWidgets()
	{
		FTBChunksClient.saveAllRegions();

		try
		{
			if (Files.exists(largeMap.dimension.directory))
			{
				Files.list(largeMap.dimension.directory)
						.map(path -> path.getFileName().toString())
						.filter(name -> name.endsWith(".png"))
						.map(name -> name.split(","))
						.filter(name -> name.length == 3)
						.map(name -> new RegionMapButton(this, XZ.of(Integer.parseInt(name[0]), Integer.parseInt(name[1]))))
						.forEach(this::add);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}

		alignWidgets();
	}

	@Override
	public void alignWidgets()
	{
		largeMap.scrollWidth = 0;
		largeMap.scrollHeight = 0;

		updateMinMax();

		int z = largeMap.getRegionButtonSize();

		largeMap.scrollWidth = (regionMaxX - regionMinX) * z;
		largeMap.scrollHeight = (regionMaxZ - regionMinZ) * z;

		for (Widget w : widgets)
		{
			if (w instanceof RegionMapButton)
			{
				double qx = ((RegionMapButton) w).pos.x;
				double qy = ((RegionMapButton) w).pos.z;
				double qw = 1D;
				double qh = 1D;

				double x = (qx - regionMinX - qw / 2D) * z;
				double y = (qy - regionMinZ - qh / 2D) * z;
				w.setPosAndSize((int) x, (int) y, (int) (z * qw), (int) (z * qh));
			}
		}

		setPosAndSize(0, 0, parent.width, parent.height);
	}

	@Override
	public void draw(Theme theme, int x, int y, int w, int h)
	{
		super.draw(theme, x, y, w, h);

		int dx = (regionMaxX - regionMinX);
		int dy = (regionMaxZ - regionMinZ);

		double px = getX() - getScrollX();
		double py = getY() - getScrollY();

		regionX = (parent.getMouseX() - px) / (double) largeMap.scrollWidth * dx + regionMinX;
		regionZ = (parent.getMouseY() - py) / (double) largeMap.scrollHeight * dy + regionMinZ;
		blockX = MathHelper.floor((regionX + 0.5D) * 512D);
		blockZ = MathHelper.floor((regionZ + 0.5D) * 512D);

		/*
		double x1 = ((pcx - startX) * 16D + MathUtils.mod(player.getPosX(), 16D));
		double z1 = ((pcz - startZ) * 16D + MathUtils.mod(player.getPosZ(), 16D));

		RenderSystem.pushMatrix();
		RenderSystem.translated(sx + x1 * FTBChunks.TILE_SIZE / 16D, sy + z1 * FTBChunks.TILE_SIZE / 16D, 0D);
		RenderSystem.rotatef(player.rotationYaw + 180F, 0F, 0F, 1F);
		RenderSystem.color4f(1F, 1F, 1F, 1F);

		Minecraft.getInstance().getTextureManager().bindTexture(FTBChunksClient.MAP_ICONS);
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
		buffer.pos(-8.5, -8, 0).color(255, 255, 255, 100).tex(0F / 16F, 0F / 16F).endVertex();
		buffer.pos(-8.5, 8, 0).color(255, 255, 255, 100).tex(0F / 16F, 1F / 16F).endVertex();
		buffer.pos(7.5, 8, 0).color(255, 255, 255, 100).tex(1F / 16F, 1F / 16F).endVertex();
		buffer.pos(7.5, -8, 0).color(255, 255, 255, 100).tex(1F / 16F, 0F / 16F).endVertex();
		tessellator.draw();

		// https://minotar.net/avatar/<short uuid>/16

		RenderSystem.popMatrix();
		*/
	}

	@Override
	public boolean mousePressed(MouseButton button)
	{
		if (super.mousePressed(button))
		{
			return true;
		}

		if (button.isLeft() && isMouseOver())
		{
			largeMap.prevMouseX = getMouseX();
			largeMap.prevMouseY = getMouseY();
			largeMap.grabbed = 1;
			return true;
		}

		return false;
	}

	@Override
	public void mouseReleased(MouseButton button)
	{
		super.mouseReleased(button);
		largeMap.grabbed = 0;
	}

	@Override
	public boolean scrollPanel(double scroll)
	{
		if (isMouseOver())
		{
			largeMap.addZoom(scroll);
			return true;
		}

		return false;
	}

	@Override
	public void drawBackground(Theme theme, int x, int y, int w, int h)
	{
		super.drawBackground(theme, x, y, w, h);
	}
}