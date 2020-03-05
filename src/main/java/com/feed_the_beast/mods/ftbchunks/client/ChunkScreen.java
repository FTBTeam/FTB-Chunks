package com.feed_the_beast.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import com.feed_the_beast.mods.ftbchunks.net.FTBChunksNet;
import com.feed_the_beast.mods.ftbchunks.net.NetClaimedChunk;
import com.feed_the_beast.mods.ftbchunks.net.NetClaimedChunkData;
import com.feed_the_beast.mods.ftbchunks.net.RequestMapDataPacket;
import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import com.feed_the_beast.mods.ftbguilibrary.icon.Icon;
import com.feed_the_beast.mods.ftbguilibrary.utils.MouseButton;
import com.feed_the_beast.mods.ftbguilibrary.widget.Button;
import com.feed_the_beast.mods.ftbguilibrary.widget.GuiBase;
import com.feed_the_beast.mods.ftbguilibrary.widget.GuiHelper;
import com.feed_the_beast.mods.ftbguilibrary.widget.Panel;
import com.feed_the_beast.mods.ftbguilibrary.widget.Theme;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import org.lwjgl.opengl.GL11;

/**
 * @author LatvianModder
 */
public class ChunkScreen extends GuiBase
{
	public class ChunkButton extends Button
	{
		public final int x, z;

		public ChunkButton(Panel panel, int _x, int _z)
		{
			super(panel, "ABC", Icon.EMPTY);
			x = _x;
			z = _z;
		}

		@Override
		public void onClicked(MouseButton mouseButton)
		{
			playClickSound();
		}
	}

	public NetClaimedChunkData data;

	@Override
	public boolean onInit()
	{
		return setFullscreen();
	}

	@Override
	public void addWidgets()
	{
		int startX = Minecraft.getInstance().player.chunkCoordX - FTBChunks.TILE_OFFSET;
		int startZ = Minecraft.getInstance().player.chunkCoordZ - FTBChunks.TILE_OFFSET;
		ThreadReloadChunkSelector.reloadArea(Minecraft.getInstance().world, startX, startZ);
		FTBChunksNet.MAIN.sendToServer(new RequestMapDataPacket());

		for (int z = 0; z < FTBChunks.TILES; z++)
		{
			for (int x = 0; x < FTBChunks.TILES; x++)
			{
				add(new ChunkButton(this, startX + x, startZ + z));
			}
		}
	}

	public void setData(NetClaimedChunkData d)
	{
		data = d;
	}

	@Override
	public void drawBackground(Theme theme, int x, int y, int w, int h)
	{
		int sx = x + (w - FTBChunks.GUI_SIZE) / 2;
		int sy = y + (h - FTBChunks.GUI_SIZE) / 2;

		int r = 70;
		int g = 70;
		int b = 70;
		int a = 100;

		GlStateManager.disableTexture();
		RenderSystem.lineWidth(Math.max(2.5F, (float) Minecraft.getInstance().getMainWindow().getFramebufferWidth() / 1920.0F * 2.5F));
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();

		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		rect(buffer, sx - 1, sy - 1, FTBChunks.GUI_SIZE + 2, FTBChunks.GUI_SIZE + 2, 0, 0, 0, 255);
		tessellator.draw();

		ThreadReloadChunkSelector.updateTexture();

		RenderSystem.enableTexture();
		GlStateManager.bindTexture(ThreadReloadChunkSelector.getTextureId());
		GuiHelper.drawTexturedRect(sx, sy, FTBChunks.GUI_SIZE, FTBChunks.GUI_SIZE, Color4I.WHITE, 0F, 0F, FTBChunks.UV, FTBChunks.UV);
		GlStateManager.disableTexture();

		buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

		for (int gy = 0; gy <= FTBChunks.TILES; gy++)
		{
			buffer.pos(sx, sy + gy * FTBChunks.TILE_SIZE, 0).color(r, g, b, a).endVertex();
			buffer.pos(sx + FTBChunks.GUI_SIZE, sy + gy * FTBChunks.TILE_SIZE, 0).color(r, g, b, a).endVertex();
		}

		for (int gx = 0; gx <= FTBChunks.TILES; gx++)
		{
			buffer.pos(sx + gx * FTBChunks.TILE_SIZE, sy, 0).color(r, g, b, a).endVertex();
			buffer.pos(sx + gx * FTBChunks.TILE_SIZE, sy + FTBChunks.GUI_SIZE, 0).color(r, g, b, a).endVertex();
		}

		tessellator.draw();

		if (data != null && !data.chunks.isEmpty())
		{
			buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
			boolean anyForceLoaded = false;

			for (NetClaimedChunk chunk : data.chunks)
			{
				int cr = (chunk.group.color >> 16) & 255;
				int cg = (chunk.group.color >> 8) & 255;
				int cb = (chunk.group.color >> 0) & 255;

				int cx = (FTBChunks.TILE_OFFSET + chunk.x) * FTBChunks.TILE_SIZE;
				int cy = (FTBChunks.TILE_OFFSET + chunk.z) * FTBChunks.TILE_SIZE;

				rect(buffer, sx + cx, sy + cy, FTBChunks.TILE_SIZE, FTBChunks.TILE_SIZE, cr, cg, cb, 100);

				if ((chunk.borders & 1) != 0)
				{
					rect(buffer, sx + cx, sy + cy, FTBChunks.TILE_SIZE, 1, cr, cg, cb, 255);
				}

				if ((chunk.borders & 2) != 0)
				{
					rect(buffer, sx + cx, sy + cy + FTBChunks.TILE_SIZE - 1, FTBChunks.TILE_SIZE, 1, cr, cg, cb, 255);
				}

				if ((chunk.borders & 4) != 0)
				{
					rect(buffer, sx + cx, sy + cy, 1, FTBChunks.TILE_SIZE, cr, cg, cb, 255);
				}

				if ((chunk.borders & 8) != 0)
				{
					rect(buffer, sx + cx + FTBChunks.TILE_SIZE - 1, sy + cy, 1, FTBChunks.TILE_SIZE, cr, cg, cb, 255);
				}

				if (chunk.group.forceLoaded)
				{
					anyForceLoaded = true;
				}
			}

			tessellator.draw();

			if (anyForceLoaded)
			{
				buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

				for (NetClaimedChunk chunk : data.chunks)
				{
					if (chunk.group.forceLoaded)
					{
						int cx = (FTBChunks.TILE_OFFSET + chunk.x) * FTBChunks.TILE_SIZE;
						int cy = (FTBChunks.TILE_OFFSET + chunk.z) * FTBChunks.TILE_SIZE;

						buffer.pos(sx + cx, sy + cy, 0).color(255, 0, 0, 100).endVertex();
						buffer.pos(sx + cx + FTBChunks.TILE_SIZE, sy + cy + FTBChunks.TILE_SIZE, 0).color(255, 0, 0, 100).endVertex();

						buffer.pos(sx + cx + FTBChunks.TILE_SIZE / 2F, sy + cy, 0).color(255, 0, 0, 100).endVertex();
						buffer.pos(sx + cx + FTBChunks.TILE_SIZE, sy + cy + FTBChunks.TILE_SIZE / 2F, 0).color(255, 0, 0, 100).endVertex();

						buffer.pos(sx + cx, sy + cy + FTBChunks.TILE_SIZE / 2F, 0).color(255, 0, 0, 100).endVertex();
						buffer.pos(sx + cx + FTBChunks.TILE_SIZE / 2F, sy + cy + FTBChunks.TILE_SIZE, 0).color(255, 0, 0, 100).endVertex();
					}
				}

				tessellator.draw();
			}
		}

		GlStateManager.enableTexture();
		GlStateManager.lineWidth(1F);
		
		/*
		EntityPlayer player = Minecraft.getMinecraft().player;

		int cx = MathUtils.chunk(player.posX);
		int cy = MathUtils.chunk(player.posZ);

		if (cx >= startX && cy >= startZ && cx < startX + TILES_GUI && cy < startZ + TILES_GUI)
		{
			double x = ((cx - startX) * 16D + MathUtils.mod(player.posX, 16D));
			double y = ((cy - startZ) * 16D + MathUtils.mod(player.posZ, 16D));

			GlStateManager.pushMatrix();
			GlStateManager.translate(ax + x * GuiChunkSelectorBase.TILE_SIZE / 16D, ay + y * GuiChunkSelectorBase.TILE_SIZE / 16D, 0D);
			GlStateManager.pushMatrix();
			GlStateManager.rotate(player.rotationYaw + 180F, 0F, 0F, 1F);
			TEX_ENTITY.draw(-8, -8, 16, 16);
			GlStateManager.popMatrix();
			ClientUtils.localPlayerHead.draw(-2, -2, 4, 4);
			GlStateManager.popMatrix();
		}
		*/

		if (data != null)
		{
			theme.drawString(I18n.format("ftbchunks.gui.claimed", data.claimed, data.maxClaimed), 3, getScreen().getScaledHeight() - 21, Color4I.WHITE, Theme.SHADOW);
			theme.drawString(I18n.format("ftbchunks.gui.force_loaded", data.loaded, data.maxLoaded), 3, getScreen().getScaledHeight() - 11, Color4I.WHITE, Theme.SHADOW);
		}
	}

	private void rect(BufferBuilder buffer, int x, int y, int w, int h, int r, int g, int b, int a)
	{
		buffer.pos(x, y + h, 0).color(r, g, b, a).endVertex();
		buffer.pos(x + w, y + h, 0).color(r, g, b, a).endVertex();
		buffer.pos(x + w, y, 0).color(r, g, b, a).endVertex();
		buffer.pos(x, y, 0).color(r, g, b, a).endVertex();
	}
}