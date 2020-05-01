package com.feed_the_beast.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import com.feed_the_beast.mods.ftbchunks.FTBChunksCommon;
import com.feed_the_beast.mods.ftbchunks.api.Waypoint;
import com.feed_the_beast.mods.ftbchunks.client.map.ClientMapChunk;
import com.feed_the_beast.mods.ftbchunks.client.map.ClientMapDimension;
import com.feed_the_beast.mods.ftbchunks.client.map.ClientMapManager;
import com.feed_the_beast.mods.ftbchunks.client.map.ClientMapRegion;
import com.feed_the_beast.mods.ftbchunks.impl.map.ColorBlend;
import com.feed_the_beast.mods.ftbchunks.impl.map.XZ;
import com.feed_the_beast.mods.ftbchunks.net.LoginData;
import com.feed_the_beast.mods.ftbchunks.net.SendChunk;
import com.feed_the_beast.mods.ftbchunks.net.SendGeneralData;
import com.feed_the_beast.mods.ftbchunks.net.SendPlayerListPacket;
import com.feed_the_beast.mods.ftbchunks.net.SendWaypoints;
import com.feed_the_beast.mods.ftbguilibrary.utils.MathUtils;
import com.feed_the_beast.mods.ftbguilibrary.widget.CustomClickEvent;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author LatvianModder
 */
public class FTBChunksClient extends FTBChunksCommon
{
	private static final ResourceLocation BUTTON_ID = new ResourceLocation("ftbchunks:open_gui");
	public static final ResourceLocation MAP_ICONS = new ResourceLocation("textures/map/map_icons.png");
	public static final ResourceLocation CIRCLE_MASK = new ResourceLocation("ftbchunks:textures/circle_mask.png");
	public static final ResourceLocation CIRCLE_BORDER = new ResourceLocation("ftbchunks:textures/circle_border.png");
	public static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();

	public static int minimapTextureId = -1;
	private static int currentPlayerChunkX, currentPlayerChunkZ;
	public static boolean blur = true;
	public static float noise = 0.05F;

	private boolean updateMinimap = false;
	public static SendGeneralData generalData;
	private long nextRegionSave = 0L;

	public void init()
	{
		MinecraftForge.EVENT_BUS.register(this);
	}

	public static void openGui()
	{
		new ChunkScreen().openGui();
	}

	@Override
	public void login(LoginData loginData)
	{
		if (ClientMapManager.inst != null)
		{
			ClientMapManager.inst.release();
		}

		ClientMapManager.inst = new ClientMapManager(loginData.serverId, FMLPaths.GAMEDIR.get().resolve("local/ftbchunks/map/" + loginData.serverId));
		ClientMapDimension.current = ClientMapManager.inst.getDimension(Minecraft.getInstance().world.dimension.getType());
		updateMinimap = true;
	}

	@Override
	public void updateGeneralData(SendGeneralData packet)
	{
		generalData = packet;
	}

	@Override
	public void updateChunk(SendChunk packet)
	{
		try
		{
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(packet.imageData));
			//System.out.println(image);
			ClientMapChunk chunk = ClientMapDimension.current.getRegion(XZ.regionFromChunk(packet.x, packet.z)).getChunk(XZ.of(packet.x, packet.z));
			Date now = new Date();
			chunk.claimedDate = packet.owner == null ? null : new Date(now.getTime() - packet.relativeTimeClaimed);
			chunk.forceLoadedDate = packet.forceLoaded && chunk.claimedDate != null ? new Date(now.getTime() - packet.relativeTimeForceLoaded) : null;
			chunk.color = packet.color;
			chunk.formattedOwner = packet.owner == null ? "" : packet.owner.getFormattedText();
			boolean updateRegion = false;
			Random random = new Random(packet.x * 31L + packet.z);

			for (int z = 0; z < 16; z++)
			{
				for (int x = 0; x < 16; x++)
				{
					int c = chunk.region.getImage().getPixelRGBA(chunk.pos.x * 16 + x, chunk.pos.z * 16 + z);
					int h0 = NativeImage.getAlpha(c);
					int r0 = NativeImage.getRed(c);
					int g0 = NativeImage.getGreen(c);
					int b0 = NativeImage.getBlue(c);

					int nc = image.getRGB(x, z);

					if (noise != 0F)
					{
						nc = ColorBlend.addBrightness(nc, random.nextFloat() * noise - noise / 2F);
					}

					int h = (nc >> 24) & 0xFF;
					int r = (nc >> 16) & 0xFF;
					int g = (nc >> 8) & 0xFF;
					int b = (nc >> 0) & 0xFF;

					if (h0 != h || r0 != r || g0 != g || b0 != b)
					{
						chunk.region.getImage().setPixelRGBA(chunk.pos.x * 16 + x, chunk.pos.z * 16 + z, NativeImage.getCombined(h, b, g, r));
						updateRegion = true;
					}
				}
			}

			if (updateRegion)
			{
				chunk.region.saveImage = true;
				updateMinimap = true;
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	@Override
	public void updateWaypoints(SendWaypoints packet)
	{
		for (ClientMapDimension dimension : ClientMapManager.inst.dimensions.values())
		{
			dimension.waypoints.clear();
		}

		for (Waypoint waypoint : packet.waypoints)
		{
			ClientMapManager.inst.getDimension(waypoint.dimension).waypoints.add(waypoint);
		}
	}

	@Override
	public void openPlayerList(SendPlayerListPacket packet)
	{
		new PlayerListScreen(packet.players, packet.allyMode).openGui();
	}

	@SubscribeEvent
	public void customClick(CustomClickEvent event)
	{
		if (event.getId().equals(BUTTON_ID))
		{
			openGui();
		}
	}

	@SubscribeEvent
	public void renderGameOverlay(RenderGameOverlayEvent.Post event)
	{
		Minecraft mc = Minecraft.getInstance();

		if (mc.player == null)
		{
			return;
		}

		if (ClientMapManager.inst == null)
		{
			return;
		}

		if (ClientMapDimension.current == null || ClientMapDimension.current.dimension != Minecraft.getInstance().world.getDimension().getType())
		{
			ClientMapDimension.current = ClientMapManager.inst.getDimension(Minecraft.getInstance().world.getDimension().getType());
		}

		long now = System.currentTimeMillis();

		if (nextRegionSave == 0L || now >= nextRegionSave)
		{
			nextRegionSave = now + 3000L;

			for (ClientMapDimension dimension : ClientMapManager.inst.dimensions.values())
			{
				for (ClientMapRegion region : dimension.regions.values())
				{
					if (region.saveImage)
					{
						EXECUTOR_SERVICE.submit(() -> {
							if (Files.notExists(dimension.directory))
							{
								Files.createDirectories(dimension.directory);
							}

							region.getImage().write(dimension.directory.resolve(region.pos.x + "," + region.pos.z + ",map.png"));
							return null;
						});

						region.saveImage = false;
					}
				}
			}
		}

		if (minimapTextureId == -1)
		{
			minimapTextureId = TextureUtil.generateTextureId();
			TextureUtil.prepareImage(minimapTextureId, FTBChunks.MINIMAP_SIZE, FTBChunks.MINIMAP_SIZE);
			updateMinimap = true;
		}

		RenderSystem.enableTexture();
		RenderSystem.bindTexture(minimapTextureId);

		if (blur)
		{
			RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
			RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		}
		else
		{
			RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
			RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		}

		int cx = mc.player.chunkCoordX;
		int cz = mc.player.chunkCoordZ;

		if (cx != currentPlayerChunkX || cz != currentPlayerChunkZ)
		{
			updateMinimap = true;
		}

		if (updateMinimap)
		{
			updateMinimap = false;

			// TODO: More math here to upload from (up to) 4 regions instead of all chunks inside them, to speed things up

			for (int mz = 0; mz < FTBChunks.TILES; mz++)
			{
				for (int mx = 0; mx < FTBChunks.TILES; mx++)
				{
					int ox = cx + mx - FTBChunks.TILE_OFFSET;
					int oz = cz + mz - FTBChunks.TILE_OFFSET;

					ClientMapRegion region = ClientMapDimension.current.getRegion(XZ.regionFromChunk(ox, oz));
					region.getImage().uploadTextureSub(0, mx * 16, mz * 16, (ox & 31) * 16, (oz & 31) * 16, 16, 16, false, false);
				}
			}

			currentPlayerChunkX = cx;
			currentPlayerChunkZ = cz;
		}

		if (mc.gameSettings.showDebugInfo)
		{
			return;
		}

		double scale = 1D * 4D / mc.getMainWindow().getGuiScaleFactor();

		int s = (int) (64D * scale);
		int x = mc.getMainWindow().getScaledWidth() - 3 - s;
		int y = 3;
		int z = 0;

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableCull();
		RenderSystem.color4f(1F, 1F, 1F, 1F);

		double border = 0D;

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();

		float f0 = 1F / (float) FTBChunks.TILES;
		float f1 = 1F - f0;

		float offX = (float) ((mc.player.getPosX() / 16D - currentPlayerChunkX - 0.5D) / (double) FTBChunks.TILES);
		float offY = (float) ((mc.player.getPosZ() / 16D - currentPlayerChunkZ - 0.5D) / (double) FTBChunks.TILES);

		RenderSystem.enableTexture();

		RenderSystem.pushMatrix();
		RenderSystem.translated(x + s / 2D, y + s / 2D, 0D);
		RenderSystem.rotatef(-mc.player.rotationYaw + 180F, 0F, 0F, 1F);

		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ZERO);
		mc.getTextureManager().bindTexture(CIRCLE_MASK);
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
		buffer.pos(-s / 2D + border, -s / 2D + border, z).color(255, 255, 255, 255).tex(0F, 0F).endVertex();
		buffer.pos(-s / 2D + border, s / 2D - border, z).color(255, 255, 255, 255).tex(0F, 1F).endVertex();
		buffer.pos(s / 2D - border, s / 2D - border, z).color(255, 255, 255, 255).tex(1F, 1F).endVertex();
		buffer.pos(s / 2D - border, -s / 2D + border, z).color(255, 255, 255, 255).tex(1F, 0F).endVertex();
		tessellator.draw();

		RenderSystem.blendFunc(GlStateManager.SourceFactor.DST_ALPHA, GlStateManager.DestFactor.ONE_MINUS_DST_ALPHA);
		RenderSystem.bindTexture(minimapTextureId);

		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
		buffer.pos(-s / 2D + border, -s / 2D + border, z).color(255, 255, 255, 255).tex(f0 + offX, f0 + offY).endVertex();
		buffer.pos(-s / 2D + border, s / 2D - border, z).color(255, 255, 255, 255).tex(f0 + offX, f1 + offY).endVertex();
		buffer.pos(s / 2D - border, s / 2D - border, z).color(255, 255, 255, 255).tex(f1 + offX, f1 + offY).endVertex();
		buffer.pos(s / 2D - border, -s / 2D + border, z).color(255, 255, 255, 255).tex(f1 + offX, f0 + offY).endVertex();
		tessellator.draw();
		RenderSystem.popMatrix();

		RenderSystem.defaultBlendFunc();
		mc.getTextureManager().bindTexture(CIRCLE_BORDER);
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
		buffer.pos(x, y, z).color(255, 255, 255, 255).tex(0F, 0F).endVertex();
		buffer.pos(x, y + s, z).color(255, 255, 255, 255).tex(0F, 1F).endVertex();
		buffer.pos(x + s, y + s, z).color(255, 255, 255, 255).tex(1F, 1F).endVertex();
		buffer.pos(x + s, y, z).color(255, 255, 255, 255).tex(1F, 0F).endVertex();
		tessellator.draw();

		RenderSystem.disableTexture();
		buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
		buffer.pos(x + s / 2D, y + 0, z).color(0, 0, 0, 30).endVertex();
		buffer.pos(x + s / 2D, y + s, z).color(0, 0, 0, 30).endVertex();
		buffer.pos(x + 0, y + s / 2D, z).color(0, 0, 0, 30).endVertex();
		buffer.pos(x + s, y + s / 2D, z).color(0, 0, 0, 30).endVertex();
		tessellator.draw();

		RenderSystem.enableTexture();

		if (!ClientMapDimension.current.waypoints.isEmpty())
		{
			for (Waypoint waypoint : ClientMapDimension.current.waypoints)
			{
				double d = MathUtils.dist(mc.player.getPosX(), mc.player.getPosZ(), waypoint.x, waypoint.z) / 3.2D * scale;

				if (d > s / 2D)
				{
					d = s / 2D;
				}

				double angle = Math.atan2(mc.player.getPosZ() - waypoint.z, mc.player.getPosX() - waypoint.x) + (-mc.player.rotationYaw) * Math.PI / 180D;

				double wx = x + s / 2D + Math.cos(angle) * d;
				double wy = y + s / 2D + Math.sin(angle) * d;
				double ws = s / 32D;

				mc.getTextureManager().bindTexture(waypoint.type.texture);
				buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);

				int r = (waypoint.color >> 16) & 0xFF;
				int g = (waypoint.color >> 8) & 0xFF;
				int b = (waypoint.color >> 0) & 0xFF;

				buffer.pos(wx - ws, wy - ws, z).color(r, g, b, 255).tex(0F, 0F).endVertex();
				buffer.pos(wx - ws, wy + ws, z).color(r, g, b, 255).tex(0F, 1F).endVertex();
				buffer.pos(wx + ws, wy + ws, z).color(r, g, b, 255).tex(1F, 1F).endVertex();
				buffer.pos(wx + ws, wy - ws, z).color(r, g, b, 255).tex(1F, 0F).endVertex();
				tessellator.draw();
			}
		}
	}

	/*
	@SubscribeEvent
	public void chunkChange(EntityEvent.EnteringChunk event)
	{
		if (event.getEntity() instanceof ClientPlayerEntity && (event.getOldChunkX() != event.getNewChunkX() || event.getOldChunkZ() != event.getNewChunkZ()))
		{
			updateMinimap = true;
		}
	}
	 */
}