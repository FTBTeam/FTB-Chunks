package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.data.ClaimedChunk;
import me.shedaniel.architectury.networking.NetworkManager;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class SendChunkPacket extends MessageBase {
	public static class SingleChunk {
		public int x, z;
		public long relativeTimeClaimed;
		public long relativeTimeForceLoaded;
		public boolean forceLoaded;

		public SingleChunk(Date nowd, int _x, int _z, @Nullable ClaimedChunk claimedChunk) {
			x = _x;
			z = _z;

			if (claimedChunk != null) {
				long now = nowd.getTime();
				relativeTimeClaimed = now - Date.from(claimedChunk.getTimeClaimed()).getTime();
				forceLoaded = claimedChunk.isForceLoaded();

				if (forceLoaded) {
					relativeTimeForceLoaded = now - Date.from(claimedChunk.getForceLoadedTime()).getTime();
				}
			}
		}

		public SingleChunk(FriendlyByteBuf buf, UUID teamId) {
			x = buf.readVarInt();
			z = buf.readVarInt();

			if (!teamId.equals(Util.NIL_UUID)) {
				relativeTimeClaimed = buf.readVarLong();
				forceLoaded = buf.readBoolean();

				if (forceLoaded) {
					relativeTimeForceLoaded = buf.readVarLong();
				}
			}
		}

		public void write(FriendlyByteBuf buf, UUID teamId) {
			buf.writeVarInt(x);
			buf.writeVarInt(z);

			if (!teamId.equals(Util.NIL_UUID)) {
				buf.writeVarLong(relativeTimeClaimed);
				buf.writeBoolean(forceLoaded);

				if (forceLoaded) {
					buf.writeVarLong(relativeTimeForceLoaded);
				}
			}
		}
	}

	public ResourceKey<Level> dimension;
	public UUID teamId;
	public SingleChunk chunk;

	public SendChunkPacket() {
	}

	SendChunkPacket(FriendlyByteBuf buf) {
		dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, buf.readResourceLocation());
		teamId = buf.readUUID();
		chunk = new SingleChunk(buf, teamId);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeResourceLocation(dimension.location());
		buf.writeUUID(teamId);
		chunk.write(buf, teamId);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBChunks.PROXY.updateChunk(this);
	}
}