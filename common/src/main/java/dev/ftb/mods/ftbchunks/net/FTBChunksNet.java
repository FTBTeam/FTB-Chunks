package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.FTBChunks;
import me.shedaniel.architectury.networking.NetworkChannel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public class FTBChunksNet {
	public static NetworkChannel MAIN;

	private static <T extends MessageBase> void register(Class<T> c, Function<FriendlyByteBuf, T> s) {
		MAIN.register(c, MessageBase::write, s, MessageBase::handle);
	}

	public static void init() {
		MAIN = NetworkChannel.create(new ResourceLocation(FTBChunks.MOD_ID, "main"));

		register(RequestMapDataPacket.class, RequestMapDataPacket::new);
		register(SendAllChunksPacket.class, SendAllChunksPacket::new);
		register(LoginDataPacket.class, LoginDataPacket::new);
		register(RequestChunkChangePacket.class, RequestChunkChangePacket::new);
		register(SendChunkPacket.class, SendChunkPacket::new);
		register(SendGeneralDataPacket.class, SendGeneralDataPacket::new);
		register(TeleportFromMapPacket.class, TeleportFromMapPacket::new);
		register(PlayerDeathPacket.class, PlayerDeathPacket::new);
		register(SendVisiblePlayerListPacket.class, SendVisiblePlayerListPacket::new);
		register(SyncTXPacket.class, SyncTXPacket::new);
		register(SyncRXPacket.class, SyncRXPacket::new);
	}
}