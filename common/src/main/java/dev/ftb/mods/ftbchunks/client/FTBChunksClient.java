package dev.ftb.mods.ftbchunks.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.*;
import dev.architectury.hooks.client.screen.ScreenAccess;
import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.ftb.mods.ftbchunks.ColorMapLoader;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.event.MapIconEvent;
import dev.ftb.mods.ftbchunks.api.client.icon.MapIcon;
import dev.ftb.mods.ftbchunks.api.client.icon.MapType;
import dev.ftb.mods.ftbchunks.api.client.icon.WaypointIcon;
import dev.ftb.mods.ftbchunks.api.client.waypoint.Waypoint;
import dev.ftb.mods.ftbchunks.client.gui.ChunkScreen;
import dev.ftb.mods.ftbchunks.client.gui.LargeMapScreen;
import dev.ftb.mods.ftbchunks.client.gui.WaypointEditorScreen;
import dev.ftb.mods.ftbchunks.client.map.*;
import dev.ftb.mods.ftbchunks.client.map.color.ColorUtils;
import dev.ftb.mods.ftbchunks.client.mapicon.*;
import dev.ftb.mods.ftbchunks.net.PartialPackets;
import dev.ftb.mods.ftbchunks.net.SendGeneralDataPacket.GeneralChunkData;
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
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.util.StringUtils;
import dev.ftb.mods.ftblibrary.util.client.ClientUtils;
import dev.ftb.mods.ftbteams.api.event.ClientTeamPropertiesChangedEvent;
import dev.ftb.mods.ftbteams.api.event.TeamEvent;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
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
import net.minecraft.server.packs.resources.ResourceProvider;
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
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.*;

import static dev.ftb.mods.ftbchunks.net.SendChunkPacket.SingleChunk;

public enum FTBChunksClient {
	INSTANCE;

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

	public KeyMapping openMapKey;
	public KeyMapping openClaimManagerKey;
	public KeyMapping zoomInKey;
	public KeyMapping zoomOutKey;
	public KeyMapping addWaypointKey;
	public KeyMapping waypointManagerKey;

	private final Map<UUID, TrackedPlayerMapIcon> longRangePlayerTracker = new HashMap<>();

	private long taskQueueTicks = 0L;
	private final Map<ChunkPos, IntOpenHashSet> rerenderCache = new HashMap<>();

	private int minimapTextureId = -1;
	private int currentPlayerChunkX, currentPlayerChunkZ;
	private double currentPlayerX, currentPlayerY, currentPlayerZ;
	private double prevPlayerX, prevPlayerY, prevPlayerZ;
	private int renderedDebugCount = 0;

	private boolean updateMinimapScheduled = false;
	private GeneralChunkData generalChunkData;
	private long nextRegionSave = 0L;
	private double prevZoom = FTBChunksClientConfig.MINIMAP_ZOOM.get();
	private long lastZoomTime = 0L;
	private final List<MapIcon> mapIcons = new ArrayList<>();
	private final List<InWorldMapIcon> inWorldMapIcons = new ArrayList<>();
	private long lastMapIconUpdate = 0L;

	private Matrix4f worldMatrix;
	private Vec3 cameraPos;

	public void init() {
		if (Minecraft.getInstance() == null) {
			return;
		}

		FTBChunksAPI._initClient(new FTBChunksClientAPIImpl());

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
//		RefreshMinimapIconsEvent.EVENT.register(this::refreshMinimapIcons);
		ClientReloadShadersEvent.EVENT.register(this::reloadShaders);
		registerPlatform();
	}

	private void registerKeys() {
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

	public Set<ChunkPos> getPendingRerender() {
		return rerenderCache.keySet();
	}

	@ExpectPlatform
	public static void registerPlatform() {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static boolean doesKeybindMatch(KeyMapping keyMapping, int keyCode, int scanCode, int modifiers) {
		throw new AssertionError();
	}

	public static boolean doesKeybindMatch(KeyMapping keyMapping, Key key) {
		return doesKeybindMatch(keyMapping, key.keyCode, key.scanCode, key.modifiers.modifiers);
	}

	public void openGui() {
		LargeMapScreen.openMap();
	}

	public void scheduleMinimapUpdate() {
		updateMinimapScheduled = true;
	}

	public void handlePlayerLogin(UUID serverId, SNBTCompoundTag config) {
		FTBChunks.LOGGER.info("Loading FTB Chunks client data from world " + serverId);
		FTBChunksWorldConfig.CONFIG.read(config);
		MapManager.startUp(serverId);
		scheduleMinimapUpdate();
		renderedDebugCount = 0;
		ChunkUpdateTask.init();
	}

	public void loggedOut(@Nullable LocalPlayer player) {
		MapManager.shutdown();
	}

	public void updateGeneralData(GeneralChunkData chunkData) {
		generalChunkData = chunkData;
	}

	public void updateChunksFromServer(ResourceKey<Level> dimId, UUID teamId, Collection<SingleChunk> chunks) {
		MapManager.getInstance().ifPresent(manager -> {
			MapDimension dimension = manager.getDimension(dimId);
			Date now = new Date();
			chunks.forEach(c -> ClientTaskQueue.queue(new UpdateChunkFromServerTask(dimension, c, teamId, now)));
		});
	}

	public void syncRegionFromServer(RegionSyncKey key, int offset, int total, byte[] data) {
		PartialPackets.REGION.read(key, offset, total, data);
	}

	public void handlePlayerDeath(GlobalPos pos, int deathNumber) {
		if (FTBChunksClientConfig.DEATH_WAYPOINTS.get() && FTBChunksWorldConfig.playerHasMapStage(Minecraft.getInstance().player)) {
			MapManager.getInstance().ifPresent(manager -> {
				MapDimension dimension = manager.getDimension(pos.dimension());
				for (WaypointImpl w : dimension.getWaypointManager()) {
					if (w.isDeathpoint()) {
						w.setHidden(true);
						w.refreshIcon();
					}
				}

				WaypointImpl deathPoint = new WaypointImpl(WaypointType.DEATH, dimension, pos.pos())
						.setName("Death #" + deathNumber)
						.setColor(0xFF0000);
				dimension.getWaypointManager().add(deathPoint);
			});
		}
	}

	public void handleBlockColorRequest() {
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
					if (mc.hitResult instanceof BlockHitResult hitResult && mc.level != null && mc.player != null) {
						ResourceLocation id = FTBChunks.BLOCK_REGISTRY.getId(mc.level.getBlockState(hitResult.getBlockPos()).getBlock());
						Window window = mc.getWindow();
						try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
							int col = image.getPixelRGBA(image.getWidth() / 2 - (int) (2D * window.getGuiScale()), image.getHeight() / 2 - (int) (2D * window.getGuiScale()));
							String s = String.format("\"%s\": \"#%06X\"", id.getPath(), ColorUtils.convertFromNative(col) & 0xFFFFFF);
							mc.player.displayClientMessage(Component.literal(id.getNamespace() + " - " + s)
									.withStyle(Style.EMPTY.applyFormat(ChatFormatting.GOLD)
											.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, s))
											.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy")))
									), false);
						}
					}
				});
			}, "Color getter").start();
		});
	}

	public void syncLoadedChunkViewFromServer(ResourceKey<Level> dimension, Long2IntMap chunks) {
		MapManager.getInstance().ifPresent(manager -> {
			manager.getDimension(dimension).updateLoadedChunkView(chunks);
			manager.updateAllRegions(false);
		});
	}

	public boolean skipBlock(BlockState state) {
		ResourceLocation id = FTBChunks.BLOCK_REGISTRY.getId(state.getBlock());
		return id == null || ColorMapLoader.getBlockColor(id).isIgnored();
	}

	public EventResult customClick(CustomClickEvent event) {
		if (event.id().equals(BUTTON_ID_MAP)) {
			openGui();
			return EventResult.interruptTrue();
		} else if (event.id().equals(BUTTON_ID_CLAIM)) {
			ChunkScreen.openChunkScreen();
			return EventResult.interruptTrue();
		}

		return EventResult.pass();
	}

	public EventResult keyPressed(Minecraft client, int keyCode, int scanCode, int action, int modifiers) {
		if (action != GLFW.GLFW_PRESS || client.screen != null || !FTBChunksWorldConfig.playerHasMapStage(client.player)) {
			return EventResult.pass();
		}
		if (doesKeybindMatch(openMapKey, keyCode, scanCode, modifiers)) {
			if (Screen.hasControlDown()) {
				SNBTCompoundTag tag = new SNBTCompoundTag();
				tag.putBoolean(FTBChunksClientConfig.MINIMAP_ENABLED.key, !FTBChunksClientConfig.MINIMAP_ENABLED.get());
				FTBChunksClientConfig.MINIMAP_ENABLED.read(tag);
				FTBChunksClientConfig.saveConfig();
			} else if (FTBChunksClientConfig.DEBUG_INFO.get() && Screen.hasAltDown()) {
				ClientTaskQueue.dumpTaskInfo();
			} else {
				openGui();
				return EventResult.interruptTrue();
			}
		} else if (doesKeybindMatch(openClaimManagerKey, keyCode, scanCode, modifiers)) {
			ChunkScreen.openChunkScreen();
			return EventResult.interruptTrue();
		} else if (doesKeybindMatch(zoomInKey, keyCode, scanCode, modifiers)) {
			return changeZoom(true);
		} else if (doesKeybindMatch(zoomOutKey, keyCode, scanCode, modifiers)) {
			return changeZoom(false);
		} else if (doesKeybindMatch(addWaypointKey, keyCode, scanCode, modifiers)) {
			return addQuickWaypoint();
		} else if (doesKeybindMatch(waypointManagerKey, keyCode, scanCode, modifiers)) {
			new WaypointEditorScreen().openGui();
		}

		return EventResult.pass();
	}

	public EventResult keyPressed(Minecraft client, Screen screen, int keyCode, int scanCode, int modifiers) {
		if (doesKeybindMatch(openMapKey, keyCode, scanCode, modifiers)) {
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
		if (player == null) return EventResult.pass();

		return MapManager.getInstance().map(manager -> {
			new EditConfigFromStringScreen<>(name, set -> {
				if (set && !name.getValue().isEmpty()) {
					MapDimension mapDimension = manager.getDimension(player.level().dimension());
					WaypointImpl waypoint = new WaypointImpl(WaypointType.DEFAULT, mapDimension, player.blockPosition())
							.setName(name.getValue())
							.setColor(Color4I.hsb(MathUtils.RAND.nextFloat(), 1F, 1F).rgba());
					mapDimension.getWaypointManager().add(waypoint);
				}
				openGui();
			}).openGuiLater();  // later needed to prevent keypress being passed into gui
			return EventResult.interruptTrue();
		}).orElse(EventResult.pass());
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

	private static long clamp(long val, long min, long max) {
		return Math.min(Math.max(val, min), max);
	}

	public float getZoom() {
		double z = FTBChunksClientConfig.MINIMAP_ZOOM.get();

		if (prevZoom != z) {
			long max = (long) (400D / z);
			long t = clamp(System.currentTimeMillis() - lastZoomTime, 0L, max);

			if (t == max) {
				lastZoomTime = 0L;
				return (float) z;
			}

			return (float) Mth.lerp(t / (double) max, prevZoom, z);
		}

		return (float) z;
	}

	public int generateTextureId(int w, int h) {
		int textureId = TextureUtil.generateTextureId();
		TextureUtil.prepareImage(textureId, w, h);
		return textureId;
	}

	public void renderHud(GuiGraphics graphics, float tickDelta) {
		Minecraft mc = Minecraft.getInstance();

		if (mc.player == null || mc.level == null || MapManager.getInstance().isEmpty() || MapDimension.getCurrent().isEmpty()) {
			return;
		}

		double playerX = Mth.lerp(tickDelta, prevPlayerX, currentPlayerX);
		double playerY = Mth.lerp(tickDelta, prevPlayerY, currentPlayerY);
		double playerZ = Mth.lerp(tickDelta, prevPlayerZ, currentPlayerZ);
		double guiScale = mc.getWindow().getGuiScale();
		int scaledWidth = mc.getWindow().getGuiScaledWidth();
		int scaledHeight = mc.getWindow().getGuiScaledHeight();
		MapDimension dim = MapDimension.getCurrent().get();

		if (dim.dimension != mc.level.dimension()) {
			MapDimension.clearCurrentDimension();
			dim = MapDimension.getCurrent().orElseThrow();
			longRangePlayerTracker.clear();
		}

		long now = System.currentTimeMillis();

		if (nextRegionSave == 0L || now >= nextRegionSave) {
			nextRegionSave = now + 60000L;
			MapManager.getInstance().ifPresent(MapManager::saveAllRegions);
		}

		if (minimapTextureId == -1) {
			minimapTextureId = generateTextureId(FTBChunks.MINIMAP_SIZE, FTBChunks.MINIMAP_SIZE);
			scheduleMinimapUpdate();
		}

		float zoom0 = getZoom();
		float zoom = zoom0 / 3.5F;
		MinimapBlurMode blurMode = FTBChunksClientConfig.MINIMAP_BLUR_MODE.get();
		boolean minimapBlur = blurMode == MinimapBlurMode.AUTO ? (zoom0 < 1.5F) : blurMode == MinimapBlurMode.ON;
		int filter = minimapBlur ? GL11.GL_LINEAR : GL11.GL_NEAREST;

		RenderSystem.bindTextureForSetup(minimapTextureId);
		RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
		RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);

		int cx = Mth.floor(playerX) >> 4;
		int cz = Mth.floor(playerZ) >> 4;

		if (cx != currentPlayerChunkX || cz != currentPlayerChunkZ) {
			scheduleMinimapUpdate();
		}

		if (updateMinimapScheduled) {
			updateMinimapScheduled = false;

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

		RenderSystem.enableDepthTest();

		PoseStack poseStack = graphics.pose();
		poseStack.pushPose();
		poseStack.translate(x + halfSizeD, y + halfSizeD, 490);

		Matrix4f m = poseStack.last().pose();

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
		poseStack.mulPose(Axis.ZP.rotationDegrees(minimapRotation + 180F));

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
		buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
		buffer.vertex(m, -halfSizeF, 0, 0F).color(0, 0, 0, 30).endVertex();
		buffer.vertex(m, halfSizeF, 0, 0F).color(0, 0, 0, 30).endVertex();
		buffer.vertex(m, 0, -halfSizeF, 0F).color(0, 0, 0, 30).endVertex();
		buffer.vertex(m, 0, halfSizeF, 0F).color(0, 0, 0, 30).endVertex();
		tessellator.end();

		poseStack.popPose();

		m = poseStack.last().pose();

		if (FTBChunksClientConfig.MINIMAP_COMPASS.get()) {
			drawMinimapCompassPoints(minimapRotation, size, halfSizeD, x, y, tessellator, buffer, m);
		}

		if (lastMapIconUpdate == 0L || (now - lastMapIconUpdate) >= FTBChunksClientConfig.MINIMAP_ICON_UPDATE_TIMER.get()) {
			lastMapIconUpdate = now;

			mapIcons.clear();
			MapIconEvent.MINIMAP.invoker().accept(new MapIconEvent(dim.dimension, mapIcons, MapType.MINIMAP));

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

			poseStack.pushPose();
			poseStack.translate(wx - ws, wy - ws - (icon.isIconOnEdge(MapType.MINIMAP, d >= halfSizeD) ? ws / 2D : 0D), 0);
			poseStack.scale(wsf, wsf, 1F);
			icon.draw(MapType.MINIMAP, graphics, 0, 0, 1, 1, d >= halfSizeD, 255);
			poseStack.popPose();
		}

		if (rotationLocked || FTBChunksClientConfig.SHOW_PLAYER_WHEN_UNLOCKED.get()) {
			// pointer icon at the map centre (player position)
			RenderSystem.setShaderTexture(0, PLAYER);
			poseStack.pushPose();
			poseStack.translate(x + halfSizeD, y + halfSizeD, 0);
			if (rotationLocked) {
				poseStack.mulPose(Axis.ZP.rotationDegrees(mc.player.getYRot() + 180F));
			}
			poseStack.scale(size / 16F, size / 16F, 1F);
			m = poseStack.last().pose();

			RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
			buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
			buffer.vertex(m, -1, -1, 0).color(255, 255, 255, 200).uv(0F, 0F).endVertex();
			buffer.vertex(m, -1, 1, 0).color(255, 255, 255, 200).uv(0F, 1F).endVertex();
			buffer.vertex(m, 1, 1, 0).color(255, 255, 255, 200).uv(1F, 1F).endVertex();
			buffer.vertex(m, 1, -1, 0).color(255, 255, 255, 200).uv(1F, 0F).endVertex();
			tessellator.end();

			poseStack.popPose();
		}

		List<Component> textList = buildMinimapTextData(mc, playerX, playerY, playerZ, dim);
		if (!textList.isEmpty()) {
			float fontScale = FTBChunksClientConfig.MINIMAP_FONT_SCALE.get().floatValue();
			float textHeight = (mc.font.lineHeight + 2) * textList.size() * fontScale;
			// draw text below minimap if there's room, above otherwise
			float yOff = y + size + textHeight >= scaledHeight ? -textHeight : size + 2f;
			poseStack.pushPose();
			poseStack.translate(x + halfSizeD, y + yOff, 0D);
			poseStack.scale(fontScale, fontScale, 1F);

			for (int i = 0; i < textList.size(); i++) {
				FormattedCharSequence text = textList.get(i).getVisualOrderText();
				int textWidth = mc.font.width(text);
				graphics.drawString(mc.font, text, -textWidth / 2, i * (mc.font.lineHeight + 2), 0xFFFFFFFF, true);
			}

			poseStack.popPose();
		}

		RenderSystem.enableDepthTest();

		if (worldMatrix != null && FTBChunksClientConfig.IN_WORLD_WAYPOINTS.get()) {
			drawInWorldIcons(mc, graphics, tickDelta, playerX, playerY, playerZ, scaledWidth, scaledHeight);
		}
	}

	private void drawMinimapCompassPoints(float minimapRotation, int size, double halfSizeD, int minimapX, int minimapY, Tesselator tessellator, BufferBuilder buffer, Matrix4f m) {
		// compass letters at the 4 cardinal points
		double d = size / 2.2D;
		float ws = size / 32F;
		for (int face = 0; face < 4; face++) {
			double angle = (minimapRotation + 180D - face * 90D) * Math.PI / 180D;

			float wx = (float) (minimapX + halfSizeD + Math.cos(angle) * d);
			float wy = (float) (minimapY + halfSizeD + Math.sin(angle) * d);

			RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
			RenderSystem.setShaderTexture(0, COMPASS[face]);
			buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
			buffer.vertex(m, wx - ws, wy - ws, 0).color(255, 255, 255, 255).uv(0F, 0F).endVertex();
			buffer.vertex(m, wx - ws, wy + ws, 0).color(255, 255, 255, 255).uv(0F, 1F).endVertex();
			buffer.vertex(m, wx + ws, wy + ws, 0).color(255, 255, 255, 255).uv(1F, 1F).endVertex();
			buffer.vertex(m, wx + ws, wy - ws, 0).color(255, 255, 255, 255).uv(1F, 0F).endVertex();
			tessellator.end();
		}
	}

	private List<Component> buildMinimapTextData(Minecraft mc, double playerX, double playerY, double playerZ, MapDimension dim) {
		List<Component> res = new ArrayList<>();

		if (FTBChunksClientConfig.MINIMAP_ZONE.get()) {
			MapRegionData data = dim.getRegion(XZ.regionFromChunk(currentPlayerChunkX, currentPlayerChunkZ)).getData();
			if (data != null) {
				data.getChunk(XZ.of(currentPlayerChunkX, currentPlayerChunkZ)).getTeam()
						.ifPresent(team -> res.add(team.getColoredName()));
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
			long memory = MapManager.getInstance().map(MapManager::estimateMemoryUsage).orElse(0L);
			res.add(Component.literal("TQ: " + ClientTaskQueue.queueSize()).withStyle(ChatFormatting.GRAY));
			res.add(Component.literal("Rgn: " + playerXZ).withStyle(ChatFormatting.GRAY));
			res.add(Component.literal("Mem: ~" + StringUtils.formatDouble00(memory / 1024D / 1024D) + " MB").withStyle(ChatFormatting.GRAY));
			res.add(Component.literal("Updates: " + renderedDebugCount).withStyle(ChatFormatting.GRAY));
			if (ChunkUpdateTask.getDebugLastTime() > 0L) {
				res.add(Component.literal(String.format("Last: %,d ns", ChunkUpdateTask.getDebugLastTime())).withStyle(ChatFormatting.GRAY));
			}
		}

		return res;
	}

	private void drawInWorldIcons(Minecraft mc, GuiGraphics graphics, float tickDelta, double playerX, double playerY, double playerZ, int scaledWidth, int scaledHeight) {
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
					worldMatrix.transform(v);
					v.div(v.w());
					float ix = ww2 + v.x() * ww2;
					float iy = wh2 - v.y() * wh2;
					double mouseDist = MathUtils.dist(ix, iy, ww2, wh2);
					InWorldMapIcon inWorldMapIcon = new InWorldMapIcon(icon, ix, iy, playerDist, mouseDist);

					if (mouseDist <= 5D * FTBChunksClientConfig.WAYPOINT_FOCUS_DISTANCE.get() && (focusedIcon == null || focusedIcon.distanceToMouse() > mouseDist)) {
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

				float minSize = 0.25f;
				float maxSize = (float) (minSize * FTBChunksClientConfig.WAYPOINT_FOCUS_SCALE.get());
				if (iconAlpha > 0) {
					float iconScale = Mth.lerp((50f - Math.min((float)icon.distanceToMouse(), 50f)) / 50f, minSize, maxSize);
					PoseStack poseStack = graphics.pose();
					poseStack.pushPose();
					poseStack.translate(icon.x(), icon.y(), icon == focusedIcon ? 50F : -100F);
					poseStack.scale(iconScale, iconScale, 1F);
					icon.icon().draw(MapType.WORLD_ICON, graphics, -8, -8, 16, 16, icon != focusedIcon, iconAlpha);
					poseStack.popPose();
				}
			}
		}

		inWorldMapIcons.clear();
	}

	public void renderWorldLast(PoseStack poseStack, Matrix4f projectionMatrix, Camera camera, float tickDelta) {
		Minecraft mc = Minecraft.getInstance();

		if (mc.options.hideGui || MapManager.getInstance().isEmpty() || mc.level == null || mc.player == null
				|| MapDimension.getCurrent().isEmpty() || !FTBChunksWorldConfig.playerHasMapStage(mc.player)) {
			return;
		}

		worldMatrix = new Matrix4f(projectionMatrix);
		worldMatrix.mul(poseStack.last().pose());
		cameraPos = camera.getPosition();

		if (!FTBChunksClientConfig.IN_WORLD_WAYPOINTS.get()) {
			return;
		}

		List<WaypointIcon> visibleWaypoints = findVisibleWaypoints(mc.player, tickDelta);
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

		for (WaypointIcon waypoint : visibleWaypoints) {
			drawWaypointBeacon(poseStack, cameraPos, depthBuffer, y1, y2, yMin, waypoint);
		}

		poseStack.popPose();

		mc.renderBuffers().bufferSource().endBatch(FTBChunksRenderTypes.WAYPOINTS_DEPTH);
	}

	private static void drawWaypointBeacon(PoseStack poseStack, Vec3 cameraPos, VertexConsumer depthBuffer, float y1, float y2, int yMin, WaypointIcon waypoint) {
		Vec3 pos = waypoint.getPos(1f);
		int alpha = waypoint.getAlpha();
		double angle = Math.atan2(cameraPos.z - pos.z, cameraPos.x - pos.x) * 180D / Math.PI;

		int r = waypoint.getColor().redi();
		int g = waypoint.getColor().greeni();
		int b = waypoint.getColor().bluei();

		poseStack.pushPose();
		poseStack.translate(pos.x, 0, pos.z);
		poseStack.mulPose(Axis.YP.rotationDegrees((float) (-angle - 135D)));

		float s = 0.6F;

		Matrix4f m = poseStack.last().pose();

		depthBuffer.vertex(m, -s, yMin, s).color(r, g, b, alpha).uv(0F, 1F).endVertex();
		depthBuffer.vertex(m, -s, y1, s).color(r, g, b, alpha).uv(0F, 0F).endVertex();
		depthBuffer.vertex(m, s, y1, -s).color(r, g, b, alpha).uv(1F, 0F).endVertex();
		depthBuffer.vertex(m, s, yMin, -s).color(r, g, b, alpha).uv(1F, 1F).endVertex();

		depthBuffer.vertex(m, -s, y1, s).color(r, g, b, alpha).uv(0F, 1F).endVertex();
		depthBuffer.vertex(m, -s, y2, s).color(r, g, b, 0).uv(0F, 0F).endVertex();
		depthBuffer.vertex(m, s, y2, -s).color(r, g, b, 0).uv(1F, 0F).endVertex();
		depthBuffer.vertex(m, s, y1, -s).color(r, g, b, alpha).uv(1F, 1F).endVertex();

		poseStack.popPose();
	}

	private List<WaypointIcon> findVisibleWaypoints(Player player, float tickDelta) {
		return MapManager.getInstance().map(manager -> {
			List<WaypointIcon> visibleWaypoints = new ArrayList<>();

			double fadeOutDistance = FTBChunksClientConfig.WAYPOINT_BEACON_FADE_DISTANCE.get();
			double fadeOutDistanceP = fadeOutDistance * 2D / 3D;

			MapDimension dim = manager.getDimension(player.level().dimension());
			for (Waypoint waypoint : dim.getWaypointManager()) {
				if (!waypoint.isHidden()) {
					double distance = Math.sqrt(waypoint.getDistanceSq(player));

					if (distance > fadeOutDistanceP && distance <= FTBChunksClientConfig.WAYPOINT_MAX_DISTANCE.get()) {
						int alpha = 150;
						if (distance < fadeOutDistance) {
							alpha = (int) (alpha * ((distance - fadeOutDistanceP) / (fadeOutDistance - fadeOutDistanceP)));
						}
						if (alpha > 0) {
							waypoint.getMapIcon().setAlpha(alpha);
							visibleWaypoints.add(waypoint.getMapIcon());
						}
					}
				}
			}

			visibleWaypoints.sort(new MapIconComparator(player.position(), tickDelta));

			return visibleWaypoints;
		}).orElse(List.of());
	}

	private EventResult screenOpened(Screen screen, ScreenAccess access) {
		if (screen instanceof PauseScreen) {
			nextRegionSave = System.currentTimeMillis() + 60000L;
			MapManager.getInstance().ifPresent(MapManager::saveAllRegions);
		}

		return EventResult.pass();
	}

	private void clientTick(Minecraft mc) {
		if (mc.level == null) return;

		MapManager.getInstance().ifPresent(manager -> {
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
				ClientTaskQueue.runQueuedTasks();
			}

			int releaseInterval = FTBChunksClientConfig.REGION_RELEASE_TIME.get();
			if (releaseInterval > 0 && level.getGameTime() % (releaseInterval * 20L) == 0) {
				manager.releaseStaleRegionData(releaseInterval * 1000L);
			}

			if (mc.screen == null) {
				manager.checkForRegionPurge();
			}

			taskQueueTicks++;
		});
	}

	private void runRerenderTasks(Level level, MapManager manager) {
		if (!rerenderCache.isEmpty()) {
			rerenderCache.forEach((chunkPos, blocks) -> {
				ChunkAccess chunkAccess = level.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false);
				if (chunkAccess != null) {
					queueOrExecute(new ChunkUpdateTask(manager, level, chunkAccess, chunkPos, blocks.toIntArray()));
				}
			});
			rerenderCache.clear();
		}
	}

	private void teamPropertiesChanged(ClientTeamPropertiesChangedEvent event) {
		MapManager.getInstance().ifPresent(manager -> manager.updateAllRegions(false));
	}

	private void mapIcons(MapIconEvent event) {
		Minecraft mc = Minecraft.getInstance();

		if (mc.level == null || mc.player == null) return;

		if (FTBChunksClientConfig.MINIMAP_WAYPOINTS.get()) {
			MapDimension.getCurrent().ifPresent(mapDimension -> {
				for (Waypoint w : mapDimension.getWaypointManager()) {
					if (!w.isHidden() || !event.getMapType().isMinimap()) {
						event.add(w.getMapIcon());
					}
				}
			});
		}

		if (FTBChunksClientConfig.MINIMAP_ENTITIES.get()) {
			for (Entity entity : mc.level.entitiesForRendering()) {
				if (entity instanceof AbstractClientPlayer || entity.getType().getCategory() == MobCategory.MISC || entity.isInvisibleTo(mc.player)) {
					continue;
				}

				Icon icon = EntityIcons.get(entity);

				if (!icon.isEmpty()) {
					if (FTBChunksClientConfig.ONLY_SURFACE_ENTITIES.get() && !mc.level.dimensionType().hasCeiling()) {
						MapDimension.getCurrent().ifPresent(mapDimension -> {
							int x = Mth.floor(entity.getX());
							int z = Mth.floor(entity.getZ());
							MapRegion region = mapDimension.getRegion(XZ.regionFromBlock(x, z));
							MapRegionData data = region.getData();
							if (data != null) {
								int y = data.height[(x & 511) + (z & 511) * 512];
								if (entity.getY() >= y - 10) {
									event.add(new EntityMapIcon(entity, icon));
								}
							}
						});
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

		if (!event.getMapType().isMinimap()) {
			event.add(new EntityMapIcon(mc.player, FaceIcon.getFace(mc.player.getGameProfile())));
		}
	}

	void refreshMinimapIcons() {
		lastMapIconUpdate = 0L;
	}

	private void reloadShaders(ResourceProvider resourceProvider, ClientReloadShadersEvent.ShadersSink sink) {
	}

	public void rerender(BlockPos pos) {
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

	public void handlePacket(ClientboundSectionBlocksUpdatePacket p) {
		SectionPos sectionPos = p.sectionPos;

		short[] positions = p.positions;

		for (short position : positions) {
			rerender(sectionPos.relativeToBlockPos(position));
		}
	}

	public void handlePacket(ClientboundLevelChunkWithLightPacket p) {
		MapManager manager = MapManager.getInstance().orElse(null);
		Level level = Minecraft.getInstance().level;

		if (level != null) {
			ChunkAccess chunkAccess = level.getChunk(p.getX(), p.getZ(), ChunkStatus.FULL, false);
			if (chunkAccess != null) {
				queueOrExecute(new ChunkUpdateTask(manager, level, chunkAccess, new ChunkPos(p.getX(), p.getZ())));
			}
		}
	}

	public void queueOrExecute(MapTask task) {
		// Implement this config later
		FTBChunks.EXECUTOR.execute(task);
	}

	public void handlePacket(ClientboundBlockUpdatePacket p) {
		rerender(p.getPos());
	}

	public void maybeClearDeathpoint(Player player) {
		int maxDist = FTBChunksClientConfig.DEATH_WAYPOINT_AUTOREMOVE_DISTANCE.get();
		MapManager.getInstance().ifPresent(manager -> {
			if (maxDist > 0 && Minecraft.getInstance().screen == null) {
				WaypointManagerImpl wpm = manager.getDimension(player.level().dimension()).getWaypointManager();
				wpm.getNearestDeathpoint(player).ifPresent(wp -> {
					if (wp.getDistanceSq(player) < maxDist * maxDist) {
						wpm.remove(wp);
						wpm.getNearestDeathpoint(player).ifPresent(wp1 -> wp1.setHidden(false));
						player.displayClientMessage(Component.translatable("ftbchunks.deathpoint_removed", wp.getName()).withStyle(ChatFormatting.YELLOW), true);
					}
				});
			}
		});
	}

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

	public List<Component> getChunkSummary() {
		if (generalChunkData == null) return List.of();

		List<Component> list = new ArrayList<>(4);

		list.add(Component.translatable("ftbchunks.gui.claimed"));
		int claimed = generalChunkData.claimed();
		int maxClaim = generalChunkData.maxClaimChunks();
		list.add(Component.literal(claimed + " / " + maxClaim)
				.withStyle(claimed > maxClaim ? ChatFormatting.RED : claimed == maxClaim ? ChatFormatting.YELLOW : ChatFormatting.GREEN));

		list.add(Component.translatable("ftbchunks.gui.force_loaded"));
		int loaded = generalChunkData.loaded();
		int maxLoaded = generalChunkData.maxForceLoadChunks();
		list.add(Component.literal(loaded + " / " + maxLoaded)
				.withStyle(loaded > maxLoaded ? ChatFormatting.RED : loaded == maxLoaded ? ChatFormatting.YELLOW : ChatFormatting.GREEN));

		return list;
	}

	public int getMinimapTextureId() {
		return minimapTextureId;
	}
}
