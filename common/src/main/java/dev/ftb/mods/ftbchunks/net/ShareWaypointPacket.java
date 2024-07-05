package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.util.NetworkHelper;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record ShareWaypointPacket(String name, GlobalPos position, ShareType shareType, Optional<List<UUID>> targets) implements CustomPacketPayload {
    public static final Type<ShareWaypointPacket> TYPE = new Type<>(FTBChunksAPI.rl("share_waypoint_packet"));

    public static final StreamCodec<FriendlyByteBuf, ShareWaypointPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ShareWaypointPacket::name,
            GlobalPos.STREAM_CODEC, ShareWaypointPacket::position,
            ShareType.STREAM_CODEC, ShareWaypointPacket::shareType,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list())), ShareWaypointPacket::targets,
            ShareWaypointPacket::new
    );

    @Override
    public Type<ShareWaypointPacket> type() {
        return TYPE;
    }

    public static void handle(ShareWaypointPacket message, NetworkManager.PacketContext context) {
        context.queue(() -> {
            ServerPlayer serverPlayer = (ServerPlayer) context.getPlayer();
            PlayerList playerList = serverPlayer.getServer().getPlayerList();
            ChatType.Bound bound2 = ChatType.bind(ChatType.CHAT, serverPlayer).withTargetName(serverPlayer.getDisplayName());
            List<ServerPlayer> playersToSend = switch (message.shareType) {
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
                case PLAYER -> message.targets.map(list -> list.stream().map(playerList::getPlayer)
                        .filter(Objects::nonNull).toList()).orElse(Collections.emptyList());
            };
            for (ServerPlayer playerListPlayer : playersToSend) {
                String cords = message.position.pos().getX() + " " + message.position.pos().getY() + " " + message.position.pos().getZ();

                String dim = message.position.dimension().location().toString();
                Component waypointText = Component.literal(message.name)
                        .withStyle(style -> style
                                .withColor(ChatFormatting.AQUA)
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(dim + " " + cords))));

                playerListPlayer.sendChatMessage(OutgoingChatMessage.create(PlayerChatMessage.system("")
                        .withUnsignedContent(Component.translatable("ftbchunks.waypoint.shared", waypointText)
                                .withStyle(style ->
                                        style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ftbchunks waypoint add " + message.name + " " + cords + " " + dim + " white true"))))), false, bound2);
            }
        });
    }

    public enum ShareType {
        SERVER,
        PARTY,
        PLAYER;

        public static final StreamCodec<FriendlyByteBuf, ShareType> STREAM_CODEC = NetworkHelper.enumStreamCodec(ShareType.class);
    }
}
