package dev.ftb.mods.ftbchunks.api.neoforge;

import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.event.ChunkChangeEvent;
import dev.ftb.mods.ftbchunks.api.event.CustomMinYEvent;
import dev.ftb.mods.ftblibrary.api.neoforge.BaseEventWithData;
import net.neoforged.bus.api.ICancellableEvent;

public class FTBChunksEvent {
    public static class RegisterCustomMinYCalculator extends BaseEventWithData<CustomMinYEvent.Data> {
        public RegisterCustomMinYCalculator(CustomMinYEvent.Data data) {
            super(data);
        }
    }

    public static class ChunkChange {
        public static class Pre extends BaseEventWithData<ChunkChangeEvent.Pre.Data> implements ICancellableEvent {
            private ClaimResult result = ClaimResult.success();

            public Pre(ChunkChangeEvent.Pre.Data data) {
                super(data);
            }

            public void setResult(ClaimResult result) {
                this.result = result;

                if (!result.isSuccess()) {
                    setCanceled(true);
                }
            }

            public ClaimResult getResult() {
                return result;
            }
        }

        public static class Post extends BaseEventWithData<ChunkChangeEvent.Post.Data> {
            public Post(ChunkChangeEvent.Post.Data data) {
                super(data);
            }
        }
    }
}
