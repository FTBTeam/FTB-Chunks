package dev.ftb.mods.ftbchunks.api.client.event;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.ftb.mods.ftbchunks.client.MapType;
import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.client.mapicon.MapIcon;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

public class MapIconEvent {
	public static final Event<Consumer<MapIconEvent>> MINIMAP = EventFactory.createLoop();
	public static final Event<Consumer<MapIconEvent>> LARGE_MAP = EventFactory.createConsumerLoop();

	private final MapDimension mapDimension;
	private final List<MapIcon> icons;
	private final MapType mapType;

	public MapIconEvent(MapDimension mapDimension, List<MapIcon> icons, MapType mapType) {
		this.mapDimension = mapDimension;
		this.icons = icons;
		this.mapType = mapType;
	}

	public ResourceKey<Level> getDimension() {
		return mapDimension.dimension;
	}

	public void add(MapIcon w) {
		icons.add(w);
	}

	public MapDimension getMapDimension() {
		return mapDimension;
	}

	public MapType getMapType() {
		return mapType;
	}
}
