package dev.ftb.mods.ftbchunks.data;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface Protection {
	Protection EDIT_BLOCK = (player, pos, hand, chunk) -> {
		Block block = player.level.getBlockState(pos).getBlock();

		if (FTBChunksAPI.EDIT_WHITELIST_TAG.contains(block)) {
			return ProtectionOverride.ALLOW;
		}

		if (chunk != null && chunk.teamData.canUse(player, FTBChunksTeamData.BLOCK_EDIT_MODE)) {
			return ProtectionOverride.ALLOW;
		}

		return ProtectionOverride.CHECK;
	};

	Protection INTERACT_BLOCK = (player, pos, hand, chunk) -> {
		Block block = player.level.getBlockState(pos).getBlock();

		if (FTBChunksAPI.INTERACT_WHITELIST_TAG.contains(block)) {
			return ProtectionOverride.ALLOW;
		}

		if (chunk != null && chunk.teamData.canUse(player, FTBChunksTeamData.BLOCK_INTERACT_MODE)) {
			return ProtectionOverride.ALLOW;
		}

		return ProtectionOverride.CHECK;
	};

	Protection RIGHT_CLICK_ITEM = (player, pos, hand, chunk) -> {
		ItemStack stack = player.getItemInHand(hand);

		if (stack.isEdible() || FTBChunksAPI.RIGHT_CLICK_WHITELIST_TAG.contains(stack.getItem())) {
			return ProtectionOverride.ALLOW;
		} else if (chunk != null && chunk.teamData.canUse(player, FTBChunksTeamData.BLOCK_INTERACT_MODE)) {
			return ProtectionOverride.ALLOW;
		} else if (chunk != null && FTBChunksAPI.RIGHT_CLICK_BLACKLIST_TAG.contains(stack.getItem())) {
			return ProtectionOverride.CHECK;
		}

		return ProtectionOverride.ALLOW;
	};

	Protection EDIT_FLUID = (player, pos, hand, chunk) -> {
		if (chunk != null && chunk.teamData.canUse(player, FTBChunksTeamData.BLOCK_EDIT_MODE)) {
			return ProtectionOverride.ALLOW;
		}

		return ProtectionOverride.CHECK;
	};

	ProtectionOverride override(ServerPlayer player, BlockPos pos, InteractionHand hand, @Nullable ClaimedChunk chunk);
}
