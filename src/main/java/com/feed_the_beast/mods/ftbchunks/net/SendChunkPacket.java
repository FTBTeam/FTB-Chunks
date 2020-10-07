package com.feed_the_beast.mods.ftbchunks.net;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import com.feed_the_beast.mods.ftbchunks.impl.ClaimedChunkImpl;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class SendChunkPacket
{
	public static class SingleChunk
	{
		public UUID ownerId;
		public int x, z;
		public int color;
		public ITextComponent owner;
		public long relativeTimeClaimed;
		public long relativeTimeForceLoaded;
		public boolean forceLoaded;

		public SingleChunk(Date nowd, int _x, int _z, @Nullable ClaimedChunkImpl claimedChunk)
		{
			x = _x;
			z = _z;

			if (claimedChunk != null)
			{
				long now = nowd.getTime();
				owner = claimedChunk.getDisplayName().deepCopy();
				color = 0xFF000000 | claimedChunk.getColor();
				relativeTimeClaimed = now - Date.from(claimedChunk.getTimeClaimed()).getTime();
				forceLoaded = claimedChunk.isForceLoaded();

				if (forceLoaded)
				{
					relativeTimeForceLoaded = now - Date.from(claimedChunk.getForceLoadedTime()).getTime();
				}
			}
		}

		public SingleChunk(PacketBuffer buf)
		{
			x = buf.readVarInt();
			z = buf.readVarInt();
			color = buf.readInt();

			if (color != 0)
			{
				owner = buf.readTextComponent();
				relativeTimeClaimed = buf.readVarLong();
				forceLoaded = buf.readBoolean();

				if (forceLoaded)
				{
					relativeTimeForceLoaded = buf.readVarLong();
				}
			}
		}

		public void write(PacketBuffer buf)
		{
			buf.writeVarInt(x);
			buf.writeVarInt(z);
			buf.writeInt(color);

			if (color != 0)
			{
				buf.writeTextComponent(owner);
				buf.writeVarLong(relativeTimeClaimed);
				buf.writeBoolean(forceLoaded);

				if (forceLoaded)
				{
					buf.writeVarLong(relativeTimeForceLoaded);
				}
			}
		}
	}

	public RegistryKey<World> dimension;
	public UUID owner;
	public SingleChunk chunk;

	public SendChunkPacket()
	{
	}

	SendChunkPacket(PacketBuffer buf)
	{
		dimension = RegistryKey.getOrCreateKey(Registry.WORLD_KEY, buf.readResourceLocation());
		owner = new UUID(buf.readLong(), buf.readLong());
		chunk = new SingleChunk(buf);
		chunk.ownerId = owner;
	}

	void write(PacketBuffer buf)
	{
		buf.writeResourceLocation(dimension.getLocation());
		buf.writeLong(owner.getMostSignificantBits());
		buf.writeLong(owner.getLeastSignificantBits());
		chunk.write(buf);
	}

	void handle(Supplier<NetworkEvent.Context> context)
	{
		context.get().enqueueWork(() -> FTBChunks.instance.proxy.updateChunk(this));
		context.get().setPacketHandled(true);
	}
}