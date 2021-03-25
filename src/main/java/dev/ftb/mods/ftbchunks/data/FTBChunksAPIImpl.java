package dev.ftb.mods.ftbchunks.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.Tags;

/**
 * @author LatvianModder
 */
public class FTBChunksAPIImpl extends FTBChunksAPI {
	public static ClaimedChunkManager manager;
	public static final Tags.IOptionalNamedTag<Block> EDIT_TAG = BlockTags.createOptional(new ResourceLocation("ftbchunks", "edit_whitelist"));
	public static final Tags.IOptionalNamedTag<Block> INTERACT_TAG = BlockTags.createOptional(new ResourceLocation("ftbchunks", "interact_whitelist"));
	public static final Tags.IOptionalNamedTag<Item> RIGHT_CLICK_BLACKLIST_TAG = ItemTags.createOptional(new ResourceLocation("ftbchunks", "right_click_blacklist"));

	@Override
	public ClaimedChunkManager getManager() {
		if (manager == null) {
			throw new NullPointerException("FTB Chunks Manager hasn't been loaded yet!");
		}

		return manager;
	}
}