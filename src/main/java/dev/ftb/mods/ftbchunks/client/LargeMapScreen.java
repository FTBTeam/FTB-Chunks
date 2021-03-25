package dev.ftb.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbguilibrary.config.ConfigString;
import com.feed_the_beast.mods.ftbguilibrary.config.gui.GuiEditConfigFromString;
import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import com.feed_the_beast.mods.ftbguilibrary.utils.Key;
import com.feed_the_beast.mods.ftbguilibrary.utils.MathUtils;
import com.feed_the_beast.mods.ftbguilibrary.utils.MouseButton;
import com.feed_the_beast.mods.ftbguilibrary.utils.StringUtils;
import com.feed_the_beast.mods.ftbguilibrary.widget.Button;
import com.feed_the_beast.mods.ftbguilibrary.widget.ContextMenuItem;
import com.feed_the_beast.mods.ftbguilibrary.widget.GuiBase;
import com.feed_the_beast.mods.ftbguilibrary.widget.GuiIcons;
import com.feed_the_beast.mods.ftbguilibrary.widget.SimpleButton;
import com.feed_the_beast.mods.ftbguilibrary.widget.Theme;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.client.map.MapRegion;
import dev.ftb.mods.ftbchunks.client.map.MapRegionData;
import dev.ftb.mods.ftbchunks.client.map.Waypoint;
import dev.ftb.mods.ftbchunks.impl.XZ;
import dev.ftb.mods.ftbchunks.net.FTBChunksNet;
import dev.ftb.mods.ftbchunks.net.RequestPlayerListPacket;
import dev.ftb.mods.ftbchunks.net.TeleportFromMapPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * @author LatvianModder
 */
public class LargeMapScreen extends GuiBase {
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
		dimension = MapDimension.getCurrent();
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

			ObfuscationReflectionHelper.setPrivateValue(MouseHandler.class, Minecraft.getInstance().mouseHandler, true, "field_198051_p"); // FIXME
			Minecraft.getInstance().mouseHandler.releaseMouse();
		}
	}

	@Override
	public void addWidgets() {
		add(regionPanel);

		add(claimChunksButton = new SimpleButton(this, new TranslatableComponent("ftbchunks.gui.claimed_chunks"), GuiIcons.MAP, (b, m) -> new ChunkScreen().openGui()));
		/*
		add(waypointsButton = new SimpleButton(this, I18n.format("ftbchunks.gui.waypoints"), GuiIcons.BEACON, (b, m) -> {
			Minecraft.getInstance().getToastGui().add(new SystemToast(SystemToast.Type.TUTORIAL_HINT, new StringTextComponent("WIP!"), null));
		}));
		 */

		add(alliesButton = new SimpleButton(this, new TranslatableComponent("ftbchunks.gui.allies"), GuiIcons.FRIENDS, (b, m) -> FTBChunksNet.MAIN.sendToServer(new RequestPlayerListPacket())));
		add(waypointsButton = new SimpleButton(this, new TranslatableComponent("ftbchunks.gui.add_waypoint"), GuiIcons.ADD, (b, m) -> {
			ConfigString name = new ConfigString();
			new GuiEditConfigFromString<>(name, set -> {
				if (set) {
					Player player = Minecraft.getInstance().player;
					Waypoint w = new Waypoint(dimension);
					w.name = name.value;
					w.color = Color4I.hsb(MathUtils.RAND.nextFloat(), 1F, 1F).rgba();
					w.x = Mth.floor(player.getX());
					w.y = Mth.floor(player.getY());
					w.z = Mth.floor(player.getZ());
					dimension.getWaypoints().add(w);
					dimension.saveData = true;
					refreshWidgets();
				}

				openGui();
			}).openGui();
		}));

		add(syncButton = new SimpleButton(this, /*new TranslationTextComponent("ftbchunks.gui.sync")*/new TextComponent("Currently disabled due to a bug!"), GuiIcons.REFRESH, (b, m) -> {
			// dimension.sync();
		}));

		add(dimensionButton = new SimpleButton(this, new TextComponent(dimension.dimension.location().getPath().replace('_', ' ')), GuiIcons.GLOBE, (b, m) -> {
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

		add(settingsButton = new SimpleButton(this, new TranslatableComponent("ftbchunks.gui.settings"), GuiIcons.SETTINGS, (b, m) -> FTBChunksClientConfig.openSettings()));
	}

	@Override
	public void alignWidgets() {
		claimChunksButton.setPosAndSize(1, 1, 16, 16);
		alliesButton.setPosAndSize(1, 19, 16, 16);
		waypointsButton.setPosAndSize(1, 37, 16, 16);
		syncButton.setPosAndSize(1, 55, 16, 16);
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
			int clickBlockX = regionPanel.blockX;
			int clickBlockZ = regionPanel.blockZ;
			List<ContextMenuItem> list = new ArrayList<>();
			list.add(new ContextMenuItem(new TranslatableComponent("ftbchunks.gui.add_waypoint"), GuiIcons.ADD, () -> {
				ConfigString name = new ConfigString();
				new GuiEditConfigFromString<>(name, set -> {
					if (set) {
						Waypoint w = new Waypoint(dimension);
						w.name = name.value;
						w.color = Color4I.hsb(MathUtils.RAND.nextFloat(), 1F, 1F).rgba();
						w.x = clickBlockX;
						w.y = regionPanel.blockY;
						w.z = clickBlockZ;
						dimension.getWaypoints().add(w);
						dimension.saveData = true;
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
		if (key.is(GLFW.GLFW_KEY_T)) {
			FTBChunksNet.MAIN.sendToServer(new TeleportFromMapPacket(regionPanel.blockX, regionPanel.blockZ, dimension.dimension));
			closeGui(false);
			return true;
		} else if (key.is(FTBChunksClient.openMapKey.getKey().getValue())) {
			closeGui(true);
			return true;
		} else if (key.is(GLFW.GLFW_KEY_W)) {
			return true;
		} else if (key.is(GLFW.GLFW_KEY_SPACE)) {
			movedToPlayer = false;
			return true;
		}

		return super.keyPressed(key);
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

		RenderSystem.disableTexture();
		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder buffer = tessellator.getBuilder();
		int r = 70;
		int g = 70;
		int b = 70;
		int a = 100;

		buffer.begin(GL11.GL_LINES, DefaultVertexFormat.POSITION_COLOR);

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
		String coords = "X: " + regionPanel.blockX + ", Y: " + (regionPanel.blockY == 0 ? "??" : regionPanel.blockY) + ", Z: " + regionPanel.blockZ;

		if (regionPanel.blockY != 0) {
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

		if (FTBChunksClientConfig.debugInfo) {
			long memory = 0L;

			for (MapDimension dim : dimension.manager.getDimensions().values()) {
				for (MapRegion region : dim.getLoadedRegions()) {
					if (region.data != null) {
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