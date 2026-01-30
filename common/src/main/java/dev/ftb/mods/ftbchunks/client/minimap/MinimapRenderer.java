package dev.ftb.mods.ftbchunks.client.minimap;

import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.event.MapIconEvent;
import dev.ftb.mods.ftbchunks.api.client.icon.MapIcon;
import dev.ftb.mods.ftbchunks.api.client.icon.MapType;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapComponentContext;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapInfoComponent;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapLayerRenderer;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapRenderContext;
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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MinimapRenderer {
    private final Lazy<MinimapRegionCutoutTexture> miniMapTexture = Lazy.of(MinimapRegionCutoutTexture::new);
    private XZ currentPlayerChunk = XZ.of(0, 0);
    private Vec3 currentPlayerPos = Vec3.ZERO;
    private Vec3 prevPlayerPos = Vec3.ZERO;
    private boolean textureRefreshRequested = true;
    private double prevZoom = FTBChunksClientConfig.MINIMAP_ZOOM.get();
    private long lastZoomTime = 0L;
    private final List<MapIcon> mapIcons = new ArrayList<>();
    private long lastMapIconUpdate = 0L;
    private final List<MinimapInfoComponent> sortedComponents = new LinkedList<>();
    // kludge to move potion effects left to avoid rendering over/under minimap in top right of screen
    private double vanillaEffectsOffsetX;

    // TODO add API to dynamically register layer renderers
    private final List<MinimapLayerRenderer> layerRenderers = List.of(
            TerrainLayerRenderer.INSTANCE,
            CrosshairsLayerRenderer.INSTANCE,
            CompassLayerRenderer.INSTANCE,
            IconLayerRenderer.INSTANCE,
            PlayerIconLayerRenderer.INSTANCE,
            InfoLayerRenderer.INSTANCE
    );

    public void tick(Minecraft mc) {
        if (mc.player != null) {
            prevPlayerPos = currentPlayerPos;
            currentPlayerPos = mc.player.position();
        }
    }

    public void render(GuiGraphics graphics, DeltaTracker tickDelta) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null || MapManager.getInstance().isEmpty() || MapDimension.getCurrent().isEmpty()) {
            return;
        }

        float partialTicks = tickDelta.getGameTimeDeltaPartialTick(false);

        Vec3 playerPos = prevPlayerPos.lerp(currentPlayerPos, partialTicks);

        MapDimension dim = MapDimension.getCurrent().get();

        XZ playerChunkPos = new XZ(Mth.floor(playerPos.x) >> 4, Mth.floor(playerPos.z) >> 4);
        if (!playerChunkPos.equals(currentPlayerChunk)) {
            requestTextureRefresh();
        }

        if (textureRefreshRequested) {
            textureRefreshRequested = false;
            currentPlayerChunk = playerChunkPos;
            miniMapTexture.get().update(dim.dimension, currentPlayerChunk);
        }

        if (mc.options.hideGui || !FTBChunksClientConfig.MINIMAP_ENABLED.get() || FTBChunksClientConfig.MINIMAP_ALPHA.get() == 0 || !FTBChunksWorldConfig.shouldShowMinimap(mc.player)) {
            return;
        }

        float baseZoom = getZoom();
        float zoom = baseZoom / 3.5F;

        // TODO: [21.8] Figure out how to do this. Game already supports bluring so we can likely figure it out from there!
//        MinimapBlurMode blurMode = FTBChunksClientConfig.MINIMAP_BLUR_MODE.get();
//        boolean minimapBlur = blurMode == MinimapBlurMode.AUTO ? (baseZoom < 1.5F) : blurMode == MinimapBlurMode.ON;
//        int filter = minimapBlur ? GL11.GL_LINEAR : GL11.GL_NEAREST;
//        RenderSystem.bindTextureForSetup(minimapTextureId);
//        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
//        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);

        float guiScale = mc.getWindow().getGuiScale();
        int scaledWidth = mc.getWindow().getGuiScaledWidth();
        int scaledHeight = mc.getWindow().getGuiScaledHeight();

        updateMinimapIconsIfNeeded(tickDelta, dim);

        float minimapScale;
        if (FTBChunksClientConfig.MINIMAP_PROPORTIONAL.get()) {
            minimapScale = 4F / guiScale;
            minimapScale *= (scaledWidth / 10f) / (minimapScale * 64f) * FTBChunksClientConfig.MINIMAP_SCALE.get().floatValue();
        } else {
            minimapScale = FTBChunksClientConfig.MINIMAP_SCALE.get().floatValue() * 4F / guiScale;
        }

        boolean rotationLocked = FTBChunksClientConfig.MINIMAP_LOCKED_NORTH.get() || FTBChunksClientConfig.SQUARE_MINIMAP.get();
        float minimapRotation = (rotationLocked ? 180F : -mc.player.getYRot()) % 360F;

        int minimapSize = (int) (64F * minimapScale);
        float halfSizeF = minimapSize / 2F;

        PanelPositioning.PanelPos minimapPos = FTBChunksClientConfig.MINIMAP_POSITION.get().getPanelPos(
                scaledWidth, scaledHeight,
                minimapSize, minimapSize,
                FTBChunksClientConfig.MINIMAP_OFFSET_X.get(), FTBChunksClientConfig.MINIMAP_OFFSET_Y.get()
        );

        // a bit of a kludge here: vanilla renders active mobeffects in the top-right; move them to the left of the minimap if necessary
        // see GuiMixin for where this is used
        if (!mc.player.getActiveEffects().isEmpty() && minimapPos.y() <= 50 && minimapPos.x() + minimapSize > scaledWidth - 50) {
            vanillaEffectsOffsetX = -(scaledWidth - minimapPos.x()) - 5;
        } else {
            vanillaEffectsOffsetX = 0;
        }

        int componentsHeight = getMinimapComponentsTotalHeight(mc, dim, playerPos);
        var componentContext = new MinimapComponentContext(
                mc, ClientUtils.getClientPlayer(), dim, currentPlayerChunk,
                playerPos, FTBChunksClientConfig.MINIMAP_SETTINGS.get()
        );
        int yAdjust = adjustMinimapYForComponents(minimapPos.y(), minimapSize, componentsHeight, scaledHeight);
        minimapPos = new PanelPositioning.PanelPos(minimapPos.x(), minimapPos.y() + yAdjust);

        MinimapRenderContext minimapCtx = new MinimapRenderContext(
                minimapPos, minimapSize,
                minimapScale, zoom,
                minimapRotation * Mth.DEG_TO_RAD, rotationLocked,
                playerPos,
                sortedComponents, componentContext, componentsHeight,
                tickDelta,
                mapIcons
        );

        Matrix3x2fStack poseStack = graphics.pose();
        poseStack.pushMatrix();
        // all minimap rendering happens with a translation to the centre of the minimap
        poseStack.translate(minimapPos.x() + halfSizeF, minimapPos.y() + halfSizeF);
        layerRenderers.forEach(r -> {
            if (r.shouldRender(minimapCtx)) {
                r.renderLayer(graphics, poseStack, minimapCtx);
            }
        });
        poseStack.popMatrix();
    }

    public void requestTextureRefresh() {
        textureRefreshRequested = true;
    }

    // See GuiMixin
    // This moves the vanilla potion effects rendering to the left of the minimap if it's in the top-right
    public double getVanillaEffectsOffsetX() {
        return vanillaEffectsOffsetX;
    }

    public void changeZoom(boolean zoomIn) {
        prevZoom = FTBChunksClientConfig.MINIMAP_ZOOM.get();
        double zoomFactor = zoomIn ? 1D : -1D;
        double zoom = Mth.clamp(prevZoom + zoomFactor, 1D, 4D);
        lastZoomTime = System.currentTimeMillis();
        FTBChunksClientConfig.MINIMAP_ZOOM.set(zoom);
    }

    public float getZoom() {
        double zoom = FTBChunksClientConfig.MINIMAP_ZOOM.get();

        if (prevZoom != zoom) {
            // interpolated for smooth zooming in/out
            long maxTime = (long) (400D / zoom);
            long zoomTime = Mth.clamp(System.currentTimeMillis() - lastZoomTime, 0L, maxTime);
            if (zoomTime == maxTime) {
                lastZoomTime = 0L;
                return (float) zoom;
            }
            return (float) Mth.lerp(zoomTime / (double) maxTime, prevZoom, zoom);
        }

        return (float) zoom;
    }

    private int adjustMinimapYForComponents(int minimapY, int minimapSize, int componentsHeight, int guiScaledHeight) {
        if (FTBChunksClientConfig.TEXT_ABOVE_MINIMAP.get()) {
            // Move map down if text would go outside the screen
            int componentsY = minimapY - componentsHeight;
            if (componentsY <= 0) {
                return -componentsY + 1;
            }
        } else {
            // Move map up if text would go outside the screen
            int renderY = minimapY + minimapSize + componentsHeight;
            if (renderY >= guiScaledHeight) {
                return -componentsHeight - 1;
            }
        }
        return 0;
    }

    private void updateMinimapIconsIfNeeded(DeltaTracker tickDelta, MapDimension dim) {
        long now = Util.getEpochMillis();
        if (lastMapIconUpdate == 0L || (now - lastMapIconUpdate) >= FTBChunksClientConfig.MINIMAP_ICON_UPDATE_TIMER.get()) {
            lastMapIconUpdate = now;

            mapIcons.clear();
            MapIconEvent.MINIMAP.invoker().accept(new MapIconEvent(dim.dimension, mapIcons, MapType.MINIMAP));

            if (mapIcons.size() >= 2) {
                mapIcons.sort(new MapIconComparator(ClientUtils.getClientPlayer().position(), tickDelta.getGameTimeDeltaPartialTick(false)));
            }
        }
    }

    private int getMinimapComponentsTotalHeight(Minecraft mc, MapDimension dim, Vec3 playerPos) {
        var context = new MinimapComponentContext(mc, ClientUtils.getClientPlayer(), dim, currentPlayerChunk, playerPos, FTBChunksClientConfig.MINIMAP_SETTINGS.get());
        int sum = 0;
        for (MinimapInfoComponent c : sortedComponents) {
            if (c.shouldRender(context)) {
                sum += c.height(context);
            }
        }
        return sum;
    }

    public Collection<MapIcon> getMapIcons() {
        return mapIcons;
    }

    public void refreshIcons() {
        lastMapIconUpdate = 0L;
    }

    public void setupComponents() {
        sortedComponents.clear();
        computeOrderedComponents();
    }

    /**
     * Handles the headache of sorting logic
     */
    private void computeOrderedComponents() {
        Map<Identifier, MinimapInfoComponent> componentMap = FTBChunksAPI.clientApi().getMinimapComponents().stream()
                .collect(Collectors.toMap(MinimapInfoComponent::id, Function.identity()));

        List<Identifier> order = FTBChunksClientConfig.MINIMAP_INFO_ORDER.get()
                .stream()
                .map(Identifier::parse)
                .collect(Collectors.toList());

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

        for (Identifier id : order) {
            MinimapInfoComponent minimapInfoComponent = componentMap.get(id);
            if (minimapInfoComponent != null && FTBChunksAPI.clientApi().isMinimapComponentEnabled(minimapInfoComponent)) {
                sortedComponents.add(minimapInfoComponent);
            }
        }
    }
}
