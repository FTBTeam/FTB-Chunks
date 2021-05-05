package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.data.ClaimedChunk;
import dev.ftb.mods.ftblibrary.net.snm.BaseS2CPacket;
import dev.ftb.mods.ftblibrary.net.snm.PacketID;
import me.shedaniel.architectury.networking.NetworkManager;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class SendChunkPacket extends BaseS2CPacket {
	public static class SingleChunk {
		public int x, z;
		public long relativeTimeClaimed;
		public long relativeTimeForceLoaded;
		public boolean forceLoaded;

		public SingleChunk(long now, int _x, int _z, @Nullable ClaimedChunk claimedChunk) {
			x = _x;
			z = _z;

			if (claimedChunk != null) {
				relativeTimeClaimed = now - claimedChunk.getTimeClaimed();
				forceLoaded = claimedChunk.isForceLoaded();

				if (forceLoaded) {
					relativeTimeForceLoaded = now - claimedChunk.getForceLoadedTime();
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

	@Override
	public PacketID getId() {
		return FTBChunksNet.SEND_CHUNK;
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