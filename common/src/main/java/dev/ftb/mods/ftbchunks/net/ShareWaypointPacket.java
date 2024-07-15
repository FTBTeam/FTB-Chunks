package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseC2SMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class ShareWaypointPacket extends BaseC2SMessage {

    private final String name;
    private final GlobalPos globalPos;
    private final ShareType shareType;
    private final List<UUID> targets;

    public ShareWaypointPacket(String name, GlobalPos globalPos, ShareType shareType, List<UUID> targets) {
        this.name = name;
        this.globalPos = globalPos;
        this.shareType = shareType;
        this.targets = targets;
    }

    public ShareWaypointPacket(FriendlyByteBuf buf) {
        name = buf.readUtf();
        globalPos = buf.readGlobalPos();
        shareType = buf.readEnum(ShareType.class);
        int size = buf.readInt();
        List<UUID> targets = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            targets.add(buf.readUUID());
        }
        this.targets = targets;
    }

    @Override
    public MessageType getType() {
        return FTBChunksNet.SHARE_WAYPOINT;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(name);
        buf.writeGlobalPos(globalPos);
        buf.writeEnum(shareType);
        buf.writeInt(targets.size());
        for (UUID target : targets) {
            buf.writeUUID(target);
        }
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        context.queue(() -> {
            ServerPlayer serverPlayer = (ServerPlayer) context.getPlayer();
            PlayerList playerList = serverPlayer.getServer().getPlayerList();
            ChatType.Bound bound2 = ChatType.bind(ChatType.CHAT, serverPlayer).withTargetName(serverPlayer.getDisplayName());
            List<ServerPlayer> playersToSend = switch (shareType) {
                case SERVER -> playerList.getPlayers();
                case PARTY -> {
                    Optional<Team> teamForPlayer = FTBTeamsAPI.api().getManager().getTeamForPlayer(serverPlayer);
                    if (teamForPlayer.isPresent()) {
                        Team team = teamForPlayer.get();
                        yield team.getMembers().stream().map(playerList::getPlayer)
                                .filter(Objects::nonNull).toList();
                    } else {
                        yield List.of(serverPlayer);
                    }
                }
                case PLAYER -> targets.stream().map(playerList::getPlayer)
                        .filter(Objects::nonNull).toList();
            };
            for (ServerPlayer playerListPlayer : playersToSend) {
                String cords = globalPos.pos().getX() + " " + globalPos.pos().getY() + " " + globalPos.pos().getZ();

                String dim = globalPos.dimension().location().toString();
                Component waypointText = Component.literal(name)
                        .withStyle(style -> style
                                .withColor(ChatFormatting.AQUA)
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(dim + " " + cords))));

                playerListPlayer.sendChatMessage(OutgoingChatMessage.create(PlayerChatMessage.system("")
                        .withUnsignedContent(Component.translatable("ftbchunks.waypoint.shared", waypointText)
                                .withStyle(style ->
                                        style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ftbchunks waypoint add-dim " + name + " " + cords + " " + dim + " white true"))))), false, bound2);
            }
        });
    }

    public enum ShareType {
        SERVER,
        PARTY,
        PLAYER;

    }
}