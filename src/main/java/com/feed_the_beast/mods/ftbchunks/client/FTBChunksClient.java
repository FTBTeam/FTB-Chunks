package com.feed_the_beast.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbchunks.ColorMapLoader;
import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import com.feed_the_beast.mods.ftbchunks.FTBChunksCommon;
import com.feed_the_beast.mods.ftbchunks.api.ChunkDimPos;
import com.feed_the_beast.mods.ftbchunks.api.Waypoint;
import com.feed_the_beast.mods.ftbchunks.client.map.ImportRegionTask;
import com.feed_the_beast.mods.ftbchunks.client.map.MapChunk;
import com.feed_the_beast.mods.ftbchunks.client.map.MapDimension;
import com.feed_the_beast.mods.ftbchunks.client.map.MapManager;
import com.feed_the_beast.mods.ftbchunks.client.map.MapRegion;
import com.feed_the_beast.mods.ftbchunks.client.map.MapTask;
import com.feed_the_beast.mods.ftbchunks.client.map.PlayerHeadTexture;
import com.feed_the_beast.mods.ftbchunks.client.map.RegionSyncKey;
import com.feed_the_beast.mods.ftbchunks.client.map.ReloadChunkTask;
import com.feed_the_beast.mods.ftbchunks.impl.PlayerLocation;
import com.feed_the_beast.mods.ftbchunks.impl.XZ;
import com.feed_the_beast.mods.ftbchunks.net.LoginDataPacket;
import com.feed_the_beast.mods.ftbchunks.net.PartialPackets;
import com.feed_the_beast.mods.ftbchunks.net.SendAllChunksPacket;
import com.feed_the_beast.mods.ftbchunks.net.SendChunkPacket;
import com.feed_the_beast.mods.ftbchunks.net.SendGeneralDataPacket;
import com.feed_the_beast.mods.ftbchunks.net.SendPlayerListPacket;
import com.feed_the_beast.mods.ftbchunks.net.SendVisiblePlayerListPacket;
import com.feed_the_beast.mods.ftbchunks.net.SendWaypointsPacket;
import com.feed_the_beast.mods.ftbguilibrary.icon.ImageIcon;
import com.feed_the_beast.mods.ftbguilibrary.utils.ClientUtils;
import com.feed_the_beast.mods.ftbguilibrary.utils.MathUtils;
import com.feed_the_beast.mods.ftbguilibrary.widget.CustomClickEvent;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.gui.screen.IngameMenuScreen;
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
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
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

	private static final List<ITextProperties> MINIMAP_TEXT_LIST = new ArrayList<>(3);

	private static final ArrayDeque<MapTask> taskQueue = new ArrayDeque<>();
	private static long taskQueueTicks = 0L;
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
		((IReloadableResourceManager) Minecraft.getInstance().getResourceManager()).addReloadListener(new EntityIcons());
		((IReloadableResourceManager) Minecraft.getInstance().getResourceManager()).addReloadListener(new ColorMapLoader());
	}

	public static void openGui()
	{
		new LargeMapScreen().openGui();
	}

	public static void saveAllRegions()
	{
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
		MapManager.inst = new MapManager(loginData.serverId, FMLPaths.GAMEDIR.get().resolve("local/ftbchunks/data/" + loginData.serverId));
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
		MapChunk chunk = MapManager.inst.getDimension(packet.dimension).getRegion(XZ.regionFromChunk(packet.chunk.x, packet.chunk.z)).getChunk(XZ.of(packet.chunk.x, packet.chunk.z));
		Date now = new Date();
		chunk.updateFrom(now, packet.chunk);
	}

	@Override
	public void updateAllChunks(SendAllChunksPacket packet)
	{
		MapDimension dimension = MapManager.inst.getDimension(packet.dimension);
		Date now = new Date();

		for (SendChunkPacket.SingleChunk c : packet.chunks)
		{
			MapChunk chunk = dimension.getRegion(XZ.regionFromChunk(c.x, c.z)).getChunk(XZ.of(c.x, c.z));
			chunk.updateFrom(now, c);
		}
	}

	@Override
	public void updateWaypoints(SendWaypointsPacket packet)
	{
		for (MapDimension dimension : MapManager.inst.getDimensions().values())
		{
			dimension.waypoints.clear();
		}

		for (Waypoint waypoint : packet.waypoints)
		{
			MapManager.inst.getDimension(waypoint.dimension).waypoints.add(waypoint);
		}

		LargeMapScreen screen = ClientUtils.getCurrentGuiAs(LargeMapScreen.class);

		if (screen != null)
		{
			screen.refreshWidgets();
		}
	}

	@Override
	public void openPlayerList(SendPlayerListPacket packet)
	{
		new PlayerListScreen(packet.players, packet.allyMode).openGui();
	}

	@Override
	public void updateVisiblePlayerList(SendVisiblePlayerListPacket packet)
	{
		PlayerLocation.CLIENT_LIST.clear();
		PlayerLocation.currentDimension = packet.dim;
		PlayerLocation.CLIENT_LIST.addAll(packet.players);
	}

	@Override
	public void importWorldMap(ServerWorld world)
	{
		Minecraft.getInstance().runAsync(() -> importWorldMap(world, MapManager.inst.getDimension(ChunkDimPos.getID(world))));
	}

	private void importWorldMap(ServerWorld world, MapDimension dimension)
	{
		Minecraft.getInstance().player.sendMessage(new StringTextComponent("WIP!"), Util.DUMMY_UUID);

		File dir = world.getServer().func_240776_a_(FolderName.field_237253_i_).toFile();
		File regFileDirectory = new File(DimensionType.func_236031_a_(world.func_234923_W_(), dir), "region");

		for (File file : regFileDirectory.listFiles())
		{
			String n = file.getName();

			if (n.startsWith("r.") && n.endsWith(".mca"))
			{
				String[] s = n.substring(2, n.length() - 4).split("\\.");
				int rx = Integer.parseInt(s[0]);
				int rz = Integer.parseInt(s[1]);
				queue(new ImportRegionTask(dimension, file, dir, rx, rz));
			}
		}

		queue(() -> Minecraft.getInstance().player.sendMessage(new StringTextComponent("Done importing " + dimension.dimension), Util.DUMMY_UUID));
	}

	@Override
	public void syncRegion(RegionSyncKey key, int offset, int total, byte[] data)
	{
		PartialPackets.REGION.read(key, offset, total, data);
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
		if (openMapKey.isPressed())
		{
			openGui();
		}
	}

	@SubscribeEvent
	public void guiKeyEvent(GuiScreenEvent.KeyboardKeyEvent.KeyboardKeyPressedEvent.Pre event)
	{
		if (openMapKey.isPressed())
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

		if (mc.player == null || event.getType() != RenderGameOverlayEvent.ElementType.ALL)
		{
			return;
		}

		if (MapManager.inst == null)
		{
			return;
		}

		MapDimension dim = MapDimension.getCurrent();

		if (!dim.dimension.equals(ChunkDimPos.getID(mc.world)))
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

					MapRegion region = dim.getRegion(XZ.regionFromChunk(ox, oz));
					region.getMapImage().uploadTextureSub(0, mx * 16, mz * 16, (ox & 31) * 16, (oz & 31) * 16, 16, 16, FTBChunksClientConfig.minimapBlur, false, false, false);
				}
			}

			currentPlayerChunkX = cx;
			currentPlayerChunkZ = cz;
		}

		if (mc.gameSettings.showDebugInfo || FTBChunksClientConfig.minimap == MinimapPosition.DISABLED || FTBChunksClientConfig.minimapVisibility == 0)
		{
			return;
		}

		float scale = (float) (FTBChunksClientConfig.minimapScale * 4D / mc.getMainWindow().getGuiScaleFactor());
		float minimapRotation = (FTBChunksClientConfig.minimapLockedNorth ? 180F : -mc.player.rotationYaw) % 360F;

		int s = (int) (64D * scale);
		int x = FTBChunksClientConfig.minimap.getX(mc.getMainWindow().getScaledWidth(), s);
		int y = FTBChunksClientConfig.minimap.getY(mc.getMainWindow().getScaledHeight(), s);
		int z = 0;

		float border = 0F;
		int alpha = FTBChunksClientConfig.minimapVisibility;

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();

		float f0 = 1F / (float) FTBChunks.TILES;
		float f1 = 1F - f0;

		float offX = (float) ((mc.player.getPosX() / 16D - currentPlayerChunkX - 0.5D) / (double) FTBChunks.TILES);
		float offY = (float) ((mc.player.getPosZ() / 16D - currentPlayerChunkZ - 0.5D) / (double) FTBChunks.TILES);

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableCull();
		RenderSystem.enableTexture();
		RenderSystem.enableDepthTest();

		MatrixStack matrixStack = event.getMatrixStack();
		matrixStack.push();
		matrixStack.translate(x + s / 2D, y + s / 2D, -10);

		matrixStack.translate(0, 0, 950);

		Matrix4f m = matrixStack.getLast().getMatrix();

		// See AdvancementTabGui
		RenderSystem.colorMask(false, false, false, false);
		mc.getTextureManager().bindTexture(CIRCLE_MASK);
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
		buffer.pos(m, -s / 2F + border, -s / 2F + border, z).color(255, 255, 255, 255).tex(0F, 0F).endVertex();
		buffer.pos(m, -s / 2F + border, s / 2F - border, z).color(255, 255, 255, 255).tex(0F, 1F).endVertex();
		buffer.pos(m, s / 2F - border, s / 2F - border, z).color(255, 255, 255, 255).tex(1F, 1F).endVertex();
		buffer.pos(m, s / 2F - border, -s / 2F + border, z).color(255, 255, 255, 255).tex(1F, 0F).endVertex();
		tessellator.draw();
		RenderSystem.colorMask(true, true, true, true);

		matrixStack.translate(0, 0, -950);
		matrixStack.rotate(Vector3f.ZP.rotationDegrees(minimapRotation + 180F));

		RenderSystem.depthFunc(GL11.GL_GEQUAL);
		RenderSystem.bindTexture(minimapTextureId);

		m = matrixStack.getLast().getMatrix();

		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
		buffer.pos(m, -s / 2F + border, -s / 2F + border, z).color(255, 255, 255, alpha).tex(f0 + offX, f0 + offY).endVertex();
		buffer.pos(m, -s / 2F + border, s / 2F - border, z).color(255, 255, 255, alpha).tex(f0 + offX, f1 + offY).endVertex();
		buffer.pos(m, s / 2F - border, s / 2F - border, z).color(255, 255, 255, alpha).tex(f1 + offX, f1 + offY).endVertex();
		buffer.pos(m, s / 2F - border, -s / 2F + border, z).color(255, 255, 255, alpha).tex(f1 + offX, f0 + offY).endVertex();
		tessellator.draw();

		matrixStack.pop();

		RenderSystem.disableDepthTest();
		RenderSystem.depthFunc(GL11.GL_LEQUAL);
		RenderSystem.defaultBlendFunc();

		m = matrixStack.getLast().getMatrix();

		mc.getTextureManager().bindTexture(CIRCLE_BORDER);
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
		buffer.pos(m, x, y, z).color(255, 255, 255, alpha).tex(0F, 0F).endVertex();
		buffer.pos(m, x, y + s, z).color(255, 255, 255, alpha).tex(0F, 1F).endVertex();
		buffer.pos(m, x + s, y + s, z).color(255, 255, 255, alpha).tex(1F, 1F).endVertex();
		buffer.pos(m, x + s, y, z).color(255, 255, 255, alpha).tex(1F, 0F).endVertex();
		tessellator.draw();

		RenderSystem.disableTexture();
		buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
		buffer.pos(m, x + s / 2F, y + 0, z).color(0, 0, 0, 30).endVertex();
		buffer.pos(m, x + s / 2F, y + s, z).color(0, 0, 0, 30).endVertex();
		buffer.pos(m, x + 0, y + s / 2F, z).color(0, 0, 0, 30).endVertex();
		buffer.pos(m, x + s, y + s / 2F, z).color(0, 0, 0, 30).endVertex();
		tessellator.draw();

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

				m = matrixStack.getLast().getMatrix();

				mc.textureManager.bindTexture(COMPASS[face]);
				buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
				buffer.pos(m, wx - ws, wy - ws, z).color(255, 255, 255, 255).tex(0F, 0F).endVertex();
				buffer.pos(m, wx - ws, wy + ws, z).color(255, 255, 255, 255).tex(0F, 1F).endVertex();
				buffer.pos(m, wx + ws, wy + ws, z).color(255, 255, 255, 255).tex(1F, 1F).endVertex();
				buffer.pos(m, wx + ws, wy - ws, z).color(255, 255, 255, 255).tex(1F, 0F).endVertex();
				tessellator.draw();
			}
		}

		if (FTBChunksClientConfig.minimapWaypoints && !dim.waypoints.isEmpty())
		{
			for (Waypoint waypoint : dim.waypoints)
			{
				double d = MathUtils.dist(mc.player.getPosX(), mc.player.getPosZ(), waypoint.x, waypoint.z) / 3.2D * scale;

				if (d > s / 2D)
				{
					d = s / 2D;
				}

				double angle = Math.atan2(mc.player.getPosZ() - waypoint.z, mc.player.getPosX() - waypoint.x) + minimapRotation * Math.PI / 180D;

				float wx = (float) (x + s / 2D + Math.cos(angle) * d);
				float wy = (float) (y + s / 2D + Math.sin(angle) * d);
				float ws = s / 32F;

				int r = (waypoint.color >> 16) & 0xFF;
				int g = (waypoint.color >> 8) & 0xFF;
				int b = (waypoint.color >> 0) & 0xFF;

				m = matrixStack.getLast().getMatrix();

				mc.getTextureManager().bindTexture(waypoint.type.texture);
				buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
				buffer.pos(m, wx - ws, wy - ws, z).color(r, g, b, 255).tex(0F, 0F).endVertex();
				buffer.pos(m, wx - ws, wy + ws, z).color(r, g, b, 255).tex(0F, 1F).endVertex();
				buffer.pos(m, wx + ws, wy + ws, z).color(r, g, b, 255).tex(1F, 1F).endVertex();
				buffer.pos(m, wx + ws, wy - ws, z).color(r, g, b, 255).tex(1F, 0F).endVertex();
				tessellator.draw();
			}
		}

		if (FTBChunksClientConfig.minimapEntities)
		{
			for (Entity entity : mc.world.getAllEntities())
			{
				if (entity instanceof AbstractClientPlayerEntity || entity.getType().getClassification() == EntityClassification.MISC || entity.getPosY() < entity.world.getHeight(Heightmap.Type.WORLD_SURFACE, MathHelper.floor(entity.getPosX()), MathHelper.floor(entity.getPosZ())) - 10)
				{
					continue;
				}

				double d = MathUtils.dist(mc.player.getPosX(), mc.player.getPosZ(), entity.getPosX(), entity.getPosZ()) / 3.2D * scale;

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

				double angle = Math.atan2(mc.player.getPosZ() - entity.getPosZ(), mc.player.getPosX() - entity.getPosX()) + minimapRotation * Math.PI / 180D;

				float wx = (float) (x + s / 2D + Math.cos(angle) * d);
				float wy = (float) (y + s / 2D + Math.sin(angle) * d);
				float ws = s / (FTBChunksClientConfig.minimapLargeEntities ? 32F : 48F);

				m = matrixStack.getLast().getMatrix();

				mc.getTextureManager().bindTexture(texture);
				RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
				RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

				buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
				buffer.pos(m, wx - ws, wy - ws, z).color(255, 255, 255, 255).tex(0F, 0F).endVertex();
				buffer.pos(m, wx - ws, wy + ws, z).color(255, 255, 255, 255).tex(0F, 1F).endVertex();
				buffer.pos(m, wx + ws, wy + ws, z).color(255, 255, 255, 255).tex(1F, 1F).endVertex();
				buffer.pos(m, wx + ws, wy - ws, z).color(255, 255, 255, 255).tex(1F, 0F).endVertex();
				tessellator.draw();
			}
		}

		if (FTBChunksClientConfig.minimapPlayerHeads && mc.world.getPlayers().size() > 1)
		{
			for (AbstractClientPlayerEntity player : mc.world.getPlayers())
			{
				if (player == mc.player)
				{
					continue;
				}

				double d = MathUtils.dist(mc.player.getPosX(), mc.player.getPosZ(), player.getPosX(), player.getPosZ()) / 3.2D * scale;

				if (d > s / 2D)
				{
					d = s / 2D;
				}

				double angle = Math.atan2(mc.player.getPosZ() - player.getPosZ(), mc.player.getPosX() - player.getPosX()) + minimapRotation * Math.PI / 180D;

				float wx = (float) (x + s / 2D + Math.cos(angle) * d);
				float wy = (float) (y + s / 2D + Math.sin(angle) * d);
				float ws = s / 32F;

				String uuid = UUIDTypeAdapter.fromUUID(player.getUniqueID());
				ResourceLocation texture = new ResourceLocation("head", uuid);

				TextureManager texturemanager = mc.getTextureManager();
				Texture t = texturemanager.getTexture(texture);

				if (t == null)
				{
					t = new PlayerHeadTexture("https://minotar.net/avatar/" + uuid + "/16", ImageIcon.MISSING_IMAGE);
					texturemanager.loadTexture(texture, t);
				}

				m = matrixStack.getLast().getMatrix();

				RenderSystem.bindTexture(t.getGlTextureId());
				buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
				buffer.pos(m, wx - ws, wy - ws, z).color(255, 255, 255, 255).tex(0F, 0F).endVertex();
				buffer.pos(m, wx - ws, wy + ws, z).color(255, 255, 255, 255).tex(0F, 1F).endVertex();
				buffer.pos(m, wx + ws, wy + ws, z).color(255, 255, 255, 255).tex(1F, 1F).endVertex();
				buffer.pos(m, wx + ws, wy - ws, z).color(255, 255, 255, 255).tex(1F, 0F).endVertex();
				tessellator.draw();
			}
		}

		if (FTBChunksClientConfig.minimapLockedNorth)
		{
			mc.getTextureManager().bindTexture(PLAYER);
			matrixStack.push();
			matrixStack.translate(x + s / 2D, y + s / 2D, z);
			matrixStack.rotate(Vector3f.ZP.rotationDegrees(mc.player.rotationYaw + 180F));
			matrixStack.scale(s / 16F, s / 16F, 1F);
			m = matrixStack.getLast().getMatrix();

			buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
			buffer.pos(m, -1, -1, 0).color(255, 255, 255, 200).tex(0F, 0F).endVertex();
			buffer.pos(m, -1, 1, 0).color(255, 255, 255, 200).tex(0F, 1F).endVertex();
			buffer.pos(m, 1, 1, 0).color(255, 255, 255, 200).tex(1F, 1F).endVertex();
			buffer.pos(m, 1, -1, 0).color(255, 255, 255, 200).tex(1F, 0F).endVertex();
			tessellator.draw();

			matrixStack.pop();
		}

		MINIMAP_TEXT_LIST.clear();

		if (FTBChunksClientConfig.minimapZone)
		{
			ITextComponent currentChunkOwner = dim.getRegion(XZ.regionFromChunk(currentPlayerChunkX, currentPlayerChunkZ)).getChunk(XZ.of(currentPlayerChunkX, currentPlayerChunkZ)).owner;

			if (currentChunkOwner != StringTextComponent.EMPTY)
			{
				MINIMAP_TEXT_LIST.add(currentChunkOwner);
			}
			else
			{
				MINIMAP_TEXT_LIST.add(new TranslationTextComponent("wilderness").mergeStyle(TextFormatting.DARK_GREEN));
			}
		}

		if (FTBChunksClientConfig.minimapXYZ)
		{
			MINIMAP_TEXT_LIST.add(new StringTextComponent(MathHelper.floor(mc.player.getPosX()) + " " + MathHelper.floor(mc.player.getPosY()) + " " + MathHelper.floor(mc.player.getPosZ())));
		}

		if (FTBChunksClientConfig.minimapBiome)
		{
			MINIMAP_TEXT_LIST.add(new TranslationTextComponent(mc.world.getBiome(mc.player.getPosition()).getTranslationKey()));
		}

		if (FTBChunksClientConfig.debugInfo)
		{
			MINIMAP_TEXT_LIST.add(new StringTextComponent("Queued tasks: " + taskQueue.size()));
		}

		if (!MINIMAP_TEXT_LIST.isEmpty())
		{
			matrixStack.push();
			matrixStack.translate(x + s / 2D, y + s + 3D, 0D);
			matrixStack.scale((float) (0.5D * scale), (float) (0.5D * scale), 1F);

			for (int i = 0; i < MINIMAP_TEXT_LIST.size(); i++)
			{
				ITextProperties bs = MINIMAP_TEXT_LIST.get(i);
				int bsw = mc.fontRenderer.func_238414_a_(bs);
				mc.fontRenderer.func_238407_a_(matrixStack, bs, -bsw / 2F, i * 11, 0xFFFFFFFF);
			}

			matrixStack.pop();
		}

		RenderSystem.enableDepthTest();
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
		if (event.phase == TickEvent.Phase.START && MapManager.inst != null && Minecraft.getInstance().world != null)
		{
			if (taskQueueTicks % FTBChunksClientConfig.taskQueueTicks == 1L)
			{
				if (!rerenderCache.isEmpty())
				{
					World world = Minecraft.getInstance().world;

					for (ChunkPos pos : rerenderCache)
					{
						queue(new ReloadChunkTask(world, pos));
					}

					rerenderCache.clear();
				}

				int s = Math.min(taskQueue.size(), MathHelper.clamp(taskQueue.size() / 10, FTBChunksClientConfig.taskQueueMin, FTBChunksClientConfig.taskQueueMax));

				MapTask[] tasks = new MapTask[s];

				for (int i = 0; i < s; i++)
				{
					tasks[i] = taskQueue.pollFirst();

					if (tasks[i] == null || tasks[i].cancelOtherTasks())
					{
						break;
					}
				}

				FTBChunks.EXECUTOR_SERVICE.execute(() -> {
					for (MapTask task : tasks)
					{
						if (task != null)
						{
							task.runMapTask();
						}
					}
				});
			}

			taskQueueTicks++;
		}
	}

	@SubscribeEvent
	public void chunkLoaded(ChunkEvent.Load event)
	{
		if (event.getWorld() instanceof World && event.getChunk() instanceof Chunk && event.getWorld().isRemote())
		{
			// queue(new ReloadChunkTask((World) event.getWorld(), event.getChunk().getPos()));
		}
	}
}