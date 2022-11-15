package dev.ftb.mods.ftbchunks.client.map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.integration.RefreshMinimapIconsEvent;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

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

                        Waypoint wp = new Waypoint(mapDimension, o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt());
                        wp.hidden = o.get("hidden").getAsBoolean();
                        wp.name = o.get("name").getAsString();
                        wp.color = 0xFFFFFF;

                        if (o.has("color")) {
                            try {
                                wp.color = Integer.decode(o.get("color").getAsString());
                            } catch (NumberFormatException ignored) {
                            }
                        }

                        if (o.has("type")) {
                            wp.type = WaypointType.TYPES.getOrDefault(o.get("type").getAsString(), WaypointType.DEFAULT);
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
            o.addProperty("hidden", w.hidden);
            o.addProperty("name", w.name);
            o.addProperty("x", w.x);
            o.addProperty("y", w.y);
            o.addProperty("z", w.z);
            o.addProperty("color", String.format("#%06X", 0xFFFFFF & w.color));
            o.addProperty("type", w.type.id);
            waypointArray.add(o);
        }

        json.add("waypoints", waypointArray);

        try (Writer writer = Files.newBufferedWriter(mapDimension.directory.resolve(WAYPOINTS_FILE))) {
            FTBChunks.GSON.toJson(json, writer);
        }
    }

    public void add(Waypoint waypoint) {
        waypoints.add(waypoint);
        if (waypoint.type == WaypointType.DEATH) {
            deathpoints.add(waypoint);
        }
        waypoint.update();
        mapDimension.saveData = true;
        RefreshMinimapIconsEvent.trigger();
    }

    public void remove(Waypoint waypoint) {
        waypoints.remove(waypoint);
        if (waypoint.type == WaypointType.DEATH) {
            deathpoints.remove(waypoint);
        }
        mapDimension.saveData = true;
        RefreshMinimapIconsEvent.trigger();
    }

    public boolean removeIf(Predicate<Waypoint> predicate) {
        if (waypoints.removeIf(predicate)) {
            deathpoints.clear();
            deathpoints.addAll(waypoints.stream().filter(w -> w.type == WaypointType.DEATH).toList());
            mapDimension.saveData = true;
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
                deathpoints.stream().min(Comparator.comparingDouble(o -> p.distanceToSqr(o.x, o.y, o.z)));
    }

    @NotNull
    @Override
    public Iterator<Waypoint> iterator() {
        return waypoints.iterator();
    }

    public boolean hasDeathpoint() {
        return !deathpoints.isEmpty();
    }
}
