package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.data.ClaimedChunk;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * @author LatvianModder
 */
public class SendChunkPacket extends BaseS2CMessage {
	public static class SingleChunk {
		public final int x, z;
		public final long relativeTimeClaimed;
		public final boolean forceLoaded;
		public final long relativeTimeForceLoaded;
		public final boolean expires;
		public final long relativeForceLoadExpiryTime;

		public SingleChunk(long now, int x, int z, @Nullable ClaimedChunk claimedChunk) {
			this.x = x;
			this.z = z;

			if (claimedChunk != null) {
				relativeTimeClaimed = now - claimedChunk.getTimeClaimed();
				forceLoaded = claimedChunk.isForceLoaded();
				expires = claimedChunk.getForceLoadExpiryTime() > 0L;
				relativeTimeForceLoaded = forceLoaded ? now - claimedChunk.getForceLoadedTime() : 0L;
				relativeForceLoadExpiryTime = expires ? claimedChunk.getForceLoadExpiryTime() - now : 0L;
			} else {
				relativeTimeClaimed = relativeTimeForceLoaded = relativeForceLoadExpiryTime = 0L;
				forceLoaded = expires = false;
			}
		}

		public SingleChunk(FriendlyByteBuf buf, UUID teamId) {
			x = buf.readVarInt();
			z = buf.readVarInt();

			if (!teamId.equals(Util.NIL_UUID)) {
				relativeTimeClaimed = buf.readVarLong();
				forceLoaded = buf.readBoolean();
				expires = buf.readBoolean();
				relativeTimeForceLoaded = forceLoaded ? buf.readVarLong() : 0L;
				relativeForceLoadExpiryTime = expires ? buf.readVarLong() : 0L;
			} else {
				relativeTimeClaimed = relativeTimeForceLoaded = relativeForceLoadExpiryTime = 0L;
				forceLoaded = expires = false;
			}
		}

		public void write(FriendlyByteBuf buf, UUID teamId) {
			buf.writeVarInt(x);
			buf.writeVarInt(z);

			if (!teamId.equals(Util.NIL_UUID)) {
				buf.writeVarLong(relativeTimeClaimed);
				buf.writeBoolean(forceLoaded);
				buf.writeBoolean(expires);
				if (forceLoaded) buf.writeVarLong(relativeTimeForceLoaded);
				if (expires) buf.writeVarLong(relativeForceLoadExpiryTime);
			}
		}
	}

	public final ResourceKey<Level> dimension;
	public final UUID teamId;
	public final SingleChunk chunk;

	public SendChunkPacket(ResourceKey<Level> dimension, UUID teamId, SingleChunk chunk) {
		this.dimension = dimension;
		this.teamId = teamId;
		this.chunk = chunk;
	}

	@Override
	public MessageType getType() {
		return FTBChunksNet.SEND_CHUNK;
	}

	SendChunkPacket(FriendlyByteBuf buf) {
		dimension = ResourceKey.create(Registries.DIMENSION, buf.readResourceLocation());
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