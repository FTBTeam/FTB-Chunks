package dev.ftb.mods.ftbchunks.util;

import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.ClaimedChunkManager;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.config.FTBChunksWorldConfig;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.api.property.PrivacyMode;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Util;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

import java.util.UUID;

public class FireSpreadHelper {
    public static boolean shouldPreventFireSpread(LevelReader levelReader, BlockPos fromPos, BlockPos toPos) {
        if (!FTBChunksWorldConfig.FIRE_SPREAD_PROTECTION.get() || FTBChunksWorldConfig.DISABLE_PROTECTION.get()) {
            return false;
        }

        if ((fromPos.getX() >> 4 != toPos.getX() >> 4 || fromPos.getZ() >> 4 != toPos.getZ() >> 4) && levelReader instanceof Level level) {
            ClaimedChunkManager mgr = FTBChunksAPI.api().getManager();
            ClaimedChunk dstClaim = mgr.getChunk(new ChunkDimPos(level, toPos));
            if (dstClaim == null) {
                return false;
            }
            ClaimedChunk srcClaim = mgr.getChunk(new ChunkDimPos(level, fromPos));
            if (srcClaim != dstClaim) {
                UUID srcId = srcClaim == null ? Util.NIL_UUID : srcClaim.getTeamData().getTeam().getTeamId();
                UUID dstId = dstClaim.getTeamData().getTeam().getTeamId();
                return !srcId.equals(dstId) && dstClaim.getTeamData().getTeam().getProperty(FTBCUtils.getPlatformEditProp()) != PrivacyMode.PUBLIC;
            }
        }
        return false;
    }
}
