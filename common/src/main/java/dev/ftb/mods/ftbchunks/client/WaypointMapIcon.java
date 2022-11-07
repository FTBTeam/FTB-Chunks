package dev.ftb.mods.ftbchunks.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ftb.mods.ftbchunks.client.map.Waypoint;
import dev.ftb.mods.ftbchunks.integration.RefreshMinimapIconsEvent;
import dev.ftb.mods.ftbchunks.integration.StaticMapIcon;
import dev.ftb.mods.ftblibrary.config.StringConfig;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.client.Minecraft;
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
		outsideIcon = Icon.EMPTY;
	}

	@Override
	public boolean isVisible(MapType mapType, double distanceToPlayer, boolean outsideVisibleArea) {
		return (!outsideVisibleArea || distanceToPlayer <= (mapType.isMinimap() ? waypoint.minimapDistance : waypoint.inWorldDistance)) && (!mapType.isWorldIcon() || distanceToPlayer >= 0.5F);
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
			List<ContextMenuItem> contextMenu = new ArrayList<>();
			contextMenu.add(new ContextMenuItem(Component.literal(waypoint.name), icon, () -> {
			}));
			contextMenu.add(ContextMenuItem.SEPARATOR);

			contextMenu.add(new ContextMenuItem(Component.translatable("gui.rename"), Icons.CHAT, () -> {
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
				contextMenu.add(new ContextMenuItem(Component.literal("Change Color"), Icons.COLOR_RGB, () -> {
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

			contextMenu.add(new ContextMenuItem(Component.literal(waypoint.hidden ? "Show" : "Hide"), Icons.BEACON, () -> {
				waypoint.hidden = !waypoint.hidden;
				waypoint.dimension.saveData = true;
				contextMenu.get(0).title = Component.literal(waypoint.hidden ? "Show" : "Hide");
				screen.refreshWidgets();
			}));


			contextMenu.add(new ContextMenuItem(Component.translatable("gui.remove"), Icons.REMOVE, () -> {
				waypoint.dimension.getWaypointManager().remove(waypoint);
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
			waypoint.dimension.getWaypointManager().remove(waypoint);
			screen.regionPanel.refreshWidgets();
			return true;
		}

		return false;
	}

	public void checkIcon() {
		if (icon == Icon.EMPTY || outsideIcon == Icon.EMPTY) {
			Color4I tint = Color4I.rgb(waypoint.color).withAlpha(waypoint.hidden ? 130 : 255);
			icon = waypoint.type.icon.withTint(tint);
			outsideIcon = waypoint.type.outsideIcon.withTint(tint);
		}
	}

	@Override
	public void draw(MapType mapType, PoseStack stack, int x, int y, int w, int h, boolean outsideVisibleArea) {
		checkIcon();
		(outsideVisibleArea || mapType.isWorldIcon() ? outsideIcon : icon).draw(stack, x, y, w, h);

		if (!outsideVisibleArea && mapType.isWorldIcon()) {
			Minecraft mc = Minecraft.getInstance();
			String ds = Mth.ceil(MathUtils.dist(pos.x, pos.y, pos.z, mc.player.getX(), mc.player.getY(), mc.player.getZ())) + " m";
			int nw = mc.font.width(waypoint.name);
			int dw = mc.font.width(ds);
			Color4I.DARK_GRAY.withAlpha(200).draw(stack, x + (w - nw) / 2 - 2, y - 14, nw + 4, 12);
			Color4I.DARK_GRAY.withAlpha(200).draw(stack, x + (w - dw) / 2 - 2, y + 18, dw + 4, 12);
			mc.font.drawShadow(stack, waypoint.name, x + (w - nw) / 2F, y - 12F, 0xFFFFFFFF);
			mc.font.drawShadow(stack, ds, x + (w - dw) / 2F, y + 20F, 0xFFFFFFFF);
			RenderSystem.enableBlend();
			RenderSystem.enableDepthTest();
		}
	}
}
