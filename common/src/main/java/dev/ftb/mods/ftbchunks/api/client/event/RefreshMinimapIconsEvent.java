package dev.ftb.mods.ftbchunks.api.client.event;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;

public class RefreshMinimapIconsEvent {
	public static final Event<Runnable> EVENT = EventFactory.createLoop();

	private RefreshMinimapIconsEvent() {
	}

	public static void trigger() {
		EVENT.invoker().run();
	}
}
