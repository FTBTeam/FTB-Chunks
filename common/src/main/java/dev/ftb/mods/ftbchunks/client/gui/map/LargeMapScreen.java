package dev.ftb.mods.ftbchunks.client.gui.map;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftbchunks.client.gui.AddWaypointOverlay;
import dev.ftb.mods.ftbchunks.client.map.*;
import dev.ftb.mods.ftbchunks.net.TeleportFromMapPacket;
import dev.ftb.mods.ftbchunks.util.HeightUtils;
import dev.ftb.mods.ftblibrary.client.config.editable.EditableColor;
import dev.ftb.mods.ftblibrary.client.config.editable.EditableString;
import dev.ftb.mods.ftblibrary.client.gui.input.Key;
import dev.ftb.mods.ftblibrary.client.gui.input.MouseButton;
import dev.ftb.mods.ftblibrary.client.gui.theme.NordTheme;
import dev.ftb.mods.ftblibrary.client.gui.theme.Theme;
import dev.ftb.mods.ftblibrary.client.gui.widget.BaseScreen;
import dev.ftb.mods.ftblibrary.client.gui.widget.ContextMenuItem;
import dev.ftb.mods.ftblibrary.client.gui.widget.ScreenWrapper;
import dev.ftb.mods.ftblibrary.client.icon.IconHelper;
import dev.ftb.mods.ftblibrary.client.util.ClientUtils;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.util.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class LargeMapScreen extends BaseScreen {
    private static final Color4I BACKGROUND_COLOR = Color4I.rgb(0x202225);

    private final RegionMapPanel regionPanel;
    private final MapScreenButtonPanel buttonPanel;
    private int zoom = 256;

    MapDimension dimension;
    int scrollWidth = 0;
    int scrollHeight = 0;
    int prevMouseX, prevMouseY;
    int grabbed = 0;

    private boolean movedToPlayer = false;
    private boolean needIconRefresh;
    private final int minZoom;

    private LargeMapScreen(MapDimension dim) {
        regionPanel = new RegionMapPanel(this);

        dimension = dim;
        regionPanel.setScrollX(0D);
        regionPanel.setScrollY(0D);

        buttonPanel = new MapScreenButtonPanel(this);

        minZoom = determineMinZoom();
    }

    public static void openMap() {
        MapDimension.getCurrent().ifPresentOrElse(
                mapDimension -> new LargeMapScreen(mapDimension).openGui(),
                () -> FTBChunks.LOGGER.warn("Missing MapDimension data?? not opening large map")
        );
    }

    @Override
    public Theme getTheme() {
        return NordTheme.THEME;
    }

    @Override
    public void onClosed() {
        super.onClosed();

        int autoRelease = FTBChunksClientConfig.AUTORELEASE_ON_MAP_CLOSE.get();
        if (autoRelease > 0) {
            dimension.getManager().scheduleRegionPurge(dimension);
        }
    }

    public ResourceKey<Level> currentDimension() {
        return dimension.dimension;
    }

    public int getRegionTileSize() {
        return zoom * 2;
    }

    public void addZoom(double up) {
        int prevZoom = zoom;

        if (up > 0D) {
            zoom *= 2;
        } else {
            zoom /= 2;
        }

        zoom = Mth.clamp(zoom, minZoom, 1024);

        if (zoom != prevZoom) {
            grabbed = 0;
            double sx = regionPanel.regionX;
            double sy = regionPanel.regionZ;
            regionPanel.resetScroll();
            regionPanel.scrollTo(sx, sy);

            Minecraft.getInstance().mouseHandler.mouseGrabbed = true;
            Minecraft.getInstance().mouseHandler.releaseMouse();
        }
    }

    @Override
    public void addWidgets() {
        add(regionPanel);
        add(buttonPanel);
    }

    void switchToDimension(ResourceKey<Level> key) {
        dimension = dimension.getManager().getDimension(key);
        refreshWidgets();
        movedToPlayer = false;
    }

    WaypointManagerImpl getWaypointManager() {
        return MapManager.getInstance().orElseThrow().getDimension(dimension.dimension).getWaypointManager();
    }

    @Override
    public void alignWidgets() {
        buttonPanel.setPosAndSize(0, 0, 18, height);
        buttonPanel.alignWidgets();
    }

    @Override
    public boolean onInit() {
        return setFullscreen();
    }

    @Override
    public boolean mousePressed(MouseButton button) {
        if (super.mousePressed(button)) {
            return true;
        }

        if (button.isLeft()) {
            prevMouseX = getMouseX();
            prevMouseY = getMouseY();
            return true;
        } else if (button.isRight()) {
            int fixedY = Math.max(regionPanel.blockY, ClientUtils.getClientLevel().getMinY());
            final BlockPos pos = new BlockPos(regionPanel.blockX, fixedY, regionPanel.blockZ);
            GlobalPos globalPos = GlobalPos.of(dimension.dimension, pos);
            List<ContextMenuItem> list = new ArrayList<>();
            Component title = Component.translatable("ftbchunks.gui.add_waypoint");
            list.add(new ContextMenuItem(title, Icons.ADD, btn -> {
                EditableString name = new EditableString();
                name.setValue("");
                EditableColor col = new EditableColor();
                col.setValue(Color4I.hsb(MathUtils.RAND.nextFloat(), 1F, 1F));
                AddWaypointOverlay.GlobalPosConfig globalPosConfig = new AddWaypointOverlay.GlobalPosConfig();
                globalPosConfig.setValue(globalPos);
                var overlay = new AddWaypointOverlay(getGui(), title, globalPosConfig, name, col, accepted -> {
                    if (accepted) {
                        MapDimension mapDimension = MapManager.getInstance().orElseThrow().getDimension(globalPosConfig.getValue().dimension());
                        WaypointImpl waypoint = new WaypointImpl(WaypointType.DEFAULT, mapDimension, globalPosConfig.getValue().pos())
                                .setName(name.getValue())
                                .setColor(col.getValue().rgba());
                        mapDimension.getWaypointManager().add(waypoint);
                        refreshWidgets();
                    }
                }).atMousePosition();
                overlay.setWidth(150);
                overlay.setX(Math.min(overlay.getX(), getWindow().getGuiScaledWidth() - 155));
                getGui().pushModalPanel(overlay);
            }));
            openContextMenu(list);
            return true;
        }

        return false;
    }

    @Override
    public boolean keyPressed(Key key) {
        if (super.keyPressed(key)) {
            return true;
        } else if (key.is(GLFW.GLFW_KEY_SPACE)) {
            movedToPlayer = false;
            return true;
        } else if (key.is(GLFW.GLFW_KEY_T)) {
            NetworkManager.sendToServer(new TeleportFromMapPacket(regionPanel.blockPos().above(), regionPanel.blockY == HeightUtils.UNKNOWN, dimension.dimension));
            closeGui(false);
            return true;
        } else if (key.is(GLFW.GLFW_KEY_G) && InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_F3)) {
            FTBChunksClientConfig.CHUNK_GRID.toggle();
            FTBChunksClientConfig.saveConfig();
            dimension.getManager().updateAllRegions(false);
            return true;
        } else if (FTBChunksClient.doesKeybindMatch(FTBChunksClient.INSTANCE.openMapKey, key.event()) && Platform.isForgeLike()) {
            // platform specific behaviour :(  why? ¯\_(ツ)_/¯
            closeGui(false);
            return true;
        }

        return false;
    }

    @Override
    public void tick() {
        super.tick();

        if (needIconRefresh) {
            regionPanel.refreshWidgets();
            needIconRefresh = false;
        }
    }

    @Override
    public boolean drawDefaultBackground(GuiGraphics graphics) {
        if (!movedToPlayer) {
            Player p = ClientUtils.getClientPlayer();
            regionPanel.resetScroll();
            regionPanel.scrollTo(p.getX() / 512D, p.getZ() / 512D);
            movedToPlayer = true;
        }

        IconHelper.renderIcon(BACKGROUND_COLOR, graphics, 0, 0, width, height);
        return false;
    }

    @Override
    public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
        if (grabbed != 0) {
            int mx = getMouseX();
            int my = getMouseY();

            if (scrollWidth > regionPanel.width) {
                regionPanel.setScrollX(Math.max(Math.min(regionPanel.getScrollX() + (prevMouseX - mx), scrollWidth - regionPanel.width), 0));
            }

            if (scrollHeight > regionPanel.height) {
                regionPanel.setScrollY(Math.max(Math.min(regionPanel.getScrollY() + (prevMouseY - my), scrollHeight - regionPanel.height), 0));
            }

            prevMouseX = mx;
            prevMouseY = my;
        }

        if (scrollWidth <= regionPanel.width) {
            regionPanel.setScrollX((scrollWidth - regionPanel.width) / 2D);
        }

        if (scrollHeight <= regionPanel.height) {
            regionPanel.setScrollY((scrollHeight - regionPanel.height) / 2D);
        }

        int s = getRegionTileSize();
        double ox = -regionPanel.getScrollX() % s;
        double oy = -regionPanel.getScrollY() % s;

        for (int gx = 0; gx <= (w / s) + 1; gx++) {
            graphics.vLine((int) (x + ox + gx * s), y, y + h, 0x64464646);
        }
        for (int gy = 0; gy <= (h / s) + 1; gy++) {
            graphics.hLine(x, x + w, (int) (y + oy + gy * s), 0x64464646);
        }
    }

    @Override
    public void drawForeground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
        String coords = "X: " + regionPanel.blockX + ", Y: " + (regionPanel.blockY == HeightUtils.UNKNOWN ? "??" : regionPanel.blockY) + ", Z: " + regionPanel.blockZ;

        if (regionPanel.blockY != HeightUtils.UNKNOWN) {
            MapRegion region = dimension.getRegion(XZ.regionFromBlock(regionPanel.blockX, regionPanel.blockZ));
            MapRegionData data = region.getData();

            if (data != null) {
                int waterLightAndBiome = data.waterLightAndBiome[regionPanel.blockIndex] & 0xFFFF;
                ResourceKey<Biome> biome = dimension.getManager().getBiomeKey(waterLightAndBiome);
                Block block = dimension.getManager().getBlock(data.getBlockIndex(regionPanel.blockIndex));
                coords = coords + " | " + I18n.get("biome." + biome.identifier().getNamespace() + "." + biome.identifier().getPath()) + " | " + I18n.get(block.getDescriptionId());

                if ((waterLightAndBiome & (1 << 15)) != 0) {
                    coords += " (in water)";
                }
            }
        }

        int coordsw = theme.getStringWidth(coords) / 2;

        IconHelper.renderIcon(BACKGROUND_COLOR.withAlpha(150), graphics, x + (w - coordsw) / 2, y + h - 6, coordsw + 4, 6);
        var poseStack = graphics.pose();
        poseStack.pushMatrix();
        poseStack.translate(x + (w - coordsw) / 2F + 2F, y + h - 5);
        poseStack.scale(0.5F, 0.5F);
        theme.drawString(graphics, coords, 0, 0, Theme.SHADOW);
        poseStack.popMatrix();

        if (FTBChunksClientConfig.DEBUG_INFO.get()) {
            long memory = MapManager.getInstance().map(MapManager::estimateMemoryUsage).orElse(0L);

            String memoryUsage = "Estimated Memory Usage: " + StringUtils.formatDouble00(memory / 1024D / 1024D) + " MB";
            int memoryUsagew = theme.getStringWidth(memoryUsage) / 2;

            IconHelper.renderIcon(BACKGROUND_COLOR.withAlpha(150), graphics, x + (w - memoryUsagew) - 2, y, memoryUsagew + 4, 6);

            poseStack.pushMatrix();
            poseStack.translate(x + (w - memoryUsagew) - 1F, y + 1);
            poseStack.scale(0.5F, 0.5F);
            theme.drawString(graphics, memoryUsage, 0, 0, Theme.SHADOW);
            poseStack.popMatrix();
        }

        if (zoom == minZoom && zoom > 1) {
            Component zoomWarn = Component.translatable("ftbchunks.zoom_warning");
            poseStack.pushMatrix();
            poseStack.translate(x + w / 2F, y + 1);
            poseStack.scale(0.5F, 0.5F);
            theme.drawString(graphics, zoomWarn, 0, 0, Color4I.rgb(0xF0C000), Theme.CENTERED);
            poseStack.popMatrix();
        }
    }

    public static void refreshIconsIfOpen() {
        if (Minecraft.getInstance().screen instanceof ScreenWrapper sw && sw.getGui() instanceof LargeMapScreen lms) {
            lms.refreshIcons();
        }
    }

    public void refreshIcons() {
        needIconRefresh = true;
    }

    private int determineMinZoom() {
        if (!FTBChunksClientConfig.MAX_ZOOM_CONSTRAINT.get()) {
            return 1;
        }

        // limit the possible zoom-out based on number of regions known about (i.e. which *could* be loaded,
        // taking up ~4MB RAM per region)

        // possible memory that could be used
        long potentialUsage = dimension.getLoadedRegions().size() * MapManager.MEMORY_PER_REGION;

        // note: this not necessarily the amount of memory that can be allocated, but this is a
        // fairly fuzzy check anyway
        long allocatedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long freeMem = Runtime.getRuntime().maxMemory() - allocatedMemory;

        long ratio = freeMem / Math.max(1, potentialUsage);

        FTBChunks.LOGGER.debug("large map: free mem = {}, potential usage = {}, ratio = {}", freeMem, potentialUsage, ratio);
        if (ratio < 8) {
            return 64;
        } else if (ratio < 16) {
            return 32;
        } else if (ratio < 32) {
            return 16;
        } else if (ratio < 64) {
            return 8;
        } else {
            return 1;
        }
    }
}
