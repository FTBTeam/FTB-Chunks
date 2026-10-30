package dev.ftb.mods.ftbchunks;

import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks server-side temporary claim visibility overrides for admin players who are editing claims for other teams.
 */
public enum ClaimVisibilityOverride {
    INSTANCE;

    private final Map<UUID, UUID> visibilityOverride = new HashMap<>();

    public void add(Player player, Team team) {
        visibilityOverride.put(player.getUUID(), team.getTeamId());
    }

    public UUID remove(Player player) {
        return visibilityOverride.remove(player.getUUID());
    }

    public boolean hasVisibility(Player player, Team team) {
        return visibilityOverride.get(player.getUUID()) == team.getTeamId();
    }
}
