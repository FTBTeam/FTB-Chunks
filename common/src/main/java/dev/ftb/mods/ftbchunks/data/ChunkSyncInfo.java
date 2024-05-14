package dev.ftb.mods.ftbchunks.data;

import com.google.common.primitives.Ints;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.client.map.MapChunk;
import dev.ftb.mods.ftbchunks.net.SendChunkPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

public record ChunkSyncInfo(int x, int z, boolean claimed, int relativeTimeClaimed, boolean forceLoaded,
                            int relativeTimeForceLoaded, boolean expires, int relativeForceLoadExpiryTime) {
    // NOTE: relative times are sent in seconds rather than milliseconds
    //  (int rather than long for network sync efficiency)
    //  allows for offsets of up to ~2 billion seconds, which is 62 years - should be enough...
    //  (come back to me with a bug report in 2085 if not)

    public static StreamCodec<FriendlyByteBuf, ChunkSyncInfo> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ChunkSyncInfo decode(FriendlyByteBuf buf) {
            int x = buf.readInt();
            int z = buf.readInt();

            if (buf.readBoolean()) {
                int relativeTimeClaimed = buf.readInt();
                byte flags = buf.readByte();
                boolean forceLoaded = (flags & 0x01) != 0;
                boolean expires = (flags & 0x02) != 0;
                int relativeTimeForceLoaded = forceLoaded ? buf.readInt() : 0;
                int relativeForceLoadExpiryTime = expires ? buf.readInt() : 0;
                return new ChunkSyncInfo(x, z, true, relativeTimeClaimed, forceLoaded, relativeTimeForceLoaded, expires, relativeForceLoadExpiryTime);
            } else {
                return new ChunkSyncInfo(x, z, false, 0, false, 0, false, 0);
            }
        }

        @Override
        public void encode(FriendlyByteBuf buf, ChunkSyncInfo chunkSyncInfo) {
            buf.writeInt(chunkSyncInfo.x);
            buf.writeInt(chunkSyncInfo.z);
            buf.writeBoolean(chunkSyncInfo.claimed);

            if (chunkSyncInfo.claimed) {
                byte flags = 0x0;
                if (chunkSyncInfo.forceLoaded) flags |= 0x1;
                if (chunkSyncInfo.expires) flags |= 0x2;

                buf.writeInt(chunkSyncInfo.relativeTimeClaimed);
                buf.writeByte(flags);
                if (chunkSyncInfo.forceLoaded) buf.writeInt(chunkSyncInfo.relativeTimeForceLoaded);
                if (chunkSyncInfo.expires) buf.writeInt(chunkSyncInfo.relativeForceLoadExpiryTime);
            }
        }
    };

    public static ChunkSyncInfo create(long now, int x, int z, @Nullable ClaimedChunk claimedChunk) {
        if (claimedChunk != null) {
            boolean forceLoaded = claimedChunk.isForceLoaded();
            boolean expires = claimedChunk.getForceLoadExpiryTime() > 0L;
            return new ChunkSyncInfo(x, z, true,
                    millisToSeconds(now - claimedChunk.getTimeClaimed()),
                    forceLoaded,
                    forceLoaded ? millisToSeconds(now - claimedChunk.getForceLoadedTime()) : 0,
                    expires,
                    expires ? millisToSeconds(claimedChunk.getForceLoadExpiryTime() - now) : 0);
        } else {
            return new ChunkSyncInfo(x, z, false, 0, false, 0, false, 0);
        }
    }

    public ChunkSyncInfo hidden() {
        return ChunkSyncInfo.create(0L, x, z, null);
    }

    private static int millisToSeconds(long ms) {
        return Ints.saturatedCast(ms / 1000L);
    }

    public MapChunk.DateInfo getDateInfo(boolean isClaimed, long now) {
        if (!isClaimed) {
            return MapChunk.NO_DATE_INFO;
        }

        Date claimed = new Date(now - relativeTimeClaimed * 1000L);
        Date forceLoaded = this.forceLoaded ? new Date(now - relativeTimeForceLoaded * 1000L) : null;
        Date expiry = this.forceLoaded && expires ? new Date(now + relativeForceLoadExpiryTime * 1000L) : null;
        return new MapChunk.DateInfo(claimed, forceLoaded, expiry);
    }
}
