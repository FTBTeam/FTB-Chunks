package dev.ftb.mods.ftbchunks.core.mixin;

import dev.ftb.mods.ftbchunks.FTBCUtils;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrame.class)
public abstract class ItemFrameMixin {
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    public void onHurt(DamageSource damageSource, float f, CallbackInfoReturnable<Boolean> cir) {
        if (FTBCUtils.shouldProtectNonLivingEntity((Entity) (Object) this, damageSource)) {
            cir.setReturnValue(false);
        }
    }
}
