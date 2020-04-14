package com.feed_the_beast.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import com.feed_the_beast.mods.ftbchunks.FTBChunksCommon;
import com.feed_the_beast.mods.ftbchunks.client.map.ClientMapChunk;
import com.feed_the_beast.mods.ftbchunks.client.map.ClientMapDimension;
import com.feed_the_beast.mods.ftbchunks.impl.map.ColorBlend;
import com.feed_the_beast.mods.ftbchunks.impl.map.XZ;
import com.feed_the_beast.mods.ftbchunks.net.SendGeneralData;
import com.feed_the_beast.mods.ftbchunks.net.SendPlayerListPacket;
import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import com.feed_the_beast.mods.ftbguilibrary.widget.CustomClickEvent;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Random;

/**
 * @author LatvianModder
 */
public class FTBChunksClient extends FTBChunksCommon
{
	private static final ResourceLocation BUTTON_ID = new ResourceLocation("ftbchunks:open_gui");
	public static final ResourceLocation MAP_ICONS = new ResourceLocation("textures/map/map_icons.png");
	public static final ResourceLocation CIRCLE_MASK = new ResourceLocation("ftbchunks:textures/circle_mask.png");
	public static final ResourceLocation CIRCLE_BORDER = new ResourceLocation("ftbchunks:textures/circle_border.png");

	public static int minimapTextureId = -1;
	public static NativeImage minimapImage = null;
	private static int currentPlayerChunkX, currentPlayerChunkZ;
	public static boolean updateMinimap = true;
	public static SendGeneralData generalData;

	public void init()
	{
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
		MinecraftForge.EVENT_BUS.register(this);
	}

	public static void openGui()
	{
		new ChunkScreen().openGui();
	}

	@Override
	public void updateGeneralData(SendGeneralData data)
	{
		generalData = data;
	}

	@Override
	public void updateChunk(int chunkX, int chunkZ, byte[] imageData)
	{
		try
		{
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
			//System.out.println(image);
			ClientMapChunk chunk = ClientMapDimension.get().getRegion(XZ.regionFromChunk(chunkX, chunkZ)).getChunk(XZ.of(chunkX, chunkZ));

			for (int y = 0; y < 16; y++)
			{
				for (int x = 0; x < 16; x++)
				{
					int c = image.getRGB(x, y);
					chunk.setRGB(x, y, c & 0x00FFFFFF);
					chunk.setHeight(x, y, c >> 24);
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	private void setup(FMLClientSetupEvent event)
	{
		//((IReloadableResourceManager) event.getMinecraftSupplier().get().getResourceManager()).addReloadListener((stage, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor) -> CompletableFuture.runAsync(() -> minimapTexture = null).thenCompose(stage::markCompleteAwaitingOthers));
	}

	@Override
	public void openPlayerList(List<SendPlayerListPacket.NetPlayer> players, int allyMode)
	{
		new PlayerListScreen(players, allyMode).openGui();
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

		if (minimapImage == null)
		{
			minimapTextureId = TextureUtil.generateTextureId();
			minimapImage = new NativeImage(FTBChunks.MINIMAP_SIZE, FTBChunks.MINIMAP_SIZE, true);
			TextureUtil.prepareImage(minimapTextureId, minimapImage.getWidth(), minimapImage.getHeight());
		}

		RenderSystem.enableTexture();
		RenderSystem.bindTexture(minimapTextureId);
		boolean blur = true;

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

		if (updateMinimap)
		{
			updateMinimap = false;
			ClientMapDimension dim = ClientMapDimension.get();
			int cx = mc.player.chunkCoordX;
			int cz = mc.player.chunkCoordZ;

			for (int mz = 0; mz < FTBChunks.TILES; mz++)
			{
				for (int mx = 0; mx < FTBChunks.TILES; mx++)
				{
					int ox = cx + mx - FTBChunks.TILE_OFFSET;
					int oz = cz + mz - FTBChunks.TILE_OFFSET;

					ClientMapChunk chunk = dim.getRegion(XZ.regionFromChunk(ox, oz)).getChunk(XZ.of(ox, oz));
					Random random = new Random(ox * 31L + oz);

					for (int z = 0; z < 16; z++)
					{
						for (int x = 0; x < 16; x++)
						{
							int by = dim.getHeight(chunk, ox * 16 + x, oz * 16 + z);

							if (by == -1)
							{
								continue;
							}

							int bn = dim.getHeight(chunk, ox * 16 + x, oz * 16 + z - 1);
							int bw = dim.getHeight(chunk, ox * 16 + x - 1, oz * 16 + z);
							float addedBrightness = random.nextFloat() * 0.05F;

							if (bn != -1 || bw != -1)
							{
								if (by > bn || by > bw)
								{
									addedBrightness += 0.15F;
								}

								if (by < bn || by < bw)
								{
									addedBrightness -= 0.15F;
								}
							}

							int c = ColorBlend.addBrightness(Color4I.rgb(chunk.getRGB(x, z)), addedBrightness);
							minimapImage.setPixelRGBA(mx * 16 + x, mz * 16 + z, NativeImage.getCombined(255, c >> 0, c >> 8, c >> 16));
						}
					}
				}
			}

			/*
			for (int y = 0; y < FTBChunks.MINIMAP_SIZE; y++)
			{
				for (int x = 0; x < FTBChunks.MINIMAP_SIZE; x++)
				{
					minimapImage.setPixelRGBA(x, y, NativeImage.getCombined(255, c >> 0, c >> 8, c >> 16));
				}
			}
			 */

			minimapImage.uploadTextureSub(0, 0, 0, false);
			currentPlayerChunkX = cx;
			currentPlayerChunkZ = cz;
		}

		if (mc.gameSettings.showDebugInfo)
		{
			return;
		}

		double scale = 1D;

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
	}

	@SubscribeEvent
	public void chunkChange(EntityEvent.EnteringChunk event)
	{
		if (event.getEntity() instanceof ClientPlayerEntity)
		{
			updateMinimap = true;
		}
	}
}