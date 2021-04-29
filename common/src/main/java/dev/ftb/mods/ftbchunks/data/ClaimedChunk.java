package dev.ftb.mods.ftbchunks.data;

import dev.ftb.mods.ftbchunks.net.SendChunkPacket;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import me.shedaniel.architectury.hooks.PlayerHooks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Date;

/**
 * @author LatvianModder
 */
public class ClaimedChunk implements ClaimResult {
	public final FTBChunksTeamData teamData;
	public final ChunkDimPos pos;
	public Instant forceLoaded;
	public Instant time;

	public ClaimedChunk(FTBChunksTeamData p, ChunkDimPos cp) {
		teamData = p;
		pos = cp;
		forceLoaded = null;
		time = Instant.now();
	}

	public FTBChunksTeamData getTeamData() {
		return teamData;
	}

	public ChunkDimPos getPos() {
		return pos;
	}

	public Instant getTimeClaimed() {
		return time;
	}

	@Override
	public boolean isSuccess() {
		return true;
	}

	@Override
	public void setClaimedTime(Instant t) {
		time = t;
		sendUpdateToAll();
	}

	@Nullable
	public Instant getForceLoadedTime() {
		return forceLoaded;
	}

	public boolean isForceLoaded() {
		return getForceLoadedTime() != null;
	}

	@Override
	public void setForceLoadedTime(@Nullable Instant time) {
		forceLoaded = time;
	}

	public boolean canEdit(ServerPlayer player, BlockState state) {
		if (FTBChunksAPI.EDIT_TAG.contains(state.getBlock()) || teamData.canUse(player, FTBChunksTeamData.BLOCK_EDIT_MODE)) {
			return true;
		}

		if (!PlayerHooks.isFake(player)) {
			FTBChunksTeamData pd = teamData.manager.getData(player);
			return pd.getBypassProtection(player);
		}

		return false;
	}

	public boolean canInteract(ServerPlayer player, BlockState state) {
		if (FTBChunksAPI.INTERACT_TAG.contains(state.getBlock()) || teamData.canUse(player, FTBChunksTeamData.BLOCK_INTERACT_MODE)) {
			return true;
		}

		if (!PlayerHooks.isFake(player)) {
			FTBChunksTeamData pd = teamData.manager.getData(player);
			return pd.getBypassProtection(player);
		}

		return false;
	}

	public boolean canRightClickItem(ServerPlayer player, ItemStack item) {
		if (teamData.canUse(player, FTBChunksTeamData.BLOCK_INTERACT_MODE)) {
			return true;
		}

		if (!PlayerHooks.isFake(player)) {
			FTBChunksTeamData pd = teamData.manager.getData(player);

			if (pd.getBypassProtection(player)) {
				return true;
			}
		}

		return !FTBChunksAPI.RIGHT_CLICK_BLACKLIST_TAG.contains(item.getItem());
	}

	public boolean canEntitySpawn(Entity entity) {
		return true;
	}

	public boolean allowExplosions() {
		return false;
	}

	public void postSetForceLoaded(boolean load) {
		ServerLevel world = getTeamData().getManager().getMinecraftServer().getLevel(getPos().dimension);

		if (world != null) {
			world.setChunkForced(getPos().x, getPos().z, load);
			sendUpdateToAll();
		}
	}

	public void sendUpdateToAll() {
		SendChunkPacket packet = new SendChunkPacket();
		packet.dimension = pos.dimension;
		packet.teamId = teamData.getTeamId();
		packet.chunk = new SendChunkPacket.SingleChunk(new Date(), pos.x, pos.z, this);
		packet.sendToAll();
	}
}