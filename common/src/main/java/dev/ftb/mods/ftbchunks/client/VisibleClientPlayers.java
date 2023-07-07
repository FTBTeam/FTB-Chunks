package dev.ftb.mods.ftbchunks.client;

import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class VisibleClientPlayers {
	private static final Set<UUID> CLIENT_LIST = new HashSet<>();

	public static void updatePlayerList(List<UUID> uuids) {
		CLIENT_LIST.clear();
		CLIENT_LIST.addAll(uuids);
	}

	public static boolean isPlayerVisible(Player player) {
		return CLIENT_LIST.contains(player.getUUID());
	}
}