package com.feed_the_beast.mods.ftbchunks.net;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.function.Predicate;

public class FTBChunksNet
{
	public static SimpleChannel MAIN;
	private static final String MAIN_VERSION = "2";

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
		//MAIN.registerMessage(2, RequestMapDataPacket.class, RequestMapDataPacket::write, RequestMapDataPacket::new, RequestMapDataPacket::handle);
		MAIN.registerMessage(3, LoginDataPacket.class, LoginDataPacket::write, LoginDataPacket::new, LoginDataPacket::handle);
		MAIN.registerMessage(4, RequestChunkChangePacket.class, RequestChunkChangePacket::write, RequestChunkChangePacket::new, RequestChunkChangePacket::handle);
		MAIN.registerMessage(5, RequestPlayerListPacket.class, RequestPlayerListPacket::write, RequestPlayerListPacket::new, RequestPlayerListPacket::handle);
		MAIN.registerMessage(6, SendPlayerListPacket.class, SendPlayerListPacket::write, SendPlayerListPacket::new, SendPlayerListPacket::handle);
		MAIN.registerMessage(7, RequestAllyStatusChangePacket.class, RequestAllyStatusChangePacket::write, RequestAllyStatusChangePacket::new, RequestAllyStatusChangePacket::handle);
		MAIN.registerMessage(8, SendChunkPacket.class, SendChunkPacket::write, SendChunkPacket::new, SendChunkPacket::handle);
		MAIN.registerMessage(9, SendGeneralDataPacket.class, SendGeneralDataPacket::write, SendGeneralDataPacket::new, SendGeneralDataPacket::handle);
		MAIN.registerMessage(10, SendWaypointsPacket.class, SendWaypointsPacket::write, SendWaypointsPacket::new, SendWaypointsPacket::handle);
		MAIN.registerMessage(11, DeleteWaypointPacket.class, DeleteWaypointPacket::write, DeleteWaypointPacket::new, DeleteWaypointPacket::handle);
		MAIN.registerMessage(12, ChangeWaypointColorPacket.class, ChangeWaypointColorPacket::write, ChangeWaypointColorPacket::new, ChangeWaypointColorPacket::handle);
		MAIN.registerMessage(13, ChangeWaypointNamePacket.class, ChangeWaypointNamePacket::write, ChangeWaypointNamePacket::new, ChangeWaypointNamePacket::handle);
		MAIN.registerMessage(14, TeleportFromMapPacket.class, TeleportFromMapPacket::write, TeleportFromMapPacket::new, TeleportFromMapPacket::handle);
	}
}