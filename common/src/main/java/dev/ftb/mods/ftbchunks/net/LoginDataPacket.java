package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.FTBChunks;
import me.shedaniel.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/**
 * @author LatvianModder
 */
public class LoginDataPacket extends MessageBase {
	public final UUID serverId;

	public LoginDataPacket(UUID id) {
		serverId = id;
	}

	LoginDataPacket(FriendlyByteBuf buf) {
		serverId = new UUID(buf.readLong(), buf.readLong());
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