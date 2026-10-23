package dev.ftb.mods.ftbchunks.util;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.FTBChunksProperties;
import dev.ftb.mods.ftbchunks.config.FTBChunksWorldConfig;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.util.NameMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.BlockUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.border.WorldBorder;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ClaimRespectingNetherPortals {
    public static ClaimResult tryCreatePortal(Entity entity, ServerLevel destLevel, BlockPos exitPos) {
        PortalBehaviour portalBehaviour = FTBChunksWorldConfig.PORTAL_CREATION.get();
        if (portalBehaviour == PortalBehaviour.ALLOW) {
            return new ClaimResult(PortalBehaviour.ALLOW, null);
        }

        if (entity instanceof ServerPlayer player) {
            var cc = FTBChunksAPI.api().getManager().getChunk(new ChunkDimPos(destLevel, exitPos));
            if (cc != null && !cc.getTeamData().canPlayerUse(player, FTBChunksProperties.BLOCK_EDIT_MODE)
                    || cc == null && FTBChunksWorldConfig.NO_WILDERNESS.get())
            {
                if (portalBehaviour == PortalBehaviour.FAILURE) {
                    portalBehaviour.notifyEntity(entity);
                    return new ClaimResult(PortalBehaviour.FAILURE, null);
                }

                // can't place a portal here; allow player to teleport (if possible) but don't create a portal
                WorldBorder border = destLevel.getWorldBorder();
                int maxY = Math.min(destLevel.getMaxY(), destLevel.getMinY() + destLevel.getLogicalHeight()) - 1;
                for (var mutPos : BlockPos.spiralAround(exitPos, 16, Direction.EAST, Direction.SOUTH)) {
                    // level#getHeight SHOULD work here - we know the chunk is loaded by PortalForcer#findClosestPortalPosition()
                    // but getHeight() still seems to be returning level#getMinBuildHeight(), which is not useful!
                    // so we'll search from max build height, which is less efficient but at least works
//                    int y1 = Math.min(y0, destLevel.getHeight(Heightmap.Types.MOTION_BLOCKING, mutablePos.getX(), mutablePos.getZ()));
                    if (border.isWithinBounds(mutPos)) {
                        // search down from exitPos.y
                        for (int y = exitPos.getY(); y > destLevel.getMinY() + 3; y -= 1) {
                            mutPos.setY(y);
                            if (isSafeExitPos(entity, destLevel, mutPos)) {
                                portalBehaviour.notifyEntity(entity);
                                return ClaimResult.withoutPortal(mutPos);
                            }
                        }
                        // no luck? search up from exitPos.y
                        for (int y = exitPos.getY() + 1; y < maxY - 2; y += 1) {
                            mutPos.setY(y);
                            if (isSafeExitPos(entity, destLevel, mutPos)) {
                                portalBehaviour.notifyEntity(entity);
                                return ClaimResult.withoutPortal(mutPos);
                            }
                        }
                    }
                }
                PortalBehaviour.FAILURE.notifyEntity(entity);
                return new ClaimResult(PortalBehaviour.FAILURE, null);
            }
        }
        return new ClaimResult(PortalBehaviour.ALLOW, null);
    }

    private static boolean isSafeExitPos(Entity entity, ServerLevel destLevel, BlockPos.MutableBlockPos mutPos) {
        mutPos.move(Direction.DOWN, 1);
        return destLevel.getBlockState(mutPos).entityCanStandOn(destLevel, mutPos, entity)
                && destLevel.getBlockState(mutPos.move(Direction.UP, 1)).isAir()
                && destLevel.getBlockState(mutPos.move(Direction.UP, 1)).isAir();
    }

    public enum PortalBehaviour {
        NO_PORTAL,  // port the player but don't create a portal
        FAILURE,    // don't port the player at all
        ALLOW;      // fall back to vanilla behavior, creating a portal

        public static final NameMap<PortalBehaviour> NAME_MAP = NameMap.of(NO_PORTAL, PortalBehaviour.values()).create();

        public void notifyEntity(Entity entity) {
            if (entity instanceof ServerPlayer sp) {
                sp.sendOverlayMessage(Component.translatable("ftbchunks.nether_portal." + NAME_MAP.getName(this)).withStyle(ChatFormatting.GOLD));
            }
        }
    }

    public record ClaimResult(PortalBehaviour type, @Nullable BlockUtil.FoundRectangle rect) {
        public Optional<BlockUtil.FoundRectangle> rectangle() {
            return Optional.ofNullable(rect);
        }

        public static ClaimResult withoutPortal(BlockPos.MutableBlockPos mut) {
            return new ClaimResult(PortalBehaviour.NO_PORTAL, new BlockUtil.FoundRectangle(mut.immutable(), 2, 3));
        }
    }
}
