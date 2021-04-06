package dev.ftb.mods.ftbchunks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ftb.mods.ftbchunks.client.map.Waypoint;
import dev.ftb.mods.ftbchunks.client.map.WaypointType;
import dev.ftb.mods.ftbguilibrary.config.StringConfig;
import dev.ftb.mods.ftbguilibrary.icon.Color4I;
import dev.ftb.mods.ftbguilibrary.icon.Icon;
import dev.ftb.mods.ftbguilibrary.utils.MathUtils;
import dev.ftb.mods.ftbguilibrary.utils.MouseButton;
import dev.ftb.mods.ftbguilibrary.utils.TooltipList;
import dev.ftb.mods.ftbguilibrary.widget.ContextMenuItem;
import dev.ftb.mods.ftbguilibrary.widget.GuiIcons;
import dev.ftb.mods.ftbguilibrary.widget.Panel;
import dev.ftb.mods.ftbguilibrary.widget.Theme;
import dev.ftb.mods.ftbguilibrary.widget.Widget;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * @author LatvianModder
 */
public class WaypointButton extends Widget {
	public final Waypoint waypoint;
	public Icon icon;

	public WaypointButton(Panel panel, Waypoint w) {
		super(panel);
		icon = Icon.getIcon(w.type.texture).withTint(Color4I.rgb(w.color).withAlpha(w.hidden ? 100 : 255));
		waypoint = w;
	}

	@Override
	public void addMouseOverText(TooltipList list) {
		list.string(waypoint.name);
		long dist = (long) MathUtils.dist(Minecraft.getInstance().player.getX(), Minecraft.getInstance().player.getZ(), waypoint.x, waypoint.z);
		list.styledString(dist + " m", ChatFormatting.GRAY);
	}

	@Override
	public void draw(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
		icon.draw(matrixStack, x, y, w, h);
	}

	public boolean mousePressed(MouseButton button) {
		if (isMouseOver() && button.isRight()) {
			List<ContextMenuItem> contextMenu = new ArrayList<>();
			contextMenu.add(new ContextMenuItem(new TextComponent(waypoint.name), icon, () -> {
			}));
			contextMenu.add(ContextMenuItem.SEPARATOR);

			contextMenu.add(new ContextMenuItem(new TranslatableComponent("gui.rename"), GuiIcons.CHAT, () -> {
				StringConfig config = new StringConfig();
				config.defaultValue = "";
				config.value = waypoint.name;
				config.onClicked(MouseButton.LEFT, b -> {
					if (b) {
						waypoint.name = config.value;
						waypoint.dimension.saveData = true;
					}

					openGui();
				});
			}));

			if (waypoint.type == WaypointType.DEFAULT) {
				contextMenu.add(new ContextMenuItem(new TextComponent("Change Color"), GuiIcons.COLOR_RGB, () -> {
					int r = (waypoint.color >> 16) & 0xFF;
					int g = (waypoint.color >> 8) & 0xFF;
					int b = (waypoint.color >> 0) & 0xFF;
					float[] hsb = Color.RGBtoHSB(r, g, b, new float[3]);
					Color4I col = Color4I.hsb(hsb[0] + 1F / 12F, hsb[1], hsb[2]);
					waypoint.color = col.rgba();
					waypoint.dimension.saveData = true;
					icon = Icon.getIcon(waypoint.type.texture).withTint(col.withAlpha(waypoint.hidden ? 100 : 255));
					contextMenu.get(0).icon = icon;
				}).setCloseMenu(false));

				contextMenu.add(new ContextMenuItem(new TextComponent(waypoint.hidden ? "Show" : "Hide"), GuiIcons.BEACON, () -> {
					waypoint.hidden = !waypoint.hidden;
					waypoint.dimension.saveData = true;
					contextMenu.get(0).title = new TextComponent(waypoint.hidden ? "Show" : "Hide");
					getGui().refreshWidgets();
				}));
			}

			contextMenu.add(new ContextMenuItem(new TranslatableComponent("gui.remove"), GuiIcons.REMOVE, () -> {
				waypoint.dimension.getWaypoints().remove(waypoint);
				waypoint.dimension.saveData = true;
				parent.widgets.remove(this);
			}));

			getGui().openContextMenu(contextMenu);
			return true;
		}

		return false;
	}
}
