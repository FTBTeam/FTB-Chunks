package dev.ftb.mods.ftbchunks.api.fabric;

import dev.ftb.mods.ftbchunks.api.event.ChunkChangeEvent;
import dev.ftb.mods.ftbchunks.api.event.CustomMinYEvent;
import dev.ftb.mods.ftblibrary.util.result.DataOutcome;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public class FTBChunksEvents {
    public static final Event<CustomMinYEvent> CUSTOM_MIN_Y = EventFactory.createArrayBacked(CustomMinYEvent.class,
            callbacks -> data -> {
                for (var c : callbacks) {
                    c.accept(data);
                }
            });


    public static Event<ChunkChangeEvent.Pre> CHUNK_CHANGE_PRE = EventFactory.createArrayBacked(ChunkChangeEvent.Pre.class,
            callbacks -> data -> {
                for (var event : callbacks) {
                    var outcome = event.apply(data);
                    if (outcome.isFail()) {
                        return outcome;
                    }
                }
                return DataOutcome.pass();
            }
    );

    public static final Event<ChunkChangeEvent.Post> CHUNK_CHANGE_POST = EventFactory.createArrayBacked(ChunkChangeEvent.Post.class,
            callbacks -> data -> {
                for (var c : callbacks) {
                    c.accept(data);
                }
            });
}
