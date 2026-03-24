package dev.ftb.mods.ftbchunks.fabric.mixin;

import dev.ftb.mods.ftbchunks.fabric.WrappedLevelCallback;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public class EntityMixin {
    @ModifyVariable(method = "setLevelCallback", argsOnly = true, ordinal = 0, at = @At("HEAD"))
    public EntityInLevelCallback onSetLevelCallback(EntityInLevelCallback callback) {
        return WrappedLevelCallback.wrap((Entity) (Object) this, callback);
    }
}
