package dev.ftb.mods.ftbchunks.client.gui;

import dev.ftb.mods.ftbchunks.api.event.MapIconEvent;
import dev.ftb.mods.ftbchunks.client.MapType;
import dev.ftb.mods.ftbchunks.client.map.MapChunk;
import dev.ftb.mods.ftbchunks.client.map.MapRegion;
import dev.ftb.mods.ftbchunks.client.map.MapRegionData;
import dev.ftb.mods.ftbchunks.client.mapicon.MapIcon;
import dev.ftb.mods.ftbchunks.client.mapicon.MapIconComparator;
import dev.ftb.mods.ftbchunks.util.HeightUtils;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * @author LatvianModder
 */
public class RegionMapPanel extends Panel {
	final LargeMapScreen largeMap;
	double regionX = 0;
	double regionZ = 0;
	int regionMinX, regionMinZ, regionMaxX, regionMaxZ;
	int blockX = 0;
	int blockY = HeightUtils.UNKNOWN;
	int blockZ = 0;
	int blockIndex = 0;
//	public Rect2i visibleArea = new Rect2i(0, 0, 0, 0);
	private final List<MapIcon> mapIcons;

	public RegionMapPanel(LargeMapScreen panel) {
		super(panel);
		largeMap = panel;
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

		setScrollX((x - regionMinX) / dx * largeMap.scrollWidth - width / 2D);
		setScrollY((y - regionMinZ) / dy * largeMap.scrollHeight - height / 2D);
	}

	public void resetScroll() {
		alignWidgets();
		setScrollX((largeMap.scrollWidth - width) / 2D);
		setScrollY((largeMap.scrollHeight - height) / 2D);
	}

	@Override
	public void addWidgets() {
		for (MapRegion region : largeMap.dimension.getRegions().values()) {
			add(new MapTileWidget(this, region));
		}

		Minecraft mc = Minecraft.getInstance();

		mapIcons.clear();
		MapIconEvent.LARGE_MAP.invoker().accept(new MapIconEvent(largeMap.dimension, mapIcons, MapType.LARGE_MAP));

		if (mapIcons.size() >= 2) {
			mapIcons.sort(new MapIconComparator(mc.player.position(), 1F));
		}

		for (MapIcon icon : mapIcons) {
			if (icon.isVisible(MapType.LARGE_MAP, MathUtils.dist(mc.player.getX(), mc.player.getZ(), icon.getPos(1F).x, icon.getPos(1F).z), false)) {
				add(new MapIconWidget(this, icon));
			}
		}

		alignWidgets();
	}

	@Override
	public void alignWidgets() {
		largeMap.scrollWidth = 0;
		largeMap.scrollHeight = 0;

		updateMinMax();

		int buttonSize = largeMap.getRegionTileSize();

		largeMap.scrollWidth = (regionMaxX - regionMinX) * buttonSize;
		largeMap.scrollHeight = (regionMaxZ - regionMinZ) * buttonSize;

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

//		int topBlockX = Mth.floor((px / (double) largeMap.scrollWidth * dx + regionMinX) * 512D);
//		int topBlockZ = Mth.floor((py / (double) largeMap.scrollHeight * dy + regionMinZ) * 512D);
//		int bottomBlockX = Mth.floor(((px + w) / (double) largeMap.scrollWidth * dx + regionMinX) * 512D);
//		int bottomBlockZ = Mth.floor(((py + h) / (double) largeMap.scrollHeight * dy + regionMinZ) * 512D);
//		visibleArea = new Rect2i(topBlockX, topBlockZ, bottomBlockX - topBlockX + 1, bottomBlockZ - topBlockZ + 1);

		regionX = (parent.getMouseX() + px) / (double) largeMap.scrollWidth * dx + regionMinX;
		regionZ = (parent.getMouseY() + py) / (double) largeMap.scrollHeight * dy + regionMinZ;
		blockX = Mth.floor(regionX * 512D);
		blockZ = Mth.floor(regionZ * 512D);
		blockIndex = (blockX & 511) + (blockZ & 511) * 512;
		blockY = HeightUtils.UNKNOWN;

		MapRegion region = largeMap.dimension.getRegions().get(XZ.regionFromBlock(blockX, blockZ));
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

		MapRegion mapRegion = largeMap.dimension.getRegions().get(XZ.regionFromBlock(blockX, blockZ));
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
			largeMap.prevMouseX = getMouseX();
			largeMap.prevMouseY = getMouseY();
			largeMap.grabbed = 1;
			return true;
		}

		return false;
	}

	@Override
	public void mouseReleased(MouseButton button) {
		super.mouseReleased(button);
		largeMap.grabbed = 0;
	}

	@Override
	public boolean scrollPanel(double scroll) {
		if (isMouseOver()) {
			largeMap.addZoom(scroll);
			return true;
		}

		return false;
	}
}