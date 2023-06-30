package dev.ftb.mods.ftbchunks.client.map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.api.event.RefreshMinimapIconsEvent;
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

public class WaypointManager implements Iterable<Waypoint> {
    private static final String WAYPOINTS_FILE = "waypoints.json";

    private final Set<Waypoint> waypoints = new HashSet<>();
    private final Set<Waypoint> deathpoints = new HashSet<>();
    private final MapDimension mapDimension;

    public WaypointManager(MapDimension mapDimension) {
        this.mapDimension = mapDimension;
    }

    public static WaypointManager fromJson(MapDimension mapDimension) {
        Path file = mapDimension.directory.resolve(WAYPOINTS_FILE);

        WaypointManager manager = new WaypointManager(mapDimension);

        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                JsonObject json = FTBChunks.GSON.fromJson(reader, JsonObject.class);

                if (json.has("waypoints")) {
                    for (JsonElement e : json.get("waypoints").getAsJsonArray()) {
                        JsonObject o = e.getAsJsonObject();

                        WaypointType type = o.has("type") ? WaypointType.forId(o.get("type").getAsString()) : WaypointType.DEFAULT;
                        Waypoint wp = new Waypoint(type, mapDimension, new BlockPos(o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt()));
                        wp.setHidden(o.get("hidden").getAsBoolean());
                        wp.setName(o.get("name").getAsString());

                        wp.setColor(0xFFFFFF);
                        if (o.has("color")) {
                            try {
                                wp.setColor(Integer.decode(o.get("color").getAsString()));
                            } catch (NumberFormatException ignored) {
                            }
                        }

                        manager.add(wp);
                        wp.update();

                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return manager;
    }

    public static void writeJson(MapDimension mapDimension, List<Waypoint> waypoints) throws IOException {
        JsonObject json = new JsonObject();
        JsonArray waypointArray = new JsonArray();

        for (Waypoint w : waypoints) {
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

    public void add(Waypoint waypoint) {
        waypoints.add(waypoint);
        if (waypoint.getType() == WaypointType.DEATH) {
            deathpoints.add(waypoint);
        }
        waypoint.update();
        mapDimension.markDirty();
        RefreshMinimapIconsEvent.trigger();
    }

    public void remove(Waypoint waypoint) {
        waypoints.remove(waypoint);
        if (waypoint.getType() == WaypointType.DEATH) {
            deathpoints.remove(waypoint);
        }
        mapDimension.markDirty();
        RefreshMinimapIconsEvent.trigger();
    }

    public boolean removeIf(Predicate<Waypoint> predicate) {
        if (waypoints.removeIf(predicate)) {
            deathpoints.clear();
            deathpoints.addAll(waypoints.stream().filter(w -> w.getType() == WaypointType.DEATH).toList());
            mapDimension.markDirty();
            RefreshMinimapIconsEvent.trigger();
            return true;
        }
        return false;
    }

    public boolean isEmpty() {
        return waypoints.isEmpty();
    }

    public Optional<Waypoint> getNearestDeathpoint(Player p) {
        return deathpoints.isEmpty() ?
                Optional.empty() :
                deathpoints.stream().min(Comparator.comparingDouble(o -> o.getDistanceSq(p)));
    }

    @NotNull
    @Override
    public Iterator<Waypoint> iterator() {
        return waypoints.iterator();
    }

    public boolean hasDeathpoint() {
        return !deathpoints.isEmpty();
    }

    public Stream<Waypoint> stream() {
        return waypoints.stream();
    }
}
