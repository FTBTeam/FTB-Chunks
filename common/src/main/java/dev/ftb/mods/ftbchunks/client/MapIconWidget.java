package dev.ftb.mods.ftbchunks.client;

import dev.ftb.mods.ftbchunks.integration.MapIcon;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class MapIconWidget extends Widget {
	public final MapIcon mapIcon;
	public final RegionMapPanel regionMapPanel;

	public MapIconWidget(RegionMapPanel panel, MapIcon m) {
		super(panel);
		mapIcon = m;
		regionMapPanel = panel;
	}

	@Override
	public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
		if (width > 0 && height > 0) {
			updatePosition(regionMapPanel.getPartialTicks());
			mapIcon.draw(MapType.LARGE_MAP, graphics, x, y, w, h, false, 255);
		}
	}

	@Override
	public void addMouseOverText(TooltipList list) {
		if (width > 0 && height > 0 && !list.shouldRender()) {
			mapIcon.addTooltip(list);
		}
	}

	@Override
	public boolean mousePressed(MouseButton button) {
		if (isMouseOver() && width > 0 && height > 0 && mapIcon.mousePressed(regionMapPanel.largeMap, button)) {
			return true;
		}

		return super.mousePressed(button);
	}

	@Override
	public boolean keyPressed(Key key) {
		if (isMouseOver() && width > 0 && height > 0 && mapIcon.keyPressed(regionMapPanel.largeMap, key)) {
			return true;
		}

		return super.keyPressed(key);
	}

	public void updatePosition(float delta) {
		int z = regionMapPanel.largeMap.getRegionButtonSize();
		Vec3 pos = mapIcon.getPos(delta);
		double qx = pos.x / 512D;
		double qy = pos.z / 512D;

		double x = (qx - regionMapPanel.regionMinX) * z - width / 2D;
		double y = (qy - regionMapPanel.regionMinZ) * z - (mapIcon.isIconOnEdge(MapType.LARGE_MAP, false) ? height : (height / 2D));
		setPos(Mth.floor(x), Mth.floor(y));
	}
}
