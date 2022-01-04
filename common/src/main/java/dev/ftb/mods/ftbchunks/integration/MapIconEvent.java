package dev.ftb.mods.ftbchunks.integration;

import dev.ftb.mods.ftbchunks.client.MapType;
import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import me.shedaniel.architectury.event.Event;
import me.shedaniel.architectury.event.EventFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

public class MapIconEvent {
	public static Event<Consumer<MapIconEvent>> MINIMAP = EventFactory.createLoop();
	public static Event<Consumer<MapIconEvent>> LARGE_MAP = EventFactory.createConsumerLoop();

	public final Minecraft mc;
	public final MapDimension mapDimension;
	private final List<MapIcon> icons;
	public final MapType mapType;

	public MapIconEvent(Minecraft _mc, MapDimension dim, List<MapIcon> w, MapType m) {
		mc = _mc;
		mapDimension = dim;
		icons = w;
		mapType = m;
	}

	public ResourceKey<Level> getDimension() {
		return mapDimension.dimension;
	}

	public void add(MapIcon w) {
		icons.add(w);
	}
}
