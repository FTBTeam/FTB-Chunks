package com.feed_the_beast.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import com.feed_the_beast.mods.ftbchunks.FTBChunksCommon;
import com.feed_the_beast.mods.ftbchunks.FTBChunksConfig;
import com.feed_the_beast.mods.ftbchunks.api.ChunkDimPos;
import com.feed_the_beast.mods.ftbchunks.api.Waypoint;
import com.feed_the_beast.mods.ftbchunks.client.map.ClientMapChunk;
import com.feed_the_beast.mods.ftbchunks.client.map.ClientMapDimension;
import com.feed_the_beast.mods.ftbchunks.client.map.ClientMapManager;
import com.feed_the_beast.mods.ftbchunks.client.map.ClientMapRegion;
import com.feed_the_beast.mods.ftbchunks.client.map.PlayerHeadTexture;
import com.feed_the_beast.mods.ftbchunks.impl.PlayerLocation;
import com.feed_the_beast.mods.ftbchunks.impl.map.ColorBlend;
import com.feed_the_beast.mods.ftbchunks.impl.map.MapTask;
import com.feed_the_beast.mods.ftbchunks.impl.map.XZ;
import com.feed_the_beast.mods.ftbchunks.net.LoginDataPacket;
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
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.monster.IMob;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.gen.Heightmap;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Random;

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

	public static final ArrayDeque<MapTask> taskQueue = new ArrayDeque<>();
	private static long taskQueueTicks = 0L;

	public static KeyBinding openMapKey;

	public static int minimapTextureId = -1;
	private static int currentPlayerChunkX, currentPlayerChunkZ;

	private boolean updateMinimap = false;
	public static SendGeneralDataPacket generalData;
	private long nextRegionSave = 0L;

	public void init()
	{
		MinecraftForge.EVENT_BUS.register(this);
		FTBChunksClientConfig.init();
		openMapKey = new KeyBinding("key.ftbchunks.map", KeyConflictContext.IN_GAME, KeyModifier.NONE, InputMappings.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.ui");
		ClientRegistry.registerKeyBinding(openMapKey);
		((IReloadableResourceManager) Minecraft.getInstance().getResourceManager()).addReloadListener(new EntityIcons());
	}

	public static void openGui()
	{
		new LargeMapScreen().openGui();
	}

	public static void saveAllRegions()
	{
		for (ClientMapDimension dimension : ClientMapManager.inst.dimensions.values())
		{
			for (ClientMapRegion region : dimension.regions.values())
			{
				if (region.saveImage)
				{
					taskQueue.addLast(region);
					region.saveImage = false;
				}
			}
		}
	}

	@Override
	public void login(LoginDataPacket loginData)
	{
		ClientMapManager.inst = new ClientMapManager(loginData.serverId, FMLPaths.GAMEDIR.get().resolve("local/ftbchunks/map/" + loginData.serverId));
		ClientMapDimension.current = ClientMapManager.inst.getDimension(ChunkDimPos.getID(Minecraft.getInstance().world));
		updateMinimap = true;
	}

	@Override
	public void updateGeneralData(SendGeneralDataPacket packet)
	{
		generalData = packet;
	}

	@Override
	public void updateChunk(SendChunkPacket packet)
	{
		try
		{
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(packet.imageData));
			ClientMapChunk chunk = ClientMapManager.inst.getDimension(packet.dimension).getRegion(XZ.regionFromChunk(packet.x, packet.z)).getChunk(XZ.of(packet.x, packet.z));
			Date now = new Date();
			chunk.claimedDate = packet.owner == null ? null : new Date(now.getTime() - packet.relativeTimeClaimed);
			chunk.forceLoadedDate = packet.forceLoaded && chunk.claimedDate != null ? new Date(now.getTime() - packet.relativeTimeForceLoaded) : null;
			chunk.color = packet.color;
			chunk.owner = packet.owner == null ? StringTextComponent.EMPTY : packet.owner;
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

					if (FTBChunksClientConfig.noise != 0D)
					{
						nc = ColorBlend.addBrightness(nc, (float) (random.nextFloat() * FTBChunksClientConfig.noise - FTBChunksClientConfig.noise / 2D));
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
	public void updateWaypoints(SendWaypointsPacket packet)
	{
		for (ClientMapDimension dimension : ClientMapManager.inst.dimensions.values())
		{
			dimension.waypoints.clear();
		}

		for (Waypoint waypoint : packet.waypoints)
		{
			ClientMapManager.inst.getDimension(waypoint.dimension).waypoints.add(waypoint);
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

		if (ClientMapManager.inst == null)
		{
			return;
		}

		String dimId = ChunkDimPos.getID(mc.world);

		if (ClientMapDimension.current == null || !ClientMapDimension.current.dimension.equals(dimId))
		{
			ClientMapDimension.current = ClientMapManager.inst.getDimension(dimId);
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

					ClientMapRegion region = ClientMapDimension.current.getRegion(XZ.regionFromChunk(ox, oz));
					region.getImage().uploadTextureSub(0, mx * 16, mz * 16, (ox & 31) * 16, (oz & 31) * 16, 16, 16, false, false);
				}
			}

			currentPlayerChunkX = cx;
			currentPlayerChunkZ = cz;
		}

		if (mc.gameSettings.showDebugInfo || FTBChunksClientConfig.minimap == MinimapPosition.DISABLED)
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
		buffer.pos(m, -s / 2F + border, -s / 2F + border, z).color(255, 255, 255, 255).tex(f0 + offX, f0 + offY).endVertex();
		buffer.pos(m, -s / 2F + border, s / 2F - border, z).color(255, 255, 255, 255).tex(f0 + offX, f1 + offY).endVertex();
		buffer.pos(m, s / 2F - border, s / 2F - border, z).color(255, 255, 255, 255).tex(f1 + offX, f1 + offY).endVertex();
		buffer.pos(m, s / 2F - border, -s / 2F + border, z).color(255, 255, 255, 255).tex(f1 + offX, f0 + offY).endVertex();
		tessellator.draw();

		matrixStack.pop();

		m = matrixStack.getLast().getMatrix();

		RenderSystem.depthFunc(GL11.GL_LEQUAL);

		RenderSystem.defaultBlendFunc();
		mc.getTextureManager().bindTexture(CIRCLE_BORDER);
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
		buffer.pos(m, x, y, z).color(255, 255, 255, 255).tex(0F, 0F).endVertex();
		buffer.pos(m, x, y + s, z).color(255, 255, 255, 255).tex(0F, 1F).endVertex();
		buffer.pos(m, x + s, y + s, z).color(255, 255, 255, 255).tex(1F, 1F).endVertex();
		buffer.pos(m, x + s, y, z).color(255, 255, 255, 255).tex(1F, 0F).endVertex();
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

		if (FTBChunksClientConfig.minimapWaypoints && !ClientMapDimension.current.waypoints.isEmpty())
		{
			for (Waypoint waypoint : ClientMapDimension.current.waypoints)
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

		if (FTBChunksClientConfig.minimapXYZ || FTBChunksClientConfig.minimapBiome)
		{
			matrixStack.push();
			matrixStack.translate(x + s / 2D, y + s + 5D, 0D);
			matrixStack.scale((float) (0.5D * scale), (float) (0.5D * scale), 1F);

			if (FTBChunksClientConfig.minimapXYZ)
			{
				String cs = MathHelper.floor(mc.player.getPosX()) + " " + MathHelper.floor(mc.player.getPosY()) + " " + MathHelper.floor(mc.player.getPosZ());
				int csw = mc.fontRenderer.getStringWidth(cs);
				mc.fontRenderer.drawStringWithShadow(matrixStack, cs, -csw / 2F, 0, 0xFFFFFFFF);
			}

			if (FTBChunksClientConfig.minimapBiome)
			{
				String bs = I18n.format(mc.world.getBiome(mc.player.getPosition()).getTranslationKey());
				int bsw = mc.fontRenderer.getStringWidth(bs);
				mc.fontRenderer.drawStringWithShadow(matrixStack, bs, -bsw / 2F, FTBChunksClientConfig.minimapXYZ ? 10 : 0, 0xFFFFFFFF);
			}

			matrixStack.pop();
		}
	}

	@SubscribeEvent
	public void loggedOut(ClientPlayerNetworkEvent.LoggedOutEvent event)
	{
		if (ClientMapManager.inst != null)
		{
			saveAllRegions();

			MapTask t;

			while ((t = taskQueue.pollFirst()) != null)
			{
				t.run();
			}

			ClientMapManager.inst.release();
			ClientMapManager.inst = null;
		}
	}

	@SubscribeEvent
	public void clientTick(TickEvent.ClientTickEvent event)
	{
		if (event.phase == TickEvent.Phase.START && ClientMapManager.inst != null && Minecraft.getInstance().world != null)
		{
			if (taskQueueTicks % FTBChunksConfig.taskQueueTicks == 1L)
			{
				int s = Math.min(taskQueue.size(), MathHelper.clamp(taskQueue.size() / 10, FTBChunksConfig.taskQueueMin, FTBChunksConfig.taskQueueMax));

				for (int i = 0; i < s; i++)
				{
					MapTask r = taskQueue.pollFirst();

					if (r != null)
					{
						r.run();

						if (r.cancelOtherTasks())
						{
							break;
						}
					}
					else
					{
						break;
					}
				}
			}

			taskQueueTicks++;
		}
	}
}