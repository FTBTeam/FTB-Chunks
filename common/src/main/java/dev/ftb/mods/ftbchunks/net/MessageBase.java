package dev.ftb.mods.ftbchunks.net;

import me.shedaniel.architectury.networking.NetworkManager;
import me.shedaniel.architectury.utils.GameInstance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public abstract class MessageBase {
	public void handle(Supplier<NetworkManager.PacketContext> context) {
		context.get().queue(() -> handle(context.get()));
	}

	public abstract void write(FriendlyByteBuf buffer);

	public abstract void handle(NetworkManager.PacketContext context);

	public void sendToServer() {
		FTBChunksNet.MAIN.sendToServer(this);
	}

	public void sendToAll() {
		FTBChunksNet.MAIN.sendToPlayers(GameInstance.getServer().getPlayerList().getPlayers(), this);
	}

	public void sendTo(ServerPlayer player) {
		FTBChunksNet.MAIN.sendToPlayer(player, this);
	}

	public void sendTo(Iterable<ServerPlayer> players) {
		for (ServerPlayer player : players) {
			sendTo(player);
		}
	}
}