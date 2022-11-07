package dev.ftb.mods.ftbchunks.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.client.map.MapRegion;
import dev.ftb.mods.ftbchunks.client.map.MapRegionData;
import dev.ftb.mods.ftbchunks.client.map.Waypoint;
import dev.ftb.mods.ftbchunks.data.HeightUtils;
import dev.ftb.mods.ftbchunks.net.TeleportFromMapPacket;
import dev.ftb.mods.ftblibrary.config.StringConfig;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigFromStringScreen;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * @author LatvianModder
 */
public class LargeMapScreen extends BaseScreen {
	public Color4I backgroundColor = Color4I.rgb(0x202225);

	public RegionMapPanel regionPanel;
	public int zoom = 256;
	public MapDimension dimension;
	public int scrollWidth = 0;
	public int scrollHeight = 0;
	public int prevMouseX, prevMouseY;
	public int grabbed = 0;
	public boolean movedToPlayer = false;
	public Button claimChunksButton, dimensionButton, waypointsButton, settingsButton, alliesButton, syncButton;

	public LargeMapScreen() {
		regionPanel = new RegionMapPanel(this);

		var dim = MapDimension.getCurrent();
		if (dim == null) {
			FTBChunks.LOGGER.warn("Closed large map screen to prevent map dimension manager crash");
			this.closeGui(false);
		}

		dimension = dim;
		regionPanel.setScrollX(0D);
		regionPanel.setScrollY(0D);
	}

	public int getRegionButtonSize() {
		return zoom * 2;
	}

	public void addZoom(double up) {
		int z = zoom;

		if (up > 0D) {
			zoom *= 2;
		} else {
			zoom /= 2;
		}

		zoom = Mth.clamp(zoom, 1, 1024);

		if (zoom != z) {
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

		add(claimChunksButton = new SimpleButton(this, Component.translatable("ftbchunks.gui.claimed_chunks"), Icons.MAP, (b, m) -> new ChunkScreen().openGui()));
		/*
		add(waypointsButton = new SimpleButton(this, I18n.format("ftbchunks.gui.waypoints"), GuiIcons.BEACON, (b, m) -> {
			Minecraft.getInstance().getToastGui().add(new SystemToast(SystemToast.Type.TUTORIAL_HINT, new StringTextComponent("WIP!"), null));
		}));
		 */

		// add(alliesButton = new SimpleButton(this, Component.translatable()("ftbchunks.gui.allies"), GuiIcons.FRIENDS, (b, m) -> {}));
		add(waypointsButton = new SimpleButton(this, Component.translatable("ftbchunks.gui.add_waypoint"), Icons.ADD, (b, m) -> {
			StringConfig name = new StringConfig();
			new EditConfigFromStringScreen<>(name, set -> {
				if (set) {
					Player player = Minecraft.getInstance().player;
					Waypoint w = new Waypoint(dimension, player.getBlockX(), player.getBlockY(), player.getBlockZ());
					w.name = name.value;
					w.color = Color4I.hsb(MathUtils.RAND.nextFloat(), 1F, 1F).rgba();
					dimension.getWaypointManager().add(w);
					refreshWidgets();
				}

				openGui();
			}).openGui();
		}));

		/*
		add(syncButton = new SimpleButton(this, new TranslationTextComponent("ftbchunks.gui.sync"), Icons.REFRESH, (b, m) -> {
			dimension.sync();
		}));
		 */

		add(dimensionButton = new SimpleButton(this, Component.literal(dimension.dimension.location().getPath().replace('_', ' ')), Icons.GLOBE, (b, m) -> {
			try {
				List<MapDimension> list = new ArrayList<>(dimension.manager.getDimensions().values());
				int i = list.indexOf(dimension);

				if (i != -1) {
					dimension = list.get(MathUtils.mod(i + (m.isLeft() ? 1 : -1), list.size()));
					refreshWidgets();
					movedToPlayer = false;
				}
			} catch (Exception ex) {
			}
		}));

		add(settingsButton = new SimpleButton(this, Component.translatable("ftbchunks.gui.settings"), Icons.SETTINGS, (b, m) -> FTBChunksClientConfig.openSettings(new ScreenWrapper(this))));
	}

	@Override
	public void alignWidgets() {
		claimChunksButton.setPosAndSize(1, 1, 16, 16);
		// alliesButton.setPosAndSize(1, 19, 16, 16);
		//waypointsButton.setPosAndSize(1, 37, 16, 16);
		//syncButton.setPosAndSize(1, 55, 16, 16);
		waypointsButton.setPosAndSize(1, 19, 16, 16);
		//syncButton.setPosAndSize(1, 37, 16, 16);
		dimensionButton.setPosAndSize(1, height - 36, 16, 16);
		settingsButton.setPosAndSize(1, height - 18, 16, 16);
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
			List<ContextMenuItem> list = new ArrayList<>();
			list.add(new ContextMenuItem(Component.translatable("ftbchunks.gui.add_waypoint"), Icons.ADD, () -> {
				StringConfig name = new StringConfig();
				new EditConfigFromStringScreen<>(name, set -> {
					if (set) {
						Waypoint w = new Waypoint(dimension, regionPanel.blockX, regionPanel.blockY, regionPanel.blockZ);
						w.name = name.value;
						w.color = Color4I.hsb(MathUtils.RAND.nextFloat(), 1F, 1F).rgba();
						dimension.getWaypointManager().add(w);
						refreshWidgets();
					}

					openGui();
				}).openGui();
			}));
			openContextMenu(list);
			return true;
		}

		return false;
	}

	@Override
	public boolean keyPressed(Key key) {
		if (FTBChunksClient.openMapKey.matches(key.keyCode, key.scanCode) || key.escOrInventory()) {
			if (key.esc() && contextMenu != null) {
				closeContextMenu();
			} else {
				closeGui(false);
			}
			return true;
		} else if (key.is(GLFW.GLFW_KEY_SPACE)) {
			movedToPlayer = false;
			return true;
		} else if (super.keyPressed(key)) {
			return true;
		} else if (key.is(GLFW.GLFW_KEY_T)) {
			new TeleportFromMapPacket(regionPanel.blockX, regionPanel.blockY + 1, regionPanel.blockZ, regionPanel.blockY == HeightUtils.UNKNOWN, dimension.dimension).sendToServer();
			closeGui(false);
			return true;
		} else if (key.is(GLFW.GLFW_KEY_G) && InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_F3)) {
			FTBChunksClientConfig.CHUNK_GRID.toggle();
			FTBChunksClientConfig.saveConfig();
			dimension.manager.updateAllRegions(false);
			return true;
		}

		return false;
	}

	@Override
	public boolean drawDefaultBackground(PoseStack matrixStack) {
		if (!movedToPlayer) {
			Player p = Minecraft.getInstance().player;
			regionPanel.resetScroll();
			regionPanel.scrollTo(p.getX() / 512D, p.getZ() / 512D);
			movedToPlayer = true;
		}

		backgroundColor.draw(matrixStack, 0, 0, width, height);
		return false;
	}

	@Override
	public void drawBackground(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
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

		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder buffer = tessellator.getBuilder();
		int r = 70;
		int g = 70;
		int b = 70;
		int a = 100;

		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
		RenderSystem.disableTexture();
		buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

		int s = getRegionButtonSize();
		double ox = -regionPanel.getScrollX() % s;
		double oy = -regionPanel.getScrollY() % s;

		for (int gx = 0; gx <= (w / s) + 1; gx++) {
			buffer.vertex(x + ox + gx * s, y, 0).color(r, g, b, a).endVertex();
			buffer.vertex(x + ox + gx * s, y + h, 0).color(r, g, b, a).endVertex();
		}

		for (int gy = 0; gy <= (h / s) + 1; gy++) {
			buffer.vertex(x, y + oy + gy * s, 0).color(r, g, b, a).endVertex();
			buffer.vertex(x + w, y + oy + gy * s, 0).color(r, g, b, a).endVertex();
		}

		tessellator.end();
		RenderSystem.enableTexture();
	}

	@Override
	public void drawForeground(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
		String coords = "X: " + regionPanel.blockX + ", Y: " + (regionPanel.blockY == HeightUtils.UNKNOWN ? "??" : regionPanel.blockY) + ", Z: " + regionPanel.blockZ;

		if (regionPanel.blockY != HeightUtils.UNKNOWN) {
			MapRegion region = dimension.getRegion(XZ.regionFromBlock(regionPanel.blockX, regionPanel.blockZ));
			MapRegionData data = region.getData();

			if (data != null) {
				int waterLightAndBiome = data.waterLightAndBiome[regionPanel.blockIndex] & 0xFFFF;
				ResourceKey<Biome> biome = dimension.manager.getBiomeKey(waterLightAndBiome);
				Block block = dimension.manager.getBlock(data.getBlockIndex(regionPanel.blockIndex));
				coords = coords + " | " + I18n.get("biome." + biome.location().getNamespace() + "." + biome.location().getPath()) + " | " + I18n.get(block.getDescriptionId());

				if ((waterLightAndBiome & (1 << 15)) != 0) {
					coords += " (in water)";
				}
			}
		}

		int coordsw = theme.getStringWidth(coords) / 2;

		backgroundColor.withAlpha(150).draw(matrixStack, x + (w - coordsw) / 2, y + h - 6, coordsw + 4, 6);
		matrixStack.pushPose();
		matrixStack.translate(x + (w - coordsw) / 2F + 2F, y + h - 5, 0F);
		matrixStack.scale(0.5F, 0.5F, 1F);
		theme.drawString(matrixStack, coords, 0, 0, Theme.SHADOW);
		matrixStack.popPose();

		if (FTBChunksClientConfig.DEBUG_INFO.get()) {
			long memory = 0L;

			for (MapDimension dim : dimension.manager.getDimensions().values()) {
				for (MapRegion region : dim.getLoadedRegions()) {
					if (region.isDataLoaded()) {
						memory += 2L * 2L * 512L * 512L; // height, waterLightAndBiome
						memory += 3L * 4L * 512L * 512L; // foliage, grass, water
					}
				}
			}

			String memoryUsage = "Estimated Memory Usage: " + StringUtils.formatDouble00(memory / 1024D / 1024D) + " MB";
			int memoryUsagew = theme.getStringWidth(memoryUsage) / 2;

			backgroundColor.withAlpha(150).draw(matrixStack, x + (w - memoryUsagew) - 2, y, memoryUsagew + 4, 6);

			matrixStack.pushPose();
			matrixStack.translate(x + (w - memoryUsagew) - 1F, y + 1, 0F);
			matrixStack.scale(0.5F, 0.5F, 1F);
			theme.drawString(matrixStack, memoryUsage, 0, 0, Theme.SHADOW);
			matrixStack.popPose();
		}
	}
}
