package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PlayerDeathPacket(GlobalPos pos, int number) implements CustomPacketPayload {
	public static final Type<PlayerDeathPacket> TYPE = new Type<>(FTBChunksAPI.rl("player_death_packet"));

	public static final StreamCodec<FriendlyByteBuf, PlayerDeathPacket> STREAM_CODEC = StreamCodec.composite(
			GlobalPos.STREAM_CODEC, PlayerDeathPacket::pos,
			ByteBufCodecs.INT, PlayerDeathPacket::number,
			PlayerDeathPacket::new
	);

	@Override
	public Type<PlayerDeathPacket> type() {
		return TYPE;
	}

	public static void handle(PlayerDeathPacket message, NetworkManager.PacketContext context) {
		context.queue(() -> FTBChunksClient.INSTANCE.handlePlayerDeath(message.pos, message.number));
	}
}