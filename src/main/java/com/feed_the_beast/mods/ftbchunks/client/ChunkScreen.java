package com.feed_the_beast.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import com.feed_the_beast.mods.ftbchunks.impl.ClaimedChunkManagerImpl;
import com.feed_the_beast.mods.ftbchunks.net.FTBChunksNet;
import com.feed_the_beast.mods.ftbchunks.net.NetClaimedChunk;
import com.feed_the_beast.mods.ftbchunks.net.NetClaimedChunkData;
import com.feed_the_beast.mods.ftbchunks.net.NetClaimedChunkGroup;
import com.feed_the_beast.mods.ftbchunks.net.RequestChunkChangePacket;
import com.feed_the_beast.mods.ftbchunks.net.RequestMapDataPacket;
import com.feed_the_beast.mods.ftbchunks.net.RequestPlayerListPacket;
import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import com.feed_the_beast.mods.ftbguilibrary.icon.Icon;
import com.feed_the_beast.mods.ftbguilibrary.utils.MouseButton;
import com.feed_the_beast.mods.ftbguilibrary.widget.Button;
import com.feed_the_beast.mods.ftbguilibrary.widget.GuiBase;
import com.feed_the_beast.mods.ftbguilibrary.widget.GuiHelper;
import com.feed_the_beast.mods.ftbguilibrary.widget.GuiIcons;
import com.feed_the_beast.mods.ftbguilibrary.widget.Panel;
import com.feed_the_beast.mods.ftbguilibrary.widget.SimpleButton;
import com.feed_the_beast.mods.ftbguilibrary.widget.Theme;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author LatvianModder
 */
public class ChunkScreen extends GuiBase
{
	public class ChunkButton extends Button
	{
		public final ChunkPos chunkPos;
		public NetClaimedChunk chunk;

		public ChunkButton(Panel panel, ChunkPos cp)
		{
			super(panel, "", Icon.EMPTY);
			setSize(FTBChunks.TILE_SIZE, FTBChunks.TILE_SIZE);
			chunkPos = cp;
		}

		@Override
		public void onClicked(MouseButton mouseButton)
		{
			selectedChunks.add(chunkPos);
		}

		@Override
		public void drawBackground(Theme theme, int x, int y, int w, int h)
		{
			if (isMouseOver() || selectedChunks.contains(chunkPos))
			{
				Color4I.WHITE.withAlpha(100).draw(x, y, w, h);

				if (isMouseButtonDown(MouseButton.LEFT) || isMouseButtonDown(MouseButton.RIGHT))
				{
					selectedChunks.add(chunkPos);
				}
			}
		}

		@Override
		public void addMouseOverText(List<String> list)
		{
			if (chunk != null)
			{
				list.add(chunk.group.formattedOwner);

				Date date = new Date();

				if (Screen.hasAltDown())
				{
					list.add(TextFormatting.GRAY + chunk.claimedDate.toLocaleString());
				}
				else
				{
					list.add(TextFormatting.GRAY + ClaimedChunkManagerImpl.prettyTimeString((date.getTime() - chunk.claimedDate.getTime()) / 1000L) + " ago");
				}

				if (chunk.forceLoadedDate != null)
				{
					list.add(TextFormatting.RED + I18n.format("ftbchunks.gui.force_loaded"));

					if (Screen.hasAltDown())
					{
						list.add(TextFormatting.GRAY + chunk.forceLoadedDate.toLocaleString());
					}
					else
					{
						list.add(TextFormatting.GRAY + ClaimedChunkManagerImpl.prettyTimeString((date.getTime() - chunk.forceLoadedDate.getTime()) / 1000L) + " ago");
					}
				}
			}
		}
	}

	public NetClaimedChunkData data;
	public List<ChunkButton> chunkButtons;
	public Set<ChunkPos> selectedChunks;

	@Override
	public boolean onInit()
	{
		return setFullscreen();
	}

	@Override
	public void addWidgets()
	{
		int sx = getX() + (width - FTBChunks.GUI_SIZE) / 2;
		int sy = getY() + (height - FTBChunks.GUI_SIZE) / 2;
		int startX = Minecraft.getInstance().player.chunkCoordX - FTBChunks.TILE_OFFSET;
		int startZ = Minecraft.getInstance().player.chunkCoordZ - FTBChunks.TILE_OFFSET;
		ThreadReloadChunkSelector.reloadArea(Minecraft.getInstance().world, startX, startZ);

		chunkButtons = new ArrayList<>();
		selectedChunks = new LinkedHashSet<>();

		for (int z = 0; z < FTBChunks.TILES; z++)
		{
			for (int x = 0; x < FTBChunks.TILES; x++)
			{
				ChunkButton button = new ChunkButton(this, new ChunkPos(startX + x, startZ + z));
				chunkButtons.add(button);
				button.setPos(sx + x * FTBChunks.TILE_SIZE, sy + z * FTBChunks.TILE_SIZE);
			}
		}

		addAll(chunkButtons);
		FTBChunksNet.MAIN.sendToServer(new RequestMapDataPacket());
		add(new SimpleButton(this, I18n.format("ftbchunks.gui.allies"), GuiIcons.FRIENDS, (simpleButton, mouseButton) -> FTBChunksNet.MAIN.sendToServer(new RequestPlayerListPacket())).setPosAndSize(2, 2, 16, 16));
	}

	public void setData(NetClaimedChunkData d)
	{
		int centerX = Minecraft.getInstance().player.chunkCoordX;
		int centerZ = Minecraft.getInstance().player.chunkCoordZ;

		for (NetClaimedChunkGroup group : d.groups)
		{
			group.formattedOwner = new StringTextComponent("").applyTextStyle(TextFormatting.AQUA).appendSibling(group.owner).getFormattedText();
		}

		Map<ChunkPos, NetClaimedChunk> chunkMap = new HashMap<>();

		for (NetClaimedChunk chunk : d.chunks)
		{
			chunkMap.put(new ChunkPos(centerX + chunk.x, centerZ + chunk.z), chunk);
		}

		for (ChunkButton button : chunkButtons)
		{
			button.chunk = chunkMap.get(new ChunkPos(button.chunkPos.x, button.chunkPos.z));
		}

		data = d;
	}

	@Override
	public void mouseReleased(MouseButton button)
	{
		super.mouseReleased(button);

		if (!selectedChunks.isEmpty())
		{
			FTBChunksNet.MAIN.sendToServer(new RequestChunkChangePacket(isShiftKeyDown() ? (button.isLeft() ? 2 : 3) : (button.isLeft() ? 0 : 1), selectedChunks));
			selectedChunks.clear();
			playClickSound();
		}
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
			List<String> list = new ArrayList<>(4);
			list.add(I18n.format("ftbchunks.gui.claimed"));
			list.add((data.claimed > data.maxClaimed ? TextFormatting.RED : data.claimed == data.maxClaimed ? TextFormatting.YELLOW : TextFormatting.GREEN) + "" + data.claimed + " / " + data.maxClaimed);
			list.add(I18n.format("ftbchunks.gui.force_loaded"));
			list.add((data.loaded > data.maxLoaded ? TextFormatting.RED : data.loaded == data.maxLoaded ? TextFormatting.YELLOW : TextFormatting.GREEN) + "" + data.loaded + " / " + data.maxLoaded);

			for (int i = 0; i < list.size(); i++)
			{
				theme.drawString(list.get(i), 3, getScreen().getScaledHeight() - 10 * (list.size() - i) - 1, Color4I.WHITE, Theme.SHADOW);
			}
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