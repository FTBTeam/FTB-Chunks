package dev.ftb.mods.ftbchunks.data;

import dev.ftb.mods.ftbchunks.event.ClaimedChunkEvent;
import dev.ftb.mods.ftbchunks.net.SendChunkPacket;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import me.shedaniel.architectury.hooks.PlayerHooks;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * @author LatvianModder
 */
public class ClaimedChunk implements ClaimResult {
	public FTBChunksTeamData teamData;
	public final ChunkDimPos pos;
	public long forceLoaded;
	public long time;

	public ClaimedChunk(FTBChunksTeamData p, ChunkDimPos cp) {
		teamData = p;
		pos = cp;
		forceLoaded = 0L;
		time = System.currentTimeMillis();
	}

	public FTBChunksTeamData getTeamData() {
		return teamData;
	}

	public ChunkDimPos getPos() {
		return pos;
	}

	public long getTimeClaimed() {
		return time;
	}

	@Override
	public boolean isSuccess() {
		return true;
	}

	@Override
	public void setClaimedTime(long t) {
		time = t;
		sendUpdateToAll();
	}

	public long getForceLoadedTime() {
		return forceLoaded;
	}

	public boolean isForceLoaded() {
		return forceLoaded > 0L;
	}

	@Override
	public void setForceLoadedTime(long time) {
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
			if (world.setChunkForced(getPos().x, getPos().z, load)) {
				sendUpdateToAll();
			}
		}
	}

	public void sendUpdateToAll() {
		SendChunkPacket packet = new SendChunkPacket();
		packet.dimension = pos.dimension;
		packet.teamId = teamData.getTeamId();
		packet.chunk = new SendChunkPacket.SingleChunk(System.currentTimeMillis(), pos.x, pos.z, this);
		packet.sendToAll(teamData.manager.getMinecraftServer());
	}

	public void unload(CommandSourceStack source) {
		if (isForceLoaded()) {
			setForceLoadedTime(0L);
			postSetForceLoaded(false);
			ClaimedChunkEvent.AFTER_UNLOAD.invoker().after(source, this);
			teamData.save();
		}
	}

	public void unclaim(CommandSourceStack source, boolean sync) {
		unload(source);

		teamData.manager.claimedChunks.remove(pos);
		ClaimedChunkEvent.AFTER_UNCLAIM.invoker().after(source, this);
		teamData.save();

		if (sync) {
			SendChunkPacket packet = new SendChunkPacket();
			packet.dimension = pos.dimension;
			packet.teamId = Util.NIL_UUID;
			packet.chunk = new SendChunkPacket.SingleChunk(System.currentTimeMillis(), pos.x, pos.z, null);
			packet.sendToAll(source.getServer());
		}
	}
}