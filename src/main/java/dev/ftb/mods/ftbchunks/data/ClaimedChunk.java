package dev.ftb.mods.ftbchunks.data;

import dev.ftb.mods.ftbchunks.net.FTBChunksNet;
import dev.ftb.mods.ftbchunks.net.SendChunkPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;
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
public class ClaimedChunk implements ClaimResult {
	public final ClaimedChunkPlayerData playerData;
	public final ChunkDimPos pos;
	public Instant forceLoaded;
	public Instant time;

	public ClaimedChunk(ClaimedChunkPlayerData p, ChunkDimPos cp) {
		playerData = p;
		pos = cp;
		forceLoaded = null;
		time = Instant.now();
	}

	public ClaimedChunkPlayerData getPlayerData() {
		return playerData;
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

	public int getColor() {
		return getPlayerData().getColor();
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
		if (FTBChunksAPI.EDIT_TAG.contains(state.getBlock()) || playerData.canUse(player, playerData.blockEditMode, false)) {
			return true;
		}

		if (!(player instanceof FakePlayer)) {
			ClaimedChunkPlayerData pd = playerData.manager.getData(player);
			return pd.getBypassProtection(player);
		}

		return false;
	}

	public boolean canInteract(ServerPlayer player, BlockState state) {
		if (FTBChunksAPI.INTERACT_TAG.contains(state.getBlock()) || playerData.canUse(player, playerData.blockInteractMode, false)) {
			return true;
		}

		if (!(player instanceof FakePlayer)) {
			ClaimedChunkPlayerData pd = playerData.manager.getData(player);
			return pd.getBypassProtection(player);
		}

		return false;
	}

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

		return !FTBChunksAPI.RIGHT_CLICK_BLACKLIST_TAG.contains(item.getItem());
	}

	public boolean canEntitySpawn(Entity entity) {
		return true;
	}

	public boolean allowExplosions() {
		return false;
	}

	public Component getDisplayName() {
		return new TextComponent("").append(getPlayerData().getDisplayName()).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(getColor())));
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