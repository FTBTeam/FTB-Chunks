package dev.ftb.mods.ftbchunks.client.gui.map;

import dev.ftb.mods.ftbchunks.api.client.event.MapIconEvent;
import dev.ftb.mods.ftbchunks.api.client.icon.MapIcon;
import dev.ftb.mods.ftbchunks.api.client.icon.MapType;
import dev.ftb.mods.ftbchunks.client.map.MapChunk;
import dev.ftb.mods.ftbchunks.client.map.MapRegion;
import dev.ftb.mods.ftbchunks.client.map.MapRegionData;
import dev.ftb.mods.ftbchunks.client.mapicon.MapIconComparator;
import dev.ftb.mods.ftbchunks.util.HeightUtils;
import dev.ftb.mods.ftblibrary.client.gui.input.MouseButton;
import dev.ftb.mods.ftblibrary.client.gui.theme.Theme;
import dev.ftb.mods.ftblibrary.client.gui.widget.Panel;
import dev.ftb.mods.ftblibrary.client.gui.widget.Widget;
import dev.ftb.mods.ftblibrary.client.util.ClientUtils;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class RegionMapPanel extends Panel {
	final LargeMapScreen largeMapScreen;
	double regionX = 0;
	double regionZ = 0;
	int regionMinX, regionMinZ, regionMaxX, regionMaxZ;
	int blockX = 0;
	int blockY = HeightUtils.UNKNOWN;
	int blockZ = 0;
	int blockIndex = 0;
	private final List<MapIcon> mapIcons;

	public RegionMapPanel(LargeMapScreen largeMapScreen) {
		super(largeMapScreen);
		this.largeMapScreen = largeMapScreen;
		mapIcons = new ArrayList<>();
	}

	public void updateMinMax() {
		regionMinX = Integer.MAX_VALUE;
		regionMinZ = Integer.MAX_VALUE;
		regionMaxX = Integer.MIN_VALUE;
		regionMaxZ = Integer.MIN_VALUE;

		for (Widget w : widgets) {
			if (w instanceof MapTileWidget tileWidget) {
				int qx = tileWidget.region.pos.x();
				int qy = tileWidget.region.pos.z();

				regionMinX = Math.min(regionMinX, qx);
				regionMinZ = Math.min(regionMinZ, qy);
				regionMaxX = Math.max(regionMaxX, qx);
				regionMaxZ = Math.max(regionMaxZ, qy);
			}
		}

		if (regionMinX == Integer.MAX_VALUE) {
			regionMinX = regionMinZ = regionMaxX = regionMaxZ = 0;
		}

		regionMinX -= 100;
		regionMinZ -= 100;
		regionMaxX += 101;
		regionMaxZ += 101;
	}

	public BlockPos blockPos() {
		return new BlockPos(blockX, blockY, blockZ);
	}

	public void scrollTo(double x, double y) {
		updateMinMax();

		double dx = (regionMaxX - regionMinX);
		double dy = (regionMaxZ - regionMinZ);

		setScrollX((x - regionMinX) / dx * largeMapScreen.scrollWidth - width / 2D);
		setScrollY((y - regionMinZ) / dy * largeMapScreen.scrollHeight - height / 2D);
	}

	public void resetScroll() {
		alignWidgets();
		setScrollX((largeMapScreen.scrollWidth - width) / 2D);
		setScrollY((largeMapScreen.scrollHeight - height) / 2D);
	}

	@Override
	public void addWidgets() {
		for (MapRegion region : largeMapScreen.dimension.getRegions().values()) {
			add(new MapTileWidget(this, region));
		}

		Player player = ClientUtils.getClientPlayer();

		mapIcons.clear();
		MapIconEvent.LARGE_MAP.invoker().accept(new MapIconEvent(largeMapScreen.dimension.dimension, mapIcons, MapType.LARGE_MAP));

		if (mapIcons.size() >= 2) {
			mapIcons.sort(new MapIconComparator(player.position(), 1F));
		}

		for (MapIcon icon : mapIcons) {
			if (icon.isVisible(MapType.LARGE_MAP, MathUtils.dist(player.getX(), player.getZ(), icon.getPos(1F).x, icon.getPos(1F).z), false)) {
				add(new MapIconWidget(this, icon));
			}
		}
	}

	@Override
	public void alignWidgets() {
		largeMapScreen.scrollWidth = 0;
		largeMapScreen.scrollHeight = 0;

		updateMinMax();

		int buttonSize = largeMapScreen.getRegionTileSize();

		largeMapScreen.scrollWidth = (regionMaxX - regionMinX) * buttonSize;
		largeMapScreen.scrollHeight = (regionMaxZ - regionMinZ) * buttonSize;

		for (Widget w : widgets) {
			if (w instanceof MapTileWidget tileWidget) {
				double qx = tileWidget.region.pos.x();
				double qy = tileWidget.region.pos.z();
				double qw = 1D;
				double qh = 1D;

				double x = (qx - regionMinX) * buttonSize;
				double y = (qy - regionMinZ) * buttonSize;
				w.setPosAndSize((int) x, (int) y, (int) (buttonSize * qw), (int) (buttonSize * qh));
			} else if (w instanceof MapIconWidget iconWidget) {
				MapIcon mapIcon = iconWidget.getMapIcon();
				double s = Math.max(mapIcon.isZoomDependant(MapType.LARGE_MAP) ? 0D : 6D, buttonSize / 128D * mapIcon.getIconScale(MapType.LARGE_MAP));

				if (s <= 1D) {
					w.setSize(0, 0);
				} else {
					w.setSize(Mth.ceil(s), Mth.ceil(s));
					iconWidget.updatePosition(1F);
				}
			}
		}

		setPosAndSize(0, 0, parent.width, parent.height);
	}

	@Override
	public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
		super.draw(graphics, theme, x, y, w, h);

		int dx = (regionMaxX - regionMinX);
		int dy = (regionMaxZ - regionMinZ);

		double px = getScrollX() - getX();
		double py = getScrollY() - getY();

		regionX = (parent.getMouseX() + px) / (double) largeMapScreen.scrollWidth * dx + regionMinX;
		regionZ = (parent.getMouseY() + py) / (double) largeMapScreen.scrollHeight * dy + regionMinZ;
		blockX = Mth.floor(regionX * 512D);
		blockZ = Mth.floor(regionZ * 512D);
		blockIndex = (blockX & 511) + (blockZ & 511) * 512;
		blockY = HeightUtils.UNKNOWN;

		MapRegion region = largeMapScreen.dimension.getRegions().get(XZ.regionFromBlock(blockX, blockZ));
		if (region != null) {
			MapRegionData data = region.getData();
			if (data != null) {
				blockY = data.height[blockIndex];
			}
		}
	}

	@Override
	public void addMouseOverText(TooltipList list) {
		super.addMouseOverText(list);

		MapRegion mapRegion = largeMapScreen.dimension.getRegions().get(XZ.regionFromBlock(blockX, blockZ));
		if (mapRegion != null) {
			MapRegionData data = mapRegion.getData();
			if (data != null) {
				MapChunk mapChunk = mapRegion.getMapChunk(XZ.of((blockX >> 4) & 31, (blockZ >> 4) & 31));
				Team team = mapChunk == null ? null : mapChunk.getTeam().orElse(null);
				if (team != null) {
					list.add(team.getName());
				}
			}
		}
	}

	@Override
	public boolean mousePressed(MouseButton button) {
		if (super.mousePressed(button)) {
			return true;
		}

		if (button.isLeft() && isMouseOver()) {
			largeMapScreen.prevMouseX = getMouseX();
			largeMapScreen.prevMouseY = getMouseY();
			largeMapScreen.grabbed = 1;
			return true;
		}

		return false;
	}

	@Override
	public void mouseReleased(MouseButton button) {
		super.mouseReleased(button);
		largeMapScreen.grabbed = 0;
	}

	@Override
	public boolean scrollPanel(double scroll) {
		if (isMouseOver()) {
			largeMapScreen.addZoom(scroll);
			return true;
		}

		return false;
	}
}
