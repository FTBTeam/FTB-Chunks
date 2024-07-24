package dev.ftb.mods.ftbchunks.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftbchunks.client.map.*;
import dev.ftb.mods.ftbchunks.net.TeleportFromMapPacket;
import dev.ftb.mods.ftbchunks.util.HeightUtils;
import dev.ftb.mods.ftblibrary.config.ColorConfig;
import dev.ftb.mods.ftblibrary.config.StringConfig;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.ui.misc.KeyReferenceScreen;
import dev.ftb.mods.ftblibrary.util.StringUtils;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
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
	private int zoom = 256;

	MapDimension dimension;
	int scrollWidth = 0;
	int scrollHeight = 0;
	int prevMouseX, prevMouseY;
	int grabbed = 0;

	private boolean movedToPlayer = false;
	private Button claimChunksButton;
	private Button dimensionButton;
	private Button waypointManagerButton;
	private Button infoButton;
	private Button settingsButton;
	private Button serverSettingsButton;
	private Button clearDeathpointsButton;
	private Button infoSortScreen;
	private boolean needIconRefresh;
	private final int minZoom;

	private LargeMapScreen(MapDimension dim) {
		regionPanel = new RegionMapPanel(this);

		dimension = dim;
		regionPanel.setScrollX(0D);
		regionPanel.setScrollY(0D);

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
		if (autoRelease > 0 && dimension != null) {
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

		add(claimChunksButton = new SimpleTooltipButton(this, Component.translatable("ftbchunks.gui.claimed_chunks"), Icons.MAP,
				(b, m) -> ChunkScreen.openChunkScreen(),
				Component.literal("[C]").withStyle(ChatFormatting.GRAY)));

		Component tooltip = Component.literal("[")
				.append(FTBChunksClient.INSTANCE.waypointManagerKey.getTranslatedKeyMessage())
				.append(Component.literal("]")).withStyle(ChatFormatting.GRAY);
		add(waypointManagerButton = new SimpleTooltipButton(this, Component.translatable("ftbchunks.gui.waypoints"), Icons.COMPASS,
				(b, m) -> new WaypointEditorScreen().openGui(), tooltip));
		add(infoButton = new SimpleButton(this, Component.translatable("ftbchunks.gui.large_map_info"), Icons.INFO,
				(b, m) -> new MapKeyReferenceScreen().openGui()));
		add(infoSortScreen = new SimpleTooltipButton(this, Component.translatable("ftbchunks.gui.sort_minimap_info"), Icons.BOOK,
				(b, m) -> new MinimapInfoSortScreen().openGui(), tooltip));
		add(clearDeathpointsButton = new ClearDeathPointButton(this));

        /*
		add(syncButton = new SimpleButton(this, new TranslationTextComponent("ftbchunks.gui.sync"), Icons.REFRESH, (b, m) -> {
			dimension.sync();
		}));
		 */

		Component dimName = Component.literal(dimension.dimension.location().getPath().replace('_', ' '));
		add(dimensionButton = new SimpleButton(this, dimName, Icons.GLOBE,
				(b, m) -> cycleVisibleDimension(m)));

		add(settingsButton = new SimpleTooltipButton(this, Component.translatable("ftbchunks.gui.settings"), Icons.SETTINGS,
				(b, m) -> FTBChunksClientConfig.openSettings(new ScreenWrapper(this)),
				Component.literal("[S]").withStyle(ChatFormatting.GRAY))
		);

		if (Minecraft.getInstance().player.hasPermissions(2)) {
			add(serverSettingsButton = new SimpleTooltipButton(this, Component.translatable("ftbchunks.gui.settings.server"),
					Icons.SETTINGS.withTint(Color4I.rgb(0xA040FF)),
					(b, m) -> FTBChunksClientConfig.openServerSettings(new ScreenWrapper(this)),
					Component.literal("[Ctrl + S]").withStyle(ChatFormatting.GRAY)
			));
		}
	}

	private WaypointManagerImpl getWaypointManager() {
		return MapManager.getInstance().orElseThrow().getDimension(dimension.dimension).getWaypointManager();
	}

	private void cycleVisibleDimension(MouseButton m) {
		try {
			List<MapDimension> list = new ArrayList<>(dimension.getManager().getDimensions().values());
			int i = list.indexOf(dimension);

			if (i != -1) {
				dimension = list.get(MathUtils.mod(i + (m.isLeft() ? 1 : -1), list.size()));
				refreshWidgets();
				movedToPlayer = false;
			}
		} catch (Exception ignored) {
		}
	}

	@Override
	public void alignWidgets() {
		// alliesButton.setPosAndSize(1, 19, 16, 16);
		// syncButton.setPosAndSize(1, 55, 16, 16);

		claimChunksButton.setPosAndSize(1, 1, 16, 16);
		waypointManagerButton.setPosAndSize(1, 19, 16, 16);
		infoButton.setPosAndSize(1, 37, 16, 16);
		infoSortScreen.setPosAndSize(1, 55, 16, 16);
		clearDeathpointsButton.setPosAndSize(1, 73, 16, 16);

		dimensionButton.setPosAndSize(1, height - 36, 16, 16);
		settingsButton.setPosAndSize(1, height - 18, 16, 16);
		if (serverSettingsButton != null) {
			serverSettingsButton.setPosAndSize(width - 18, height - 18, 16, 16);
		}
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
			final BlockPos pos = new BlockPos(regionPanel.blockX, regionPanel.blockY, regionPanel.blockZ);
			List<ContextMenuItem> list = new ArrayList<>();
			list.add(new ContextMenuItem(Component.translatable("ftbchunks.gui.add_waypoint"), Icons.ADD, btn -> {
				StringConfig name = new StringConfig();
				name.setValue("");
				ColorConfig col = new ColorConfig();
				col.setValue(Color4I.hsb(MathUtils.RAND.nextFloat(), 1F, 1F));
				var overlay = new AddWaypointOverlay(getGui(), name, col, accepted -> {
                    if (accepted) {
                        WaypointImpl waypoint = new WaypointImpl(WaypointType.DEFAULT, dimension, pos)
                                .setName(name.getValue())
                                .setColor(col.getValue().rgba());
                        dimension.getWaypointManager().add(waypoint);
                        refreshWidgets();
                    }
                }).atMousePosition();
				overlay.setWidth(150);
				overlay.setX(Math.min(overlay.getX(), getScreen().getGuiScaledWidth() - 155));
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
			new TeleportFromMapPacket(regionPanel.blockPos().above(), regionPanel.blockY == HeightUtils.UNKNOWN, dimension.dimension).sendToServer();
			closeGui(false);
			return true;
		} else if (key.is(GLFW.GLFW_KEY_G) && InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_F3)) {
			FTBChunksClientConfig.CHUNK_GRID.toggle();
			FTBChunksClientConfig.saveConfig();
			dimension.getManager().updateAllRegions(false);
			return true;
		} else if (key.is(GLFW.GLFW_KEY_C)) {
			claimChunksButton.onClicked(MouseButton.LEFT);
			return true;
		} else if (key.is(GLFW.GLFW_KEY_S)) {
			if (Screen.hasControlDown()) {
				if (serverSettingsButton != null) {
					serverSettingsButton.onClicked(MouseButton.LEFT);
				}
			} else {
				settingsButton.onClicked(MouseButton.LEFT);
			}
			return true;
		} else if (FTBChunksClient.doesKeybindMatch(FTBChunksClient.INSTANCE.waypointManagerKey, key)) {
			waypointManagerButton.onClicked(MouseButton.LEFT);
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
			Player p = Minecraft.getInstance().player;
			regionPanel.resetScroll();
			regionPanel.scrollTo(p.getX() / 512D, p.getZ() / 512D);
			movedToPlayer = true;
		}

		BACKGROUND_COLOR.draw(graphics, 0, 0, width, height);
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
				coords = coords + " | " + I18n.get("biome." + biome.location().getNamespace() + "." + biome.location().getPath()) + " | " + I18n.get(block.getDescriptionId());

				if ((waterLightAndBiome & (1 << 15)) != 0) {
					coords += " (in water)";
				}
			}
		}

		int coordsw = theme.getStringWidth(coords) / 2;

		BACKGROUND_COLOR.withAlpha(150).draw(graphics, x + (w - coordsw) / 2, y + h - 6, coordsw + 4, 6);
		PoseStack poseStack = graphics.pose();
		poseStack.pushPose();
		poseStack.translate(x + (w - coordsw) / 2F + 2F, y + h - 5, 0F);
		poseStack.scale(0.5F, 0.5F, 1F);
		theme.drawString(graphics, coords, 0, 0, Theme.SHADOW);
		poseStack.popPose();

		if (FTBChunksClientConfig.DEBUG_INFO.get()) {
			long memory = MapManager.getInstance().map(MapManager::estimateMemoryUsage).orElse(0L);

			String memoryUsage = "Estimated Memory Usage: " + StringUtils.formatDouble00(memory / 1024D / 1024D) + " MB";
			int memoryUsagew = theme.getStringWidth(memoryUsage) / 2;

			BACKGROUND_COLOR.withAlpha(150).draw(graphics, x + (w - memoryUsagew) - 2, y, memoryUsagew + 4, 6);

			poseStack.pushPose();
			poseStack.translate(x + (w - memoryUsagew) - 1F, y + 1, 0F);
			poseStack.scale(0.5F, 0.5F, 1F);
			theme.drawString(graphics, memoryUsage, 0, 0, Theme.SHADOW);
			poseStack.popPose();
		}

		if (zoom == minZoom && zoom > 1) {
			Component zoomWarn = Component.translatable("ftbchunks.zoom_warning");
			poseStack.pushPose();
			poseStack.translate(x + w / 2F, y + 1, 0F);
			poseStack.scale(0.5F, 0.5F, 1F);
			theme.drawString(graphics, zoomWarn, 0, 0, Color4I.rgb(0xF0C000), Theme.CENTERED);
			poseStack.popPose();
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

	private static class SimpleTooltipButton extends SimpleButton {
		private final List<Component> tooltipLines;

		public SimpleTooltipButton(Panel panel, Component text, Icon icon, Callback c, Component tooltipLine) {
			super(panel, text, icon, c);
			this.tooltipLines = List.of(tooltipLine);
		}

		@Override
		public void addMouseOverText(TooltipList list) {
			super.addMouseOverText(list);
			tooltipLines.forEach(list::add);
		}
	}

	private static class MapKeyReferenceScreen extends KeyReferenceScreen {
		public MapKeyReferenceScreen() {
			super("ftbchunks.gui.large_map_info.text");
		}

		@Override
		public Component getTitle() {
			return Component.translatable("ftbchunks.gui.large_map_info");
		}
	}

	private class ClearDeathPointButton extends SimpleButton {
		public ClearDeathPointButton(Panel panel) {
			super(panel, Component.translatable("ftbchunks.gui.clear_deathpoints"), Icons.CLOSE, (b, m) -> {
				if (getWaypointManager().removeIf(wp -> wp.getType() == WaypointType.DEATH)) {
					refreshWidgets();
				}
			});
		}

		@Override
		public boolean shouldDraw() {
			return super.shouldDraw() && getWaypointManager().hasDeathpoint();
		}

		@Override
		public boolean isEnabled() {
			return shouldDraw();
		}
	}

}
