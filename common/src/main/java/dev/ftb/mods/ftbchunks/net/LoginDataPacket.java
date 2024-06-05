package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record LoginDataPacket(UUID serverId, SNBTCompoundTag config) implements CustomPacketPayload {
	public static final Type<LoginDataPacket> TYPE = new Type<>(FTBChunksAPI.rl("login_data_packet"));

	public static final StreamCodec<FriendlyByteBuf, LoginDataPacket> STREAM_CODEC = StreamCodec.composite(
			UUIDUtil.STREAM_CODEC, LoginDataPacket::serverId,
			SNBTCompoundTag.STREAM_CODEC, LoginDataPacket::config,
			LoginDataPacket::new
	);

	@Override
	public Type<LoginDataPacket> type() {
		return TYPE;
	}

	public static void handle(LoginDataPacket message, NetworkManager.PacketContext context) {
		context.queue(() -> FTBChunksClient.INSTANCE.handlePlayerLogin(message.serverId, message.config));
	}
}