package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.Optional;

public record SendPlayerPositionPacket(ResolvableProfile profile, Optional<BlockPos> pos) implements CustomPacketPayload {
    public static final Type<SendPlayerPositionPacket> TYPE = new Type<>(FTBChunksAPI.rl("send_player_position_packet"));

    public static final StreamCodec<FriendlyByteBuf, SendPlayerPositionPacket> STREAM_CODEC = StreamCodec.composite(
            ResolvableProfile.STREAM_CODEC, SendPlayerPositionPacket::profile,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), SendPlayerPositionPacket::pos,
            SendPlayerPositionPacket::new
    );

    public static SendPlayerPositionPacket startTracking(ServerPlayer player) {
        return new SendPlayerPositionPacket(new ResolvableProfile(player.getGameProfile()), Optional.of(player.blockPosition()));
    }

    public static SendPlayerPositionPacket stopTracking(ServerPlayer player) {
        return new SendPlayerPositionPacket(new ResolvableProfile(player.getGameProfile()), Optional.empty());
    }

    @Override
    public Type<SendPlayerPositionPacket> type() {
        return TYPE;
    }

    public static void handle(SendPlayerPositionPacket message, NetworkManager.PacketContext context) {
        FTBChunksClient.INSTANCE.updateTrackedPlayerPos(message.profile.gameProfile(), message.pos.orElse(null));
    }
}
