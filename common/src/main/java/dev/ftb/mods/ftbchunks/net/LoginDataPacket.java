package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftblibrary.snbt.SNBTNet;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class LoginDataPacket extends BaseS2CMessage {
	private final UUID serverId;
	private final SNBTCompoundTag config;

	public LoginDataPacket(UUID serverId, SNBTCompoundTag config) {
		this.serverId = serverId;
		this.config = config;
	}

	LoginDataPacket(FriendlyByteBuf buf) {
		serverId = buf.readUUID();
		config = SNBTNet.readCompound(buf);
	}

	@Override
	public MessageType getType() {
		return FTBChunksNet.LOGIN_DATA;
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeUUID(serverId);
		SNBTNet.write(buf, config);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBChunksClient.handlePlayerLogin(serverId, config);
	}
}