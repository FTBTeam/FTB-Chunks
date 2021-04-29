package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftbchunks.ColorMapLoader;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.core.BlockStateFTBC;
import dev.ftb.mods.ftbchunks.net.SendChunkPacket;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftbteams.data.ClientTeam;
import dev.ftb.mods.ftbteams.data.ClientTeamManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class MapChunk {
	public final MapRegion region;
	public final XZ pos;
	public long modified;

	public ClientTeam team;
	public Date claimedDate;
	public Date forceLoadedDate;

	public MapChunk(MapRegion r, XZ p) {
		region = r;
		pos = p;
		modified = 0L;

		team = null;
		claimedDate = null;
		forceLoadedDate = null;
	}

	@Nullable
	public ClientTeam getTeam() {
		// TODO: Check invalidity
		return team;
	}

	public boolean connects(MapChunk chunk) {
		return Objects.equals(getTeam(), chunk.getTeam());
	}

	public XZ getActualPos() {
		return XZ.of((region.pos.x << 5) + pos.x, (region.pos.z << 5) + pos.z);
	}

	public static boolean isWater(BlockState state) {
		return state instanceof BlockStateFTBC ? ((BlockStateFTBC) state).getFTBCIsWater() : state.getFluidState().getType().isSame(Fluids.WATER);
	}

	public static boolean skipBlock(BlockState state) {
		ResourceLocation id = FTBChunks.BLOCK_REGISTRY.getId(state.getBlock());
		return id == null || ColorMapLoader.getBlockColor(id).isIgnored();
	}

	public static BlockPos.MutableBlockPos getHeight(@Nullable ChunkAccess chunk, BlockPos.MutableBlockPos pos, boolean[] flags) {
		int topY = pos.getY();

		if (topY == -1) {
			pos.setY(-1);
			return pos;
		}

		for (int by = topY; by > 0; by--) {
			pos.setY(by);
			BlockState state = chunk.getBlockState(pos);

			if (by == topY || state.getBlock() == Blocks.BEDROCK) {
				for (; by > 0; by--) {
					pos.setY(by);
					state = chunk.getBlockState(pos);

					if (state.isAir()) {
						break;
					}
				}
			}

			boolean water = isWater(state);
			flags[0] |= water;

			if (!water && !skipBlock(state)) {
				pos.setY(by);
				return pos;
			}
		}

		pos.setY(-1);
		return pos;
	}

	public MapChunk created() {
		region.update(true);
		return this;
	}

	public MapChunk offsetBlocking(int x, int z) {
		XZ pos = getActualPos().offset(x, z);
		return region.dimension.getRegion(XZ.regionFromChunk(pos.x, pos.z)).getDataBlocking().getChunk(pos);
	}

	public void updateFrom(Date now, SendChunkPacket.SingleChunk packet, UUID t) {
		team = ClientTeamManager.INSTANCE.teamMap.get(t);
		claimedDate = team == null ? null : new Date(now.getTime() - packet.relativeTimeClaimed);
		forceLoadedDate = packet.forceLoaded && claimedDate != null ? new Date(now.getTime() - packet.relativeTimeForceLoaded) : null;
		region.update(false);
	}
}