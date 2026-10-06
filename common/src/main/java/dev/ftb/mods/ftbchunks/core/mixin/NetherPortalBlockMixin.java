package dev.ftb.mods.ftbchunks.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ftb.mods.ftbchunks.util.ClaimRespectingNetherPortals;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.portal.PortalForcer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(NetherPortalBlock.class)
public abstract class NetherPortalBlockMixin {
    @WrapOperation(method = "getExitPortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/portal/PortalForcer;createPortal(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction$Axis;)Ljava/util/Optional;"))
    private Optional<BlockUtil.FoundRectangle> ftbc$createPortal(PortalForcer instance, BlockPos blockPos, Direction.Axis axis, Operation<Optional<BlockUtil.FoundRectangle>> original, ServerLevel serverLevel, Entity entity, BlockPos blockPos1, BlockPos blockPos2, boolean bl, WorldBorder worldBorder) {
        var result = ClaimRespectingNetherPortals.tryCreatePortal(entity, ((PortalForcerAccessor) instance).getLevel(), blockPos2);
        return switch (result.type()) {
            case NO_PORTAL -> result.rectangle();
            case FAILURE -> Optional.empty();
            case ALLOW -> original.call(instance, blockPos, axis);
        };
    }
}
