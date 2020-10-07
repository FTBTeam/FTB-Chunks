package com.feed_the_beast.mods.ftbchunks.net;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class PlayerDeathPacket
{
	public final RegistryKey<World> dimension;
	public final int x, z, number;

	public PlayerDeathPacket(RegistryKey<World> dim, int _x, int _z, int num)
	{
		dimension = dim;
		x = _x;
		z = _z;
		number = num;
	}

	PlayerDeathPacket(PacketBuffer buf)
	{
		dimension = RegistryKey.getOrCreateKey(Registry.WORLD_KEY, buf.readResourceLocation());
		x = buf.readVarInt();
		z = buf.readVarInt();
		number = buf.readVarInt();
	}

	void write(PacketBuffer buf)
	{
		buf.writeResourceLocation(dimension.getLocation());
		buf.writeVarInt(x);
		buf.writeVarInt(z);
		buf.writeVarInt(number);
	}

	void handle(Supplier<NetworkEvent.Context> context)
	{
		context.get().enqueueWork(() -> FTBChunks.instance.proxy.playerDeath(this));
		context.get().setPacketHandled(true);
	}
}