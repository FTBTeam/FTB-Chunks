package dev.ftb.mods.ftbchunks.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.*;
import dev.architectury.hooks.client.screen.ScreenAccess;
import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.platform.Platform;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.ftb.mods.ftbchunks.ColorMapLoader;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.FTBChunksClientAPI;
import dev.ftb.mods.ftbchunks.api.client.event.MapIconEvent;
import dev.ftb.mods.ftbchunks.api.client.event.WaypointManagerEvent;
import dev.ftb.mods.ftbchunks.api.client.icon.MapIcon;
import dev.ftb.mods.ftbchunks.api.client.icon.MapType;
import dev.ftb.mods.ftbchunks.api.client.waypoint.Waypoint;
import dev.ftb.mods.ftbchunks.api.client.waypoint.WaypointManager;
import dev.ftb.mods.ftbchunks.client.gui.*;
import dev.ftb.mods.ftbchunks.client.map.*;
import dev.ftb.mods.ftbchunks.client.map.color.ColorUtils;
import dev.ftb.mods.ftbchunks.client.mapicon.EntityIconUtils;
import dev.ftb.mods.ftbchunks.client.mapicon.EntityMapIcon;
import dev.ftb.mods.ftbchunks.client.mapicon.InWorldMapIcon;
import dev.ftb.mods.ftbchunks.client.minimap.MinimapRenderer;
import dev.ftb.mods.ftbchunks.client.minimap.components.*;
import dev.ftb.mods.ftbchunks.data.ChunkSyncInfo;
import dev.ftb.mods.ftbchunks.net.PartialPackets;
import dev.ftb.mods.ftbchunks.net.SendGeneralDataPacket.GeneralChunkData;
import dev.ftb.mods.ftblibrary.client.config.editable.EditableString;
import dev.ftb.mods.ftblibrary.client.gui.CustomClickEvent;
import dev.ftb.mods.ftblibrary.client.gui.widget.BaseScreen;
import dev.ftb.mods.ftblibrary.client.util.ClientUtils;
import dev.ftb.mods.ftblibrary.icon.EntityIconLoader;
import dev.ftb.mods.ftblibrary.icon.FaceIcon;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.util.ModUtils;
import dev.ftb.mods.ftbteams.api.event.ClientTeamPropertiesChangedEvent;
import dev.ftb.mods.ftbteams.api.event.TeamEvent;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TODO: Split into classes, this file is way over sized!
 *       - Split the minimap into render layers that can be registered via an API
 *          - This would split out the various layers, background, icons, borders, etc.
 */
public enum FTBChunksClient {
    INSTANCE;

    public static final ExecutorService MAP_EXECUTOR = Executors.newSingleThreadExecutor();

    private static final Identifier BUTTON_ID_MAP = FTBChunksAPI.id("open_gui");
    private static final Identifier BUTTON_ID_CLAIM = FTBChunksAPI.id("open_claim_gui");

    private static final KeyMapping.Category KEY_CATEGORY = new KeyMapping.Category(FTBChunksAPI.id("keys"));

    @Nullable
    public KeyMapping openMapKey;
    @Nullable
    public KeyMapping toggleMinimapKey;
    @Nullable
    public KeyMapping openClaimManagerKey;
    @Nullable
    public KeyMapping zoomInKey;
    @Nullable
    public KeyMapping zoomOutKey;
    @Nullable
    public KeyMapping addWaypointKey;
    @Nullable
    public KeyMapping waypointManagerKey;

    private long taskQueueTicks = 0L;
    private final Map<ChunkPos, IntOpenHashSet> rerenderCache = new HashMap<>();
    private int renderedDebugCount = 0;
    private GeneralChunkData generalChunkData = GeneralChunkData.NONE;
    private long nextRegionSave = 0L;
    private final List<InWorldMapIcon> inWorldMapIcons = new ArrayList<>();
    private Vec3 cameraPos = Vec3.ZERO;
    @Nullable
    private Matrix4f worldMatrix;
    @Nullable
    private Matrix4f savedProjectionMatrix;
    private final MinimapRenderer minimapRenderer = new MinimapRenderer();
    private final LongRangePlayerTrackerClient longRangePlayerTracker = new LongRangePlayerTrackerClient();

    public void init() {
        FTBChunksAPI._initClient(new FTBChunksClientAPIImpl());

        registerKeys();

        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, new ColorMapLoader(), FTBChunksAPI.id("colormap"));
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(this::loggedOut);
        CustomClickEvent.EVENT.register(this::customClick);
        ClientRawInputEvent.KEY_PRESSED.register(this::keyPressed);
        ClientScreenInputEvent.KEY_PRESSED_PRE.register(this::keyPressed);
        ClientGuiEvent.RENDER_HUD.register(this::renderHud);
        ClientGuiEvent.INIT_PRE.register(this::screenOpened);
        ClientTickEvent.CLIENT_PRE.register(this::clientTick);
        TeamEvent.CLIENT_PROPERTIES_CHANGED.register(this::teamPropertiesChanged);
        MapIconEvent.LARGE_MAP.register(this::gatherMapIcons);
        MapIconEvent.MINIMAP.register(this::gatherMapIcons);

        registerPlatform();

        if (ModUtils.isDevMode()) {
            WaypointManagerEvent.AVAILABLE.register(this::waypointManagerAvailable);
        }

        // Register minimap components
        FTBChunksClientAPI clientApi = FTBChunksAPI.clientApi();
        clientApi.registerMinimapComponent(new PlayerPosInfoComponent());
        clientApi.registerMinimapComponent(new ZoneInfoComponent());
        clientApi.registerMinimapComponent(new BiomeComponent());
        clientApi.registerMinimapComponent(new GameTimeComponent());
        clientApi.registerMinimapComponent(new RealTimeComponent());
        clientApi.registerMinimapComponent(new FPSComponent());
        clientApi.registerMinimapComponent(new DebugComponent());

        ClientLifecycleEvent.CLIENT_STARTED.register(this::clientStarted);
    }

    private void waypointManagerAvailable(WaypointManager mgr) {
        // only called in dev mode, testing transient waypoints
        if (ClientUtils.getClientLevel().dimension().equals(Level.OVERWORLD)) {
            mgr.addTransientWaypointAt(new BlockPos(0, 65, 0), "Transient Dev-Mode Waypoint").setColor(0x808080);
        }
    }

    private void clientStarted(Minecraft minecraft) {
        minimapRenderer.setupComponents();
    }

    private void registerKeys() {
        // Keybinding to open Large map screen
        openMapKey = new KeyMapping("key.ftbchunks.map", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, KEY_CATEGORY);
        KeyMappingRegistry.register(openMapKey);

        // Keybinding to toggle the minimap
        toggleMinimapKey = new KeyMapping("key.ftbchunks.toggle_minimap", InputConstants.Type.KEYSYM, -1, KEY_CATEGORY);
        KeyMappingRegistry.register(toggleMinimapKey);

        // Keybinding to open claim manager screen
        openClaimManagerKey = new KeyMapping("key.ftbchunks.claim_manager", InputConstants.Type.KEYSYM, -1, KEY_CATEGORY);
        KeyMappingRegistry.register(openClaimManagerKey);

        // Keybindings to zoom in minimap
        zoomInKey = new KeyMapping("key.ftbchunks.minimap.zoomIn", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_EQUAL, KEY_CATEGORY);
        KeyMappingRegistry.register(zoomInKey);

        zoomOutKey = new KeyMapping("key.ftbchunks.minimap.zoomOut", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_MINUS, KEY_CATEGORY);
        KeyMappingRegistry.register(zoomOutKey);

        // Keybinding to quick-add waypoint at current position
        addWaypointKey = new KeyMapping("key.ftbchunks.add_waypoint", InputConstants.Type.KEYSYM, -1, KEY_CATEGORY);
        KeyMappingRegistry.register(addWaypointKey);

        // Keybinding to open the waypoint manager screen
        waypointManagerKey = new KeyMapping("key.ftbchunks.waypoint_manager", InputConstants.Type.KEYSYM, -1, KEY_CATEGORY);
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
    public static boolean doesKeybindMatch(@Nullable KeyMapping keyMapping, KeyEvent keyEvent) {
        throw new AssertionError();
    }

    public void openGui() {
        LargeMapScreen.openMap();
    }

    public void handlePlayerLogin(UUID serverId) {
        FTBChunks.LOGGER.info("Loading FTB Chunks client data from world {}", serverId);
        MapManager.startUp(serverId);
        minimapRenderer.requestTextureRefresh();
        renderedDebugCount = 0;
        ChunkUpdateTask.init();
    }

    public void loggedOut(@Nullable LocalPlayer player) {
        MapManager.shutdown();
    }

    public void updateGeneralData(GeneralChunkData chunkData) {
        generalChunkData = chunkData;
    }

    public void updateChunksFromServer(ResourceKey<Level> dimId, UUID teamId, Collection<ChunkSyncInfo> chunkSyncInfoList) {
        MapManager.getInstance().ifPresent(manager -> {
            MapDimension dimension = manager.getDimension(dimId);
            Date now = new Date();
            chunkSyncInfoList.forEach(chunkSyncInfo -> ClientTaskQueue.queue(new UpdateChunkFromServerTask(dimension, chunkSyncInfo, teamId, now)));
        });
    }

    public void syncRegionFromServer(RegionSyncKey key, int offset, int total, byte[] data) {
        PartialPackets.REGION.read(key, offset, total, data);
    }

    public void handlePlayerDeath(GlobalPos pos, int deathNumber) {
        if (FTBChunksClientConfig.DEATH_WAYPOINTS.get() && FTBChunksWorldConfig.playerHasMapStage(ClientUtils.getClientPlayer())) {
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
                        Identifier id = FTBChunks.BLOCK_REGISTRY.getId(mc.level.getBlockState(hitResult.getBlockPos()).getBlock());
                        Window window = mc.getWindow();
                        Screenshot.takeScreenshot(mc.getMainRenderTarget(), image -> {
                            int col = image.getPixel(image.getWidth() / 2 - (int) (2D * window.getGuiScale()), image.getHeight() / 2 - (int) (2D * window.getGuiScale()));
                            String s = String.format("\"%s\": \"#%06X\"", id.getPath(), ColorUtils.convertFromNative(col) & 0xFFFFFF);
                            mc.player.displayClientMessage(Component.literal(id.getNamespace() + " - " + s)
                                    .withStyle(Style.EMPTY.applyFormat(ChatFormatting.GOLD)
                                            .withClickEvent(new ClickEvent.CopyToClipboard(s))
                                            .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy")))
                                    ), false);
                        });
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
        Identifier id = FTBChunks.BLOCK_REGISTRY.getId(state.getBlock());
        return id == null || ColorMapLoader.getBlockColor(id).isIgnored();
    }

    public EventResult customClick(CustomClickEvent event) {
        if (FTBChunksWorldConfig.playerHasMapStage(ClientUtils.getClientPlayer())) {
            if (event.id().equals(BUTTON_ID_MAP)) {
                openGui();
                return EventResult.interruptTrue();
            } else if (event.id().equals(BUTTON_ID_CLAIM)) {
                ChunkScreen.openChunkScreen();
                return EventResult.interruptTrue();
            }
        }

        return EventResult.pass();
    }

    public EventResult keyPressed(Minecraft client, int action, KeyEvent keyEvent) {
        if (action != GLFW.GLFW_PRESS
                || client.screen != null
                || !FTBChunksWorldConfig.playerHasMapStage(ClientUtils.getClientPlayer())
                || InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), InputConstants.KEY_F3))
        {
            return EventResult.pass();
        }

        if (doesKeybindMatch(openMapKey, keyEvent)) {
            openGui();
            return EventResult.interruptTrue();
        } else if (doesKeybindMatch(toggleMinimapKey, keyEvent)) {
            FTBChunksClientConfig.MINIMAP_ENABLED.set(!FTBChunksClientConfig.MINIMAP_ENABLED.get());
            FTBChunksClientConfig.saveConfig();
            return EventResult.interruptTrue();
        } else if (doesKeybindMatch(openClaimManagerKey, keyEvent)) {
            try {
                ChunkScreen.openChunkScreen();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return EventResult.interruptTrue();
        } else if (doesKeybindMatch(zoomInKey, keyEvent)) {
            minimapRenderer.changeZoom(true);
            return EventResult.interruptTrue();
        } else if (doesKeybindMatch(zoomOutKey, keyEvent)) {
            minimapRenderer.changeZoom(false);
            return EventResult.interruptTrue();
        } else if (doesKeybindMatch(addWaypointKey, keyEvent)) {
            return addQuickWaypoint();
        } else if (doesKeybindMatch(waypointManagerKey, keyEvent)) {
            new WaypointEditorScreen().openGui();
            return EventResult.interruptTrue();
        }

        return EventResult.pass();
    }

    public EventResult keyPressed(Minecraft client, Screen screen, KeyEvent keyEvent) {
        if (doesKeybindMatch(openMapKey, keyEvent) && Platform.isFabric()) {
            // platform specific behaviour :(  why? ¯\_(ツ)_/¯
            LargeMapScreen gui = ClientUtils.getCurrentGuiAs(LargeMapScreen.class);
            if (gui != null && !gui.anyModalPanelOpen()) {
                gui.closeGui(false);
                return EventResult.interruptTrue();
            }
        }

        return EventResult.pass();
    }

    private EventResult addQuickWaypoint() {
        EditableString name = new EditableString();
        Player player = Minecraft.getInstance().player;
        if (player == null) return EventResult.pass();

        return MapManager.getInstance().map(manager -> {
            BaseScreen screen = new WaypointAddScreen(name, player);
            screen.openGuiLater();
            // later needed to prevent keypress being passed into gui
            return EventResult.interruptTrue();
        }).orElse(EventResult.pass());
    }

    public void renderHud(GuiGraphics graphics, DeltaTracker tickDelta) {
        minimapRenderer.render(graphics, tickDelta);

        if (FTBChunksClientConfig.IN_WORLD_WAYPOINTS.get()) {
            renderInWorldIcons(graphics, tickDelta, ClientUtils.getClientPlayer().position());
        }
    }

    private void renderInWorldIcons(GuiGraphics graphics, DeltaTracker tickDelta, Vec3 playerPos) {
        int scaledWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int scaledHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        if (worldMatrix == null || cameraPos == null) {
            return;
        }

        float scaledWidth2 = scaledWidth / 2F;
        float scaledHeight2 = scaledHeight / 2F;
        InWorldMapIcon focusedIcon = null;

        Player player = ClientUtils.getClientPlayer();
        for (MapIcon icon : minimapRenderer.getMapIcons()) {
            Vec3 pos = icon.getPos(tickDelta.getGameTimeDeltaPartialTick(false));
            double playerDist = pos.distanceTo(playerPos);

            if (icon.isVisible(MapType.WORLD_ICON, playerDist, false)) {
                Vector4f v = new Vector4f((float) (pos.x - cameraPos.x), (float) (pos.y - cameraPos.y), (float) (pos.z - cameraPos.z), 1F);
                double lookAngle = player.getLookAngle().dot(new Vec3(v.x(), v.y(), v.z()).normalize());
                if (lookAngle > 0) {  // icon in front of the player
                    worldMatrix.transform(v);
                    v.div(v.w());
                    float ix = scaledWidth2 + v.x() * scaledWidth2;
                    float iy = scaledHeight2 - v.y() * scaledHeight2;
                    double mouseDist = MathUtils.dist(ix, iy, scaledWidth2, scaledHeight2);
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
                    float iconScale = Mth.lerp((50f - Math.min((float) icon.distanceToMouse(), 50f)) / 50f, minSize, maxSize);
                    Matrix3x2fStack poseStack = graphics.pose();
                    poseStack.pushMatrix();
                    poseStack.translate(icon.x(), icon.y());
                    poseStack.scale(iconScale, iconScale);
                    icon.icon().draw(MapType.WORLD_ICON, graphics, -8, -8, 16, 16, icon != focusedIcon, iconAlpha);
                    poseStack.popMatrix();
                }
            }
        }

        inWorldMapIcons.clear();
    }

    public void copyProjectionMatrix(Matrix4f projectionMatrix) {
        savedProjectionMatrix = new Matrix4f(projectionMatrix);
    }

    public void renderLevelStage(Matrix4fc modelViewMatrix, Vec3 cameraPosIn, DeltaTracker tickDelta) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.options.hideGui || MapManager.getInstance().isEmpty() || mc.level == null || mc.player == null
                || MapDimension.getCurrent().isEmpty() || !FTBChunksWorldConfig.playerHasMapStage(mc.player)) {
            return;
        }

        // save these for use by renderInWorldIcons()
        worldMatrix = new Matrix4f(savedProjectionMatrix).mul(modelViewMatrix);
        cameraPos = new Vec3(cameraPosIn.x, cameraPosIn.y, cameraPosIn.z);

        WaypointBeaconRenderer.renderBeacons(mc, tickDelta, cameraPos);
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
            minimapRenderer.tick(mc);

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

            long now = Util.getEpochMillis();
            if (now >= nextRegionSave) {
                nextRegionSave = now + 60000L;
                manager.saveAllRegions();
            }

            MapDimension.getCurrent().ifPresent(dim -> {
                if (dim.dimension != mc.level.dimension()) {
                    MapDimension.clearCurrentDimension();
                    longRangePlayerTracker.clear();
                }
            });

            taskQueueTicks++;
        });
    }

    private void requestRerender(BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        IntOpenHashSet set = rerenderCache.computeIfAbsent(chunkPos, k -> new IntOpenHashSet());

        if (set.add((pos.getX() & 15) + ((pos.getZ() & 15) * 16))) {
            if (FTBChunksClientConfig.DEBUG_INFO.get()) {
                renderedDebugCount++;
            }
        }
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

    private void gatherMapIcons(MapIconEvent event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null || MapDimension.getCurrent().isEmpty()) return;

        MapDimension mapDimension = MapDimension.getCurrent().get();

        if (FTBChunksClientConfig.MINIMAP_WAYPOINTS.get()) {
            for (Waypoint w : mapDimension.getWaypointManager()) {
                if (!w.isHidden() || !event.getMapType().isMinimap()) {
                    w.getMapIcon().ifPresent(event::add);
                }
            }
        }

        if (FTBChunksClientConfig.MINIMAP_ENTITIES.get()) {
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (!EntityIconUtils.shouldEntityRender(entity, mc.player)) {
                    continue;
                }

                Icon<?> icon = EntityIconLoader.getIcon(entity);
                Optional<EntityIconLoader.EntityIconSettings> settings = EntityIconLoader.getSettings(entity.getType());
                if (settings.isEmpty()) {
                    continue;
                }
                EntityIconLoader.WidthHeight wh = settings.get().widthHeight();

                if (!icon.isEmpty()) {
                    if (FTBChunksClientConfig.ONLY_SURFACE_ENTITIES.get() && !mc.level.dimensionType().hasCeiling()) {
                        int x = Mth.floor(entity.getX());
                        int z = Mth.floor(entity.getZ());
                        MapRegion region = mapDimension.getRegion(XZ.regionFromBlock(x, z));
                        MapRegionData data = region.getData();
                        if (data != null) {
                            int y = data.height[(x & 511) + (z & 511) * 512];
                            if (entity.getY() >= y - 10) {
                                event.add(new EntityMapIcon(entity, icon, wh));
                            }
                        }
                    } else {
                        event.add(new EntityMapIcon(entity, icon, wh));
                    }
                }
            }
        }

        if (FTBChunksClientConfig.MINIMAP_PLAYER_HEADS.get()) {
            if (mc.level.players().size() > 1) {
                for (Player player : mc.level.players()) {
                    if (player == mc.player || player.isInvisibleTo(mc.player) || !VisibleClientPlayers.isPlayerVisible(player)) {
                        continue;
                    }

                    // this player is tracked by vanilla, so we don't need to track it on long-range tracking
                    longRangePlayerTracker.removePlayer(player.getUUID());

                    event.add(new EntityMapIcon(player, FaceIcon.getFace(player.getGameProfile(), true)));
                }
            }

            longRangePlayerTracker.addAllToEvent(event);
        }

        if (!event.getMapType().isMinimap()) {
            PointerIconMode pointerIconMode = FTBChunksClientConfig.POINTER_ICON_MODE.get();

            if (pointerIconMode.showFace()) {
                event.add(new EntityMapIcon(mc.player, FaceIcon.getFace(mc.player.getGameProfile(), true)));
            }

            if (pointerIconMode.showPointer()) {
                event.add(new PointerIcon());
            }
        }

    }

    public MinimapRenderer getMinimapRenderer() {
        return minimapRenderer;
    }

    public void handlePacket(ClientboundSectionBlocksUpdatePacket p) {
        SectionPos sectionPos = p.sectionPos;

        short[] positions = p.positions;

        for (short position : positions) {
            requestRerender(sectionPos.relativeToBlockPos(position));
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
        MAP_EXECUTOR.execute(task);
    }

    public void handlePacket(ClientboundBlockUpdatePacket p) {
        requestRerender(p.getPos());
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
                        player.displayClientMessage(Component.translatable("ftbchunks.deathpoint_removed", wp.getDisplayName()).withStyle(ChatFormatting.YELLOW), true);
                    }
                });
            }
        });
    }

    public List<Component> getChunkSummary() {
        if (generalChunkData == GeneralChunkData.NONE) return List.of();

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

    public GeneralChunkData getGeneralChunkData() {
        return generalChunkData;
    }

    @Nullable
    public static Waypoint addWaypoint(String name, GlobalPos position, int color) {
        return FTBChunksAPI.clientApi().getWaypointManager(position.dimension()).map(mgr -> {
            Waypoint wp = mgr.addWaypointAt(position.pos(), name);
            wp.setColor(color);
            return wp;
        }).orElse(null);
    }

    public int getRenderedDebugCount() {
        return renderedDebugCount;
    }

    public LongRangePlayerTrackerClient getLongRangePlayerTracker() {
        return longRangePlayerTracker;
    }

    public static void openIconSettingsScreen() {
        Minecraft.getInstance().submit(() -> new EntityIconSettingsScreen(true).openGui());
    }
}
