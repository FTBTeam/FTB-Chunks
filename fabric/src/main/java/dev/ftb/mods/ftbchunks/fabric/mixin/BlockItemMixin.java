package dev.ftb.mods.ftbchunks.fabric.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ftb.mods.ftbchunks.fabric.FabricEventListeners;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @Inject(method = "place",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/BlockItem;placeBlock(Lnet/minecraft/world/item/context/BlockPlaceContext;Lnet/minecraft/world/level/block/state/BlockState;)Z"),
            cancellable = true)
    private void place(BlockPlaceContext _0, CallbackInfoReturnable<InteractionResult> cir, @Local(name = "updatedPlaceContext") BlockPlaceContext context) {
        if (!FabricEventListeners.get().canPlaceBlock(context.getPlayer(), context.getClickedPos())) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
