package dev.ftb.mods.ftbchunks.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.event.AddMapIconEvent;
import dev.ftb.mods.ftbchunks.api.client.event.AddMinimapLayerEvent;
import dev.ftb.mods.ftbchunks.api.client.event.WaypointManagerAvailableEvent;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapRenderContext;
import dev.ftb.mods.ftbchunks.api.client.waypoint.Waypoint;
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
import dev.ftb.mods.ftbchunks.config.FTBChunksClientConfig;
import dev.ftb.mods.ftbchunks.config.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.net.SendGeneralDataPacket.GeneralChunkData;
import dev.ftb.mods.ftbchunks.util.FTBCUtils;
import dev.ftb.mods.ftblibrary.client.config.editable.EditableString;
import dev.ftb.mods.ftblibrary.client.gui.widget.BaseScreen;
import dev.ftb.mods.ftblibrary.client.util.ClientUtils;
import dev.ftb.mods.ftblibrary.icon.EntityIconLoader;
import dev.ftb.mods.ftblibrary.icon.FaceIcon;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.platform.client.PlatformClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.joml.Matrix3x2fStack;

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

    // Keybinding to open Large map screen
    public static final KeyMapping openMapKey
            = new KeyMapping("key.ftbchunks.map", InputConstants.Type.KEYSYM, InputConstants.KEY_M, KEY_CATEGORY);
    // Keybinding to toggle the minimap
    public static final KeyMapping toggleMinimapKey
            = new KeyMapping("key.ftbchunks.toggle_minimap", InputConstants.Type.KEYSYM, -1, KEY_CATEGORY);
    // Keybinding to open claim manager screen
    public static final KeyMapping openClaimManagerKey
            = new KeyMapping("key.ftbchunks.claim_manager", InputConstants.Type.KEYSYM, -1, KEY_CATEGORY);
    // Keybinding to zoom in minimap
    public static final KeyMapping zoomInKey
            = new KeyMapping("key.ftbchunks.minimap.zoomIn", InputConstants.Type.KEYSYM, InputConstants.KEY_EQUALS, KEY_CATEGORY);
    // Keybinding to zoom out minimap
    public static final KeyMapping zoomOutKey
            = new KeyMapping("key.ftbchunks.minimap.zoomOut", InputConstants.Type.KEYSYM, InputConstants.KEY_MINUS, KEY_CATEGORY);
    // Keybinding to quick-add waypoint at current position
    public static final KeyMapping addWaypointKey
            = new KeyMapping("key.ftbchunks.add_waypoint", InputConstants.Type.KEYSYM, -1, KEY_CATEGORY);
    // Keybinding to open the waypoint manager screen
    public static final KeyMapping waypointManagerKey
            = new KeyMapping("key.ftbchunks.waypoint_manager", InputConstants.Type.KEYSYM, -1, KEY_CATEGORY);

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

        PlatformClient.get().addResourcePackReloadListener(FTBChunksAPI.MOD_ID, FTBChunksAPI.id("colormap"), new ColorMapLoader());

        MinimapComponentSetup.registerComponents();

        return this;
    }

    public void addTestMinimapLayer(AddMinimapLayerEvent.Data event) {
        event.addLayer(FTBChunksAPI.id("test"), FTBChunksClient::renderTestMinimapLayer, AddMinimapLayerEvent.Order.atEnd());
    }

    private void registerKeyMappings() {
        PlatformClient.get().registerKeyMapping(FTBChunksAPI.MOD_ID,
                openMapKey, toggleMinimapKey, openClaimManagerKey, zoomInKey, zoomOutKey, addWaypointKey, waypointManagerKey
        );
    }

    public void onClientStarted(Minecraft ignoredMc) {
        minimapRenderer.addExtraRenderLayers();
        minimapRenderer.setupComponents();

        FTBCUtils.postMinYEvent(true);
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

    public void onWaypointManagerAvailable(WaypointManagerAvailableEvent.Data event) {
        // only in dev mode: testing transient waypoints
        if (ClientUtils.getClientLevel().dimension().equals(Level.OVERWORLD)) {
            event.manager().addTransientWaypointAt(new BlockPos(0, 65, 0), "Transient Dev-Mode Waypoint").setColor(0x808080);
        }
    }

    public void onPlayerQuit() {
        MapManager.shutdown();
    }

    public boolean handleCustomClick(Identifier id) {
        if (FTBChunksWorldConfig.playerHasMapStage(ClientUtils.getClientPlayer())) {
            if (id.equals(BUTTON_ID_MAP)) {
                LargeMapScreen.openMap();
                return true;
            } else if (id.equals(BUTTON_ID_CLAIM)) {
                ChunkScreen.openChunkScreen();
                return true;
            }
        }

        return false;
    }

    private void checkKeyPresses(Minecraft client) {
        if (client.screen != null || !FTBChunksWorldConfig.playerHasMapStage(ClientUtils.getClientPlayer())) {
            return;
        }

        if (openMapKey.isDown()) {
            LargeMapScreen.openMap();
        } else if (toggleMinimapKey.isDown()) {
            FTBChunksClientConfig.MINIMAP_ENABLED.set(!FTBChunksClientConfig.MINIMAP_ENABLED.get());
            FTBChunksClientConfig.saveConfig();
        } else if (openClaimManagerKey.isDown()) {
            ChunkScreen.openChunkScreen();
        } else if (zoomInKey.isDown()) {
            minimapRenderer.changeZoom(true);
        } else if (zoomOutKey.isDown()) {
            minimapRenderer.changeZoom(false);
        } else if (addWaypointKey.isDown()) {
            addQuickWaypoint();
        } else if (waypointManagerKey.isDown()) {
            new WaypointEditorScreen().openGui();
        }
    }

    public void onRenderHUD(GuiGraphicsExtractor graphics, DeltaTracker tickDelta) {
        if (FTBChunksClientConfig.IN_WORLD_WAYPOINTS.get()) {
            inWorldIconRenderer.renderInWorldIcons(graphics, tickDelta, ClientUtils.getClientPlayer().position(), minimapRenderer.getMapIcons());
        }

        minimapRenderer.render(graphics, tickDelta);
    }

    public void onGuiInit(Screen screen) {
        if (screen instanceof PauseScreen) {
            nextRegionSave = System.currentTimeMillis() + 60000L;
            MapManager.getInstance().ifPresent(MapManager::saveAllRegions);
        }
    }

    public void onClientTick(Minecraft mc) {
        if (mc.level == null) return;

        MapManager.getInstance().ifPresent(manager -> {
            checkKeyPresses(mc);

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

    public void onTeamPropertiesChanged() {
        MapManager.getInstance().ifPresent(manager -> manager.updateAllRegions(false));
    }

    public void onMapIconEvent(AddMapIconEvent.Data event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null || MapDimension.getCurrent().isEmpty()) return;

        MapDimension mapDimension = MapDimension.getCurrent().orElseThrow();

        if (FTBChunksClientConfig.MINIMAP_WAYPOINTS.get()) {
            for (Waypoint w : mapDimension.getWaypointManager()) {
                if (!w.isHidden() || !event.mapType().isMinimap()) {
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

        if (!event.mapType().isMinimap()) {
            PointerIconMode pointerIconMode = FTBChunksClientConfig.POINTER_ICON_MODE.get();
            if (pointerIconMode.showFace()) {
                event.add(new EntityMapIcon(mc.player, FaceIcon.getFace(mc.player.getGameProfile(), true)));
            }
            if (pointerIconMode.showPointer()) {
                event.add(new PointerIcon());
            }
        }
    }

    public void updateGeneralData(GeneralChunkData chunkData) {
        generalChunkData = chunkData;
    }

    private void addQuickWaypoint() {
        MapManager.getInstance().ifPresent(manager -> {
            BaseScreen screen = new WaypointAddScreen(new EditableString(), ClientUtils.getClientPlayer());
            // later needed to prevent keypress being passed into gui
            screen.openGuiLater();
        });
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
                        player.sendOverlayMessage(Component.translatable("ftbchunks.deathpoint_removed", wp.getDisplayName()).withStyle(ChatFormatting.YELLOW));
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
