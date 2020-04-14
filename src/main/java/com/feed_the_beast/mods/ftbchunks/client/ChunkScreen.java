package com.feed_the_beast.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import com.feed_the_beast.mods.ftbchunks.client.map.ClientMapChunk;
import com.feed_the_beast.mods.ftbchunks.client.map.ClientMapDimension;
import com.feed_the_beast.mods.ftbchunks.impl.ClaimedChunkManagerImpl;
import com.feed_the_beast.mods.ftbchunks.impl.map.XZ;
import com.feed_the_beast.mods.ftbchunks.net.FTBChunksNet;
import com.feed_the_beast.mods.ftbchunks.net.RequestChunkChangePacket;
import com.feed_the_beast.mods.ftbchunks.net.RequestMapDataPacket;
import com.feed_the_beast.mods.ftbchunks.net.RequestPlayerListPacket;
import com.feed_the_beast.mods.ftbchunks.net.SendGeneralData;
import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import com.feed_the_beast.mods.ftbguilibrary.icon.Icon;
import com.feed_the_beast.mods.ftbguilibrary.utils.MathUtils;
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
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author LatvianModder
 */
public class ChunkScreen extends GuiBase
{
	public class ChunkButton extends Button
	{
		public final XZ chunkPos;
		public ClientMapChunk chunk;

		public ChunkButton(Panel panel, XZ xz)
		{
			super(panel, "", Icon.EMPTY);
			setSize(FTBChunks.TILE_SIZE, FTBChunks.TILE_SIZE);
			chunkPos = xz;
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
		@SuppressWarnings("deprecation")
		public void addMouseOverText(List<String> list)
		{
			if (chunk != null)
			{
				list.add(chunk.formattedOwner);

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

	public List<ChunkButton> chunkButtons;
	public Set<XZ> selectedChunks;

	@Override
	public boolean onInit()
	{
		return setFullscreen();
	}

	@Override
	public void addWidgets()
	{
		int sx = getX() + (width - FTBChunks.MINIMAP_SIZE) / 2;
		int sy = getY() + (height - FTBChunks.MINIMAP_SIZE) / 2;
		PlayerEntity player = Minecraft.getInstance().player;
		int startX = player.chunkCoordX - FTBChunks.TILE_OFFSET;
		int startZ = player.chunkCoordZ - FTBChunks.TILE_OFFSET;
		//FTBChunks.CLIENT_EXECUTOR.execute(new ReloadMinimapTask(player.world, startX, startZ));

		chunkButtons = new ArrayList<>();
		selectedChunks = new LinkedHashSet<>();

		for (int z = 0; z < FTBChunks.TILES; z++)
		{
			for (int x = 0; x < FTBChunks.TILES; x++)
			{
				ChunkButton button = new ChunkButton(this, XZ.of(startX + x, startZ + z));
				button.chunk = ClientMapDimension.get().getRegion(XZ.regionFromChunk(startX + x, startZ + z)).getChunk(button.chunkPos);
				chunkButtons.add(button);
				button.setPos(sx + x * FTBChunks.TILE_SIZE, sy + z * FTBChunks.TILE_SIZE);
			}
		}

		addAll(chunkButtons);
		FTBChunksNet.MAIN.sendToServer(new RequestMapDataPacket(player.chunkCoordX - FTBChunks.TILE_OFFSET, player.chunkCoordZ - FTBChunks.TILE_OFFSET, player.chunkCoordX + FTBChunks.TILE_OFFSET, player.chunkCoordZ + FTBChunks.TILE_OFFSET));
		add(new SimpleButton(this, I18n.format("ftbchunks.gui.allies"), GuiIcons.FRIENDS, (simpleButton, mouseButton) -> FTBChunksNet.MAIN.sendToServer(new RequestPlayerListPacket())).setPosAndSize(2, 2, 16, 16));
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
		int sx = x + (w - FTBChunks.MINIMAP_SIZE) / 2;
		int sy = y + (h - FTBChunks.MINIMAP_SIZE) / 2;
		PlayerEntity player = Minecraft.getInstance().player;
		int cx = player.chunkCoordX;
		int cz = player.chunkCoordZ;
		int startX = cx - FTBChunks.TILE_OFFSET;
		int startZ = cz - FTBChunks.TILE_OFFSET;

		int r = 70;
		int g = 70;
		int b = 70;
		int a = 100;

		RenderSystem.lineWidth(Math.max(2.5F, (float) Minecraft.getInstance().getMainWindow().getFramebufferWidth() / 1920.0F * 2.5F));
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();

		RenderSystem.enableTexture();
		RenderSystem.bindTexture(FTBChunksClient.minimapTextureId);
		RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		GuiHelper.drawTexturedRect(sx, sy, FTBChunks.MINIMAP_SIZE, FTBChunks.MINIMAP_SIZE, Color4I.WHITE, 0F, 0F, 1F, 1F);
		GlStateManager.disableTexture();

		buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

		for (int gy = 1; gy < FTBChunks.TILES; gy++)
		{
			buffer.pos(sx, sy + gy * FTBChunks.TILE_SIZE, 0).color(r, g, b, a).endVertex();
			buffer.pos(sx + FTBChunks.MINIMAP_SIZE, sy + gy * FTBChunks.TILE_SIZE, 0).color(r, g, b, a).endVertex();
		}

		for (int gx = 1; gx < FTBChunks.TILES; gx++)
		{
			buffer.pos(sx + gx * FTBChunks.TILE_SIZE, sy, 0).color(r, g, b, a).endVertex();
			buffer.pos(sx + gx * FTBChunks.TILE_SIZE, sy + FTBChunks.MINIMAP_SIZE, 0).color(r, g, b, a).endVertex();
		}

		tessellator.draw();

		if (!InputMappings.isKeyDown(Minecraft.getInstance().getMainWindow().getHandle(), 258))
		{
			buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
			boolean anyForceLoaded = false;

			for (ChunkButton chunkButton : chunkButtons)
			{
				if (chunkButton.chunk.forceLoaded)
				{
					anyForceLoaded = true;
				}
			}

			tessellator.draw();

			if (anyForceLoaded)
			{
				buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

				for (ChunkButton chunkButton : chunkButtons)
				{
					if (chunkButton.chunk.forceLoaded)
					{
						int lx = sx + (FTBChunks.TILE_OFFSET + (cx - startX)) * FTBChunks.TILE_SIZE;
						int lz = sy + (FTBChunks.TILE_OFFSET + (cz - startZ)) * FTBChunks.TILE_SIZE;

						buffer.pos(lx, lz, 0).color(255, 0, 0, 100).endVertex();
						buffer.pos(lx + FTBChunks.TILE_SIZE, lz + FTBChunks.TILE_SIZE, 0).color(255, 0, 0, 100).endVertex();

						buffer.pos(lx + FTBChunks.TILE_SIZE / 2D, lz, 0).color(255, 0, 0, 100).endVertex();
						buffer.pos(lx + FTBChunks.TILE_SIZE, lz + FTBChunks.TILE_SIZE / 2D, 0).color(255, 0, 0, 100).endVertex();

						buffer.pos(lx, lz + FTBChunks.TILE_SIZE / 2D, 0).color(255, 0, 0, 100).endVertex();
						buffer.pos(lx + FTBChunks.TILE_SIZE / 2D, lz + FTBChunks.TILE_SIZE, 0).color(255, 0, 0, 100).endVertex();
					}
				}

				tessellator.draw();
			}
		}

		GlStateManager.enableTexture();
		GlStateManager.lineWidth(1F);

		if (cx >= startX && cz >= startZ && cx < startX + FTBChunks.MINIMAP_SIZE && cz < startZ + FTBChunks.MINIMAP_SIZE)
		{
			double x1 = ((cx - startX) * 16D + MathUtils.mod(player.getPosX(), 16D));
			double z1 = ((cz - startZ) * 16D + MathUtils.mod(player.getPosZ(), 16D));

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

			RenderSystem.popMatrix();
		}

		SendGeneralData d = FTBChunksClient.generalData;

		if (d != null)
		{
			List<String> list = new ArrayList<>(4);
			list.add(I18n.format("ftbchunks.gui.claimed"));
			list.add((d.claimed > d.maxClaimed ? TextFormatting.RED : d.claimed == d.maxClaimed ? TextFormatting.YELLOW : TextFormatting.GREEN) + "" + d.claimed + " / " + d.maxClaimed);
			list.add(I18n.format("ftbchunks.gui.force_loaded"));
			list.add((d.loaded > d.maxLoaded ? TextFormatting.RED : d.loaded == d.maxLoaded ? TextFormatting.YELLOW : TextFormatting.GREEN) + "" + d.loaded + " / " + d.maxLoaded);

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