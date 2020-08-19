package com.feed_the_beast.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbchunks.api.ChunkDimPos;
import com.feed_the_beast.mods.ftbchunks.api.Waypoint;
import com.feed_the_beast.mods.ftbchunks.client.map.MapChunk;
import com.feed_the_beast.mods.ftbchunks.client.map.MapRegion;
import com.feed_the_beast.mods.ftbchunks.impl.XZ;
import com.feed_the_beast.mods.ftbguilibrary.utils.MouseButton;
import com.feed_the_beast.mods.ftbguilibrary.utils.TooltipList;
import com.feed_the_beast.mods.ftbguilibrary.widget.Panel;
import com.feed_the_beast.mods.ftbguilibrary.widget.Theme;
import com.feed_the_beast.mods.ftbguilibrary.widget.Widget;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;

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
	public int blockY = 0;
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
				int qx = ((RegionMapButton) w).region.pos.x;
				int qy = ((RegionMapButton) w).region.pos.z;

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

		regionMinX -= 100;
		regionMinZ -= 100;
		regionMaxX += 101;
		regionMaxZ += 101;
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
		for (MapRegion region : largeMap.dimension.getRegions().values())
		{
			add(new RegionMapButton(this, region));
		}

		for (Waypoint waypoint : largeMap.dimension.waypoints)
		{
			add(new WaypointButton(this, waypoint));
		}

		String dimId = ChunkDimPos.getID(Minecraft.getInstance().world);

		for (AbstractClientPlayerEntity player : Minecraft.getInstance().world.getPlayers())
		{
			if (largeMap.dimension.dimension.equals(dimId))
			{
				add(new PlayerButton(this, player));
			}
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
				double qx = ((RegionMapButton) w).region.pos.x;
				double qy = ((RegionMapButton) w).region.pos.z;
				double qw = 1D;
				double qh = 1D;

				double x = (qx - regionMinX) * z;
				double y = (qy - regionMinZ) * z;
				w.setPosAndSize((int) x, (int) y, (int) (z * qw), (int) (z * qh));
			}
			else if (w instanceof WaypointButton)
			{
				double qx = ((WaypointButton) w).waypoint.x / 512D;
				double qy = ((WaypointButton) w).waypoint.z / 512D;
				int s = Math.max(4, z / 128);

				double x = (qx - regionMinX) * z - s / 2D;
				double y = (qy - regionMinZ) * z - s / 2D;
				w.setPosAndSize((int) x, (int) y, s, s);
			}
			else if (w instanceof PlayerButton)
			{
				double qx = ((PlayerButton) w).playerX / 512D;
				double qy = ((PlayerButton) w).playerZ / 512D;
				int s = Math.max(4, z / 128);

				double x = (qx - regionMinX) * z - s / 2D;
				double y = (qy - regionMinZ) * z - s / 2D;
				w.setPosAndSize((int) x, (int) y, s, s);
			}
		}

		setPosAndSize(0, 0, parent.width, parent.height);
	}

	@Override
	public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h)
	{
		super.draw(matrixStack, theme, x, y, w, h);

		int dx = (regionMaxX - regionMinX);
		int dy = (regionMaxZ - regionMinZ);

		double px = getX() - getScrollX();
		double py = getY() - getScrollY();

		regionX = (parent.getMouseX() - px) / (double) largeMap.scrollWidth * dx + regionMinX;
		regionZ = (parent.getMouseY() - py) / (double) largeMap.scrollHeight * dy + regionMinZ;
		blockX = MathHelper.floor(regionX * 512D);
		blockZ = MathHelper.floor(regionZ * 512D);
		blockY = 0;

		MapRegion r = largeMap.dimension.getRegions().get(XZ.regionFromBlock(blockX, blockZ));

		if (r != null)
		{
			MapChunk c = r.getChunks().get(XZ.of((blockX >> 4) & 31, (blockZ >> 4) & 31));

			if (c != null)
			{
				blockY = c.getHeight(blockX, blockZ);
			}
		}

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
	public void addMouseOverText(TooltipList list)
	{
		super.addMouseOverText(list);

		MapRegion r = largeMap.dimension.getRegions().get(XZ.regionFromBlock(blockX, blockZ));

		if (r != null)
		{
			MapChunk c = r.getChunks().get(XZ.of((blockX >> 4) & 31, (blockZ >> 4) & 31));

			if (c != null && c.owner != StringTextComponent.EMPTY)
			{
				list.add(c.owner);
			}
		}
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
	public void drawBackground(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h)
	{
		super.drawBackground(matrixStack, theme, x, y, w, h);
	}
}