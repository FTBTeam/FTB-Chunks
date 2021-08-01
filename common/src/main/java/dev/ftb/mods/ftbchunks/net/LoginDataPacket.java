package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftblibrary.snbt.SNBTNet;
import me.shedaniel.architectury.networking.NetworkManager;
import me.shedaniel.architectury.networking.simple.BaseS2CMessage;
import me.shedaniel.architectury.networking.simple.MessageType;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/**
 * @author LatvianModder
 */
public class LoginDataPacket extends BaseS2CMessage {
	public final UUID serverId;
	public final SNBTCompoundTag config;

	public LoginDataPacket(UUID id, SNBTCompoundTag c) {
		serverId = id;
		config = c;
	}

	LoginDataPacket(FriendlyByteBuf buf) {
		serverId = new UUID(buf.readLong(), buf.readLong());
		config = SNBTNet.readCompound(buf);
	}

	@Override
	public MessageType getType() {
		return FTBChunksNet.LOGIN_DATA;
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeLong(serverId.getMostSignificantBits());
		buf.writeLong(serverId.getLeastSignificantBits());
		SNBTNet.write(buf, config);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBChunks.PROXY.login(this);
	}
}