package com.feed_the_beast.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbchunks.net.FTBChunksNet;
import com.feed_the_beast.mods.ftbchunks.net.RequestAllyStatusChangePacket;
import com.feed_the_beast.mods.ftbchunks.net.SendPlayerListPacket;
import com.feed_the_beast.mods.ftbguilibrary.misc.GuiButtonListBase;
import com.feed_the_beast.mods.ftbguilibrary.utils.MouseButton;
import com.feed_the_beast.mods.ftbguilibrary.widget.GuiIcons;
import com.feed_the_beast.mods.ftbguilibrary.widget.Panel;
import com.feed_the_beast.mods.ftbguilibrary.widget.SimpleTextButton;
import com.feed_the_beast.mods.ftbguilibrary.widget.WidgetType;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;

import java.util.List;

/**
 * @author LatvianModder
 */
public class PlayerListScreen extends GuiButtonListBase
{
	public final List<SendPlayerListPacket.NetPlayer> players;

	public PlayerListScreen(List<SendPlayerListPacket.NetPlayer> p)
	{
		setTitle(I18n.format("ftbchunks.gui.allies"));
		players = p;
	}

	@Override
	public void addButtons(Panel panel)
	{
		for (SendPlayerListPacket.NetPlayer p : players)
		{
			panel.add(new SimpleTextButton(panel, (p.isFake() ? TextFormatting.YELLOW : TextFormatting.WHITE) + p.name, p.isAlly() ? GuiIcons.REMOVE : GuiIcons.ADD)
			{
				@Override
				public void addMouseOverText(List<String> list)
				{
					if (p.isFake())
					{
						list.add("Fake player");
						list.add("UUID: " + p.uuid);
					}
				}

				@Override
				public WidgetType getWidgetType()
				{
					if (p.isBanned())
					{
						return WidgetType.DISABLED;
					}

					return super.getWidgetType();
				}

				@Override
				public void onClicked(MouseButton mouseButton)
				{
					playClickSound();

					if (icon == GuiIcons.ADD)
					{
						icon = GuiIcons.REMOVE;
					}
					else
					{
						icon = GuiIcons.ADD;
					}

					FTBChunksNet.MAIN.sendToServer(new RequestAllyStatusChangePacket(p.uuid));
				}
			});
		}
	}
}