package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientNet;
import dev.ftb.mods.ftblibrary.platform.network.PacketContext;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record LoginDataPacket(UUID serverId) implements CustomPacketPayload {
	public static final Type<LoginDataPacket> TYPE = new Type<>(FTBChunksAPI.id("login_data_packet"));

	public static final StreamCodec<FriendlyByteBuf, LoginDataPacket> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, LoginDataPacket::serverId,
			LoginDataPacket::new
	);

	@Override
	public Type<LoginDataPacket> type() {
		return TYPE;
	}

	public static void handle(LoginDataPacket message, PacketContext context) {
		FTBChunksClientNet.handleLoginDataPacket(message.serverId);
	}
}
