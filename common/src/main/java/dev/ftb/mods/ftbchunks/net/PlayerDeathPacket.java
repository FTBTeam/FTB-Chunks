package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientNet;
import dev.ftb.mods.ftblibrary.platform.network.PacketContext;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PlayerDeathPacket(GlobalPos pos, int number) implements CustomPacketPayload {
	public static final Type<PlayerDeathPacket> TYPE = new Type<>(FTBChunksAPI.id("player_death_packet"));

	public static final StreamCodec<FriendlyByteBuf, PlayerDeathPacket> STREAM_CODEC = StreamCodec.composite(
			GlobalPos.STREAM_CODEC, PlayerDeathPacket::pos,
			ByteBufCodecs.INT, PlayerDeathPacket::number,
			PlayerDeathPacket::new
	);

	@Override
	public Type<PlayerDeathPacket> type() {
		return TYPE;
	}

	public static void handle(PlayerDeathPacket message, PacketContext context) {
		FTBChunksClientNet.handlePlayerDeathPacket(message.pos, message.number);
	}
}
