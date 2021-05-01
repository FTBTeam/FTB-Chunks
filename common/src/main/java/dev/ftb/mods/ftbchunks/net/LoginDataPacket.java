package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftblibrary.net.BasePacket;
import dev.ftb.mods.ftblibrary.net.PacketID;
import me.shedaniel.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/**
 * @author LatvianModder
 */
public class LoginDataPacket extends BasePacket {
	public final UUID serverId;

	public LoginDataPacket(UUID id) {
		serverId = id;
	}

	LoginDataPacket(FriendlyByteBuf buf) {
		serverId = new UUID(buf.readLong(), buf.readLong());
	}

	@Override
	public PacketID getId() {
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