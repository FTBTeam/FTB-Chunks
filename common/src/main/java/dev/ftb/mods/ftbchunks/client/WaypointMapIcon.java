package dev.ftb.mods.ftbchunks.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ftb.mods.ftbchunks.client.map.Waypoint;
import dev.ftb.mods.ftbchunks.integration.StaticMapIcon;
import dev.ftb.mods.ftblibrary.config.StringConfig;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class WaypointMapIcon extends StaticMapIcon {
	public final Waypoint waypoint;
	public Icon outsideIcon;

	public double distance;
	public int alpha;

	public WaypointMapIcon(Waypoint w) {
		super(new Vec3(w.x + 0.5D, w.y + 0.5D, w.z + 0.5D));
		waypoint = w;
		outsideIcon = Color4I.empty();
	}

	@Override
	public boolean isVisible(MapType mapType, double distanceToPlayer, boolean outsideVisibleArea) {
		if (outsideVisibleArea || distanceToPlayer > waypoint.getDrawDistance(mapType.isMinimap())) {
			return false;
		}
		return !mapType.isWorldIcon() || distanceToPlayer >= 0.5F;
	}

	@Override
	public boolean isIconOnEdge(MapType mapType, boolean outsideVisibleArea) {
		return !outsideVisibleArea;
	}

	@Override
	public int getPriority() {
		return 1000;
	}

	@Override
	public void addTooltip(TooltipList list) {
		list.string(waypoint.name);
		super.addTooltip(list);
	}

	@Override
	public boolean mousePressed(LargeMapScreen screen, MouseButton button) {
		if (super.mousePressed(screen, button)) {
			return true;
		} else if (button.isRight()) {
			openWPContextMenu(screen);
			return true;
		}

		return false;
	}

	private void openWPContextMenu(LargeMapScreen screen) {
		List<ContextMenuItem> contextMenu = new ArrayList<>();
		contextMenu.add(makeTitleMenuItem());
		contextMenu.add(ContextMenuItem.SEPARATOR);

		contextMenu.add(new ContextMenuItem(Component.translatable("gui.rename"), Icons.CHAT, () -> {
			StringConfig config = new StringConfig();
			config.setValue(waypoint.name);
			config.onClicked(MouseButton.LEFT, accepted -> {
				if (accepted) {
					waypoint.name = config.getValue();
					waypoint.dimension.saveData = true;
				}
				screen.openGui();
			});
		}));

		if (waypoint.type.canChangeColor) {
			contextMenu.add(new ContextMenuItem(Component.translatable("ftbchunks.gui.change_color"), Icons.COLOR_RGB, () -> {
				int r = (waypoint.color >> 16) & 0xFF;
				int g = (waypoint.color >> 8) & 0xFF;
				int b = waypoint.color & 0xFF;
				float[] hsb = Color.RGBtoHSB(r, g, b, new float[3]);
				float add = Widget.isShiftKeyDown() ? -1F/12F : 1F/12F;
				Color4I col = Color4I.hsb(hsb[0] + add, hsb[1], hsb[2]);
				waypoint.color = col.rgba();
				waypoint.dimension.saveData = true;
				icon = Color4I.empty();
				outsideIcon = Color4I.empty();
				checkIcon();
			}).setCloseMenu(false));
		}

		contextMenu.add(new ContextMenuItem(Component.literal(waypoint.hidden ? "Show" : "Hide"), Icons.BEACON, () -> {
			waypoint.hidden = !waypoint.hidden;
			waypoint.dimension.saveData = true;
			screen.refreshWidgets();
		}));

		contextMenu.add(new ContextMenuItem(Component.translatable("gui.remove"), Icons.REMOVE, () -> {
			waypoint.dimension.getWaypointManager().remove(waypoint);
			screen.regionPanel.refreshWidgets();
		}));

		screen.openContextMenu(contextMenu);
	}

	private ContextMenuItem makeTitleMenuItem() {
		return new ContextMenuItem(Component.literal(waypoint.name), icon, () -> {}) {
			@Override
			public Icon getIcon() {
				return icon;
			}
		};
	}

	@Override
	public boolean keyPressed(LargeMapScreen screen, Key key) {
		if (super.keyPressed(screen, key)) {
			return true;
		} else if (key.is(GLFW.GLFW_KEY_DELETE)) {
			waypoint.dimension.getWaypointManager().remove(waypoint);
			screen.regionPanel.refreshWidgets();
			return true;
		}

		return false;
	}

	public void checkIcon() {
		if (icon.isEmpty() || outsideIcon.isEmpty()) {
			Color4I tint = Color4I.rgb(waypoint.color).withAlpha(waypoint.hidden ? 130 : 255);
			icon = waypoint.type.icon.withTint(tint);
			outsideIcon = waypoint.type.outsideIcon.withTint(tint);
		}
	}

	@Override
	public void draw(MapType mapType, GuiGraphics graphics, int x, int y, int w, int h, boolean outsideVisibleArea, int iconAlpha) {
		checkIcon();

		Icon toDraw = outsideVisibleArea || mapType.isWorldIcon() ? outsideIcon : icon;
		if (iconAlpha < 255 && toDraw instanceof ImageIcon img) {
			img.withColor(img.color.withAlpha(iconAlpha)).draw(graphics, x, y, w, h);
		} else {
			toDraw.draw(graphics, x, y, w, h);
		}

		if (!outsideVisibleArea && mapType.isWorldIcon()) {
			Minecraft mc = Minecraft.getInstance();
			String ds = Mth.ceil(MathUtils.dist(pos.x, pos.y, pos.z, mc.player.getX(), mc.player.getY(), mc.player.getZ())) + " m";
			int nw = mc.font.width(waypoint.name);
			int dw = mc.font.width(ds);
			Color4I.DARK_GRAY.withAlpha(200).draw(graphics, x + (w - nw) / 2 - 2, y - 14, nw + 4, 12);
			Color4I.DARK_GRAY.withAlpha(200).draw(graphics, x + (w - dw) / 2 - 2, y + 18, dw + 4, 12);
			graphics.drawString(mc.font, waypoint.name, x + (w - nw) / 2, y - 12, 0xFFFFFFFF, true);
			graphics.drawString(mc.font, ds, x + (w - dw) / 2, y + 20, 0xFFFFFFFF, true);
			RenderSystem.enableBlend();
			RenderSystem.enableDepthTest();
		}
	}
}
