package dev.ftb.mods.ftbchunks.api.event;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.ftb.mods.ftbchunks.api.LevelMinYCalculator;

public interface CustomMinYEvent {
    Event<CustomMinYEvent> REGISTER = EventFactory.createLoop();

    void register(CustomMinYRegistry registry);

    interface CustomMinYRegistry {
        void register(LevelMinYCalculator calculator);
    }
}
