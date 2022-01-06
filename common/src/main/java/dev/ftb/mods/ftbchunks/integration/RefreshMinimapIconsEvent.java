package dev.ftb.mods.ftbchunks.integration;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;

public class RefreshMinimapIconsEvent {
	public static Event<Runnable> EVENT = EventFactory.createLoop();

	private RefreshMinimapIconsEvent() {
	}

	public static void trigger() {
		EVENT.invoker().run();
	}
}
