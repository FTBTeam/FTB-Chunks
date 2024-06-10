package dev.ftb.mods.ftbchunks.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.*;
import dev.architectury.hooks.client.screen.ScreenAccess;
import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.platform.Platform;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.ftb.mods.ftbchunks.ColorMapLoader;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.FTBChunksCommon;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.client.map.*;
import dev.ftb.mods.ftbchunks.client.map.color.ColorUtils;
import dev.ftb.mods.ftbchunks.integration.*;
import dev.ftb.mods.ftbchunks.net.*;
import dev.ftb.mods.ftblibrary.config.StringConfig;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigFromStringScreen;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.FaceIcon;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftblibrary.ui.CustomClickEvent;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import dev.ftb.mods.ftblibrary.util.ClientUtils;
import dev.ftb.mods.ftblibrary.util.StringUtils;
import dev.ftb.mods.ftbteams.data.ClientTeam;
import dev.ftb.mods.ftbteams.event.ClientTeamPropertiesChangedEvent;
import dev.ftb.mods.ftbteams.event.TeamEvent;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * @author LatvianModder
 */
public class FTBChunksClient extends FTBChunksCommon {
	private static final ResourceLocation BUTTON_ID_MAP = new ResourceLocation("ftbchunks:open_gui");
	private static final ResourceLocation BUTTON_ID_CLAIM = new ResourceLocation("ftbchunks:open_claim_gui");

	public static final ResourceLocation CIRCLE_MASK = new ResourceLocation("ftbchunks:textures/circle_mask.png");
	public static final ResourceLocation CIRCLE_BORDER = new ResourceLocation("ftbchunks:textures/circle_border.png");
	public static final ResourceLocation SQUARE_MASK = new ResourceLocation("ftbchunks:textures/square_mask.png");
	public static final ResourceLocation SQUARE_BORDER = new ResourceLocation("ftbchunks:textures/square_border.png");
	public static final ResourceLocation PLAYER = new ResourceLocation("ftbchunks:textures/player.png");
	public static final ResourceLocation[] COMPASS = {
			new ResourceLocation("ftbchunks:textures/compass_e.png"),
			new ResourceLocation("ftbchunks:textures/compass_n.png"),
			new ResourceLocation("ftbchunks:textures/compass_w.png"),
			new ResourceLocation("ftbchunks:textures/compass_s.png"),
	};

	private static final Map<UUID, TrackedPlayerMapIcon> longRangePlayerTracker = new HashMap<>();

	private static final ArrayDeque<MapTask> taskQueue = new ArrayDeque<>();
	private static final Map<ChunkPos, IntOpenHashSet> rerenderCache = new HashMap<>();
	private static long taskQueueTicks = 0L;

	public static KeyMapping openMapKey;
	public static KeyMapping openClaimManagerKey;
	public static KeyMapping zoomInKey;
	public static KeyMapping zoomOutKey;
	public static KeyMapping addWaypointKey;
	public static KeyMapping waypointManagerKey;

	public static int minimapTextureId = -1;
	private int currentPlayerChunkX, currentPlayerChunkZ;
	private double currentPlayerX, currentPlayerY, currentPlayerZ;
	private double prevPlayerX, prevPlayerY, prevPlayerZ;
	private static int renderedDebugCount = 0;

	public static boolean updateMinimap = false;
	public static boolean alwaysRenderChunksOnMap = false;
	public static SendGeneralDataPacket generalData;
	private long nextRegionSave = 0L;
	private double prevZoom = FTBChunksClientConfig.MINIMAP_ZOOM.get();
	private long lastZoomTime = 0L;
	private final List<MapIcon> mapIcons = new ArrayList<>();
	private final List<InWorldMapIcon> inWorldMapIcons = new ArrayList<>();
	private long lastMapIconUpdate = 0L;

	private static Matrix4f worldMatrix;
	private static Vec3 cameraPos;

	@Override
	public void init() {
		if (Minecraft.getInstance() == null) {
			return;
		}

		FTBChunksClientConfig.init();
		registerKeys();

		ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, new EntityIcons());
		ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, new ColorMapLoader());
		ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(this::loggedOut);
		CustomClickEvent.EVENT.register(this::customClick);
		ClientRawInputEvent.KEY_PRESSED.register(this::keyPressed);
		ClientScreenInputEvent.KEY_PRESSED_PRE.register(this::keyPressed);
		ClientGuiEvent.RENDER_HUD.register(this::renderHud);
		ClientGuiEvent.INIT_PRE.register(this::screenOpened);
		ClientTickEvent.CLIENT_PRE.register(this::clientTick);
		TeamEvent.CLIENT_PROPERTIES_CHANGED.register(this::teamPropertiesChanged);
		MapIconEvent.LARGE_MAP.register(this::mapIcons);
		MapIconEvent.MINIMAP.register(this::mapIcons);
		RefreshMinimapIconsEvent.EVENT.register(this::refreshMinimapIcons);
		ClientReloadShadersEvent.EVENT.register(this::reloadShaders);
		registerPlatform();
	}

	private static void registerKeys() {
		// Keybinding to open Large map screen
		openMapKey = new KeyMapping("key.ftbchunks.map", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.ftbchunks");
		KeyMappingRegistry.register(openMapKey);

		// Keybinding to open claim manager screen
		openClaimManagerKey = new KeyMapping("key.ftbchunks.claim_manager", InputConstants.Type.KEYSYM, -1, "key.categories.ftbchunks");
		KeyMappingRegistry.register(openClaimManagerKey);

		// Keybindings to zoom in minimap
		zoomInKey = new KeyMapping("key.ftbchunks.minimap.zoomIn", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_EQUAL, "key.categories.ftbchunks");
		KeyMappingRegistry.register(zoomInKey);

		zoomOutKey = new KeyMapping("key.ftbchunks.minimap.zoomOut", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_MINUS, "key.categories.ftbchunks");
		KeyMappingRegistry.register(zoomOutKey);

		// Keybinding to quick-add waypoint at current position
		addWaypointKey = new KeyMapping("key.ftbchunks.add_waypoint", InputConstants.Type.KEYSYM, -1, "key.categories.ftbchunks");
		KeyMappingRegistry.register(addWaypointKey);

		// Keybinding to open the waypoint manager screen
		waypointManagerKey = new KeyMapping("key.ftbchunks.waypoint_manager", InputConstants.Type.KEYSYM, -1, "key.categories.ftbchunks");
		KeyMappingRegistry.register(waypointManagerKey);
	}

	public static Set<ChunkPos> getPendingRerender() {
		return rerenderCache.keySet();
	}

	public static void queue(MapTask task) {
		taskQueue.addLast(task);
	}

	@ExpectPlatform
	public static void registerPlatform() {
		throw new AssertionError();
	}

	public static void openGui() {
		new LargeMapScreen().openGui();
	}

	public static void saveAllRegions() {
		if (MapManager.inst == null) {
			return;
		}

		for (MapDimension dimension : MapManager.inst.getDimensions().values()) {
			for (MapRegion region : dimension.getLoadedRegions()) {
				if (region.saveData) {
					queue(region);
					region.saveData = false;
				}
			}

			if (dimension.saveData) {
				queue(dimension);
				dimension.saveData = false;
			}
		}

		if (MapManager.inst.saveData) {
			queue(MapManager.inst);
			MapManager.inst.saveData = false;
		}
	}

	@Override
	public void login(LoginDataPacket loginData) {
		FTBChunks.LOGGER.info("Loading FTB Chunks client data from world " + loginData.serverId);
		FTBChunksWorldConfig.CONFIG.read(loginData.config);

		Path dir = Platform.getGameFolder().resolve("local/ftbchunks/data/" + loginData.serverId);

		if (Files.notExists(dir)) {
			try {
				Files.createDirectories(dir);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		MapManager.inst = new MapManager(loginData.serverId, dir);
		updateMinimap = true;
		renderedDebugCount = 0;
		ChunkUpdateTask.debugLastTime = 0L;
	}

	public void loggedOut(@Nullable LocalPlayer player) {
		MapManager manager = MapManager.inst;

		if (manager != null) {
			saveAllRegions();

			MapTask task;

			while ((task = taskQueue.pollFirst()) != null) {
				try {
					task.runMapTask();
				} catch (Exception ex) {
					FTBChunks.LOGGER.error("Failed to run task " + task);
					ex.printStackTrace();
				}
			}

			MapDimension.updateCurrent();
			manager.release();
		}

		MapManager.inst = null;
	}

	@Override
	public void updateGeneralData(SendGeneralDataPacket packet) {
		generalData = packet;
	}

	@Override
	public void updateChunk(SendChunkPacket packet) {
		if (MapManager.inst == null) {
			return;
		}

		MapDimension dimension = MapManager.inst.getDimension(packet.dimension);
		Date now = new Date();
		queue(new UpdateChunkFromServerTask(dimension, packet.chunk, packet.teamId, now));
	}

	@Override
	public void updateAllChunks(SendManyChunksPacket packet) {
		if (MapManager.inst == null) {
			return;
		}

		MapDimension dimension = MapManager.inst.getDimension(packet.dimension);
		Date now = new Date();

		for (SendChunkPacket.SingleChunk c : packet.chunks) {
			queue(new UpdateChunkFromServerTask(dimension, c, packet.teamId, now));
		}
	}

	@Override
	public void syncRegion(RegionSyncKey key, int offset, int total, byte[] data) {
		PartialPackets.REGION.read(key, offset, total, data);
	}

	@Override
	public void playerDeath(PlayerDeathPacket packet) {
		if (FTBChunksClientConfig.DEATH_WAYPOINTS.get() && FTBChunksWorldConfig.playerHasMapStage(Minecraft.getInstance().player)) {
			MapDimension dimension = MapManager.inst.getDimension(packet.dimension);

			for (Waypoint w : dimension.getWaypointManager()) {
				if (w.type == WaypointType.DEATH) {
					w.hidden = true;
					w.update();
				}
			}

			Waypoint w = new Waypoint(dimension, packet.x, packet.y, packet.z);
			w.name = "Death #" + packet.number;
			w.type = WaypointType.DEATH;
			w.color = 0xFF0000;
			dimension.getWaypointManager().add(w);
		}
	}

	@Override
	public int blockColor() {
		Minecraft mc = Minecraft.getInstance();

		mc.submit(() -> {
			mc.setScreen(null);

			new Thread(() -> {
				try {
					Thread.sleep(50L);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				mc.submit(() -> {
					if (mc.hitResult instanceof BlockHitResult) {
						ResourceLocation id = Registry.BLOCK.getKey(mc.level.getBlockState(((BlockHitResult) mc.hitResult).getBlockPos()).getBlock());
						Window window = mc.getWindow();
						try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
							int col = image.getPixelRGBA(image.getWidth() / 2 - (int) (2D * window.getGuiScale()), image.getHeight() / 2 - (int) (2D * window.getGuiScale()));
							String s = String.format("\"%s\": \"#%06X\"", id.getPath(), ColorUtils.convertFromNative(col) & 0xFFFFFF);
							mc.player.displayClientMessage(Component.literal(id.getNamespace() + " - " + s).withStyle(Style.EMPTY.applyFormat(ChatFormatting.GOLD).withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, s)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy")))), false);
						}
					}
				});
			}, "Color getter").start();
		});

		return 1;
	}

	@Override
	public void updateLoadedChunkView(ResourceKey<Level> dimension, Long2IntMap chunks) {
		MapDimension dim = MapManager.inst.getDimension(dimension);
		dim.loadedChunkView = chunks;
		MapManager.inst.updateAllRegions(false);
	}

	@Override
	public boolean skipBlock(BlockState state) {
		ResourceLocation id = FTBChunks.BLOCK_REGISTRY.getId(state.getBlock());
		return id == null || ColorMapLoader.getBlockColor(id).isIgnored();
	}

	@Override
	public void addWaypoint(Player player, String name, BlockPos pos, int color) {
		MapDimension mapDimension = MapManager.inst.getDimension(player.level.dimension());
		Waypoint w = new Waypoint(mapDimension, pos.getX(), pos.getY(), pos.getZ());
		w.name = name;
		w.color = color;
		mapDimension.getWaypointManager().add(w);
	}

	public EventResult customClick(CustomClickEvent event) {
		if (FTBChunksWorldConfig.playerHasMapStage(Minecraft.getInstance().player)) {
			if (event.id().equals(BUTTON_ID_MAP)) {
				openGui();
				return EventResult.interruptTrue();
			} else if (event.id().equals(BUTTON_ID_CLAIM)) {
				new ChunkScreen().openGui();
				return EventResult.interruptTrue();
			}
		}
		return EventResult.pass();
	}

	public EventResult keyPressed(Minecraft client, int keyCode, int scanCode, int action, int modifiers) {
		if (action != GLFW.GLFW_PRESS || client.screen != null || !FTBChunksWorldConfig.playerHasMapStage(client.player)) {
			return EventResult.pass();
		}
		if (openMapKey.matches(keyCode, scanCode)) {
			if (Screen.hasControlDown()) {
				SNBTCompoundTag tag = new SNBTCompoundTag();
				tag.putBoolean(FTBChunksClientConfig.MINIMAP_ENABLED.key, !FTBChunksClientConfig.MINIMAP_ENABLED.get());
				FTBChunksClientConfig.MINIMAP_ENABLED.read(tag);
				FTBChunksClientConfig.saveConfig();
			} else if (FTBChunksClientConfig.DEBUG_INFO.get() && Screen.hasAltDown()) {
				FTBChunks.LOGGER.info("=== Task Queue: " + taskQueue.size());

				for (MapTask task : taskQueue) {
					FTBChunks.LOGGER.info(task.toString());
				}

				FTBChunks.LOGGER.info("===");
			} else {
				openGui();
				return EventResult.interruptTrue();
			}
		} else if (openClaimManagerKey.matches(keyCode, scanCode)) {
			new ChunkScreen().openGui();
			return EventResult.interruptTrue();
		} else if (zoomInKey.matches(keyCode, scanCode)) {
			return changeZoom(true);
		} else if (zoomOutKey.matches(keyCode, scanCode)) {
			return changeZoom(false);
		} else if (addWaypointKey.matches(keyCode, scanCode)) {
			return addQuickWaypoint();
		} else if (waypointManagerKey.matches(keyCode, scanCode)) {
			new WaypointEditorScreen().openGui();
		}

		return EventResult.pass();
	}

	public EventResult keyPressed(Minecraft client, Screen screen, int keyCode, int scanCode, int modifiers) {
		if (openMapKey.matches(keyCode, scanCode)) {
			LargeMapScreen gui = ClientUtils.getCurrentGuiAs(LargeMapScreen.class);

			if (gui != null) {
				gui.closeGui(false);
				return EventResult.interruptTrue();
			}
		}

		return EventResult.pass();
	}

	private EventResult addQuickWaypoint() {
		StringConfig name = new StringConfig();
		Player player = Minecraft.getInstance().player;
		if (player == null || MapManager.inst == null) return EventResult.pass();

		new EditConfigFromStringScreen<>(name, set -> {
			if (set && !name.value.isEmpty()) {
				MapDimension mapDimension = MapManager.inst.getDimension(player.level.dimension());
				Waypoint w = new Waypoint(mapDimension, player.getBlockX(), player.getBlockY(), player.getBlockZ());
				w.name = name.value;
				w.color = Color4I.hsb(MathUtils.RAND.nextFloat(), 1F, 1F).rgba();
				mapDimension.getWaypointManager().add(w);
			}
			openGui();
		}).openGuiLater();  // later need to prevent keypress being passed into gui

		return EventResult.interruptTrue();
	}

	private EventResult changeZoom(boolean zoomIn) {
		prevZoom = FTBChunksClientConfig.MINIMAP_ZOOM.get();
		double zoom = prevZoom;
		double zoomFactor = zoomIn ? 1D : -1D;

		if (zoom + zoomFactor > 4D) {
			zoom = 4D;
		} else if (zoom + zoomFactor < 1D) {
			zoom = 1D;
		} else {
			zoom += zoomFactor;
		}

		lastZoomTime = System.currentTimeMillis();
		FTBChunksClientConfig.MINIMAP_ZOOM.set(zoom);
		return EventResult.interruptTrue();
	}

	public float getZoom() {
		double z = FTBChunksClientConfig.MINIMAP_ZOOM.get();

		if (prevZoom != z) {
			long max = (long) (400D / z);
			long t = Mth.clamp(System.currentTimeMillis() - lastZoomTime, 0L, max);

			if (t == max) {
				lastZoomTime = 0L;
				return (float) z;
			}

			return (float) Mth.lerp(t / (double) max, prevZoom, z);
		}

		return (float) z;
	}

	public static int generateTextureId(int w, int h) {
		// int textureId = TextureUtil.generateTextureId();
		// TextureUtil.prepareImage(textureId, w, h);
		int textureId = GlStateManager._genTexture();
		RenderSystem.bindTextureForSetup(textureId);
		GlStateManager._texParameter(3553, 33085, 0);
		GlStateManager._texParameter(3553, 33082, 0);
		GlStateManager._texParameter(3553, 33083, 0);
		GlStateManager._texParameter(3553, 34049, 0.0F);
		GlStateManager._texImage2D(3553, 0, NativeImage.InternalGlFormat.RGBA.glFormat(), w, h, 0, 6408, 5121, null);
		return textureId;
	}

	public void renderHud(PoseStack matrixStack, float tickDelta) {
		Minecraft mc = Minecraft.getInstance();

		if (mc.player == null || mc.level == null || MapManager.inst == null) {
			return;
		}

		double playerX = Mth.lerp(tickDelta, prevPlayerX, currentPlayerX);
		double playerY = Mth.lerp(tickDelta, prevPlayerY, currentPlayerY);
		double playerZ = Mth.lerp(tickDelta, prevPlayerZ, currentPlayerZ);
		double guiScale = mc.getWindow().getGuiScale();
		int scaledWidth = mc.getWindow().getGuiScaledWidth();
		int scaledHeight = mc.getWindow().getGuiScaledHeight();
		MapDimension dim = MapDimension.getCurrent();
		if (dim == null) {
			return;
		}

		if (dim.dimension != mc.level.dimension()) {
			MapDimension.updateCurrent();
			dim = MapDimension.getCurrent();
			longRangePlayerTracker.clear();
		}

		long now = System.currentTimeMillis();

		if (nextRegionSave == 0L || now >= nextRegionSave) {
			nextRegionSave = now + 60000L;
			saveAllRegions();
		}

		if (minimapTextureId == -1) {
			minimapTextureId = generateTextureId(FTBChunks.MINIMAP_SIZE, FTBChunks.MINIMAP_SIZE);
			updateMinimap = true;
		}

		float zoom0 = getZoom();
		float zoom = zoom0 / 3.5F;
		MinimapBlurMode blurMode = FTBChunksClientConfig.MINIMAP_BLUR_MODE.get();
		boolean minimapBlur = blurMode == MinimapBlurMode.AUTO ? (zoom0 < 1.5F) : blurMode == MinimapBlurMode.ON;
		int filter = minimapBlur ? GL11.GL_LINEAR : GL11.GL_NEAREST;

		RenderSystem.enableTexture();
		RenderSystem.bindTextureForSetup(minimapTextureId);
		RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
		RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);

		int cx = Mth.floor(playerX) >> 4;
		int cz = Mth.floor(playerZ) >> 4;

		if (cx != currentPlayerChunkX || cz != currentPlayerChunkZ) {
			updateMinimap = true;
		}

		if (updateMinimap) {
			updateMinimap = false;

			// TODO: More math here to upload from (up to) 4 regions instead of all chunks inside them, to speed things up

			for (int mz = 0; mz < FTBChunks.TILES; mz++) {
				for (int mx = 0; mx < FTBChunks.TILES; mx++) {
					int ox = cx + mx - FTBChunks.TILE_OFFSET;
					int oz = cz + mz - FTBChunks.TILE_OFFSET;

					MapRegion region = dim.getRegion(XZ.regionFromChunk(ox, oz));
					region.getRenderedMapImage().upload(0, mx * 16, mz * 16, (ox & 31) * 16, (oz & 31) * 16, 16, 16, minimapBlur, false, false, false);
				}
			}

			currentPlayerChunkX = cx;
			currentPlayerChunkZ = cz;
		}

		if (mc.options.renderDebug || !FTBChunksClientConfig.MINIMAP_ENABLED.get() || FTBChunksClientConfig.MINIMAP_VISIBILITY.get() == 0 || !FTBChunksWorldConfig.shouldShowMinimap(mc.player)) {
			return;
		}

		float scale;
		if (FTBChunksClientConfig.MINIMAP_PROPORTIONAL.get()) {
			scale = (float) (4D / guiScale);
			scale *= (scaledWidth / 10f) / (scale * 64D) * FTBChunksClientConfig.MINIMAP_SCALE.get();
		} else {
			scale = (float) (FTBChunksClientConfig.MINIMAP_SCALE.get() * 4D / guiScale);
		}

		boolean rotationLocked = FTBChunksClientConfig.MINIMAP_LOCKED_NORTH.get() || FTBChunksClientConfig.SQUARE_MINIMAP.get();
		float minimapRotation = (rotationLocked ? 180F : -mc.player.getYRot()) % 360F;

		int size = (int) (64D * scale);
		double halfSizeD = size / 2D;
		float halfSizeF = size / 2F;

		var minimapPosition = FTBChunksClientConfig.MINIMAP_POSITION.get();

		int x = minimapPosition.getX(scaledWidth, size);
		int y = minimapPosition.getY(scaledHeight, size);
		int z = 0;

		// Apply the offset adjusting for the maps position to ensure all positive numbers offset toward the screen centre
		int offsetX = FTBChunksClientConfig.MINIMAP_OFFSET_X.get();
		int offsetY = FTBChunksClientConfig.MINIMAP_OFFSET_Y.get();
		var offsetConditional = FTBChunksClientConfig.MINIMAP_POSITION_OFFSET_CONDITION.get();
		if (offsetConditional.test(minimapPosition)) {
			x += minimapPosition.posX == 0 ? offsetX : -offsetX;
			y -= minimapPosition.posY > 1 ? offsetY : -offsetY;
		}

		float border = 0F;
		int alpha = FTBChunksClientConfig.MINIMAP_VISIBILITY.get();

		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder buffer = tessellator.getBuilder();

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableCull();
		RenderSystem.enableTexture();
		RenderSystem.enableDepthTest();

		matrixStack.pushPose();
		matrixStack.translate(x + halfSizeD, y + halfSizeD, 490 + z);

		Matrix4f m = matrixStack.last().pose();

		// Draw the minimap cutout mask - see AdvancementTab for a vanilla example of using colorMask()
		RenderSystem.colorMask(false, false, false, false);
		RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
		RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
		RenderSystem.setShaderTexture(0, FTBChunksClientConfig.SQUARE_MINIMAP.get() ? SQUARE_MASK : CIRCLE_MASK);
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		buffer.vertex(m, -halfSizeF + border, -halfSizeF + border, 0F).color(255, 255, 255, 255).uv(0F, 0F).endVertex();
		buffer.vertex(m, -halfSizeF + border, halfSizeF - border, 0F).color(255, 255, 255, 255).uv(0F, 1F).endVertex();
		buffer.vertex(m, halfSizeF - border, halfSizeF - border, 0F).color(255, 255, 255, 255).uv(1F, 1F).endVertex();
		buffer.vertex(m, halfSizeF - border, -halfSizeF + border, 0F).color(255, 255, 255, 255).uv(1F, 0F).endVertex();
		tessellator.end();
		RenderSystem.colorMask(true, true, true, true);

		// minimap rotation
		matrixStack.mulPose(Vector3f.ZP.rotationDegrees(minimapRotation + 180F));

		RenderSystem.depthFunc(GL11.GL_GEQUAL);

		// draw the map itself
		float halfSizeBorderF = halfSizeF - border;
		float offX = 0.5F + (float) ((MathUtils.mod(playerX, 16D) / 16D - 0.5D) / (double) FTBChunks.TILES);
		float offZ = 0.5F + (float) ((MathUtils.mod(playerZ, 16D) / 16D - 0.5D) / (double) FTBChunks.TILES);
		float zws = 2F / (FTBChunks.TILES * zoom);

		RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
		RenderSystem.setShaderTexture(0, minimapTextureId);
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		buffer.vertex(m, -halfSizeBorderF, -halfSizeBorderF, 0F).color(255, 255, 255, alpha).uv(offX - zws, offZ - zws).endVertex();
		buffer.vertex(m, -halfSizeBorderF, halfSizeBorderF, 0F).color(255, 255, 255, alpha).uv(offX - zws, offZ + zws).endVertex();
		buffer.vertex(m, halfSizeBorderF, halfSizeBorderF, 0F).color(255, 255, 255, alpha).uv(offX + zws, offZ + zws).endVertex();
		buffer.vertex(m, halfSizeBorderF, -halfSizeBorderF, 0F).color(255, 255, 255, alpha).uv(offX + zws, offZ - zws).endVertex();
		tessellator.end();

		RenderSystem.disableDepthTest();
		RenderSystem.depthFunc(GL11.GL_LEQUAL);
		RenderSystem.defaultBlendFunc();

		// draw the map border
		RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
		RenderSystem.setShaderTexture(0, FTBChunksClientConfig.SQUARE_MINIMAP.get() ? SQUARE_BORDER : CIRCLE_BORDER);
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		buffer.vertex(m, -halfSizeF, -halfSizeF, 0F).color(255, 255, 255, alpha).uv(0F, 0F).endVertex();
		buffer.vertex(m, -halfSizeF, halfSizeF, 0F).color(255, 255, 255, alpha).uv(0F, 1F).endVertex();
		buffer.vertex(m, halfSizeF, halfSizeF, 0F).color(255, 255, 255, alpha).uv(1F, 1F).endVertex();
		buffer.vertex(m, halfSizeF, -halfSizeF, 0F).color(255, 255, 255, alpha).uv(1F, 0F).endVertex();
		tessellator.end();

		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.disableTexture();

		buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
		buffer.vertex(m, -halfSizeF, 0, 0F).color(0, 0, 0, 30).endVertex();
		buffer.vertex(m, halfSizeF, 0, 0F).color(0, 0, 0, 30).endVertex();
		buffer.vertex(m, 0, -halfSizeF, 0F).color(0, 0, 0, 30).endVertex();
		buffer.vertex(m, 0, halfSizeF, 0F).color(0, 0, 0, 30).endVertex();
		tessellator.end();

		matrixStack.popPose();

		m = matrixStack.last().pose();

		RenderSystem.enableTexture();

		if (FTBChunksClientConfig.MINIMAP_COMPASS.get()) {
			// compass letters at the 4 cardinal points
			for (int face = 0; face < 4; face++) {
				double d = size / 2.2D;

				double angle = (minimapRotation + 180D - face * 90D) * Math.PI / 180D;

				float wx = (float) (x + halfSizeD + Math.cos(angle) * d);
				float wy = (float) (y + halfSizeD + Math.sin(angle) * d);
				float ws = size / 32F;

				RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
				RenderSystem.setShaderTexture(0, COMPASS[face]);
				buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
				buffer.vertex(m, wx - ws, wy - ws, z).color(255, 255, 255, 255).uv(0F, 0F).endVertex();
				buffer.vertex(m, wx - ws, wy + ws, z).color(255, 255, 255, 255).uv(0F, 1F).endVertex();
				buffer.vertex(m, wx + ws, wy + ws, z).color(255, 255, 255, 255).uv(1F, 1F).endVertex();
				buffer.vertex(m, wx + ws, wy - ws, z).color(255, 255, 255, 255).uv(1F, 0F).endVertex();
				tessellator.end();
			}
		}

		if (lastMapIconUpdate == 0L || (now - lastMapIconUpdate) >= FTBChunksClientConfig.MINIMAP_ICON_UPDATE_TIMER.get()) {
			lastMapIconUpdate = now;

			mapIcons.clear();
			MapIconEvent.MINIMAP.invoker().accept(new MapIconEvent(mc, dim, mapIcons, MapType.MINIMAP));

			if (mapIcons.size() >= 2) {
				mapIcons.sort(new MapIconComparator(mc.player.position(), tickDelta));
			}
		}

		for (MapIcon icon : mapIcons) {
			// map icons (waypoints, entities...)
			Vec3 pos = icon.getPos(tickDelta);
			double distance = MathUtils.dist(playerX, playerZ, pos.x, pos.z);
			double d = distance * scale * zoom;

			if (!icon.isVisible(MapType.MINIMAP, distance, d > halfSizeD)) {
				continue;
			}

			if (d > halfSizeD) {
				d = halfSizeD;
			}

			double angle = Math.atan2(playerZ - pos.z, playerX - pos.x) + minimapRotation * Math.PI / 180D;

			double ws = size / (32D / icon.getIconScale(MapType.MINIMAP));
			double wx = x + halfSizeD + Math.cos(angle) * d;
			double wy = y + halfSizeD + Math.sin(angle) * d;
			float wsf = (float) (ws * 2D);

			matrixStack.pushPose();
			matrixStack.translate(wx - ws, wy - ws - (icon.isIconOnEdge(MapType.MINIMAP, d >= halfSizeD) ? ws / 2D : 0D), z);
			matrixStack.scale(wsf, wsf, 1F);
			icon.draw(MapType.MINIMAP, matrixStack, 0, 0, 1, 1, d >= halfSizeD, 255);
			matrixStack.popPose();
		}

		if (rotationLocked || FTBChunksClientConfig.SHOW_PLAYER_WHEN_UNLOCKED.get()) {
			// pointer icon at the map centre (player position)
			RenderSystem.setShaderTexture(0, PLAYER);
			matrixStack.pushPose();
			matrixStack.translate(x + halfSizeD, y + halfSizeD, z);
			if (rotationLocked) {
				matrixStack.mulPose(Vector3f.ZP.rotationDegrees(mc.player.getYRot() + 180F));
			}
			matrixStack.scale(size / 16F, size / 16F, 1F);
			m = matrixStack.last().pose();

			RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
			buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
			buffer.vertex(m, -1, -1, 0).color(255, 255, 255, 200).uv(0F, 0F).endVertex();
			buffer.vertex(m, -1, 1, 0).color(255, 255, 255, 200).uv(0F, 1F).endVertex();
			buffer.vertex(m, 1, 1, 0).color(255, 255, 255, 200).uv(1F, 1F).endVertex();
			buffer.vertex(m, 1, -1, 0).color(255, 255, 255, 200).uv(1F, 0F).endVertex();
			tessellator.end();

			matrixStack.popPose();
		}

		List<Component> textList = buildMinimapTextData(mc, playerX, playerY, playerZ, dim);
		if (!textList.isEmpty()) {
			// keep font scaling to a multiple of 0.5; anything else looks disgusting
			float s1 = Math.max(1f, (float)Math.round(scale)) / 2f;
			float textHeight = (mc.font.lineHeight + 2) * textList.size() * s1;
			// draw text below minimap if there's room, above otherwise
			float yOff = y + size + textHeight >= scaledHeight ? -textHeight : size + 2f;
			matrixStack.pushPose();
			matrixStack.translate(x + halfSizeD, y + yOff, 0D);
			matrixStack.scale(s1, s1, 1F);

			for (int i = 0; i < textList.size(); i++) {
				FormattedCharSequence text = textList.get(i).getVisualOrderText();
				int textWidth = mc.font.width(text);
				mc.font.drawShadow(matrixStack, text, -textWidth / 2F, i * (mc.font.lineHeight + 2), 0xFFFFFFFF);
			}

			matrixStack.popPose();
		}

		RenderSystem.enableDepthTest();

		if (worldMatrix != null && FTBChunksClientConfig.IN_WORLD_WAYPOINTS.get()) {
			drawInWorldIcons(mc, matrixStack, tickDelta, playerX, playerY, playerZ, scaledWidth, scaledHeight);
		}
	}

	private List<Component> buildMinimapTextData(Minecraft mc, double playerX, double playerY, double playerZ, MapDimension dim) {
		List<Component> res = new ArrayList<>();

		if (FTBChunksClientConfig.MINIMAP_ZONE.get()) {
			MapRegionData data = dim.getRegion(XZ.regionFromChunk(currentPlayerChunkX, currentPlayerChunkZ)).getData();
			if (data != null) {
				ClientTeam team = data.getChunk(XZ.of(currentPlayerChunkX, currentPlayerChunkZ)).getTeam();
				if (team != null) {
					res.add(team.getColoredName());
				}
			}
		}

		if (FTBChunksClientConfig.MINIMAP_XYZ.get()) {
			res.add(Component.literal(Mth.floor(playerX) + " " + Mth.floor(playerY) + " " + Mth.floor(playerZ)));
		}

		if (FTBChunksClientConfig.MINIMAP_BIOME.get()) {
			Holder<Biome> biome = mc.level.getBiome(mc.player.blockPosition());
			biome.unwrapKey().ifPresent(e ->
					res.add(Component.translatable("biome." + e.location().getNamespace() + "." + e.location().getPath()))
			);
		}

		if (FTBChunksClientConfig.DEBUG_INFO.get()) {
			XZ playerXZ = XZ.regionFromChunk(currentPlayerChunkX, currentPlayerChunkZ);
			long memory = MapManager.inst.estimateMemoryUsage();
			res.add(Component.literal("Queued tasks: " + taskQueue.size()));
			res.add(Component.literal("Region: " + playerXZ));
			res.add(Component.literal("Est. Memory: " + StringUtils.formatDouble00(memory / 1024D / 1024D) + " MB"));
			res.add(Component.literal("Total updates: " + renderedDebugCount));

			if (ChunkUpdateTask.debugLastTime > 0L) {
				res.add(Component.literal(String.format("LU: %,d ns", ChunkUpdateTask.debugLastTime)));
			}
		}

		return res;
	}

	private void drawInWorldIcons(Minecraft mc, PoseStack matrixStack, float tickDelta, double playerX, double playerY, double playerZ, int scaledWidth, int scaledHeight) {
		GuiHelper.setupDrawing();
		float ww2 = scaledWidth / 2F;
		float wh2 = scaledHeight / 2F;
		InWorldMapIcon focusedIcon = null;

		for (MapIcon icon : mapIcons) {
			Vec3 pos = icon.getPos(tickDelta);
			double playerDist = MathUtils.dist(pos.x, pos.y, pos.z, playerX, playerY, playerZ);

			if (icon.isVisible(MapType.WORLD_ICON, playerDist, false)) {
				Vector4f v = new Vector4f((float) (pos.x - cameraPos.x), (float) (pos.y - cameraPos.y), (float) (pos.z - cameraPos.z), 1F);
				double lookAngle = mc.player.getLookAngle().dot(new Vec3(v.x(), v.y(), v.z()).normalize());
				if (lookAngle > 0) {  // icon in front of the player
					v.transform(worldMatrix);
					v.perspectiveDivide();
					float ix = ww2 + v.x() * ww2;
					float iy = wh2 - v.y() * wh2;
					double mouseDist = MathUtils.dist(ix, iy, ww2, wh2);
					InWorldMapIcon inWorldMapIcon = new InWorldMapIcon(icon, ix, iy, playerDist, mouseDist);

					if (mouseDist <= 5D && (focusedIcon == null || focusedIcon.distanceToMouse() > mouseDist)) {
						focusedIcon = inWorldMapIcon;
					}

					inWorldMapIcons.add(inWorldMapIcon);
				}
			}
		}

		double fadeStart = FTBChunksClientConfig.WAYPOINT_DOT_FADE_DISTANCE.get();
		double fadeMin = fadeStart * 2D / 3D;

		for (InWorldMapIcon icon : inWorldMapIcons) {
			if (icon.distanceToPlayer() > fadeMin) {
				int iconAlpha = icon.distanceToPlayer() < fadeStart ?
						(int) (255 * ((icon.distanceToPlayer() - fadeMin) / (fadeStart - fadeMin))) :
						255;

				if (iconAlpha > 0) {
					float iconScale = Mth.lerp((50f - Math.min((float)icon.distanceToMouse(), 50f)) / 50f, 0.25f, 0.5f);
					matrixStack.pushPose();
					matrixStack.translate(icon.x(), icon.y(), icon == focusedIcon ? 50F : -100F);
					matrixStack.scale(iconScale, iconScale, 1F);
					icon.icon().draw(MapType.WORLD_ICON, matrixStack, -8, -8, 16, 16, icon != focusedIcon, iconAlpha);
					matrixStack.popPose();
				}
			}
		}

		inWorldMapIcons.clear();
	}

	public void renderWorldLast(PoseStack poseStack, Matrix4f projectionMatrix, Camera camera, float tickDelta) {
		Minecraft mc = Minecraft.getInstance();

		if (mc.options.hideGui || MapManager.inst == null || mc.level == null || mc.player == null
				|| MapDimension.getCurrent() == null || !FTBChunksWorldConfig.playerHasMapStage(mc.player)) {
			return;
		}

		worldMatrix = projectionMatrix.copy();
		worldMatrix.multiply(poseStack.last().pose());
		cameraPos = camera.getPosition();

		if (!FTBChunksClientConfig.IN_WORLD_WAYPOINTS.get()) {
			return;
		}

		List<WaypointMapIcon> visibleWaypoints = findVisibleWaypoints(MapDimension.getCurrent(), mc.player, tickDelta);
		if (visibleWaypoints.isEmpty()) {
			return;
		}

		Vec3 cameraPos = camera.getPosition();
		poseStack.pushPose();
		poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

		VertexConsumer depthBuffer = mc.renderBuffers().bufferSource().getBuffer(FTBChunksRenderTypes.WAYPOINTS_DEPTH);

		float y1 = (float) (cameraPos.y + 30D);
		float y2 = y1 + 70F;

		int yMin = mc.level.getMinBuildHeight();

		for (WaypointMapIcon waypoint : visibleWaypoints) {
			double angle = Math.atan2(cameraPos.z - waypoint.pos.z, cameraPos.x - waypoint.pos.x) * 180D / Math.PI;

			int r = (waypoint.waypoint.color >> 16) & 0xFF;
			int g = (waypoint.waypoint.color >> 8) & 0xFF;
			int b = (waypoint.waypoint.color >> 0) & 0xFF;

			poseStack.pushPose();
			poseStack.translate(waypoint.pos.x, 0, waypoint.pos.z);
			poseStack.mulPose(Vector3f.YP.rotationDegrees((float) (-angle - 135D)));

			float s = 0.6F;

			Matrix4f m = poseStack.last().pose();

			depthBuffer.vertex(m, -s, yMin, s).color(r, g, b, waypoint.alpha).uv(0F, 1F).endVertex();
			depthBuffer.vertex(m, -s, y1, s).color(r, g, b, waypoint.alpha).uv(0F, 0F).endVertex();
			depthBuffer.vertex(m, s, y1, -s).color(r, g, b, waypoint.alpha).uv(1F, 0F).endVertex();
			depthBuffer.vertex(m, s, yMin, -s).color(r, g, b, waypoint.alpha).uv(1F, 1F).endVertex();

			depthBuffer.vertex(m, -s, y1, s).color(r, g, b, waypoint.alpha).uv(0F, 1F).endVertex();
			depthBuffer.vertex(m, -s, y2, s).color(r, g, b, 0).uv(0F, 0F).endVertex();
			depthBuffer.vertex(m, s, y2, -s).color(r, g, b, 0).uv(1F, 0F).endVertex();
			depthBuffer.vertex(m, s, y1, -s).color(r, g, b, waypoint.alpha).uv(1F, 1F).endVertex();

			poseStack.popPose();
		}

		poseStack.popPose();

		mc.renderBuffers().bufferSource().endBatch(FTBChunksRenderTypes.WAYPOINTS_DEPTH);
	}

	private List<WaypointMapIcon> findVisibleWaypoints(MapDimension dim, Player player, float tickDelta) {
		List<WaypointMapIcon> visibleWaypoints = new ArrayList<>();

		double fadeOutDistance = FTBChunksClientConfig.WAYPOINT_BEACON_FADE_DISTANCE.get();
		double fadeOutDistanceP = fadeOutDistance * 2D / 3D;

		for (Waypoint waypoint : dim.getWaypointManager()) {
			if (!waypoint.hidden) {
				double distance = MathUtils.dist(player.getX(), player.getZ(), waypoint.x + 0.5D, waypoint.z + 0.5D);

				if (distance > fadeOutDistanceP && distance <= FTBChunksClientConfig.WAYPOINT_MAX_DISTANCE.get()) {
					int alpha = 150;
					if (distance < fadeOutDistance) {
						alpha = (int) (alpha * ((distance - fadeOutDistanceP) / (fadeOutDistance - fadeOutDistanceP)));
					}

					if (alpha > 0) {
						waypoint.mapIcon.distance = distance;
						waypoint.mapIcon.alpha = alpha;
						visibleWaypoints.add(waypoint.mapIcon);
					}
				}
			}
		}

		visibleWaypoints.sort(new MapIconComparator(player.position(), tickDelta));

		return visibleWaypoints;
	}

	private EventResult screenOpened(Screen screen, ScreenAccess access) {
		if (screen instanceof PauseScreen) {
			nextRegionSave = System.currentTimeMillis() + 60000L;
			saveAllRegions();
		}

		return EventResult.pass();
	}

	private void clientTick(Minecraft mc) {
		MapManager manager = MapManager.inst;

		if (manager != null && mc.level != null) {
			if (mc.player != null) {
				prevPlayerX = currentPlayerX;
				prevPlayerY = currentPlayerY;
				prevPlayerZ = currentPlayerZ;
				currentPlayerX = mc.player.getX();
				currentPlayerY = mc.player.getY();
				currentPlayerZ = mc.player.getZ();
			}

			Level level = Objects.requireNonNull(mc.level);
			
			if (taskQueueTicks % FTBChunksClientConfig.RERENDER_QUEUE_TICKS.get() == 0L) {
				runRerenderTasks(level, manager);
			}

			if (taskQueueTicks % FTBChunksClientConfig.TASK_QUEUE_TICKS.get() == 0L) {
				runQueuedTasks();
			}
			
			int releaseInterval = FTBChunksClientConfig.REGION_RELEASE_TIME.get();
			if (releaseInterval > 0 && level.getGameTime() % (releaseInterval * 20L) == 0) {
				manager.releaseStaleRegionData(releaseInterval * 1000L);
			}

			if (mc.screen == null) {
				manager.checkForRegionPurge();
			}

			taskQueueTicks++;
		}
	}
	
	private static void runRerenderTasks(Level level, MapManager manager) {
		if (!rerenderCache.isEmpty()) {
			long biomeZoomSeed = level.getBiomeManager().biomeZoomSeed;
			rerenderCache.forEach((chunkPos, blocks) -> {
				ChunkAccess chunkAccess = level.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false);
				if (chunkAccess != null) {
					queueOrExecute(new ChunkUpdateTask(manager, level, chunkAccess, chunkPos, biomeZoomSeed, blocks.toIntArray()));
				}
			});
			rerenderCache.clear();
		}
	}

	private static void runQueuedTasks() {
		int nTasks = Math.min(taskQueue.size(), FTBChunksClientConfig.TASK_QUEUE_MAX.get());

		if (nTasks > 0) {
			MapTask[] tasks = new MapTask[nTasks];

			for (int i = 0; i < nTasks; i++) {
				tasks[i] = taskQueue.pollFirst();
				if (tasks[i] == null || tasks[i].cancelOtherTasks()) {
					break;
				}
			}

			for (MapTask task : tasks) {
				if (task != null) {
					try {
						task.runMapTask();
					} catch (Exception ex) {
						FTBChunks.LOGGER.error("Failed to run task " + task);
						ex.printStackTrace();
					}
				}
			}
		}
	}

	private void teamPropertiesChanged(ClientTeamPropertiesChangedEvent event) {
		if (MapManager.inst != null) {
			MapManager.inst.updateAllRegions(false);
		}
	}

	private void mapIcons(MapIconEvent event) {
		Minecraft mc = event.mc;

		if (mc.level == null || mc.player == null) return;

		if (FTBChunksClientConfig.MINIMAP_WAYPOINTS.get()) {
			for (Waypoint w : event.mapDimension.getWaypointManager()) {
				if (!w.hidden || !event.mapType.isMinimap()) {
					event.add(w.mapIcon);
				}
			}
		}

		if (FTBChunksClientConfig.MINIMAP_ENTITIES.get()) {
			for (Entity entity : mc.level.entitiesForRendering()) {
				if (entity instanceof AbstractClientPlayer || entity.getType().getCategory() == MobCategory.MISC || entity.isInvisibleTo(mc.player)) {
					continue;
				}

				Icon icon = EntityIcons.get(entity);

				if (icon != Color4I.EMPTY) {
					if (FTBChunksClientConfig.ONLY_SURFACE_ENTITIES.get() && !mc.level.dimensionType().hasCeiling()) {
						int x = Mth.floor(entity.getX());
						int z = Mth.floor(entity.getZ());
						MapRegion region = event.mapDimension.getRegion(XZ.regionFromBlock(x, z));

						MapRegionData data = region.getData();

						if (data != null) {
							int y = data.height[(x & 511) + (z & 511) * 512];

							if (entity.getY() >= y - 10) {
								event.add(new EntityMapIcon(entity, icon));
							}
						}
					} else {
						event.add(new EntityMapIcon(entity, icon));
					}
				}
			}
		}

		if (FTBChunksClientConfig.MINIMAP_PLAYER_HEADS.get()) {
			if (mc.level.players().size() > 1) {
				for (AbstractClientPlayer player : mc.level.players()) {
					if (player == mc.player || player.isInvisibleTo(mc.player) || !VisibleClientPlayers.isPlayerVisible(player)) {
						continue;
					}

					// this player is tracked by vanilla, so we don't need to track it on long-range tracking
					if (longRangePlayerTracker.remove(player.getUUID()) != null) {
						LargeMapScreen.refreshIconsIfOpen();
					}

					event.add(new EntityMapIcon(player, FaceIcon.getFace(player.getGameProfile())));
				}
			}
			longRangePlayerTracker.forEach((id, icon) -> event.add(icon));
		}

		if (!event.mapType.isMinimap()) {
			event.add(new EntityMapIcon(mc.player, FaceIcon.getFace(mc.player.getGameProfile())));
		}
	}

	public void refreshMinimapIcons() {
		lastMapIconUpdate = 0L;
	}

	private void reloadShaders(ResourceManager resourceManager, ClientReloadShadersEvent.ShadersSink sink) {
	}

	public static void rerender(BlockPos pos) {
		ChunkPos chunkPos = new ChunkPos(pos);
		IntOpenHashSet set = rerenderCache.get(chunkPos);

		if (set == null) {
			set = new IntOpenHashSet();
			rerenderCache.put(chunkPos, set);
		}

		if (set.add((pos.getX() & 15) + ((pos.getZ() & 15) * 16))) {
			if (FTBChunksClientConfig.DEBUG_INFO.get()) {
				renderedDebugCount++;
			}
		}
	}

	public static void handlePacket(ClientboundSectionBlocksUpdatePacket p) {
		SectionPos sectionPos = p.sectionPos;

		short[] positions = p.positions;

		for (short position : positions) {
			rerender(sectionPos.relativeToBlockPos(position));
		}
	}

	public static void handlePacket(ClientboundLevelChunkWithLightPacket p) {
		MapManager manager = MapManager.inst;

		Level level = Minecraft.getInstance().level;

		if (level != null) {
			ChunkAccess chunkAccess = level.getChunk(p.getX(), p.getZ(), ChunkStatus.FULL, false);

			if (chunkAccess != null) {
				long biomeZoomSeed = level.getBiomeManager().biomeZoomSeed;
				queueOrExecute(new ChunkUpdateTask(manager, level, chunkAccess, new ChunkPos(p.getX(), p.getZ()), biomeZoomSeed, ChunkUpdateTask.ALL_BLOCKS));
			}
		}
	}

	public static void queueOrExecute(MapTask task) {
		// Implement this config later
		FTBChunks.EXECUTOR.execute(task);
	}

	public static void handlePacket(ClientboundBlockUpdatePacket p) {
		rerender(p.getPos());
	}

	@Override
	public void maybeClearDeathpoint(Player player) {
		int maxDist = FTBChunksClientConfig.DEATH_WAYPOINT_AUTOREMOVE_DISTANCE.get();
		if (MapManager.inst != null && maxDist > 0 && Minecraft.getInstance().screen == null) {
			WaypointManager wpm = MapManager.inst.getDimension(player.level.dimension()).getWaypointManager();
			wpm.getNearestDeathpoint(player).ifPresent(wp -> {
				if (player.distanceToSqr(wp.x, wp.y, wp.z) < maxDist * maxDist) {
					wpm.remove(wp);
					wpm.getNearestDeathpoint(player).ifPresent(wp1 -> wp1.hidden = false);
					player.displayClientMessage(Component.translatable("ftbchunks.deathpoint_removed", wp.name).withStyle(ChatFormatting.YELLOW), true);
				}
			});
		}
	}

	@Override
	public void updateTrackedPlayerPos(GameProfile profile, BlockPos pos, boolean valid) {
		// called periodically when a player (outside of vanilla entity tracking range) that this client is tracking moves
		boolean changed = false;
		if (!valid) {
			// invalid block pos indicates player should no longer be tracked on this client
			// - player is either no longer in this world, or is now within the vanilla tracking range
			changed = longRangePlayerTracker.remove(profile.getId()) != null;
		} else {
			TrackedPlayerMapIcon icon = longRangePlayerTracker.get(profile.getId());
			if (icon == null) {
				longRangePlayerTracker.put(profile.getId(), new TrackedPlayerMapIcon(profile, Vec3.atCenterOf(pos), FaceIcon.getFace(profile)));
				changed = true;
			} else {
				icon.setPos(Vec3.atCenterOf(pos));
			}
		}
		if (changed) {
			LargeMapScreen.refreshIconsIfOpen();
		}
	}

}
