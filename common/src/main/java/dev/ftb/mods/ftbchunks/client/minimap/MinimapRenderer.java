package dev.ftb.mods.ftbchunks.client.minimap;

import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.event.MapIconEvent;
import dev.ftb.mods.ftbchunks.api.client.event.MinimapLayerEvent;
import dev.ftb.mods.ftbchunks.api.client.event.MinimapLayerEvent.PositionedLayer;
import dev.ftb.mods.ftbchunks.api.client.icon.MapIcon;
import dev.ftb.mods.ftbchunks.api.client.icon.MapType;
import dev.ftb.mods.ftbchunks.api.client.minimap.*;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.client.map.MapManager;
import dev.ftb.mods.ftbchunks.client.mapicon.MapIconComparator;
import dev.ftb.mods.ftbchunks.client.minimap.layers.*;
import dev.ftb.mods.ftblibrary.client.util.ClientUtils;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.util.Lazy;
import dev.ftb.mods.ftblibrary.util.PanelPositioning;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MinimapRenderer {
    private static final float ZOOM_FUDGE_FACTOR = 3.5F;

    private final Lazy<MinimapRegionCutoutTexture> miniMapTexture = Lazy.of(MinimapRegionCutoutTexture::new);
    private XZ currentPlayerChunk = XZ.of(0, 0);
    private Vec3 currentPlayerPos = Vec3.ZERO;
    private Vec3 prevPlayerPos = Vec3.ZERO;
    private boolean textureRefreshRequested = true;
    private float prevZoom = FTBChunksClientConfig.MINIMAP_ZOOM.get().floatValue();
    private long lastZoomTime = 0L;
    private final List<MapIcon> mapIcons = new ArrayList<>();
    private long lastMapIconUpdate = 0L;
    private final List<MinimapInfoComponent> sortedComponents = new LinkedList<>();
    // kludge to move potion effects left to avoid rendering over/under minimap in top right of screen
    private double vanillaEffectsOffsetX;
    private int cachedComponentHeight = -1;  // <0 means recalculate
    private final List<PositionedLayer> layerRenderers = new ArrayList<>();

    public MinimapRenderer() {
        addBuiltinRenderLayer(DefaultRenderLayers.TERRAIN, TerrainLayerRenderer.INSTANCE);
        addBuiltinRenderLayer(DefaultRenderLayers.CROSSHAIRS, CrosshairsLayerRenderer.INSTANCE);
        addBuiltinRenderLayer(DefaultRenderLayers.COMPASS, CompassLayerRenderer.INSTANCE);
        addBuiltinRenderLayer(DefaultRenderLayers.ICONS, IconLayerRenderer.INSTANCE);
        addBuiltinRenderLayer(DefaultRenderLayers.PLAYER, PlayerIconLayerRenderer.INSTANCE);
        addBuiltinRenderLayer(DefaultRenderLayers.INFO, InfoLayerRenderer.INSTANCE);
    }

    public void addExtraRenderLayers() {
        List<PositionedLayer> toAdd = new ArrayList<>();
        MinimapLayerEvent.ADD_LAYERS.invoker().accept(new MinimapLayerEvent(toAdd::add));
        toAdd.forEach(this::addRenderLayer);
    }

    private void addRenderLayer(PositionedLayer layer) {
        int insertPos;
        Identifier other = layer.order().otherLayer();
        if (other == null) {
            insertPos = layer.order().after() ? layerRenderers.size() : 0;
        } else {
            int otherIndex = IntStream.range(0, layerRenderers.size())
                    .filter(i -> layerRenderers.get(i).id().equals(other))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Attempted to order against unregistered layer " + other));
            insertPos = otherIndex + (layer.order().after() ? 1 : 0);
        }
        layerRenderers.add(insertPos, layer);
    }

    private void addBuiltinRenderLayer(Identifier id, MinimapLayerRenderer renderer) {
        addRenderLayer(new PositionedLayer(id, renderer, MinimapLayerEvent.Order.atEnd()));
    }

    public void tick(Minecraft mc) {
        if (mc.player != null) {
            prevPlayerPos = currentPlayerPos;
            currentPlayerPos = mc.player.position();
        }
    }

    public void render(GuiGraphicsExtractor graphics, DeltaTracker tickDelta) {
        Minecraft mc = Minecraft.getInstance();

        if (shouldSkipMinimapRendering(mc)) {
            return;
        }

        Player player = ClientUtils.getClientPlayer();
        MapDimension dim = MapDimension.getCurrent().orElseThrow();
        float partialTick = tickDelta.getGameTimeDeltaPartialTick(false);
        Vec3 playerPos = prevPlayerPos.lerp(currentPlayerPos, partialTick);

        refreshMinimapTextureIfNeeded(playerPos, dim);
        refreshIconsIfNeeded(playerPos, partialTick, dim);

        int scaledWidth = mc.getWindow().getGuiScaledWidth();
        int scaledHeight = mc.getWindow().getGuiScaledHeight();
        float minimapScale = calculateMinimapScale(mc.getWindow().getGuiScale(), scaledWidth);
        int minimapSize = (int) (64F * minimapScale);

        PanelPositioning.PanelPos minimapPos = FTBChunksClientConfig.MINIMAP_POSITION.get().getPanelPos(
                scaledWidth, scaledHeight,
                minimapSize, minimapSize,
                FTBChunksClientConfig.MINIMAP_OFFSET_X.get(), FTBChunksClientConfig.MINIMAP_OFFSET_Y.get()
        );

        updateVanillaEffectsOffsetX(player, minimapPos, minimapSize, scaledWidth);

        int componentsHeight = getMinimapComponentsTotalHeight(dim, playerPos);
        var componentContext = new MinimapComponentContext(
                dim, currentPlayerChunk,
                playerPos, FTBChunksClientConfig.MINIMAP_SETTINGS.get()
        );
        int yAdjust = adjustMinimapYForComponents(minimapPos.y(), minimapSize, componentsHeight, scaledHeight);
        minimapPos = new PanelPositioning.PanelPos(minimapPos.x(), minimapPos.y() + yAdjust);

        boolean rotationLocked = FTBChunksClientConfig.MINIMAP_LOCKED_NORTH.get() || FTBChunksClientConfig.SQUARE_MINIMAP.get();
        float minimapRotation = (rotationLocked ? 180F : -player.getYRot()) % 360F;

        MinimapRenderContext minimapCtx = new MinimapRenderContext(
                minimapPos, minimapSize,
                minimapScale, getInterpolatedZoom() / ZOOM_FUDGE_FACTOR,
                minimapRotation * Mth.DEG_TO_RAD, rotationLocked,
                playerPos,
                sortedComponents, componentContext, componentsHeight,
                tickDelta,
                mapIcons
        );

        Matrix3x2fStack poseStack = graphics.pose();
        poseStack.pushMatrix();
        // all minimap rendering happens with a translation to the centre of the minimap
        poseStack.translate(minimapPos.x() + minimapSize / 2F, minimapPos.y() + minimapSize / 2F);
        layerRenderers.forEach(r -> {
            if (r.renderer().shouldExtract(minimapCtx)) {
                r.renderer().extractLayer(graphics, poseStack, minimapCtx);
            }
        });
        poseStack.popMatrix();
    }

    private static boolean shouldSkipMinimapRendering(Minecraft mc) {
        return mc.player == null || mc.level == null || mc.options.hideGui
                || MapManager.getInstance().isEmpty()
                || MapDimension.getCurrent().isEmpty()
                || !FTBChunksClientConfig.MINIMAP_ENABLED.get()
                || FTBChunksClientConfig.MINIMAP_ALPHA.get() == 0
                || !FTBChunksWorldConfig.shouldShowMinimap(mc.player);
    }

    public void refreshMinimapTextureIfNeeded(Vec3 playerPos, MapDimension dim) {
        XZ playerChunkPos = new XZ(Mth.floor(playerPos.x) >> 4, Mth.floor(playerPos.z) >> 4);
        if (!playerChunkPos.equals(currentPlayerChunk) || textureRefreshRequested) {
            textureRefreshRequested = false;
            currentPlayerChunk = playerChunkPos;
            miniMapTexture.get().update(dim.dimension, currentPlayerChunk);
        }
    }

    public void requestTextureRefresh() {
        textureRefreshRequested = true;
    }

    private void updateVanillaEffectsOffsetX(Player player, PanelPositioning.PanelPos minimapPos, int minimapSize, int scaledWidth) {
        // A bit of a kludge here: vanilla renders active mobeffects in the top-right; move them to the left of the minimap if necessary
        // - see GuiMixin for where it's used
        if (!player.getActiveEffects().isEmpty() && minimapPos.y() <= 50 && minimapPos.x() + minimapSize > scaledWidth - 50) {
            vanillaEffectsOffsetX = -(scaledWidth - minimapPos.x()) - 5;
        } else {
            vanillaEffectsOffsetX = 0;
        }
    }

    // See above
    public double getVanillaEffectsOffsetX() {
        return vanillaEffectsOffsetX;
    }

    public void changeZoom(boolean zoomIn) {
        prevZoom = FTBChunksClientConfig.MINIMAP_ZOOM.get().floatValue();
        lastZoomTime = Util.getEpochMillis();
        double newZoom = Mth.clamp(prevZoom + (zoomIn ? 1D : -1D), 1D, 4D);
        FTBChunksClientConfig.MINIMAP_ZOOM.set(newZoom);

        if (FTBChunksClientConfig.shouldBlurTexture(newZoom) != FTBChunksClientConfig.shouldBlurTexture(prevZoom)) {
            requestTextureRefresh();
        }
    }

    public Collection<MapIcon> getMapIcons() {
        return mapIcons;
    }

    public void refreshIcons() {
        lastMapIconUpdate = 0L;
    }

    public void setupComponents() {
        sortedComponents.clear();
        cachedComponentHeight = -1;

        Map<Identifier, MinimapInfoComponent> componentMap = FTBChunksAPI.clientApi().getMinimapComponents().stream()
                .collect(Collectors.toMap(MinimapInfoComponent::id, Function.identity()));

        List<Identifier> order = FTBChunksClientConfig.MINIMAP_INFO_ORDER.get()
                .stream()
                .map(Identifier::parse)
                .collect(Collectors.toCollection(ArrayList::new));  // needs to be mutable

        addMissingComponents(componentMap, order);

        for (Identifier id : order) {
            MinimapInfoComponent minimapInfoComponent = componentMap.get(id);
            if (minimapInfoComponent != null && FTBChunksAPI.clientApi().isMinimapComponentEnabled(minimapInfoComponent)) {
                sortedComponents.add(minimapInfoComponent);
            }
        }
    }

    private static float calculateMinimapScale(float guiScale, int scaledWidth) {
        float minimapScale;
        if (FTBChunksClientConfig.MINIMAP_PROPORTIONAL.get()) {
            minimapScale = 4F / guiScale;
            minimapScale *= (scaledWidth / 10f) / (minimapScale * 64f) * FTBChunksClientConfig.MINIMAP_SCALE.get().floatValue();
        } else {
            minimapScale = FTBChunksClientConfig.MINIMAP_SCALE.get().floatValue() * 4F / guiScale;
        }
        return minimapScale;
    }

    private static int adjustMinimapYForComponents(int minimapY, int minimapSize, int componentsHeight, int guiScaledHeight) {
        if (FTBChunksClientConfig.TEXT_ABOVE_MINIMAP.get()) {
            // Move map down if text would go off the top
            int componentsY = minimapY - componentsHeight;
            if (componentsY <= 0) {
                return -componentsY + 1;
            }
        } else {
            // Move map up if text would go off the bottom
            int renderY = minimapY + minimapSize + componentsHeight;
            if (renderY >= guiScaledHeight) {
                return -componentsHeight - 1;
            }
        }
        return 0;
    }

    private float getInterpolatedZoom() {
        float zoom = FTBChunksClientConfig.MINIMAP_ZOOM.get().floatValue();

        if (prevZoom != zoom) {
            // interpolated for smooth zooming in/out
            long maxTime = (long) (400F / zoom);
            long zoomTime = Mth.clamp(Util.getEpochMillis() - lastZoomTime, 0L, maxTime);
            if (zoomTime == maxTime) {
                lastZoomTime = 0L;
                return zoom;
            }
            return Mth.lerp(zoomTime / (float) maxTime, prevZoom, zoom);
        }

        return zoom;
    }

    private void refreshIconsIfNeeded(Vec3 playerPos, float partialTick, MapDimension dim) {
        long now = Util.getEpochMillis();
        if (now - lastMapIconUpdate >= FTBChunksClientConfig.MINIMAP_ICON_UPDATE_TIMER.get()) {
            lastMapIconUpdate = now;

            mapIcons.clear();
            MapIconEvent.MINIMAP.invoker().accept(new MapIconEvent(dim.dimension, mapIcons, MapType.MINIMAP));

            if (mapIcons.size() >= 2) {
                mapIcons.sort(new MapIconComparator(playerPos, partialTick));
            }
        }
    }

    private int getMinimapComponentsTotalHeight(MapDimension dim, Vec3 playerPos) {
        if (cachedComponentHeight < 0) {
            var context = new MinimapComponentContext(dim, currentPlayerChunk, playerPos, FTBChunksClientConfig.MINIMAP_SETTINGS.get());
            int sum = 0;
            for (MinimapInfoComponent c : sortedComponents) {
                if (c.shouldRender(context)) {
                    sum += c.height(context);
                }
            }
            cachedComponentHeight = sum;
        }
        return cachedComponentHeight;
    }

    private static void addMissingComponents(Map<Identifier, MinimapInfoComponent> componentMap, List<Identifier> order) {
        // Adds any missing components to the end of the list
        boolean save = false;
        for (Identifier location : componentMap.keySet()) {
            if (!order.contains(location)) {
                order.add(location);
                save = true;
            }
        }

        if (save) {
            FTBChunksClientConfig.MINIMAP_INFO_ORDER.set(order.stream().map(Identifier::toString).collect(Collectors.toList()));
            FTBChunksClientConfig.saveConfig();
        }
    }
}
