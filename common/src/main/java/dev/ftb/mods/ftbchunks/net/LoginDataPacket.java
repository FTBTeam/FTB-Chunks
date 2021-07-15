package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.FTBChunks;
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

	public LoginDataPacket(UUID id) {
		serverId = id;
	}

	LoginDataPacket(FriendlyByteBuf buf) {
		serverId = new UUID(buf.readLong(), buf.readLong());
	}

	@Override
	public MessageType getType() {
		return FTBChunksNet.LOGIN_DATA;
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeLong(serverId.getMostSignificantBits());
		buf.writeLong(serverId.getLeastSignificantBits());
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBChunks.PROXY.login(this);
	}
}