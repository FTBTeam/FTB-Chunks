package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftbchunks.net.SendChunkPacket;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftbteams.data.ClientTeam;
import dev.ftb.mods.ftbteams.data.ClientTeamManager;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class MapChunk {
	public static final int VERSION = 4;

	public final MapRegion region;
	public final XZ pos;
	public long modified;
	public int version;

	public ClientTeam team;
	public Date claimedDate;
	public Date forceLoadedDate;

	public MapChunk(MapRegion r, XZ p) {
		region = r;
		pos = p;
		modified = 0L;
		version = 0;

		team = null;
		claimedDate = null;
		forceLoadedDate = null;
	}

	@Nullable
	public ClientTeam getTeam() {
		if (team != null && team.invalid) {
			team = team.manager.getTeam(team.getId());
		}

		return team;
	}

	public boolean connects(MapChunk chunk) {
		return Objects.equals(getTeam(), chunk.getTeam());
	}

	public XZ getActualPos() {
		return XZ.of((region.pos.x << 5) + pos.x, (region.pos.z << 5) + pos.z);
	}

	public MapChunk created() {
		region.update(true);
		return this;
	}

	public MapChunk offsetBlocking(int x, int z) {
		XZ pos = getActualPos().offset(x, z);
		return region.dimension.getRegion(XZ.regionFromChunk(pos.x, pos.z)).getDataBlocking().getChunk(pos);
	}

	public void updateFrom(Date now, SendChunkPacket.SingleChunk packet, UUID t) {
		team = ClientTeamManager.INSTANCE.teamMap.get(t);
		claimedDate = team == null ? null : new Date(now.getTime() - packet.relativeTimeClaimed);
		forceLoadedDate = packet.forceLoaded && claimedDate != null ? new Date(now.getTime() - packet.relativeTimeForceLoaded) : null;
		region.update(false);
	}
}