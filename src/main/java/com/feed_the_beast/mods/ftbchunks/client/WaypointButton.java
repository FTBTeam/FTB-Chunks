package com.feed_the_beast.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbchunks.api.Waypoint;
import com.feed_the_beast.mods.ftbchunks.api.WaypointType;
import com.feed_the_beast.mods.ftbchunks.net.ChangeWaypointColorPacket;
import com.feed_the_beast.mods.ftbchunks.net.ChangeWaypointNamePacket;
import com.feed_the_beast.mods.ftbchunks.net.DeleteWaypointPacket;
import com.feed_the_beast.mods.ftbchunks.net.FTBChunksNet;
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
		icon = Icon.getIcon(w.type.texture).withTint(Color4I.rgb(w.color));
		waypoint = w;
	}

	@Override
	public void addMouseOverText(TooltipList list)
	{
		list.string(waypoint.name);

		if (!waypoint.owner.isEmpty())
		{
			list.styledString(waypoint.owner, TextFormatting.GRAY);
		}

		long dist = (long) MathUtils.dist(Minecraft.getInstance().player.getPosX(), Minecraft.getInstance().player.getPosZ(), waypoint.x, waypoint.z);
		list.styledString(dist + " m", TextFormatting.GRAY);
	}

	@Override
	public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h)
	{
		icon.draw(x, y, w, h);
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
						FTBChunksNet.MAIN.sendToServer(new ChangeWaypointNamePacket(waypoint.id, waypoint.name));
					}

					openGui();
				});
			}));

			if (waypoint.type == WaypointType.DEFAULT)
			{
				contextMenu.add(new ContextMenuItem(new StringTextComponent("Change Color"), GuiIcons.COLOR_RGB, () -> {
					Color4I col = Color4I.hsb(MathUtils.RAND.nextFloat(), 1F, 1F);
					icon = Icon.getIcon(waypoint.type.texture).withTint(col);
					FTBChunksNet.MAIN.sendToServer(new ChangeWaypointColorPacket(waypoint.id, col.rgba()));
					contextMenu.get(0).icon = icon;
				}).setCloseMenu(false));
			}

			/*
			contextMenu.add(new ContextMenuItem("Change Privacy", GuiIcons.COLOR_RGB, () -> {
				FTBChunksNet.MAIN.sendToServer(new ChangeWaypointPrivacyPacket(waypoint.id, col.rgba()));
			}).setCloseMenu(false));
			 */

			contextMenu.add(new ContextMenuItem(new TranslationTextComponent("gui.remove"), GuiIcons.REMOVE, () -> {
				((LargeMapScreen) getGui()).dimension.waypoints.remove(waypoint);
				parent.widgets.remove(this);
				FTBChunksNet.MAIN.sendToServer(new DeleteWaypointPacket(waypoint.id));
			}));

			getGui().openContextMenu(contextMenu);
			return true;
		}

		return false;
	}
}
