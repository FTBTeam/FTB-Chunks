package dev.ftb.mods.ftbchunks.client;

import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks which players are visible to this client player based on their team privacy settings
 */
public final class VisibleClientPlayers {
	private static final Set<UUID> CLIENT_LIST = new HashSet<>();

	public static void updatePlayerList(List<UUID> uuids) {
		// see PlayerVisibilityPacket
		CLIENT_LIST.clear();
		CLIENT_LIST.addAll(uuids);
	}

	public static boolean isPlayerVisible(Player player) {
		return CLIENT_LIST.contains(player.getUUID());
	}
}