package dev.ftb.mods.ftbchunks;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbchunks.client.map.RegionSyncKey;
import dev.ftb.mods.ftbchunks.net.SendChunkPacket;
import dev.ftb.mods.ftbchunks.net.SendGeneralDataPacket;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collection;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class FTBChunksCommon {
	public void init() {
	}

	public void login(UUID serverId, SNBTCompoundTag tag) {
	}

	public void updateGeneralData(SendGeneralDataPacket.GeneralChunkData chunkData) {
	}

//	public void updateChunk(SendChunkPacket packet) {
//	}
//
//	public void updateAllChunks(SendManyChunksPacket packet) {
//	}

	public void updateChunks(ResourceKey<Level> dimId, UUID teamId, Collection<SendChunkPacket.SingleChunk> chunks) {

	}

	public void importWorldMap(ServerLevel world) {
	}

	public void syncRegion(RegionSyncKey key, int offset, int total, byte[] data) {
	}

	public void playerDeath(GlobalPos pos, int number) {
	}

	public int blockColor() {
		return 0;
	}

	public void updateLoadedChunkView(ResourceKey<Level> dimension, Long2IntMap chunks) {
	}

	public boolean skipBlock(BlockState state) {
		return false;
	}

	public void maybeClearDeathpoint(Player player) {
	}

	public void updateTrackedPlayerPos(GameProfile profile, BlockPos pos, boolean valid) {
	}
}