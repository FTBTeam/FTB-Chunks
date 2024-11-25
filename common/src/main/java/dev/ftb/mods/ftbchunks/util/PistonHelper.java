package dev.ftb.mods.ftbchunks.util;

import dev.architectury.platform.Platform;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.ClaimedChunkManager;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.FTBChunksProperties;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.property.PrivacyMode;
import dev.ftb.mods.ftbteams.api.property.PrivacyProperty;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;

import java.util.UUID;

public class PistonHelper {
    /**
     * Check if a vanilla piston (or sticky piston) can work across claims. At least one of the following
     * requirements must be met:
     * <ul>
     *     <li>Piston protection is disabled in server config</li>
     *     <li>The moved blocks will all end up in a chunk owned by the same team as the piston base's chunk</li>
     *     <li>The moved blocks will all end up in an unclaimed chunk, or in a chunk that allows public "edit block" access</li>
     * </ul>
     * The same restrictions apply to any blocks that would be destroyed by the piston.
     * @param level the level
     * @param pistonPos position of the piston itself
     * @param resolver the piston's structure resolver (with {@code resolve()} already called successfully), which can be used to determine the affected block positions
     * @return true if the piston should be prevented from moving, false to let it move
     */
    public static boolean shouldPreventPistonMovement(Level level, BlockPos pistonPos, PistonStructureResolver resolver) {
        if (!level.isClientSide && FTBChunksWorldConfig.PISTON_PROTECTION.get() && !FTBChunksWorldConfig.DISABLE_PROTECTION.get()) {
            PrivacyProperty editProp = Platform.isFabric() ?
                    FTBChunksProperties.BLOCK_EDIT_AND_INTERACT_MODE :
                    FTBChunksProperties.BLOCK_EDIT_MODE;
            ClaimedChunkManager mgr = FTBChunksAPI.api().getManager();
            ClaimedChunk srcClaim = mgr.getChunk(new ChunkDimPos(level, pistonPos));
            for (BlockPos pos : resolver.getToPush()) {
                // check ownership of position block would be moving from
                if (prevent(editProp, srcClaim, mgr.getChunk(new ChunkDimPos(level, pos)))) {
                    return true;
                }

                BlockPos toPos = pos.relative(resolver.getPushDirection());
                if (pos.getX() >> 4 != toPos.getX() >> 4 || pos.getZ() >> 4 != toPos.getZ() >> 4) {
                    // if different chunk, also check position block would be moving to
                    if (prevent(editProp, srcClaim, mgr.getChunk(new ChunkDimPos(level, toPos)))) {
                        return true;
                    }
                }
            }
            for (BlockPos pos : resolver.getToDestroy()) {
                // check ownership of positions the blocks are in now
                if (prevent(editProp, srcClaim, mgr.getChunk(new ChunkDimPos(level, pos)))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean prevent(PrivacyProperty editProp, ClaimedChunk srcClaim, ClaimedChunk dstClaim) {
        if (srcClaim != dstClaim && dstClaim != null) {
            UUID srcId = srcClaim == null ? Util.NIL_UUID : srcClaim.getTeamData().getTeam().getTeamId();
            UUID dstId = dstClaim.getTeamData().getTeam().getTeamId();
            return !srcId.equals(dstId) && dstClaim.getTeamData().getTeam().getProperty(editProp) != PrivacyMode.PUBLIC;
        }
        return false;
    }
}
