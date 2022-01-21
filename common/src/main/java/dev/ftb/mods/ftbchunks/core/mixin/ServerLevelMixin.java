package dev.ftb.mods.ftbchunks.core.mixin;

import dev.ftb.mods.ftbchunks.core.ChunkLoadingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author LatvianModder
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
	@Inject(method = "isPositionEntityTicking(Lnet/minecraft/world/level/ChunkPos;)Z", at = @At("RETURN"), cancellable = true)
	private void isPositionEntityTickingChunkFTBC(ChunkPos pos, CallbackInfoReturnable<Boolean> ci) {
		if (!ci.getReturnValue() && ChunkLoadingHelper.isPositionEntityTickingChunk(((ServerLevel) (Object) this).dimension(), pos)) {
			ci.setReturnValue(true);
		}
	}

	@Inject(method = "isPositionEntityTicking(Lnet/minecraft/core/BlockPos;)Z", at = @At("RETURN"), cancellable = true)
	private void isPositionEntityTickingBlockFTBC(BlockPos pos, CallbackInfoReturnable<Boolean> ci) {
		if (!ci.getReturnValue() && ChunkLoadingHelper.isPositionEntityTickingBlock(((ServerLevel) (Object) this).dimension(), pos)) {
			ci.setReturnValue(true);
		}
	}

	@Inject(method = "shouldTickBlocksAt", at = @At("RETURN"), cancellable = true)
	private void shouldTickBlocksAtFTBC(long pos, CallbackInfoReturnable<Boolean> ci) {
		if (!ci.getReturnValue() && ChunkLoadingHelper.shouldTickBlocksAt(((ServerLevel) (Object) this).dimension(), pos)) {
			ci.setReturnValue(true);
		}
	}

	@Inject(method = "isPositionTickingWithEntitiesLoaded", at = @At("RETURN"), cancellable = true)
	private void isPositionTickingWithEntitiesLoadedFTBC(long pos, CallbackInfoReturnable<Boolean> ci) {
		if (!ci.getReturnValue() && ChunkLoadingHelper.isPositionTickingWithEntitiesLoaded(((ServerLevel) (Object) this).dimension(), pos)) {
			ci.setReturnValue(true);
		}
	}
}
