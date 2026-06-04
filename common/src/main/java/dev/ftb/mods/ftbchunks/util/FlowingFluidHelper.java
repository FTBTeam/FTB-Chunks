package dev.ftb.mods.ftbchunks.util;

import dev.architectury.platform.Platform;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.FTBChunksProperties;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.property.PrivacyMode;
import dev.ftb.mods.ftbteams.api.property.PrivacyProperty;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public class FlowingFluidHelper {
    public static void applyFluidSpreadRestrictions(Level level, BlockPos pos, Map<Direction, FluidState> result) {
        if (!result.isEmpty() && FTBChunksWorldConfig.FLOWING_FLUID_PROTECTION.get() && !FTBChunksWorldConfig.DISABLE_PROTECTION.get()) {
            PrivacyProperty editProp = Platform.isFabric() ?
                    FTBChunksProperties.BLOCK_EDIT_AND_INTERACT_MODE :
                    FTBChunksProperties.BLOCK_EDIT_MODE;

            var srcClaim = FTBChunksAPI.api().getManager().getChunk(new ChunkDimPos(level, pos));
            BlockPos sectionPos = new BlockPos(pos.getX() & 0xf, pos.getY(), pos.getZ() & 0xf);

            if (sectionPos.getX() == 0 && result.containsKey(Direction.WEST)) {
                checkDir(level, pos, result, srcClaim, Direction.WEST, editProp);
            } else if (sectionPos.getX() == 15 && result.containsKey(Direction.EAST)) {
                checkDir(level, pos, result, srcClaim, Direction.EAST, editProp);
            }
            if (sectionPos.getZ() == 0 && result.containsKey(Direction.NORTH)) {
                checkDir(level, pos, result, srcClaim, Direction.NORTH, editProp);
            } else if (sectionPos.getZ() == 15 && result.containsKey(Direction.SOUTH)) {
                checkDir(level, pos, result, srcClaim, Direction.SOUTH, editProp);
            }
        }
    }

    private static void checkDir(Level level, BlockPos pos, Map<Direction,FluidState> result, @Nullable ClaimedChunk srcClaim, Direction dir, PrivacyProperty editProp) {
        BlockPos newPos = pos.relative(dir);
        var dstClaim = FTBChunksAPI.api().getManager().getChunk(new ChunkDimPos(level, newPos));
        if (srcClaim != dstClaim && dstClaim != null) {
            UUID srcId = srcClaim == null ? Util.NIL_UUID : srcClaim.getTeamData().getTeam().getTeamId();
            UUID dstId = dstClaim.getTeamData().getTeam().getTeamId();
            if (!srcId.equals(dstId) && dstClaim.getTeamData().getTeam().getProperty(editProp) != PrivacyMode.PUBLIC) {
                result.remove(dir);
            }
        }
    }
}
