package dev.ftb.mods.ftbchunks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ftb.mods.ftbchunks.client.map.Waypoint;
import dev.ftb.mods.ftbchunks.integration.RefreshMinimapIconsEvent;
import dev.ftb.mods.ftbchunks.integration.StaticMapIcon;
import dev.ftb.mods.ftblibrary.config.StringConfig;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
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
		outsideIcon = Icon.EMPTY;
	}

	@Override
	public boolean isVisible(MapType mapType, double distanceToPlayer, boolean outsideVisibleArea) {
		return !outsideVisibleArea || distanceToPlayer <= (mapType.isMinimap() ? waypoint.minimapDistance : waypoint.inWorldDistance);
	}

	@Override
	public boolean isIconOnEdge(MapType mapType, boolean outsideVisibleArea) {
		return !outsideVisibleArea;
	}

	@Override
	public int getImportance() {
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
			List<ContextMenuItem> contextMenu = new ArrayList<>();
			contextMenu.add(new ContextMenuItem(new TextComponent(waypoint.name), icon, () -> {
			}));
			contextMenu.add(ContextMenuItem.SEPARATOR);

			contextMenu.add(new ContextMenuItem(new TranslatableComponent("gui.rename"), Icons.CHAT, () -> {
				StringConfig config = new StringConfig();
				config.defaultValue = "";
				config.value = waypoint.name;
				config.onClicked(MouseButton.LEFT, b -> {
					if (b) {
						waypoint.name = config.value;
						waypoint.dimension.saveData = true;
					}

					screen.openGui();
				});
			}));

			if (waypoint.type.canChangeColor) {
				contextMenu.add(new ContextMenuItem(new TextComponent("Change Color"), Icons.COLOR_RGB, () -> {
					int r = (waypoint.color >> 16) & 0xFF;
					int g = (waypoint.color >> 8) & 0xFF;
					int b = (waypoint.color >> 0) & 0xFF;
					float[] hsb = Color.RGBtoHSB(r, g, b, new float[3]);
					Color4I col = Color4I.hsb(hsb[0] + 1F / 12F, hsb[1], hsb[2]);
					waypoint.color = col.rgba();
					waypoint.dimension.saveData = true;
					icon = Icon.EMPTY;
					outsideIcon = Icon.EMPTY;
					checkIcon();
					contextMenu.get(0).icon = icon;
				}).setCloseMenu(false));
			}

			contextMenu.add(new ContextMenuItem(new TextComponent(waypoint.hidden ? "Show" : "Hide"), Icons.BEACON, () -> {
				waypoint.hidden = !waypoint.hidden;
				waypoint.dimension.saveData = true;
				contextMenu.get(0).title = new TextComponent(waypoint.hidden ? "Show" : "Hide");
				screen.refreshWidgets();
			}));

			contextMenu.add(new ContextMenuItem(new TranslatableComponent("gui.remove"), Icons.REMOVE, () -> {
				waypoint.dimension.getWaypoints().remove(waypoint);
				waypoint.dimension.saveData = true;
				RefreshMinimapIconsEvent.trigger();
				screen.regionPanel.refreshWidgets();
			}));

			screen.openContextMenu(contextMenu);
			return true;
		}

		return false;
	}

	@Override
	public boolean keyPressed(LargeMapScreen screen, Key key) {
		if (super.keyPressed(screen, key)) {
			return true;
		} else if (key.is(GLFW.GLFW_KEY_DELETE)) {
			waypoint.dimension.getWaypoints().remove(waypoint);
			waypoint.dimension.saveData = true;
			RefreshMinimapIconsEvent.trigger();
			screen.regionPanel.refreshWidgets();
			return true;
		}

		return false;
	}

	public void checkIcon() {
		if (icon == Icon.EMPTY || outsideIcon == Icon.EMPTY) {
			Color4I tint = Color4I.rgb(waypoint.color);
			Color4I col = waypoint.hidden ? Color4I.WHITE.withAlpha(130) : Color4I.WHITE;
			icon = waypoint.type.icon.withTint(tint).withColor(col);
			outsideIcon = waypoint.type.outsideIcon.withTint(tint).withColor(col);
		}
	}

	@Override
	public void draw(MapType mapType, PoseStack stack, int x, int y, int w, int h, boolean outsideVisibleArea) {
		checkIcon();
		(outsideVisibleArea ? outsideIcon : icon).draw(stack, x, y, w, h);
	}
}
