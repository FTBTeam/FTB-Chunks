package dev.ftb.mods.ftbchunks.data;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.Tags;

import java.util.UUID;

/**
 * @author LatvianModder
 */
public class FTBChunksAPI {
	public static final Tags.IOptionalNamedTag<Block> EDIT_TAG = BlockTags.createOptional(new ResourceLocation("ftbchunks", "edit_whitelist"));
	public static final Tags.IOptionalNamedTag<Block> INTERACT_TAG = BlockTags.createOptional(new ResourceLocation("ftbchunks", "interact_whitelist"));
	public static final Tags.IOptionalNamedTag<Item> RIGHT_CLICK_BLACKLIST_TAG = ItemTags.createOptional(new ResourceLocation("ftbchunks", "right_click_blacklist"));

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
}