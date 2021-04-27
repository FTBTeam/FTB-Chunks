package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.data.ClaimedChunkTeamData;
import dev.ftb.mods.ftbchunks.data.PlayerLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

/**
 * @author LatvianModder
 */
public class VisiblePlayerListItem {
	public ServerPlayer player;
	public ClaimedChunkTeamData data;
	public PlayerLocation location;

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		VisiblePlayerListItem that = (VisiblePlayerListItem) o;
		return location.equals(that.location);
	}

	@Override
	public int hashCode() {
		return Objects.hash(location);
	}
}
