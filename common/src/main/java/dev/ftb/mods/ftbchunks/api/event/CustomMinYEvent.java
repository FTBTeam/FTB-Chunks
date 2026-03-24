package dev.ftb.mods.ftbchunks.api.event;

import dev.ftb.mods.ftbchunks.api.LevelMinYCalculator;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/// This event is fired (on both client and server) during the mod setup phase. It can be used to define custom minimum
/// Y-levels for particular areas in particular levels. Intended to be used to conceal certain areas from being mapped.
///
/// Multiple [LevelMinYCalculator] objects can be registered, and will be evaluated in order. If none of the
/// registered calculators apply (i.e. all return `OptionalInt.empty()`), then the default of
/// `level.getMinBuildHeight()` is used.
///
/// Corresponding platform-native events to listen to:
/// * `FTBChunksEvent.RegisterCustomMinYCalculator` (NeoForge)
/// * `FTBChunksEvents.CUSTOM_MIN_Y` (Fabric)
@FunctionalInterface
public interface CustomMinYEvent extends Consumer<CustomMinYEvent.Data> {
    record Data(List<LevelMinYCalculator> list) {
        public void register(LevelMinYCalculator calculator) {
            list.add(calculator);
        }

        @Override
        public List<LevelMinYCalculator> list() {
            return Collections.unmodifiableList(list);
        }
    }
}
