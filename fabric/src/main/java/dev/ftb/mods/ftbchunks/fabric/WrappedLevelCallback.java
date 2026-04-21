package dev.ftb.mods.ftbchunks.fabric;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.EntityInLevelCallback;

public class WrappedLevelCallback {
    public static EntityInLevelCallback wrap(Entity e, EntityInLevelCallback wrapped) {
        if (wrapped == null || wrapped == EntityInLevelCallback.NULL || !(e instanceof ServerPlayer)) {
            return wrapped;
        }

        return new EntityInLevelCallback() {
            private long prevKey = SectionPos.asLong(e.blockPosition());

            @Override
            public void onMove() {
                wrapped.onMove();

                var key = SectionPos.asLong(e.blockPosition());
                if (key != prevKey) {
                    ChunkPos cp = ChunkPos.containing(e.blockPosition());
                    FabricEventListeners.INSTANCE.onSectionChange((ServerPlayer) e, cp.x(), cp.z());
                    prevKey = key;
                }
            }

            @Override
            public void onRemove(Entity.RemovalReason reason) {
                wrapped.onRemove(reason);
            }
        };
    }
}
