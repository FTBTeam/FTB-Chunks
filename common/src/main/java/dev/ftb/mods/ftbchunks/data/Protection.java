package dev.ftb.mods.ftbchunks.data;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface Protection {
	Protection EDIT_BLOCK = (player, pos, hand, chunk) -> {
		BlockState blockState = player.level.getBlockState(pos);

		if (blockState.is(FTBChunksAPI.EDIT_WHITELIST_TAG)) {
			return ProtectionOverride.ALLOW;
		}

		if (chunk != null && chunk.teamData.canUse(player, FTBChunksTeamData.BLOCK_EDIT_MODE)) {
			return ProtectionOverride.ALLOW;
		}

		return ProtectionOverride.CHECK;
	};

	Protection INTERACT_BLOCK = (player, pos, hand, chunk) -> {
		BlockState blockState = player.level.getBlockState(pos);

		if (blockState.is(FTBChunksAPI.INTERACT_WHITELIST_TAG)) {
			return ProtectionOverride.ALLOW;
		}

		if (chunk != null && chunk.teamData.canUse(player, FTBChunksTeamData.BLOCK_INTERACT_MODE)) {
			return ProtectionOverride.ALLOW;
		}

		return ProtectionOverride.CHECK;
	};

	Protection RIGHT_CLICK_ITEM = (player, pos, hand, chunk) -> {
		ItemStack stack = player.getItemInHand(hand);

		//FTBChunksAPI.RIGHT_CLICK_WHITELIST_TAG.contains(stack.getItem())
		if (stack.isEdible() || stack.is(FTBChunksAPI.RIGHT_CLICK_WHITELIST_TAG)) {
			return ProtectionOverride.ALLOW;
		} else if (chunk != null && chunk.teamData.canUse(player, FTBChunksTeamData.BLOCK_INTERACT_MODE)) {
			return ProtectionOverride.ALLOW;
			//FTBChunksAPI.RIGHT_CLICK_BLACKLIST_TAG.contains(stack.getItem())
		} else if (chunk != null && stack.is(FTBChunksAPI.RIGHT_CLICK_BLACKLIST_TAG)) {
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
