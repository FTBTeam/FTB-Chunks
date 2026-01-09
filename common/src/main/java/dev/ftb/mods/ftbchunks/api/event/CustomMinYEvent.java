package dev.ftb.mods.ftbchunks.api.event;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.ftb.mods.ftbchunks.api.LevelMinYCalculator;

/**
 * This event is fired (on both client and server) during the mod setup phase. It can be used to define custom minimum
 * Y-levels for particular areas in particular levels. Intended to be used to conceal certain areas from being mapped.
 * <p>
 * Multiple {@link LevelMinYCalculator} objects can be registered, and will be evaluated in order. If none of the
 * registered calculators apply (i.e. all return {@code OptionalInt.empty()}), then the default of
 * {@code level.getMinBuildHeight()} is used.
 */
public interface CustomMinYEvent {
    Event<CustomMinYEvent> REGISTER = EventFactory.createLoop();

    /**
     * Register a new calculator method.
     *
     * @param registry the custom min-Y calculator registry
     */
    void register(CustomMinYRegistry registry);

    interface CustomMinYRegistry {
        void register(LevelMinYCalculator calculator);
    }
}
