package dev.ftb.mods.ftbchunks.core.mixin;

import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.data.Protection;
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
	@Inject(method = "interactAt", at = @At("HEAD"), cancellable = true)
	public void onInteractAt(Player player, Vec3 vec3, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir) {
		// this is a hack, but necessary since Forge's PlayerInteractEvent.EntityInteractSpecific event is currently broken in 1.18.2 (as of 40.1.84)
		//   and Architectury doesn't currently handle this event at all on Forge or Fabric
		ArmorStand armorStand = (ArmorStand) (Object) this;
		if (!player.level.isClientSide && FTBChunksAPI.getManager().protect(player, interactionHand, armorStand.blockPosition(), Protection.INTERACT_ENTITY, armorStand)) {
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}
}
