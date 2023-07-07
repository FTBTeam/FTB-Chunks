package dev.ftb.mods.ftbchunks.api.client.event;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.ftb.mods.ftbchunks.api.client.icon.MapIcon;
import dev.ftb.mods.ftbchunks.api.client.icon.MapType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

public class MapIconEvent {
	/**
	 * Fired when FTB Chunks is gathering icons to add to the minimap. Handlers for this event should run quickly,
	 * since it's called quite frequently to keep minimap icons up-to-date, and a slow handler can negatively impact
	 * client FPS.
	 */
	public static final Event<Consumer<MapIconEvent>> MINIMAP = EventFactory.createLoop();

	/**
	 * Fired when FTB Chunks is gathering icons to add to the large (full-screen) map. While less performance-critical
	 * than the minimap event (only called while the fullscreen map is open), handlers should still pay attention to
	 * performance.
	 */
	public static final Event<Consumer<MapIconEvent>> LARGE_MAP = EventFactory.createConsumerLoop();

	private final List<MapIcon> icons;
	private final MapType mapType;
	private final ResourceKey<Level> dimension;

	public MapIconEvent(ResourceKey<Level> dimension, List<MapIcon> icons, MapType mapType) {
		this.dimension = dimension;
		this.icons = icons;
		this.mapType = mapType;
	}

	/**
	 * Get the dimension that the map is currently rendering.
	 *
	 * @return the current dimension
	 */
	public ResourceKey<Level> getDimension() {
		return dimension;
	}

	/**
	 * Add a map icon to the map.
	 *
	 * @param mapIcon the icon to add
	 */
	public void add(MapIcon mapIcon) {
		icons.add(mapIcon);
	}

	/**
	 * Get the type of map being added to (large map, minimap, or in-world icons)
	 *
	 * @return the map type
	 */
	public MapType getMapType() {
		return mapType;
	}
}
