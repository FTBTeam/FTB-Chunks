package com.feed_the_beast.mods.ftbchunks.net;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.function.Predicate;

public class FTBChunksNet {
	public static SimpleChannel MAIN;
	private static final String MAIN_VERSION = "5";

	public static void init() {
		Predicate<String> validator = v -> MAIN_VERSION.equals(v) || NetworkRegistry.ABSENT.equals(v) || NetworkRegistry.ACCEPTVANILLA.equals(v);

		MAIN = NetworkRegistry.ChannelBuilder
				.named(new ResourceLocation("ftbchunks:main"))
				.clientAcceptedVersions(validator)
				.serverAcceptedVersions(validator)
				.networkProtocolVersion(() -> MAIN_VERSION)
				.simpleChannel();

		MAIN.registerMessage(1, RequestMapDataPacket.class, RequestMapDataPacket::write, RequestMapDataPacket::new, RequestMapDataPacket::handle);
		MAIN.registerMessage(2, SendAllChunksPacket.class, SendAllChunksPacket::write, SendAllChunksPacket::new, SendAllChunksPacket::handle);
		MAIN.registerMessage(3, LoginDataPacket.class, LoginDataPacket::write, LoginDataPacket::new, LoginDataPacket::handle);
		MAIN.registerMessage(4, RequestChunkChangePacket.class, RequestChunkChangePacket::write, RequestChunkChangePacket::new, RequestChunkChangePacket::handle);
		MAIN.registerMessage(5, RequestPlayerListPacket.class, RequestPlayerListPacket::write, RequestPlayerListPacket::new, RequestPlayerListPacket::handle);
		MAIN.registerMessage(6, SendPlayerListPacket.class, SendPlayerListPacket::write, SendPlayerListPacket::new, SendPlayerListPacket::handle);
		MAIN.registerMessage(7, RequestAllyStatusChangePacket.class, RequestAllyStatusChangePacket::write, RequestAllyStatusChangePacket::new, RequestAllyStatusChangePacket::handle);
		MAIN.registerMessage(8, SendChunkPacket.class, SendChunkPacket::write, SendChunkPacket::new, SendChunkPacket::handle);
		MAIN.registerMessage(9, SendGeneralDataPacket.class, SendGeneralDataPacket::write, SendGeneralDataPacket::new, SendGeneralDataPacket::handle);
		//10
		//11
		//12
		//13
		MAIN.registerMessage(14, TeleportFromMapPacket.class, TeleportFromMapPacket::write, TeleportFromMapPacket::new, TeleportFromMapPacket::handle);
		MAIN.registerMessage(15, PlayerDeathPacket.class, PlayerDeathPacket::write, PlayerDeathPacket::new, PlayerDeathPacket::handle);
		MAIN.registerMessage(16, SendVisiblePlayerListPacket.class, SendVisiblePlayerListPacket::write, SendVisiblePlayerListPacket::new, SendVisiblePlayerListPacket::handle);
		MAIN.registerMessage(17, SyncTXPacket.class, SyncTXPacket::write, SyncTXPacket::new, SyncTXPacket::handle);
		MAIN.registerMessage(18, SyncRXPacket.class, SyncRXPacket::write, SyncRXPacket::new, SyncRXPacket::handle);
	}
}