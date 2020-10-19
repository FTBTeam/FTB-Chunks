package com.feed_the_beast.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbchunks.client.map.Waypoint;
import com.feed_the_beast.mods.ftbchunks.client.map.WaypointType;
import com.feed_the_beast.mods.ftbguilibrary.config.ConfigString;
import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import com.feed_the_beast.mods.ftbguilibrary.icon.Icon;
import com.feed_the_beast.mods.ftbguilibrary.utils.MathUtils;
import com.feed_the_beast.mods.ftbguilibrary.utils.MouseButton;
import com.feed_the_beast.mods.ftbguilibrary.utils.TooltipList;
import com.feed_the_beast.mods.ftbguilibrary.widget.ContextMenuItem;
import com.feed_the_beast.mods.ftbguilibrary.widget.GuiIcons;
import com.feed_the_beast.mods.ftbguilibrary.widget.Panel;
import com.feed_the_beast.mods.ftbguilibrary.widget.Theme;
import com.feed_the_beast.mods.ftbguilibrary.widget.Widget;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author LatvianModder
 */
public class WaypointButton extends Widget
{
	public final Waypoint waypoint;
	public Icon icon;

	public WaypointButton(Panel panel, Waypoint w)
	{
		super(panel);
		icon = Icon.getIcon(w.type.texture).withTint(Color4I.rgb(w.color).withAlpha(w.hidden ? 100 : 255));
		waypoint = w;
	}

	@Override
	public void addMouseOverText(TooltipList list)
	{
		list.string(waypoint.name);
		long dist = (long) MathUtils.dist(Minecraft.getInstance().player.getPosX(), Minecraft.getInstance().player.getPosZ(), waypoint.x, waypoint.z);
		list.styledString(dist + " m", TextFormatting.GRAY);
	}

	@Override
	public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h)
	{
		icon.draw(matrixStack, x, y, w, h);
	}

	public boolean mousePressed(MouseButton button)
	{
		if (isMouseOver() && button.isRight())
		{
			List<ContextMenuItem> contextMenu = new ArrayList<>();
			contextMenu.add(new ContextMenuItem(new StringTextComponent(waypoint.name), icon, () -> {}));
			contextMenu.add(ContextMenuItem.SEPARATOR);

			contextMenu.add(new ContextMenuItem(new TranslationTextComponent("gui.rename"), GuiIcons.CHAT, () -> {
				ConfigString config = new ConfigString();
				config.defaultValue = "";
				config.value = waypoint.name;
				config.onClicked(MouseButton.LEFT, b -> {
					if (b)
					{
						waypoint.name = config.value;
						waypoint.dimension.saveData = true;
					}

					openGui();
				});
			}));

			if (waypoint.type == WaypointType.DEFAULT)
			{
				contextMenu.add(new ContextMenuItem(new StringTextComponent("Change Color"), GuiIcons.COLOR_RGB, () -> {
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

				contextMenu.add(new ContextMenuItem(new StringTextComponent(waypoint.hidden ? "Show" : "Hide"), GuiIcons.BEACON, () -> {
					waypoint.hidden = !waypoint.hidden;
					waypoint.dimension.saveData = true;
					contextMenu.get(0).title = new StringTextComponent(waypoint.hidden ? "Show" : "Hide");
					getGui().refreshWidgets();
				}));
			}

			contextMenu.add(new ContextMenuItem(new TranslationTextComponent("gui.remove"), GuiIcons.REMOVE, () -> {
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
