package com.feed_the_beast.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbchunks.net.FTBChunksNet;
import com.feed_the_beast.mods.ftbchunks.net.RequestAllyStatusChangePacket;
import com.feed_the_beast.mods.ftbchunks.net.SendPlayerListPacket;
import com.feed_the_beast.mods.ftbguilibrary.misc.GuiButtonListBase;
import com.feed_the_beast.mods.ftbguilibrary.utils.MouseButton;
import com.feed_the_beast.mods.ftbguilibrary.utils.TooltipList;
import com.feed_the_beast.mods.ftbguilibrary.widget.GuiIcons;
import com.feed_the_beast.mods.ftbguilibrary.widget.Panel;
import com.feed_the_beast.mods.ftbguilibrary.widget.SimpleTextButton;
import com.feed_the_beast.mods.ftbguilibrary.widget.WidgetType;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.List;

/**
 * @author LatvianModder
 */
public class PlayerListScreen extends GuiButtonListBase
{
	public final List<SendPlayerListPacket.NetPlayer> players;
	public final int allyMode;

	public PlayerListScreen(List<SendPlayerListPacket.NetPlayer> p, int a)
	{
		setHasSearchBox(true);

		players = p;
		allyMode = a;

		switch (allyMode)
		{
			case 0:
				setTitle(new TranslationTextComponent("ftbchunks.gui.ally_whitelist"));
				break;
			case 1:
				setTitle(new TranslationTextComponent("ftbchunks.gui.ally_blacklist"));
				break;
			case 2:
				setTitle(new StringTextComponent("Forced whitelist"));
				break;
			case 3:
				setTitle(new StringTextComponent("Forced blacklist"));
				break;
		}
	}

	@Override
	public void addButtons(Panel panel)
	{
		for (SendPlayerListPacket.NetPlayer p : players)
		{
			panel.add(new SimpleTextButton(panel, new StringTextComponent(p.name).mergeStyle(p.isFake() ? TextFormatting.YELLOW : TextFormatting.WHITE), p.isAlly() ? GuiIcons.REMOVE : GuiIcons.ADD)
			{
				@Override
				public void addMouseOverText(TooltipList list)
				{
					if (p.isFake())
					{
						list.string("Fake player");
						list.string("UUID: " + p.uuid);
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