package dev.ftb.mods.ftbchunks.client.gui;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.api.client.waypoint.Waypoint;
import dev.ftb.mods.ftbchunks.net.ShareWaypointPacket;
import dev.ftb.mods.ftblibrary.icon.FaceIcon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftblibrary.ui.NordButton;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.ui.misc.AbstractButtonListScreen;
import dev.ftb.mods.ftblibrary.ui.misc.SimpleToast;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.client.KnownClientPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class WaypointShareMenu {
    public static Optional<ContextMenuItem> makeShareMenu(Player sharingPlayer, Waypoint waypoint) {
        List<ContextMenuItem> items = new ArrayList<>();

        if (FTBChunksWorldConfig.WAYPOINT_SHARING_SERVER.get()) {
            items.add(new ContextMenuItem(Component.translatable("ftbchunks.waypoint.share.server"), Icons.BEACON,
                    b -> shareWaypoint(waypoint, ShareWaypointPacket.ShareType.SERVER, List.of())));
        }
        if (FTBChunksWorldConfig.WAYPOINT_SHARING_PARTY.get()) {
            items.add(new ContextMenuItem(Component.translatable("ftbchunks.waypoint.share.party"), Icons.BELL,
                    b -> shareWaypoint(waypoint, ShareWaypointPacket.ShareType.PARTY, List.of())));
        }
        if (FTBChunksWorldConfig.WAYPOINT_SHARING_PLAYERS.get()) {
            items.add(new ContextMenuItem(Component.translatable("ftbchunks.waypoint.share.player"), Icons.PLAYER, b -> {
                Collection<KnownClientPlayer> knownClientPlayers = FTBTeamsAPI.api().getClientManager().knownClientPlayers();
                List<GameProfile> list = knownClientPlayers.stream()
                        .filter(KnownClientPlayer::online)
                        .filter(p -> !p.id().equals(sharingPlayer.getGameProfile().getId()))
                        .map(KnownClientPlayer::profile).toList();
                List<GameProfile> selectedProfiles = new ArrayList<>();
                new AbstractButtonListScreen() {
                    @Override
                    protected void doCancel() {
                        closeGui();
                    }

                    @Override
                    protected void doAccept() {
                        List<UUID> toShare = selectedProfiles.stream().map(GameProfile::getId).toList();
                        if (!toShare.isEmpty()) {
                            shareWaypoint(waypoint, ShareWaypointPacket.ShareType.PLAYER, toShare);
                        }
                        closeGui();
                    }

                    @Override
                    public void addButtons(Panel panel) {
                        for (GameProfile gameProfile : list) {
                            Component unchecked = (Component.literal("☐ ")).append(gameProfile.getName());
                            Component checked = (Component.literal("☑ ").withStyle(ChatFormatting.GREEN)).append(gameProfile.getName());
                            NordButton widget = new NordButton(panel, unchecked, FaceIcon.getFace(gameProfile)) {
                                @Override
                                public void onClicked(MouseButton button) {
                                    if (selectedProfiles.contains(gameProfile)) {
                                        selectedProfiles.remove(gameProfile);
                                        title = unchecked;
                                    } else {
                                        selectedProfiles.add(gameProfile);
                                        title = checked;
                                    }
                                    playClickSound();
                                }
                            };
                            panel.add(widget);
                        }
                    }
                }.openGui();
            }));
        }

        return items.isEmpty() ?
                Optional.empty() :
                Optional.of(ContextMenuItem.subMenu(Component.translatable("ftbchunks.waypoint.share"), Icons.INFO, items));
    }

    private static void shareWaypoint(Waypoint waypoint, ShareWaypointPacket.ShareType type, List<UUID> targets) {
        GlobalPos waypointPos = GlobalPos.of(waypoint.getDimension(), waypoint.getPos());
        new ShareWaypointPacket(waypoint.getName(), waypointPos, type, targets).sendToServer();
        SimpleToast.info(Component.translatable("ftbchunks.waypoint.shared_by_you", waypoint.getName()), Component.empty());
    }
}
