package dev.ftb.mods.ftbchunks.integration;

import me.shedaniel.architectury.event.Event;
import me.shedaniel.architectury.event.EventFactory;

public class RefreshMinimapIconsEvent {
	public static Event<Runnable> EVENT = EventFactory.createLoop();

	private RefreshMinimapIconsEvent() {
	}

	public static void trigger() {
		EVENT.invoker().run();
	}
}
