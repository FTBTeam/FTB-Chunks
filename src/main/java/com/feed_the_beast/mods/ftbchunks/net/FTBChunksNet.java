package com.feed_the_beast.mods.ftbchunks.net;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.function.Predicate;

public class FTBChunksNet
{
	public static SimpleChannel MAIN;
	private static final String MAIN_VERSION = "1";

	public static void init()
	{
		Predicate<String> validator = v -> MAIN_VERSION.equals(v) || NetworkRegistry.ABSENT.equals(v) || NetworkRegistry.ACCEPTVANILLA.equals(v);

		MAIN = NetworkRegistry.ChannelBuilder
				.named(new ResourceLocation("ftbchunks:main"))
				.clientAcceptedVersions(validator)
				.serverAcceptedVersions(validator)
				.networkProtocolVersion(() -> MAIN_VERSION)
				.simpleChannel();

		MAIN.registerMessage(1, RequestMapDataPacket.class, RequestMapDataPacket::write, RequestMapDataPacket::new, RequestMapDataPacket::handle);
		MAIN.registerMessage(2, SendMapDataPacket.class, SendMapDataPacket::write, SendMapDataPacket::new, SendMapDataPacket::handle);
		MAIN.registerMessage(3, SendColorMapPacket.class, SendColorMapPacket::write, SendColorMapPacket::new, SendColorMapPacket::handle);
		MAIN.registerMessage(4, RequestChunkChangePacket.class, RequestChunkChangePacket::write, RequestChunkChangePacket::new, RequestChunkChangePacket::handle);
	}
}