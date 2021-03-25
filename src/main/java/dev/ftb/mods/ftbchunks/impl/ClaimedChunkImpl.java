package dev.ftb.mods.ftbchunks.impl;

import dev.ftb.mods.ftbchunks.api.ChunkDimPos;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.ClaimedChunkPlayerData;
import dev.ftb.mods.ftbchunks.net.FTBChunksNet;
import dev.ftb.mods.ftbchunks.net.SendChunkPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Date;

/**
 * @author LatvianModder
 */
public class ClaimedChunkImpl implements ClaimedChunk {
	public final ClaimedChunkPlayerDataImpl playerData;
	public final ChunkDimPos pos;
	public Instant forceLoaded;
	public ClaimedChunkGroupImpl group;
	public Instant time;

	public ClaimedChunkImpl(ClaimedChunkPlayerDataImpl p, ChunkDimPos cp) {
		playerData = p;
		pos = cp;
		forceLoaded = null;
		time = Instant.now();
	}

	@Override
	public ClaimedChunkPlayerDataImpl getPlayerData() {
		return playerData;
	}

	@Override
	public ChunkDimPos getPos() {
		return pos;
	}

	@Nullable
	@Override
	public ClaimedChunkGroupImpl getGroup() {
		return group;
	}

	@Override
	public Instant getTimeClaimed() {
		return time;
	}

	@Override
	public void setClaimedTime(Instant t) {
		time = t;
		sendUpdateToAll();
	}

	@Override
	@Nullable
	public Instant getForceLoadedTime() {
		return forceLoaded;
	}

	@Override
	public void setForceLoadedTime(@Nullable Instant time) {
		forceLoaded = time;
	}

	@Override
	public boolean canEdit(ServerPlayer player, BlockState state) {
		if (FTBChunksAPIImpl.EDIT_TAG.contains(state.getBlock()) || playerData.canUse(player, playerData.blockEditMode, false)) {
			return true;
		}

		if (!(player instanceof FakePlayer)) {
			ClaimedChunkPlayerData pd = playerData.manager.getData(player);
			return pd.getBypassProtection(player);
		}

		return false;
	}

	@Override
	public boolean canInteract(ServerPlayer player, BlockState state) {
		if (FTBChunksAPIImpl.INTERACT_TAG.contains(state.getBlock()) || playerData.canUse(player, playerData.blockInteractMode, false)) {
			return true;
		}

		if (!(player instanceof FakePlayer)) {
			ClaimedChunkPlayerData pd = playerData.manager.getData(player);
			return pd.getBypassProtection(player);
		}

		return false;
	}

	@Override
	public boolean canRightClickItem(ServerPlayer player, ItemStack item) {
		if (playerData.canUse(player, playerData.blockInteractMode, false)) {
			return true;
		}

		if (!(player instanceof FakePlayer)) {
			ClaimedChunkPlayerData pd = playerData.manager.getData(player);

			if (pd.getBypassProtection(player)) {
				return true;
			}
		}

		return !FTBChunksAPIImpl.RIGHT_CLICK_BLACKLIST_TAG.contains(item.getItem());
	}

	@Override
	public boolean canEntitySpawn(Entity entity) {
		return true;
	}

	@Override
	public boolean allowExplosions() {
		return false;
	}

	public void postSetForceLoaded(boolean load) {
		ServerLevel world = getPlayerData().getManager().getMinecraftServer().getLevel(getPos().dimension);

		if (world != null) {
			world.setChunkForced(getPos().x, getPos().z, load);
			sendUpdateToAll();
		}
	}

	public void sendUpdateToAll() {
		SendChunkPacket packet = new SendChunkPacket();
		packet.dimension = pos.dimension;
		packet.owner = playerData.getUuid();
		packet.chunk = new SendChunkPacket.SingleChunk(new Date(), pos.x, pos.z, this);
		FTBChunksNet.MAIN.send(PacketDistributor.ALL.noArg(), packet);
	}
}