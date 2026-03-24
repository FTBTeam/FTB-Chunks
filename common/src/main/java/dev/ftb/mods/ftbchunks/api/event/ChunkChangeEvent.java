package dev.ftb.mods.ftbchunks.api.event;

import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftblibrary.platform.event.TypedEvent;
import dev.ftb.mods.ftblibrary.util.result.DataOutcome;
import net.minecraft.commands.CommandSourceStack;

import java.util.function.Consumer;
import java.util.function.Function;

/// Events which are fired before and after a chunk operation is carried out: claiming, un-claiming, force-loading, or
/// un-force-loading.
///
/// The `Pre` events are cancellable. They must **not** be used to alter any mod state that you maintain,
/// since they may be fired for a simulated operation. Use the `Post` events if you intend to update any state.
///
/// Corresponding platform-native events to listen to:
/// * `FTBChunksEvent.ChunkChange.Pre` and `FTBChunksEvent.ChunkChange.Post` (NeoForge)
/// * `FTBChunksEvents.CHUNK_CHANGE_PRE` and `FTBChunksEvents.CHUNK_CHANGE_POST` (Fabric)
public class ChunkChangeEvent {
    public enum Operation {
        CLAIM("claim"),
        UNCLAIM("unclaim"),
        LOAD("load"),
        UNLOAD("unload");

        private final String name;

        Operation(String name) {
            this.name = name;
        }

        public static Operation createOnClient(boolean isLeftMouse, boolean isShift) {
            return isShift ?
                    (isLeftMouse ? Operation.LOAD : Operation.UNLOAD) :
                    (isLeftMouse ? Operation.CLAIM : Operation.UNCLAIM);

        }
    }

    @FunctionalInterface
    public interface Pre extends Function<Pre.Data, DataOutcome<ClaimResult>> {
        TypedEvent<ChunkChangeEvent.Pre.Data, DataOutcome<ClaimResult>> TYPE = TypedEvent.of(ChunkChangeEvent.Pre.Data.class);

        record Data(CommandSourceStack sourceStack, ClaimedChunk claimedChunk, Operation operation) {
        }
    }

    @FunctionalInterface
    public interface Post extends Consumer<Post.Data> {
        record Data(CommandSourceStack sourceStack, ClaimedChunk claimedChunk, Operation operation) {
        }
    }
}
