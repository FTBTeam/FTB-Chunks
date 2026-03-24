package dev.ftb.mods.ftbchunks.core.mixin;

import dev.ftb.mods.ftbchunks.api.Protection;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStand.class)
public abstract class ArmorStandMixin {
	@Inject(method = "interact", at = @At("HEAD"), cancellable = true)
	public void onInteractAt(Player player, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
		ArmorStand armorStand = (ArmorStand) (Object) this;
		if (!player.level().isClientSide() && ClaimedChunkManagerImpl.getInstance().shouldPreventInteraction(player, hand, armorStand.blockPosition(), Protection.INTERACT_ENTITY, armorStand)) {
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}
}
