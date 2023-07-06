package dev.ftb.mods.ftbchunks.client.map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.waypoint.Waypoint;
import dev.ftb.mods.ftbchunks.api.client.waypoint.WaypointManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class WaypointManagerImpl implements Iterable<WaypointImpl>, WaypointManager {
    private static final String WAYPOINTS_FILE = "waypoints.json";

    private final Set<WaypointImpl> waypoints = new HashSet<>();
    private final Set<WaypointImpl> deathpoints = new HashSet<>();
    private final MapDimension mapDimension;

    public WaypointManagerImpl(MapDimension mapDimension) {
        this.mapDimension = mapDimension;
    }

    public static WaypointManagerImpl fromJson(MapDimension mapDimension) {
        Path file = mapDimension.directory.resolve(WAYPOINTS_FILE);

        WaypointManagerImpl manager = new WaypointManagerImpl(mapDimension);

        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                JsonObject json = FTBChunks.GSON.fromJson(reader, JsonObject.class);

                if (json.has("waypoints")) {
                    for (JsonElement e : json.get("waypoints").getAsJsonArray()) {
                        JsonObject o = e.getAsJsonObject();

                        WaypointType type = o.has("type") ? WaypointType.forId(o.get("type").getAsString()) : WaypointType.DEFAULT;
                        WaypointImpl wp = new WaypointImpl(type, mapDimension, new BlockPos(o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt()))
                                .setHidden(o.get("hidden").getAsBoolean())
                                .setName(o.get("name").getAsString())
                                .setColor(0xFFFFFF);

                        if (o.has("color")) {
                            try {
                                wp.setColor(Integer.decode(o.get("color").getAsString()));
                            } catch (NumberFormatException ignored) {
                            }
                        }

                        manager.add(wp);
                        wp.refreshIcon();

                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return manager;
    }

    public static void writeJson(MapDimension mapDimension, List<WaypointImpl> waypoints) throws IOException {
        JsonObject json = new JsonObject();
        JsonArray waypointArray = new JsonArray();

        for (WaypointImpl w : waypoints) {
            JsonObject o = new JsonObject();
            o.addProperty("hidden", w.isHidden());
            o.addProperty("name", w.getName());
            o.addProperty("x", w.getPos().getX());
            o.addProperty("y", w.getPos().getY());
            o.addProperty("z", w.getPos().getZ());
            o.addProperty("color", String.format("#%06X", 0xFFFFFF & w.getColor()));
            o.addProperty("type", w.getType().getId());
            waypointArray.add(o);
        }

        json.add("waypoints", waypointArray);

        try (Writer writer = Files.newBufferedWriter(mapDimension.directory.resolve(WAYPOINTS_FILE))) {
            FTBChunks.GSON.toJson(json, writer);
        }
    }

    public void add(WaypointImpl waypoint) {
        if (waypoints.add(waypoint)) {
            if (waypoint.isDeathpoint()) {
                deathpoints.add(waypoint);
            }
            waypoint.refreshIcon();
            mapDimension.markDirty();
            FTBChunksAPI.clientApi().requestMinimapIconRefresh();
        }
    }

    public void remove(WaypointImpl waypoint) {
        if (waypoints.remove(waypoint)) {
            if (waypoint.isDeathpoint()) {
                deathpoints.remove(waypoint);
            }
            mapDimension.markDirty();
            FTBChunksAPI.clientApi().requestMinimapIconRefresh();
        }
    }

    public boolean removeIf(Predicate<WaypointImpl> predicate) {
        if (waypoints.removeIf(predicate)) {
            deathpoints.clear();
            deathpoints.addAll(waypoints.stream().filter(w -> w.getType() == WaypointType.DEATH).toList());
            mapDimension.markDirty();
            FTBChunksAPI.clientApi().requestMinimapIconRefresh();
            return true;
        }
        return false;
    }

    public boolean isEmpty() {
        return waypoints.isEmpty();
    }

    @Override
    public Optional<WaypointImpl> getNearestDeathpoint(Player player) {
        return deathpoints.isEmpty() ?
                Optional.empty() :
                deathpoints.stream().min(Comparator.comparingDouble(o -> o.getDistanceSq(player)));
    }

    @NotNull
    @Override
    public Iterator<WaypointImpl> iterator() {
        return waypoints.iterator();
    }

    public boolean hasDeathpoint() {
        return !deathpoints.isEmpty();
    }

    public Stream<WaypointImpl> stream() {
        return waypoints.stream();
    }

    /* API methods below here */

    @Override
    public Waypoint addWaypointAt(BlockPos pos, String name) {
        WaypointImpl waypoint = new WaypointImpl(WaypointType.DEFAULT, mapDimension, pos).setName(name);
        add(waypoint);
        return waypoint;
    }

    @Override
    public boolean removeWaypointAt(BlockPos pos) {
        WaypointImpl impl = new WaypointImpl(WaypointType.DEFAULT, mapDimension, pos);
        if (waypoints.contains(impl)) {
            remove(impl);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeWaypoint(Waypoint waypoint) {
        return removeWaypointAt(waypoint.getPos());
    }

    @Override
    public Collection<Waypoint> getAllWaypoints() {
        return Collections.unmodifiableCollection(waypoints);
    }
}
