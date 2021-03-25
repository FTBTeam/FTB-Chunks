package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.impl.ClaimedChunkImpl;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class SendChunkPacket {
	public static class SingleChunk {
		public UUID ownerId;
		public int x, z;
		public int color;
		public Component owner;
		public long relativeTimeClaimed;
		public long relativeTimeForceLoaded;
		public boolean forceLoaded;

		public SingleChunk(Date nowd, int _x, int _z, @Nullable ClaimedChunkImpl claimedChunk) {
			x = _x;
			z = _z;

			if (claimedChunk != null) {
				long now = nowd.getTime();
				owner = claimedChunk.getDisplayName();
				color = 0xFF000000 | claimedChunk.getColor();
				relativeTimeClaimed = now - Date.from(claimedChunk.getTimeClaimed()).getTime();
				forceLoaded = claimedChunk.isForceLoaded();

				if (forceLoaded) {
					relativeTimeForceLoaded = now - Date.from(claimedChunk.getForceLoadedTime()).getTime();
				}
			}
		}

		public SingleChunk(FriendlyByteBuf buf) {
			x = buf.readVarInt();
			z = buf.readVarInt();
			color = buf.readInt();

			if (color != 0) {
				owner = buf.readComponent();
				relativeTimeClaimed = buf.readVarLong();
				forceLoaded = buf.readBoolean();

				if (forceLoaded) {
					relativeTimeForceLoaded = buf.readVarLong();
				}
			}
		}

		public void write(FriendlyByteBuf buf) {
			buf.writeVarInt(x);
			buf.writeVarInt(z);
			buf.writeInt(color);

			if (color != 0) {
				buf.writeComponent(owner);
				buf.writeVarLong(relativeTimeClaimed);
				buf.writeBoolean(forceLoaded);

				if (forceLoaded) {
					buf.writeVarLong(relativeTimeForceLoaded);
				}
			}
		}
	}

	public ResourceKey<Level> dimension;
	public UUID owner;
	public SingleChunk chunk;

	public SendChunkPacket() {
	}

	SendChunkPacket(FriendlyByteBuf buf) {
		dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, buf.readResourceLocation());
		owner = new UUID(buf.readLong(), buf.readLong());
		chunk = new SingleChunk(buf);
		chunk.ownerId = owner;
	}

	void write(FriendlyByteBuf buf) {
		buf.writeResourceLocation(dimension.location());
		buf.writeLong(owner.getMostSignificantBits());
		buf.writeLong(owner.getLeastSignificantBits());
		chunk.write(buf);
	}

	void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> FTBChunks.instance.proxy.updateChunk(this));
		context.get().setPacketHandled(true);
	}
}