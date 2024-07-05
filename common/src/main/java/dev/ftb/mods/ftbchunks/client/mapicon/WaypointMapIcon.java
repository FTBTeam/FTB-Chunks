package dev.ftb.mods.ftbchunks.client.mapicon;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.api.client.icon.MapType;
import dev.ftb.mods.ftbchunks.api.client.icon.WaypointIcon;
import dev.ftb.mods.ftbchunks.client.gui.LargeMapScreen;
import dev.ftb.mods.ftbchunks.client.map.WaypointImpl;
import dev.ftb.mods.ftbchunks.net.ShareWaypointPacket;
import dev.ftb.mods.ftbchunks.net.TeleportFromMapPacket;
import dev.ftb.mods.ftblibrary.config.ColorConfig;
import dev.ftb.mods.ftblibrary.config.StringConfig;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.FaceIcon;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.ColorSelectorPanel;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftblibrary.ui.NordButton;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.ui.misc.AbstractButtonListScreen;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.client.KnownClientPlayer;
import dev.ftb.mods.ftbteams.client.gui.PlayerListScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.Commands;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class WaypointMapIcon extends StaticMapIcon implements WaypointIcon {
	private final WaypointImpl waypoint;
	private Icon outsideIcon;
	private int alpha;

	public WaypointMapIcon(WaypointImpl waypoint) {
		super(waypoint.getPos());

		this.waypoint = waypoint;

		outsideIcon = Color4I.empty();
	}

	@Override
	public int getAlpha() {
		return alpha;
	}

	@Override
	public void setAlpha(int alpha) {
		this.alpha = alpha;
	}

	@Override
	public Color4I getColor() {
		return Color4I.rgb(waypoint.getColor());
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
		list.string(waypoint.getName());
		super.addTooltip(list);
	}

	@Override
	public boolean onMousePressed(BaseScreen screen, MouseButton button) {
		if (super.onMousePressed(screen, button)) {
			return true;
		} else if (button.isRight() && screen instanceof LargeMapScreen lms) {
			openWPContextMenu(lms);
			return true;
		}

		return false;
	}

	private void openWPContextMenu(LargeMapScreen screen) {
		List<ContextMenuItem> contextMenu = new ArrayList<>();
		contextMenu.add(makeTitleMenuItem());
		contextMenu.add(ContextMenuItem.SEPARATOR);

		LocalPlayer player = Minecraft.getInstance().player;
		GlobalPos waypointPos = new GlobalPos(waypoint.getDimension(), waypoint.getPos());
		if(player.hasPermissions(Commands.LEVEL_GAMEMASTERS)) {
			contextMenu.add(new ContextMenuItem(Component.translatable("ftbchunks.gui.teleport"), ItemIcon.getItemIcon(Items.ENDER_PEARL), b -> {
				NetworkManager.sendToServer(new TeleportFromMapPacket(waypoint.getPos().above(), false, waypoint.getDimension()));
				screen.closeGui(false);
			}));
		}

		boolean shareServer = FTBChunksWorldConfig.WAYPOINT_SHARING_SERVER.get();
		boolean shareParty = FTBChunksWorldConfig.WAYPOINT_SHARING_PARTY.get();
		boolean sharePlayers = FTBChunksWorldConfig.WAYPOINT_SHARING_TEAM.get();

		List<ContextMenuItem> shareMenu = new ArrayList<>();
		if(shareServer) {
			shareMenu.add(new ContextMenuItem(Component.translatable("ftbchunks.waypoint.share.server"), Icons.BEACON, b -> {
				NetworkManager.sendToServer(new ShareWaypointPacket(waypoint.getName(), waypointPos, ShareWaypointPacket.ShareType.SERVER, Optional.empty()));
				screen.closeGui(false);
			}));
		}
		if(shareParty) {
			shareMenu.add(new ContextMenuItem(Component.translatable("ftbchunks.waypoint.share.party"), Icons.BELL, b -> {
				NetworkManager.sendToServer(new ShareWaypointPacket(waypoint.getName(), waypointPos, ShareWaypointPacket.ShareType.PARTY, Optional.empty()));
				screen.closeGui(false);
			}));
		}
		if(sharePlayers) {
			shareMenu.add(new ContextMenuItem(Component.translatable("ftbchunks.waypoint.share.player"), Icons.PLAYER, b -> {
				Collection<KnownClientPlayer> knownClientPlayers = FTBTeamsAPI.api().getClientManager().knownClientPlayers();
				List<GameProfile> list = knownClientPlayers.stream()
						.filter(KnownClientPlayer::online)
						.filter(p -> !p.id().equals(player.getGameProfile().getId()))
						.map(KnownClientPlayer::profile).toList();
				List<GameProfile> selectedProfiles = new ArrayList<>();
				new AbstractButtonListScreen() {

					@Override
					protected void doCancel() {
						screen.closeGui(true);
					}

					@Override
					protected void doAccept() {
						NetworkManager.sendToServer(new ShareWaypointPacket(waypoint.getName(), waypointPos, ShareWaypointPacket.ShareType.PLAYER, Optional.of(selectedProfiles.stream().map(GameProfile::getId).toList())));
						screen.closeGui(false);
					}

					@Override
					public void addButtons(Panel panel) {
						for (GameProfile gameProfile : list) {
							Component unchecked = (Component.literal("☐ ")).append(gameProfile.getName());
							Component checked = (Component.literal("☑ ").withStyle(ChatFormatting.GREEN)).append(gameProfile.getName());
							NordButton widget = new NordButton(panel, unchecked, FaceIcon.getFace(gameProfile)) {
								@Override
								public void onClicked(MouseButton button) {
									if(selectedProfiles.contains(gameProfile)) {
										selectedProfiles.remove(gameProfile);
										title = unchecked;
									} else {
										selectedProfiles.add(gameProfile);
										title = checked;
									}
									screen.refreshWidgets();
									playClickSound();
								}
							};
							panel.add(widget);
						}
					}
				}.openGui();

			}));

		}
		if(shareServer || shareParty || sharePlayers) {
			contextMenu.add(ContextMenuItem.subMenu(Component.translatable("ftbchunks.waypoint.share"), Icons.INFO, shareMenu));
		}

		contextMenu.add(new ContextMenuItem(Component.translatable("gui.rename"), Icons.CHAT, b -> {
			StringConfig config = new StringConfig();
			config.setValue(waypoint.getName());
			config.onClicked(b, MouseButton.LEFT, accepted -> {
				if (accepted) {
					waypoint.setName(config.getValue());
				}
				screen.openGui();
			});
		}));

		if (waypoint.getType().canChangeColor()) {
			contextMenu.add(new ContextMenuItem(Component.translatable("ftbchunks.gui.change_color"), Icons.COLOR_RGB, btn -> {
				ColorConfig col = new ColorConfig();
				col.setValue(Color4I.rgb(waypoint.getColor()));
				ColorSelectorPanel.popupAtMouse(btn.getGui(), col, accepted -> {
					if (accepted) {
						waypoint.setColor(col.getValue().rgba());
						icon = Color4I.empty();
						outsideIcon = Color4I.empty();
						checkIcon();
					}
				});
			}));
		}

		contextMenu.add(new ContextMenuItem(Component.translatable("ftbchunks.label." + (waypoint.isHidden() ? "show" : "hide")), Icons.BEACON, b -> {
			waypoint.setHidden(!waypoint.isHidden());
			screen.refreshWidgets();
		}));

		contextMenu.add(new ContextMenuItem(Component.translatable("gui.remove"), Icons.REMOVE, b -> {
			waypoint.removeFromManager();
			screen.refreshIcons();
		}));

		screen.openContextMenu(contextMenu);
	}

	private ContextMenuItem makeTitleMenuItem() {
		return new ContextMenuItem(Component.literal(waypoint.getName()), icon, null) {
			@Override
			public Icon getIcon() {
				return icon;
			}
		};
	}

	@Override
	public boolean onKeyPressed(BaseScreen screen, Key key) {
		if (super.onKeyPressed(screen, key)) {
			return true;
		} else if (key.is(GLFW.GLFW_KEY_DELETE) && screen instanceof LargeMapScreen lms) {
			waypoint.removeFromManager();
			lms.refreshIcons();
			return true;
		}

		return false;
	}

	public void checkIcon() {
		if (icon.isEmpty() || outsideIcon.isEmpty()) {
			Color4I tint = Color4I.rgb(waypoint.getColor()).withAlpha(waypoint.isHidden() ? 130 : 255);
			icon = waypoint.getType().getIcon().withTint(tint);
			outsideIcon = waypoint.getType().getOutsideIcon().withTint(tint);
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
			int nw = mc.font.width(waypoint.getName());
			int dw = mc.font.width(ds);
			Color4I.DARK_GRAY.withAlpha(200).draw(graphics, x + (w - nw) / 2 - 2, y - 14, nw + 4, 12);
			Color4I.DARK_GRAY.withAlpha(200).draw(graphics, x + (w - dw) / 2 - 2, y + 18, dw + 4, 12);
			graphics.drawString(mc.font, waypoint.getName(), x + (w - nw) / 2, y - 12, 0xFFFFFFFF, true);
			graphics.drawString(mc.font, ds, x + (w - dw) / 2, y + 20, 0xFFFFFFFF, true);
			RenderSystem.enableBlend();
			RenderSystem.enableDepthTest();
		}
	}
}
