package dev.ftb.mods.ftbchunks.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.*;
import dev.architectury.hooks.client.screen.ScreenAccess;
import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.platform.Platform;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.event.MapIconEvent;
import dev.ftb.mods.ftbchunks.api.client.event.MinimapLayerEvent;
import dev.ftb.mods.ftbchunks.api.client.event.WaypointManagerEvent;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapRenderContext;
import dev.ftb.mods.ftbchunks.api.client.waypoint.Waypoint;
import dev.ftb.mods.ftbchunks.api.client.waypoint.WaypointManager;
import dev.ftb.mods.ftbchunks.client.gui.EntityIconSettingsScreen;
import dev.ftb.mods.ftbchunks.client.gui.PointerIcon;
import dev.ftb.mods.ftbchunks.client.gui.WaypointAddScreen;
import dev.ftb.mods.ftbchunks.client.gui.WaypointEditorScreen;
import dev.ftb.mods.ftbchunks.client.gui.map.ChunkScreen;
import dev.ftb.mods.ftbchunks.client.gui.map.LargeMapScreen;
import dev.ftb.mods.ftbchunks.client.map.*;
import dev.ftb.mods.ftbchunks.client.mapicon.EntityIconUtils;
import dev.ftb.mods.ftbchunks.client.mapicon.EntityMapIcon;
import dev.ftb.mods.ftbchunks.client.minimap.MinimapRenderer;
import dev.ftb.mods.ftbchunks.client.minimap.components.MinimapComponentSetup;
import dev.ftb.mods.ftbchunks.net.SendGeneralDataPacket.GeneralChunkData;
import dev.ftb.mods.ftblibrary.client.config.editable.EditableString;
import dev.ftb.mods.ftblibrary.client.gui.CustomClickEvent;
import dev.ftb.mods.ftblibrary.client.gui.widget.BaseScreen;
import dev.ftb.mods.ftblibrary.client.util.ClientUtils;
import dev.ftb.mods.ftblibrary.icon.EntityIconLoader;
import dev.ftb.mods.ftblibrary.icon.FaceIcon;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.util.ModUtils;
import dev.ftb.mods.ftbteams.api.event.ClientTeamPropertiesChangedEvent;
import dev.ftb.mods.ftbteams.api.event.TeamEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private long nextRegionSave = 0L;
    private GeneralChunkData generalChunkData = GeneralChunkData.NONE;
    private final MinimapRenderer minimapRenderer = new MinimapRenderer();
    private final InWorldIconRenderer inWorldIconRenderer = new InWorldIconRenderer();
    private final LongRangePlayerTrackerClient longRangePlayerTracker = new LongRangePlayerTrackerClient();
    private final RerenderTracker rerenderTracker = new RerenderTracker();

    public FTBChunksClient init() {
        FTBChunksAPI._initClient(new FTBChunksClientAPIImpl());

        registerKeyMappings();

        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, new ColorMapLoader(), FTBChunksAPI.id("colormap"));

        ClientLifecycleEvent.CLIENT_STARTED.register(this::onClientStarted);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(this::onPlayerQuit);
        CustomClickEvent.EVENT.register(this::onCustomClick);
        ClientRawInputEvent.KEY_PRESSED.register(this::onKeyPressedRaw);
        ClientScreenInputEvent.KEY_PRESSED_PRE.register(this::onKeyPressedScreen);
        ClientGuiEvent.RENDER_HUD.register(this::onRenderHUD);
        ClientGuiEvent.INIT_PRE.register(this::onGuiInit);
        ClientTickEvent.CLIENT_PRE.register(this::onClientTick);
        TeamEvent.CLIENT_PROPERTIES_CHANGED.register(this::onTermPropertiesChanged);
        MapIconEvent.LARGE_MAP.register(this::onMapIconEvent);
        MapIconEvent.MINIMAP.register(this::onMapIconEvent);

        if (ModUtils.isDevMode()) {
            WaypointManagerEvent.AVAILABLE.register(this::onWaypointManagerAvailable);
            MinimapLayerEvent.ADD_LAYERS.register(this::addTestLayer);
        }

        minimapRenderer.addExtraRenderLayers();

        // Register minimap components
        MinimapComponentSetup.registerComponents();

        return this;
    }

    private void addTestLayer(MinimapLayerEvent event) {
        event.addLayer(FTBChunksAPI.id("test"), (graphics, poseStack, ctx) -> renderTestMinimapLayer(graphics, poseStack, ctx), MinimapLayerEvent.Order.atEnd());
    }

    private void registerKeyMappings() {
        // Keybinding to open Large map screen
        openMapKey = new KeyMapping("key.ftbchunks.map", InputConstants.Type.KEYSYM, InputConstants.KEY_M, KEY_CATEGORY);
        KeyMappingRegistry.register(openMapKey);

        // Keybinding to toggle the minimap
        toggleMinimapKey = new KeyMapping("key.ftbchunks.toggle_minimap", InputConstants.Type.KEYSYM, -1, KEY_CATEGORY);
        KeyMappingRegistry.register(toggleMinimapKey);

        // Keybinding to open claim manager screen
        openClaimManagerKey = new KeyMapping("key.ftbchunks.claim_manager", InputConstants.Type.KEYSYM, -1, KEY_CATEGORY);
        KeyMappingRegistry.register(openClaimManagerKey);

        // Keybindings to zoom in minimap
        zoomInKey = new KeyMapping("key.ftbchunks.minimap.zoomIn", InputConstants.Type.KEYSYM, InputConstants.KEY_EQUALS, KEY_CATEGORY);
        KeyMappingRegistry.register(zoomInKey);

        zoomOutKey = new KeyMapping("key.ftbchunks.minimap.zoomOut", InputConstants.Type.KEYSYM, InputConstants.KEY_MINUS, KEY_CATEGORY);
        KeyMappingRegistry.register(zoomOutKey);

        // Keybinding to quick-add waypoint at current position
        addWaypointKey = new KeyMapping("key.ftbchunks.add_waypoint", InputConstants.Type.KEYSYM, -1, KEY_CATEGORY);
        KeyMappingRegistry.register(addWaypointKey);

        // Keybinding to open the waypoint manager screen
        waypointManagerKey = new KeyMapping("key.ftbchunks.waypoint_manager", InputConstants.Type.KEYSYM, -1, KEY_CATEGORY);
        KeyMappingRegistry.register(waypointManagerKey);
    }

    private void onClientStarted(Minecraft minecraft) {
        minimapRenderer.setupComponents();
    }

    private static void renderTestMinimapLayer(GuiGraphicsExtractor graphics, Matrix3x2fStack poseStack, MinimapRenderContext ctx) {
        // only in dev mode: testing minimap layer registration event
        if (ClientUtils.getClientPlayer().isCrouching()) {
            poseStack.pushMatrix();
            poseStack.translate(-ctx.size() / 2f + 2, -ctx.size() / 2f + 2);
            poseStack.scale(ctx.scale() / 2f);
            graphics.text(Minecraft.getInstance().font, "Crouch!", 0, 0, 0xFF80FF80);
            poseStack.popMatrix();
        }
    }

    private void onWaypointManagerAvailable(WaypointManager mgr) {
        // only in dev mode: testing transient waypoints
        if (ClientUtils.getClientLevel().dimension().equals(Level.OVERWORLD)) {
            mgr.addTransientWaypointAt(new BlockPos(0, 65, 0), "Transient Dev-Mode Waypoint").setColor(0x808080);
        }
    }

    private void onPlayerQuit(@Nullable LocalPlayer player) {
        MapManager.shutdown();
    }

    private EventResult onCustomClick(CustomClickEvent event) {
        if (FTBChunksWorldConfig.playerHasMapStage(ClientUtils.getClientPlayer())) {
            if (event.id().equals(BUTTON_ID_MAP)) {
                LargeMapScreen.openMap();
                return EventResult.interruptTrue();
            } else if (event.id().equals(BUTTON_ID_CLAIM)) {
                ChunkScreen.openChunkScreen();
                return EventResult.interruptTrue();
            }
        }

        return EventResult.pass();
    }

    private EventResult onKeyPressedRaw(Minecraft client, int action, KeyEvent keyEvent) {
        if (action != InputConstants.PRESS
                || client.screen != null
                || !FTBChunksWorldConfig.playerHasMapStage(ClientUtils.getClientPlayer())
                || InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), InputConstants.KEY_F3))
        {
            return EventResult.pass();
        }

        if (doesKeybindMatch(openMapKey, keyEvent)) {
            LargeMapScreen.openMap();
            return EventResult.interruptTrue();
        } else if (doesKeybindMatch(toggleMinimapKey, keyEvent)) {
            FTBChunksClientConfig.MINIMAP_ENABLED.set(!FTBChunksClientConfig.MINIMAP_ENABLED.get());
            FTBChunksClientConfig.saveConfig();
            return EventResult.interruptTrue();
        } else if (doesKeybindMatch(openClaimManagerKey, keyEvent)) {
            ChunkScreen.openChunkScreen();
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

    private EventResult onKeyPressedScreen(Minecraft client, Screen screen, KeyEvent keyEvent) {
        // platform specific behaviour :(  why? ¯\_(ツ)_/¯
        if (doesKeybindMatch(openMapKey, keyEvent) && Platform.isFabric()) {
            LargeMapScreen gui = ClientUtils.getCurrentGuiAs(LargeMapScreen.class);
            if (gui != null && !gui.anyModalPanelOpen()) {
                gui.closeGui(false);
                return EventResult.interruptTrue();
            }
        }

        return EventResult.pass();
    }

    private void onRenderHUD(GuiGraphics graphics, DeltaTracker tickDelta) {
        if (FTBChunksClientConfig.IN_WORLD_WAYPOINTS.get()) {
            inWorldIconRenderer.renderInWorldIcons(graphics, tickDelta, ClientUtils.getClientPlayer().position(), minimapRenderer.getMapIcons());
        }

        minimapRenderer.render(graphics, tickDelta);
    }

    private EventResult onGuiInit(Screen screen, ScreenAccess access) {
        if (screen instanceof PauseScreen) {
            nextRegionSave = System.currentTimeMillis() + 60000L;
            MapManager.getInstance().ifPresent(MapManager::saveAllRegions);
        }

        return EventResult.pass();
    }

    private void onClientTick(Minecraft mc) {
        if (mc.level == null) return;

        MapManager.getInstance().ifPresent(manager -> {
            minimapRenderer.tick(mc);

            Level level = Objects.requireNonNull(mc.level);

            if (taskQueueTicks % FTBChunksClientConfig.RERENDER_QUEUE_TICKS.get() == 0L) {
                rerenderTracker.run(level, manager);
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

            if (mc.player != null && mc.player.tickCount % 20 == 0) {
                maybeClearDeathpoint(mc.player);
            }

            manager.firePendingUpdateEvents();

            taskQueueTicks++;
        });
    }

    private void onTermPropertiesChanged(ClientTeamPropertiesChangedEvent event) {
        MapManager.getInstance().ifPresent(manager -> manager.updateAllRegions(false));
    }

    private void onMapIconEvent(MapIconEvent event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null || MapDimension.getCurrent().isEmpty()) return;

        MapDimension mapDimension = MapDimension.getCurrent().orElseThrow();

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

    @SuppressWarnings("unused")
    @ExpectPlatform
    public static boolean doesKeybindMatch(@Nullable KeyMapping keyMapping, KeyEvent keyEvent) {
        throw new AssertionError();
    }

    public void updateGeneralData(GeneralChunkData chunkData) {
        generalChunkData = chunkData;
    }

    private EventResult addQuickWaypoint() {
        return MapManager.getInstance().map(manager -> {
            BaseScreen screen = new WaypointAddScreen(new EditableString(), ClientUtils.getClientPlayer());
            screen.openGuiLater();
            // later needed to prevent keypress being passed into gui
            return EventResult.interruptTrue();
        }).orElse(EventResult.pass());
    }

    private void maybeClearDeathpoint(Player player) {
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

    public static Optional<Waypoint> addWaypoint(String name, GlobalPos position, int color) {
        return FTBChunksAPI.clientApi().getWaypointManager(position.dimension())
                .map(mgr -> mgr.addWaypointAt(position.pos(), name).setColor(color));
    }

    public MinimapRenderer getMinimapRenderer() {
        return minimapRenderer;
    }

    public InWorldIconRenderer getInWorldIconRenderer() {
        return inWorldIconRenderer;
    }

    public GeneralChunkData getGeneralChunkData() {
        return generalChunkData;
    }

    public LongRangePlayerTrackerClient getLongRangePlayerTracker() {
        return longRangePlayerTracker;
    }

    public RerenderTracker getRerenderTracker() {
        return rerenderTracker;
    }

    public static void openIconSettingsScreen() {
        new EntityIconSettingsScreen(true).openGuiLater();
    }
}
