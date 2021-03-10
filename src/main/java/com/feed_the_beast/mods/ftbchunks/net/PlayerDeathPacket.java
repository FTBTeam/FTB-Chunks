package com.feed_the_beast.mods.ftbchunks.net;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class PlayerDeathPacket {
	public final ResourceKey<Level> dimension;
	public final int x, y, z, number;

	public PlayerDeathPacket(ResourceKey<Level> dim, int _x, int _y, int _z, int num) {
		dimension = dim;
		x = _x;
		y = _y;
		z = _z;
		number = num;
	}

	PlayerDeathPacket(FriendlyByteBuf buf) {
		dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, buf.readResourceLocation());
		x = buf.readVarInt();
		y = buf.readVarInt();
		z = buf.readVarInt();
		number = buf.readVarInt();
	}

	void write(FriendlyByteBuf buf) {
		buf.writeResourceLocation(dimension.location());
		buf.writeVarInt(x);
		buf.writeVarInt(y);
		buf.writeVarInt(z);
		buf.writeVarInt(number);
	}

	void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> FTBChunks.instance.proxy.playerDeath(this));
		context.get().setPacketHandled(true);
	}
}