package dev.ftb.mods.ftbchunks.client;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbchunks.api.client.event.AddMapIconEvent;
import dev.ftb.mods.ftbchunks.client.gui.map.LargeMapScreen;
import dev.ftb.mods.ftbchunks.client.mapicon.TrackedPlayerMapIcon;
import dev.ftb.mods.ftblibrary.icon.FaceIcon;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/// Allows for tracking players on the map who are outside of normal vanilla tracking range
/// (thus are not present on the client as entities to be tracked).
public class LongRangePlayerTrackerClient {
    private final Map<UUID, TrackedPlayerMapIcon> tracked = new HashMap<>();

    public void clear() {
        tracked.clear();
    }

    public void removePlayer(UUID playerId) {
        if (tracked.remove(playerId) != null) {
            LargeMapScreen.refreshIconsIfOpen();
        }
    }

    public void addAllToEvent(AddMapIconEvent.Data event) {
        tracked.values().forEach(event::add);
    }

    public void updatePlayerPos(GameProfile profile, @Nullable BlockPos pos) {
        // called periodically when a player (outside of vanilla entity tracking range), that this client is tracking, moves
        // see SendPlayerPositionPacket
        boolean changed = false;
        if (pos == null) {
            // null block pos indicates player should no longer be tracked on this client
            // - player is either no longer in this world, or is now within the vanilla tracking range
            changed = tracked.remove(profile.id()) != null;
        } else {
            TrackedPlayerMapIcon icon = tracked.get(profile.id());
            if (icon == null) {
                tracked.put(profile.id(), new TrackedPlayerMapIcon(profile, Vec3.atCenterOf(pos), FaceIcon.getFace(profile, true)));
                changed = true;
            } else {
                icon.setPos(Vec3.atCenterOf(pos));
            }
        }
        if (changed) {
            LargeMapScreen.refreshIconsIfOpen();
        }
    }
}
