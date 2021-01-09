package com.feed_the_beast.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbchunks.ColorMapLoader;
import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import com.feed_the_beast.mods.ftbchunks.FTBChunksCommon;
import com.feed_the_beast.mods.ftbchunks.client.map.MapDimension;
import com.feed_the_beast.mods.ftbchunks.client.map.MapManager;
import com.feed_the_beast.mods.ftbchunks.client.map.MapRegion;
import com.feed_the_beast.mods.ftbchunks.client.map.MapRegionData;
import com.feed_the_beast.mods.ftbchunks.client.map.MapTask;
import com.feed_the_beast.mods.ftbchunks.client.map.PlayerHeadTexture;
import com.feed_the_beast.mods.ftbchunks.client.map.RegionSyncKey;
import com.feed_the_beast.mods.ftbchunks.client.map.ReloadChunkTask;
import com.feed_the_beast.mods.ftbchunks.client.map.UpdateChunkFromServerTask;
import com.feed_the_beast.mods.ftbchunks.client.map.Waypoint;
import com.feed_the_beast.mods.ftbchunks.client.map.WaypointType;
import com.feed_the_beast.mods.ftbchunks.impl.PlayerLocation;
import com.feed_the_beast.mods.ftbchunks.impl.XZ;
import com.feed_the_beast.mods.ftbchunks.net.LoginDataPacket;
import com.feed_the_beast.mods.ftbchunks.net.PartialPackets;
import com.feed_the_beast.mods.ftbchunks.net.PlayerDeathPacket;
import com.feed_the_beast.mods.ftbchunks.net.SendAllChunksPacket;
import com.feed_the_beast.mods.ftbchunks.net.SendChunkPacket;
import com.feed_the_beast.mods.ftbchunks.net.SendGeneralDataPacket;
import com.feed_the_beast.mods.ftbchunks.net.SendPlayerListPacket;
import com.feed_the_beast.mods.ftbchunks.net.SendVisiblePlayerListPacket;
import com.feed_the_beast.mods.ftbguilibrary.icon.ImageIcon;
import com.feed_the_beast.mods.ftbguilibrary.utils.ClientUtils;
import com.feed_the_beast.mods.ftbguilibrary.utils.MathUtils;
import com.feed_the_beast.mods.ftbguilibrary.widget.CustomClickEvent;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.gui.screen.IngameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.monster.IMob;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * @author LatvianModder
 */
public class FTBChunksClient extends FTBChunksCommon
{
	private static final ResourceLocation BUTTON_ID = new ResourceLocation("ftbchunks:open_gui");
	public static final ResourceLocation CIRCLE_MASK = new ResourceLocation("ftbchunks:textures/circle_mask.png");
	public static final ResourceLocation CIRCLE_BORDER = new ResourceLocation("ftbchunks:textures/circle_border.png");
	public static final ResourceLocation PLAYER = new ResourceLocation("ftbchunks:textures/player.png");
	public static final ResourceLocation[] COMPASS = {
			new ResourceLocation("ftbchunks:textures/compass_e.png"),
			new ResourceLocation("ftbchunks:textures/compass_n.png"),
			new ResourceLocation("ftbchunks:textures/compass_w.png"),
			new ResourceLocation("ftbchunks:textures/compass_s.png"),
	};

	private static final List<ITextComponent> MINIMAP_TEXT_LIST = new ArrayList<>(3);

	private static final ArrayDeque<MapTask> taskQueue = new ArrayDeque<>();
	public static long taskQueueTicks = 0L;
	public static final HashSet<ChunkPos> rerenderCache = new HashSet<>();

	public static void queue(MapTask task)
	{
		taskQueue.addLast(task);
	}

	public static KeyBinding openMapKey;

	public static int minimapTextureId = -1;
	private static int currentPlayerChunkX, currentPlayerChunkZ;

	public static boolean updateMinimap = false;
	public static boolean alwaysRenderChunksOnMap = false;
	public static SendGeneralDataPacket generalData;
	private static long nextRegionSave = 0L;

	public void init()
	{
		MinecraftForge.EVENT_BUS.register(this);
		FTBChunksClientConfig.init();
		openMapKey = new KeyBinding("key.ftbchunks.map", KeyConflictContext.IN_GAME, KeyModifier.NONE, InputMappings.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.ui");
		ClientRegistry.registerKeyBinding(openMapKey);
		((IReloadableResourceManager) Minecraft.getInstance().getResourceManager()).registerReloadListener(new EntityIcons());
		((IReloadableResourceManager) Minecraft.getInstance().getResourceManager()).registerReloadListener(new ColorMapLoader());
	}

	public static void openGui()
	{
		new LargeMapScreen().openGui();
	}

	public static void saveAllRegions()
	{
		if (MapManager.inst == null)
		{
			return;
		}

		for (MapDimension dimension : MapManager.inst.getDimensions().values())
		{
			for (MapRegion region : dimension.getLoadedRegions())
			{
				if (region.saveData)
				{
					queue(region);
					region.saveData = false;
				}
			}

			if (dimension.saveData)
			{
				queue(dimension);
				dimension.saveData = false;
			}
		}

		if (MapManager.inst.saveData)
		{
			queue(MapManager.inst);
			MapManager.inst.saveData = false;
		}
	}

	@Override
	public void login(LoginDataPacket loginData)
	{
		Path dir = FMLPaths.GAMEDIR.get().resolve("local/ftbchunks/data/" + loginData.serverId);

		if (Files.notExists(dir))
		{
			try
			{
				Files.createDirectories(dir);
			}
			catch (Exception ex)
			{
				throw new RuntimeException(ex);
			}
		}

		MapManager.inst = new MapManager(loginData.serverId, dir);
		updateMinimap = true;
	}

	@SubscribeEvent
	public void loggedOut(ClientPlayerNetworkEvent.LoggedOutEvent event)
	{
		if (MapManager.inst != null)
		{
			saveAllRegions();

			MapTask t;

			while ((t = taskQueue.pollFirst()) != null)
			{
				t.runMapTask();
			}

			MapDimension.updateCurrent();
			MapManager.inst.release();
			MapManager.inst = null;
		}
	}

	@Override
	public void updateGeneralData(SendGeneralDataPacket packet)
	{
		generalData = packet;
	}

	@Override
	public void updateChunk(SendChunkPacket packet)
	{
		MapDimension dimension = MapManager.inst.getDimension(packet.dimension);
		Date now = new Date();
		queue(new UpdateChunkFromServerTask(dimension, packet.chunk, now));
	}

	@Override
	public void updateAllChunks(SendAllChunksPacket packet)
	{
		MapDimension dimension = MapManager.inst.getDimension(packet.dimension);
		Date now = new Date();

		for (SendChunkPacket.SingleChunk c : packet.chunks)
		{
			queue(new UpdateChunkFromServerTask(dimension, c, now));
		}
	}

	@Override
	public void openPlayerList(SendPlayerListPacket packet)
	{
		new AllyScreen(packet.players, packet.allyMode).openGui();
	}

	@Override
	public void updateVisiblePlayerList(SendVisiblePlayerListPacket packet)
	{
		PlayerLocation.CLIENT_LIST.clear();
		PlayerLocation.currentDimension = packet.dim;
		PlayerLocation.CLIENT_LIST.addAll(packet.players);
	}

	@Override
	public void syncRegion(RegionSyncKey key, int offset, int total, byte[] data)
	{
		PartialPackets.REGION.read(key, offset, total, data);
	}

	@Override
	public void playerDeath(PlayerDeathPacket packet)
	{
		if (FTBChunksClientConfig.deathWaypoints)
		{
			MapDimension dimension = MapManager.inst.getDimension(packet.dimension);
			Waypoint w = new Waypoint(dimension);
			w.name = "Death #" + packet.number;
			w.x = packet.x;
			w.z = packet.z;
			w.type = WaypointType.DEATH;
			dimension.getWaypoints().add(w);
			dimension.saveData = true;
		}
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
	public void keyEvent(InputEvent.KeyInputEvent event)
	{
		if (openMapKey.isDown())
		{
			if (FTBChunksClientConfig.debugInfo && Screen.hasControlDown())
			{
				FTBChunks.LOGGER.info("=== Task Queue: " + taskQueue.size());

				for (MapTask task : taskQueue)
				{
					FTBChunks.LOGGER.info(task.toString());
				}

				FTBChunks.LOGGER.info("===");
			}
			else
			{
				openGui();
			}
		}
	}

	@SubscribeEvent
	public void guiKeyEvent(GuiScreenEvent.KeyboardKeyEvent.KeyboardKeyPressedEvent.Pre event)
	{
		if (openMapKey.isDown())
		{
			LargeMapScreen gui = ClientUtils.getCurrentGuiAs(LargeMapScreen.class);

			if (gui != null)
			{
				gui.closeGui(false);
			}
		}
	}

	@SubscribeEvent
	public void renderGameOverlay(RenderGameOverlayEvent.Post event)
	{
		Minecraft mc = Minecraft.getInstance();

		if (mc.player == null || mc.level == null || MapManager.inst == null || event.getType() != RenderGameOverlayEvent.ElementType.ALL)
		{
			return;
		}

		MapDimension dim = MapDimension.getCurrent();

		if (dim.dimension != mc.level.dimension())
		{
			MapDimension.updateCurrent();
			dim = MapDimension.getCurrent();
		}

		long now = System.currentTimeMillis();

		if (nextRegionSave == 0L || now >= nextRegionSave)
		{
			nextRegionSave = now + 60000L;
			saveAllRegions();
		}

		if (minimapTextureId == -1)
		{
			minimapTextureId = TextureUtil.generateTextureId();
			TextureUtil.prepareImage(minimapTextureId, FTBChunks.MINIMAP_SIZE, FTBChunks.MINIMAP_SIZE);
			updateMinimap = true;
		}

		RenderSystem.enableTexture();
		RenderSystem.bindTexture(minimapTextureId);

		if (FTBChunksClientConfig.minimapBlur)
		{
			RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
			RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		}
		else
		{
			RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
			RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		}

		int cx = mc.player.xChunk;
		int cz = mc.player.zChunk;

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

					MapRegion region = dim.getRegion(XZ.regionFromChunk(ox, oz));
					region.getRenderedMapImage().upload(0, mx * 16, mz * 16, (ox & 31) * 16, (oz & 31) * 16, 16, 16, FTBChunksClientConfig.minimapBlur, false, false, false);
				}
			}

			currentPlayerChunkX = cx;
			currentPlayerChunkZ = cz;
		}

		if (mc.options.renderDebug || FTBChunksClientConfig.minimap == MinimapPosition.DISABLED || FTBChunksClientConfig.minimapVisibility == 0)
		{
			return;
		}

		float scale = (float) (FTBChunksClientConfig.minimapScale * 4D / mc.getWindow().getGuiScale());
		float minimapRotation = (FTBChunksClientConfig.minimapLockedNorth ? 180F : -mc.player.yRot) % 360F;

		int s = (int) (64D * scale);
		int x = FTBChunksClientConfig.minimap.getX(mc.getWindow().getGuiScaledWidth(), s);
		int y = FTBChunksClientConfig.minimap.getY(mc.getWindow().getGuiScaledHeight(), s);
		int z = 0;

		float border = 0F;
		int alpha = FTBChunksClientConfig.minimapVisibility;

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuilder();

		float f0 = 1F / (float) FTBChunks.TILES;
		float f1 = 1F - f0;

		float offX = (float) ((mc.player.getX() / 16D - currentPlayerChunkX - 0.5D) / (double) FTBChunks.TILES);
		float offY = (float) ((mc.player.getZ() / 16D - currentPlayerChunkZ - 0.5D) / (double) FTBChunks.TILES);

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableCull();
		RenderSystem.enableTexture();
		RenderSystem.enableDepthTest();
		RenderSystem.enableAlphaTest();

		MatrixStack matrixStack = event.getMatrixStack();
		matrixStack.pushPose();
		matrixStack.translate(x + s / 2D, y + s / 2D, -10);

		matrixStack.translate(0, 0, 950);

		Matrix4f m = matrixStack.last().pose();

		// See AdvancementTabGui
		RenderSystem.colorMask(false, false, false, false);
		mc.getTextureManager().bind(CIRCLE_MASK);
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
		buffer.vertex(m, -s / 2F + border, -s / 2F + border, z).color(255, 255, 255, 255).uv(0F, 0F).endVertex();
		buffer.vertex(m, -s / 2F + border, s / 2F - border, z).color(255, 255, 255, 255).uv(0F, 1F).endVertex();
		buffer.vertex(m, s / 2F - border, s / 2F - border, z).color(255, 255, 255, 255).uv(1F, 1F).endVertex();
		buffer.vertex(m, s / 2F - border, -s / 2F + border, z).color(255, 255, 255, 255).uv(1F, 0F).endVertex();
		tessellator.end();
		RenderSystem.colorMask(true, true, true, true);

		matrixStack.translate(0, 0, -950);
		matrixStack.mulPose(Vector3f.ZP.rotationDegrees(minimapRotation + 180F));

		RenderSystem.depthFunc(GL11.GL_GEQUAL);
		RenderSystem.bindTexture(minimapTextureId);

		m = matrixStack.last().pose();

		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
		buffer.vertex(m, -s / 2F + border, -s / 2F + border, z).color(255, 255, 255, alpha).uv(f0 + offX, f0 + offY).endVertex();
		buffer.vertex(m, -s / 2F + border, s / 2F - border, z).color(255, 255, 255, alpha).uv(f0 + offX, f1 + offY).endVertex();
		buffer.vertex(m, s / 2F - border, s / 2F - border, z).color(255, 255, 255, alpha).uv(f1 + offX, f1 + offY).endVertex();
		buffer.vertex(m, s / 2F - border, -s / 2F + border, z).color(255, 255, 255, alpha).uv(f1 + offX, f0 + offY).endVertex();
		tessellator.end();

		matrixStack.popPose();

		RenderSystem.disableDepthTest();
		RenderSystem.depthFunc(GL11.GL_LEQUAL);
		RenderSystem.defaultBlendFunc();

		m = matrixStack.last().pose();

		mc.getTextureManager().bind(CIRCLE_BORDER);
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
		buffer.vertex(m, x, y, z).color(255, 255, 255, alpha).uv(0F, 0F).endVertex();
		buffer.vertex(m, x, y + s, z).color(255, 255, 255, alpha).uv(0F, 1F).endVertex();
		buffer.vertex(m, x + s, y + s, z).color(255, 255, 255, alpha).uv(1F, 1F).endVertex();
		buffer.vertex(m, x + s, y, z).color(255, 255, 255, alpha).uv(1F, 0F).endVertex();
		tessellator.end();

		RenderSystem.disableTexture();
		buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
		buffer.vertex(m, x + s / 2F, y + 0, z).color(0, 0, 0, 30).endVertex();
		buffer.vertex(m, x + s / 2F, y + s, z).color(0, 0, 0, 30).endVertex();
		buffer.vertex(m, x + 0, y + s / 2F, z).color(0, 0, 0, 30).endVertex();
		buffer.vertex(m, x + s, y + s / 2F, z).color(0, 0, 0, 30).endVertex();
		tessellator.end();

		RenderSystem.enableTexture();

		if (FTBChunksClientConfig.minimapCompass)
		{
			for (int face = 0; face < 4; face++)
			{
				double d = s / 2.2D;

				double angle = (minimapRotation + 180D - face * 90D) * Math.PI / 180D;

				float wx = (float) (x + s / 2D + Math.cos(angle) * d);
				float wy = (float) (y + s / 2D + Math.sin(angle) * d);
				float ws = s / 32F;

				m = matrixStack.last().pose();

				mc.textureManager.bind(COMPASS[face]);
				buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
				buffer.vertex(m, wx - ws, wy - ws, z).color(255, 255, 255, 255).uv(0F, 0F).endVertex();
				buffer.vertex(m, wx - ws, wy + ws, z).color(255, 255, 255, 255).uv(0F, 1F).endVertex();
				buffer.vertex(m, wx + ws, wy + ws, z).color(255, 255, 255, 255).uv(1F, 1F).endVertex();
				buffer.vertex(m, wx + ws, wy - ws, z).color(255, 255, 255, 255).uv(1F, 0F).endVertex();
				tessellator.end();
			}
		}

		double magicNumber = 3.2D;

		if (FTBChunksClientConfig.minimapWaypoints && !dim.getWaypoints().isEmpty())
		{
			for (Waypoint waypoint : dim.getWaypoints())
			{
				if (waypoint.hidden)
				{
					continue;
				}

				double distance = MathUtils.dist(mc.player.getX(), mc.player.getZ(), waypoint.x + 0.5D, waypoint.z + 0.5D);

				if (distance > waypoint.minimapDistance)
				{
					continue;
				}

				double d = distance / magicNumber * scale;

				if (d > s / 2D)
				{
					d = s / 2D;
				}

				double angle = Math.atan2(mc.player.getZ() - waypoint.z - 0.5D, mc.player.getX() - waypoint.x - 0.5D) + minimapRotation * Math.PI / 180D;

				float wx = (float) (x + s / 2D + Math.cos(angle) * d);
				float wy = (float) (y + s / 2D + Math.sin(angle) * d);
				float ws = s / 32F;

				int r = (waypoint.color >> 16) & 0xFF;
				int g = (waypoint.color >> 8) & 0xFF;
				int b = (waypoint.color >> 0) & 0xFF;

				m = matrixStack.last().pose();

				mc.getTextureManager().bind(waypoint.type.texture);
				buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
				buffer.vertex(m, wx - ws, wy - ws, z).color(r, g, b, 255).uv(0F, 0F).endVertex();
				buffer.vertex(m, wx - ws, wy + ws, z).color(r, g, b, 255).uv(0F, 1F).endVertex();
				buffer.vertex(m, wx + ws, wy + ws, z).color(r, g, b, 255).uv(1F, 1F).endVertex();
				buffer.vertex(m, wx + ws, wy - ws, z).color(r, g, b, 255).uv(1F, 0F).endVertex();
				tessellator.end();
			}
		}

		if (FTBChunksClientConfig.minimapEntities)
		{
			for (Entity entity : mc.level.entitiesForRendering())
			{
				if (entity instanceof AbstractClientPlayerEntity || entity.getType().getCategory() == EntityClassification.MISC || entity.getY() < entity.level.getHeight(Heightmap.Type.WORLD_SURFACE, MathHelper.floor(entity.getX()), MathHelper.floor(entity.getZ())) - 10)
				{
					continue;
				}

				double d = MathUtils.dist(mc.player.getX(), mc.player.getZ(), entity.getX(), entity.getZ()) / magicNumber * scale;

				if (d > s / 2D)
				{
					continue;
				}

				ResourceLocation texture = EntityIcons.ENTITY_ICONS.get(entity.getType());

				if (texture == EntityIcons.INVISIBLE)
				{
					continue;
				}
				else if (texture == null || !FTBChunksClientConfig.minimapEntityHeads)
				{
					if (entity instanceof IMob)
					{
						texture = EntityIcons.HOSTILE;
					}
					else
					{
						texture = EntityIcons.NORMAL;
					}
				}

				double angle = Math.atan2(mc.player.getZ() - entity.getZ(), mc.player.getX() - entity.getX()) + minimapRotation * Math.PI / 180D;

				float wx = (float) (x + s / 2D + Math.cos(angle) * d);
				float wy = (float) (y + s / 2D + Math.sin(angle) * d);
				float ws = s / (FTBChunksClientConfig.minimapLargeEntities ? 32F : 48F);

				m = matrixStack.last().pose();

				mc.getTextureManager().bind(texture);
				RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
				RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

				buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
				buffer.vertex(m, wx - ws, wy - ws, z).color(255, 255, 255, 255).uv(0F, 0F).endVertex();
				buffer.vertex(m, wx - ws, wy + ws, z).color(255, 255, 255, 255).uv(0F, 1F).endVertex();
				buffer.vertex(m, wx + ws, wy + ws, z).color(255, 255, 255, 255).uv(1F, 1F).endVertex();
				buffer.vertex(m, wx + ws, wy - ws, z).color(255, 255, 255, 255).uv(1F, 0F).endVertex();
				tessellator.end();
			}
		}

		if (FTBChunksClientConfig.minimapPlayerHeads && mc.level.players().size() > 1)
		{
			for (AbstractClientPlayerEntity player : mc.level.players())
			{
				if (player == mc.player)
				{
					continue;
				}

				double d = MathUtils.dist(mc.player.getX(), mc.player.getZ(), player.getX(), player.getZ()) / magicNumber * scale;

				if (d > s / 2D)
				{
					d = s / 2D;
				}

				double angle = Math.atan2(mc.player.getZ() - player.getZ(), mc.player.getX() - player.getX()) + minimapRotation * Math.PI / 180D;

				float wx = (float) (x + s / 2D + Math.cos(angle) * d);
				float wy = (float) (y + s / 2D + Math.sin(angle) * d);
				float ws = s / 32F;

				String uuid = UUIDTypeAdapter.fromUUID(player.getUUID());
				ResourceLocation texture = new ResourceLocation("head", uuid);

				TextureManager texturemanager = mc.getTextureManager();
				Texture t = texturemanager.getTexture(texture);

				if (t == null)
				{
					t = new PlayerHeadTexture("https://minotar.net/avatar/" + uuid + "/16", ImageIcon.MISSING_IMAGE);
					texturemanager.register(texture, t);
				}

				m = matrixStack.last().pose();

				RenderSystem.bindTexture(t.getId());
				buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
				buffer.vertex(m, wx - ws, wy - ws, z).color(255, 255, 255, 255).uv(0F, 0F).endVertex();
				buffer.vertex(m, wx - ws, wy + ws, z).color(255, 255, 255, 255).uv(0F, 1F).endVertex();
				buffer.vertex(m, wx + ws, wy + ws, z).color(255, 255, 255, 255).uv(1F, 1F).endVertex();
				buffer.vertex(m, wx + ws, wy - ws, z).color(255, 255, 255, 255).uv(1F, 0F).endVertex();
				tessellator.end();
			}
		}

		if (FTBChunksClientConfig.minimapLockedNorth)
		{
			mc.getTextureManager().bind(PLAYER);
			matrixStack.pushPose();
			matrixStack.translate(x + s / 2D, y + s / 2D, z);
			matrixStack.mulPose(Vector3f.ZP.rotationDegrees(mc.player.yRot + 180F));
			matrixStack.scale(s / 16F, s / 16F, 1F);
			m = matrixStack.last().pose();

			buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
			buffer.vertex(m, -1, -1, 0).color(255, 255, 255, 200).uv(0F, 0F).endVertex();
			buffer.vertex(m, -1, 1, 0).color(255, 255, 255, 200).uv(0F, 1F).endVertex();
			buffer.vertex(m, 1, 1, 0).color(255, 255, 255, 200).uv(1F, 1F).endVertex();
			buffer.vertex(m, 1, -1, 0).color(255, 255, 255, 200).uv(1F, 0F).endVertex();
			tessellator.end();

			matrixStack.popPose();
		}

		MINIMAP_TEXT_LIST.clear();

		if (FTBChunksClientConfig.minimapZone)
		{
			MapRegionData data = dim.getRegion(XZ.regionFromChunk(currentPlayerChunkX, currentPlayerChunkZ)).getData();

			if (data != null)
			{
				ITextComponent currentChunkOwner = data.getChunk(XZ.of(currentPlayerChunkX, currentPlayerChunkZ)).owner;

				if (currentChunkOwner != StringTextComponent.EMPTY)
				{
					MINIMAP_TEXT_LIST.add(currentChunkOwner);
				}
			}
		}

		if (FTBChunksClientConfig.minimapXYZ)
		{
			MINIMAP_TEXT_LIST.add(new StringTextComponent(MathHelper.floor(mc.player.getX()) + " " + MathHelper.floor(mc.player.getY()) + " " + MathHelper.floor(mc.player.getZ())));
		}

		if (FTBChunksClientConfig.minimapBiome)
		{
			RegistryKey<Biome> biome = mc.level.getBiomeName(mc.player.blockPosition()).orElse(null);

			if (biome != null)
			{
				MINIMAP_TEXT_LIST.add(new TranslationTextComponent("biome." + biome.location().getNamespace() + "." + biome.location().getPath()));
			}
		}

		if (FTBChunksClientConfig.debugInfo)
		{
			XZ r = XZ.regionFromChunk(currentPlayerChunkX, currentPlayerChunkZ);
			MINIMAP_TEXT_LIST.add(new StringTextComponent("Queued tasks: " + taskQueue.size()));
			MINIMAP_TEXT_LIST.add(new StringTextComponent(r.toRegionString()));
		}

		if (!MINIMAP_TEXT_LIST.isEmpty())
		{
			matrixStack.pushPose();
			matrixStack.translate(x + s / 2D, y + s + 3D, 0D);
			matrixStack.scale((float) (0.5D * scale), (float) (0.5D * scale), 1F);

			for (int i = 0; i < MINIMAP_TEXT_LIST.size(); i++)
			{
				IReorderingProcessor bs = MINIMAP_TEXT_LIST.get(i).getVisualOrderText();
				int bsw = mc.font.width(bs);
				mc.font.drawShadow(matrixStack, bs, -bsw / 2F, i * 11, 0xFFFFFFFF);
			}

			matrixStack.popPose();
		}

		RenderSystem.enableDepthTest();
	}

	@SubscribeEvent
	public void renderWorldLast(RenderWorldLastEvent event)
	{
		Minecraft mc = Minecraft.getInstance();

		if (mc.options.hideGui || !FTBChunksClientConfig.inWorldWaypoints || MapManager.inst == null || mc.level == null || mc.player == null)
		{
			return;
		}

		MapDimension dim = MapDimension.getCurrent();

		if (dim.getWaypoints().isEmpty())
		{
			return;
		}

		List<Waypoint> visibleWaypoints = new ArrayList<>();

		for (Waypoint waypoint : dim.getWaypoints())
		{
			if (waypoint.hidden)
			{
				continue;
			}

			waypoint.distance = MathUtils.dist(mc.player.getX(), mc.player.getZ(), waypoint.x + 0.5D, waypoint.z + 0.5D);

			if (waypoint.distance <= 8D || waypoint.distance > waypoint.inWorldDistance)
			{
				continue;
			}

			waypoint.alpha = 150;

			if (waypoint.distance < 12D)
			{
				waypoint.alpha = (int) (waypoint.alpha * ((waypoint.distance - 8D) / 4D));
			}

			if (waypoint.alpha <= 0)
			{
				continue;
			}

			visibleWaypoints.add(waypoint);
		}

		if (visibleWaypoints.isEmpty())
		{
			return;
		}

		if (visibleWaypoints.size() >= 2)
		{
			visibleWaypoints.sort(Comparator.comparingDouble(value -> -value.distance));
		}

		ActiveRenderInfo activeRenderInfo = mc.getEntityRenderDispatcher().camera;
		Vector3d projectedView = activeRenderInfo.getPosition();
		MatrixStack ms = event.getMatrixStack();
		ms.pushPose();
		ms.translate(-projectedView.x, -projectedView.y, -projectedView.z);

		IVertexBuilder depthBuffer = mc.renderBuffers().bufferSource().getBuffer(FTBChunksRenderTypes.WAYPOINTS_DEPTH);

		float h = (float) (projectedView.y + 30D);
		float h2 = h + 70F;

		for (Waypoint waypoint : visibleWaypoints)
		{
			double angle = Math.atan2(projectedView.z - waypoint.z - 0.5D, projectedView.x - waypoint.x - 0.5D) * 180D / Math.PI;

			int r = (waypoint.color >> 16) & 0xFF;
			int g = (waypoint.color >> 8) & 0xFF;
			int b = (waypoint.color >> 0) & 0xFF;

			ms.pushPose();
			ms.translate(waypoint.x + 0.5D, 0, waypoint.z + 0.5D);
			ms.mulPose(Vector3f.YP.rotationDegrees((float) (-angle - 135D)));

			float s = 0.6F;

			Matrix4f m = ms.last().pose();

			depthBuffer.vertex(m, -s, 0, s).color(r, g, b, waypoint.alpha).uv(0F, 1F).endVertex();
			depthBuffer.vertex(m, -s, h, s).color(r, g, b, waypoint.alpha).uv(0F, 0F).endVertex();
			depthBuffer.vertex(m, s, h, -s).color(r, g, b, waypoint.alpha).uv(1F, 0F).endVertex();
			depthBuffer.vertex(m, s, 0, -s).color(r, g, b, waypoint.alpha).uv(1F, 1F).endVertex();

			depthBuffer.vertex(m, -s, h, s).color(r, g, b, waypoint.alpha).uv(0F, 1F).endVertex();
			depthBuffer.vertex(m, -s, h2, s).color(r, g, b, 0).uv(0F, 0F).endVertex();
			depthBuffer.vertex(m, s, h2, -s).color(r, g, b, 0).uv(1F, 0F).endVertex();
			depthBuffer.vertex(m, s, h, -s).color(r, g, b, waypoint.alpha).uv(1F, 1F).endVertex();

			ms.popPose();
		}

		ms.popPose();

		mc.renderBuffers().bufferSource().endBatch(FTBChunksRenderTypes.WAYPOINTS_DEPTH);
	}

	@SubscribeEvent
	public void screenOpened(GuiScreenEvent.InitGuiEvent.Pre event)
	{
		if (event.getGui() instanceof IngameMenuScreen)
		{
			nextRegionSave = System.currentTimeMillis() + 60000L;
			saveAllRegions();
		}
	}

	@SubscribeEvent
	public void clientTick(TickEvent.ClientTickEvent event)
	{
		if (event.phase == TickEvent.Phase.START && MapManager.inst != null && Minecraft.getInstance().level != null)
		{
			if (taskQueueTicks % FTBChunksClientConfig.rerenderQueueTicks == 0L)
			{
				if (!rerenderCache.isEmpty())
				{
					World world = Minecraft.getInstance().level;

					for (ChunkPos pos : rerenderCache)
					{
						queue(new ReloadChunkTask(world, pos));
					}

					rerenderCache.clear();
				}
			}

			if (taskQueueTicks % FTBChunksClientConfig.taskQueueTicks == 0L)
			{
				int s = Math.min(taskQueue.size(), FTBChunksClientConfig.taskQueueMax);

				if (s > 0)
				{
					MapTask[] tasks = new MapTask[s];

					for (int i = 0; i < s; i++)
					{
						tasks[i] = taskQueue.pollFirst();

						if (tasks[i] == null || tasks[i].cancelOtherTasks())
						{
							break;
						}
					}

					Util.backgroundExecutor().execute(() -> {
						for (MapTask task : tasks)
						{
							if (task != null)
							{
								task.runMapTask();
							}
						}
					});
				}
			}

			taskQueueTicks++;
		}
	}
}
