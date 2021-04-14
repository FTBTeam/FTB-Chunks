package dev.ftb.mods.ftbchunks.data;

import dev.ftb.mods.ftbchunks.net.SendGeneralDataPacket;
import me.shedaniel.architectury.hooks.TagHooks;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.UUID;

/**
 * @author LatvianModder
 */
public class FTBChunksAPI {
	public static final Tag<Block> EDIT_TAG = TagHooks.getBlockOptional(new ResourceLocation("ftbchunks", "edit_whitelist"));
	public static final Tag<Block> INTERACT_TAG = TagHooks.getBlockOptional(new ResourceLocation("ftbchunks", "interact_whitelist"));
	public static final Tag<Item> RIGHT_CLICK_BLACKLIST_TAG = TagHooks.getItemOptional(new ResourceLocation("ftbchunks", "right_click_blacklist"));

	public static ClaimedChunkManager manager;

	public static ClaimedChunkManager getManager() {
		if (manager == null) {
			throw new NullPointerException("FTB Chunks Manager hasn't been loaded yet!");
		}

		return manager;
	}

	public static ClaimResult claimAsPlayer(ServerPlayer player, ResourceKey<Level> dimension, ChunkPos pos, boolean checkOnly) {
		return getManager().getData(player).claim(player.createCommandSourceStack(), new ChunkDimPos(dimension, pos), checkOnly);
	}

	public static ClaimResult claimAsServer(UUID id, ResourceKey<Level> dimension, ChunkPos pos, boolean checkOnly) {
		return getManager().getData(id, "Server").claim(getManager().server.createCommandSourceStack(), new ChunkDimPos(dimension, pos), checkOnly);
	}

	public static void syncPlayer(ServerPlayer player) {
		SendGeneralDataPacket.send(getManager().getData(player), player);
	}
}