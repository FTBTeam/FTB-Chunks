package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftbchunks.net.SendChunkPacket;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.world.entity.player.Player;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class MapChunk {
	public static final int VERSION = 4;

	public static final DateInfo NO_DATE_INFO = new DateInfo(null, null, null);

	private final MapRegion region;
	private final XZ pos;

	private long modified;
	private int version;
	private Team team;
	private DateInfo dateInfo;

	public MapChunk(MapRegion region, XZ pos) {
		this.region = region;
		this.pos = pos;
		modified = 0L;
		version = 0;

		team = null;
		dateInfo = NO_DATE_INFO;
	}

	public void forceUpdate() {
		modified = System.currentTimeMillis();
		region.update(true);
	}

	public MapRegionData getRegionData() {
		return region.getDataBlocking();
	}

	public XZ getPos() {
		return pos;
	}

	public Optional<Date> getClaimedDate() {
		return Optional.ofNullable(dateInfo.claimed);
	}

	public Optional<Date> getForceLoadedDate() {
		return Optional.ofNullable(dateInfo.forceLoaded);
	}

	public Optional<Date> getForceLoadExpiryDate() {
		return Optional.ofNullable(dateInfo.expiry);
	}

	public long getModified() {
		return modified;
	}

	public void setModified(long modified) {
		this.modified = modified;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public void writeData(DataOutput dataOutput) throws IOException {
		dataOutput.writeByte(version);
		dataOutput.writeByte(pos.x());
		dataOutput.writeByte(pos.z());
		dataOutput.writeLong(modified);
	}

	public Optional<Team> getTeam() {
		if (team != null && !team.isValid()) {
			team = FTBTeamsAPI.api().getClientManager().getTeamByID(team.getId()).orElse(null);
		}

		return Optional.ofNullable(team);
	}

	public boolean connects(MapChunk chunk) {
		return Objects.equals(getTeam(), chunk.getTeam());
	}

	public XZ getActualPos() {
		return XZ.of((region.pos.x() << 5) + pos.x(), (region.pos.z() << 5) + pos.z());
	}

	public MapChunk created() {
		region.update(true);
		return this;
	}

	public MapChunk offsetBlocking(int x, int z) {
		XZ pos = getActualPos().offset(x, z);
		return region.dimension.getRegion(XZ.regionFromChunk(pos.x(), pos.z())).getDataBlocking().getChunk(pos);
	}

	public void updateFromServer(Date now, SendChunkPacket.SingleChunk packet, UUID teamId) {
		team = FTBTeamsAPI.api().getClientManager().getTeamByID(teamId).orElse(null);
		dateInfo = packet.getDateInfo(team != null, now.getTime());
		region.update(false);
	}

	public void updateForceLoadExpiryDate(long now, long offset) {
		dateInfo = dateInfo.withExpiryDate(offset == 0L ? null : new Date(now + offset));
	}

	public boolean isTeamMember(Player player) {
		return team != null && team.getRankForPlayer(player.getUUID()).isMemberOrBetter();
	}

	public record DateInfo(Date claimed, Date forceLoaded, Date expiry) {
		public DateInfo withExpiryDate(Date newExpiry) {
			return new DateInfo(claimed, forceLoaded, newExpiry);
		}
	}
}