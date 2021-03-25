package dev.ftb.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbguilibrary.icon.Icon;
import com.feed_the_beast.mods.ftbguilibrary.misc.GuiButtonListBase;
import com.feed_the_beast.mods.ftbguilibrary.utils.MouseButton;
import com.feed_the_beast.mods.ftbguilibrary.utils.TooltipList;
import com.feed_the_beast.mods.ftbguilibrary.widget.GuiIcons;
import com.feed_the_beast.mods.ftbguilibrary.widget.Panel;
import com.feed_the_beast.mods.ftbguilibrary.widget.SimpleTextButton;
import com.feed_the_beast.mods.ftbguilibrary.widget.WidgetType;
import dev.ftb.mods.ftbchunks.net.FTBChunksNet;
import dev.ftb.mods.ftbchunks.net.RequestAllyStatusChangePacket;
import dev.ftb.mods.ftbchunks.net.SendPlayerListPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextComponent;

import java.util.List;

/**
 * @author LatvianModder
 */
public class AllyScreen extends GuiButtonListBase {
	public final List<SendPlayerListPacket.NetPlayer> players;
	public final int allyMode;

	public AllyScreen(List<SendPlayerListPacket.NetPlayer> p, int a) {
		setHasSearchBox(true);

		players = p;
		allyMode = a;

		setTitle(new TextComponent("Allies"));
	}

	@Override
	public void addButtons(Panel panel) {
		for (SendPlayerListPacket.NetPlayer p : players) {
			Icon icon;

			if (p.isAlly() && p.isAllyBack()) {
				icon = GuiIcons.CHECK;
			} else if (p.isAlly()) {
				icon = GuiIcons.RIGHT;
			} else if (p.isAllyBack()) {
				icon = GuiIcons.LEFT;
			} else {
				icon = GuiIcons.ADD_GRAY;
			}

			panel.add(new SimpleTextButton(panel, new TextComponent(p.name).withStyle(p.isFake() ? ChatFormatting.YELLOW : ChatFormatting.WHITE), icon) {
				@Override
				public void addMouseOverText(TooltipList list) {
					if (p.isFake()) {
						list.string("Fake player");
						list.string("UUID: " + p.uuid);
					} else if (p.isAlly() != p.isAllyBack()) {
						list.string("Pending invite...");

						if (p.isAllyBack()) {
							list.styledString("Click to accept", ChatFormatting.GRAY);
							list.styledString("Right-click to deny", ChatFormatting.DARK_GRAY);
						} else {
							list.styledString("Click to cancel", ChatFormatting.GRAY);
						}
					}
				}

				@Override
				public WidgetType getWidgetType() {
					if (p.isBanned()) {
						return WidgetType.DISABLED;
					}

					return super.getWidgetType();
				}

				@Override
				public void onClicked(MouseButton mouseButton) {
					playClickSound();

					boolean cancelPending = !mouseButton.isLeft() && !p.isAlly() && p.isAllyBack();

					if (cancelPending) {
						p.flags &= ~SendPlayerListPacket.NetPlayer.ALLY_BACK;
					} else {
						if (p.isAlly()) {
							p.flags &= ~SendPlayerListPacket.NetPlayer.ALLY;
						} else {
							p.flags |= SendPlayerListPacket.NetPlayer.ALLY;
						}
					}

					if (p.isAlly() && p.isAllyBack()) {
						icon = GuiIcons.CHECK;
					} else if (p.isAlly()) {
						icon = GuiIcons.RIGHT;
					} else if (p.isAllyBack()) {
						icon = GuiIcons.LEFT;
					} else {
						icon = GuiIcons.ADD_GRAY;
					}

					FTBChunksNet.MAIN.sendToServer(new RequestAllyStatusChangePacket(p.uuid, !cancelPending));
				}
			});
		}
	}
}