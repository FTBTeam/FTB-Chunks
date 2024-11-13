package dev.ftb.mods.ftbchunks.data;

import dev.ftb.mods.ftbchunks.FTBCUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface Protection {
	Protection EDIT_BLOCK = (player, pos, hand, chunk, entity) -> {
		BlockState blockState = player.level.getBlockState(pos);

		if (blockState.is(FTBChunksAPI.EDIT_BLACKLIST_TAG)) {
			return ProtectionOverride.CHECK;
		}

		if (blockState.is(FTBChunksAPI.EDIT_WHITELIST_TAG)) {
			return ProtectionOverride.ALLOW;
		}

		if (chunk != null && chunk.teamData.canUse(player, FTBChunksTeamData.BLOCK_EDIT_MODE)) {
			return ProtectionOverride.ALLOW;
		}

		return ProtectionOverride.CHECK;
	};

	Protection BREAK_BLOCK = (player, pos, hand, chunk, entity) -> {
		BlockState blockState = player.level.getBlockState(pos);

		if (blockState.is(FTBChunksAPI.EDIT_WHITELIST_TAG)) {
			return ProtectionOverride.ALLOW;
		}

		if (chunk != null && chunk.teamData.canBreak(player, FTBChunksTeamData.BLOCK_EDIT_MODE, false, blockState)) {
			return ProtectionOverride.ALLOW;
		}

		return ProtectionOverride.CHECK;
	};

	Protection LEFT_CLICK_BLOCK = (player, pos, hand, chunk, entity) -> {
		BlockState blockState = player.level.getBlockState(pos);

		if (blockState.is(FTBChunksAPI.EDIT_WHITELIST_TAG)) {
			return ProtectionOverride.ALLOW;
		}

		if (chunk != null && chunk.teamData.canBreak(player, FTBChunksTeamData.BLOCK_EDIT_MODE, true, blockState)) {
			return ProtectionOverride.ALLOW;
		}

		return ProtectionOverride.CHECK;
	};

	Protection INTERACT_BLOCK = (player, pos, hand, chunk, entity) -> {
		BlockState blockState = player.level.getBlockState(pos);

		if (blockState.is(FTBChunksAPI.INTERACT_WHITELIST_TAG)) {
			return ProtectionOverride.ALLOW;
		}

		if (chunk != null && chunk.teamData.canUse(player, FTBChunksTeamData.BLOCK_INTERACT_MODE)) {
			return ProtectionOverride.ALLOW;
		}

		return ProtectionOverride.CHECK;
	};

	Protection RIGHT_CLICK_ITEM = (player, pos, hand, chunk, entity) -> {
		ItemStack stack = player.getItemInHand(hand);

		if (stack.isEdible() || FTBCUtils.isBeneficialPotion(stack) || stack.is(FTBChunksAPI.RIGHT_CLICK_WHITELIST_TAG)) {
			return ProtectionOverride.ALLOW;
		} else if (chunk != null && chunk.teamData.canUse(player, FTBChunksTeamData.BLOCK_INTERACT_MODE)) {
			return ProtectionOverride.ALLOW;
		} else if (chunk != null && stack.is(FTBChunksAPI.RIGHT_CLICK_BLACKLIST_TAG)) {
			return ProtectionOverride.CHECK;
		}

		return ProtectionOverride.ALLOW;
	};

	Protection EDIT_FLUID = (player, pos, hand, chunk, entity) -> {
		if (chunk != null && chunk.teamData.canUse(player, FTBChunksTeamData.BLOCK_EDIT_MODE)) {
			return ProtectionOverride.ALLOW;
		}

		return ProtectionOverride.CHECK;
	};

	Protection INTERACT_ENTITY = (player, pos, hand, chunk, entity) -> {
		if (entity != null && entity.getType().is(FTBChunksAPI.ENTITY_INTERACT_WHITELIST_TAG)) {
			return ProtectionOverride.ALLOW;
		} else if (chunk != null && chunk.teamData.canUse(player, FTBChunksTeamData.ENTITY_INTERACT_MODE)) {
			return ProtectionOverride.ALLOW;
		}

		return ProtectionOverride.CHECK;
	};

	Protection ATTACK_NONLIVING_ENTITY = (player, pos, hand, chunk, entity) -> {
		if (entity != null && entity.getType().is(FTBChunksAPI.NONLIVING_ENTITY_ATTACK_WHITELIST_TAG)) {
			return ProtectionOverride.ALLOW;
		} else if (chunk != null && chunk.teamData.canUse(player, FTBChunksTeamData.NONLIVING_ENTITY_ATTACK_MODE)) {
			return ProtectionOverride.ALLOW;
		}

		return ProtectionOverride.CHECK;
	};

	Protection ATTACK_LIVING_ENTITY = (player, pos, hand, chunk, entity) -> {
		if (entity != null && entity.getType().is(FTBChunksAPI.LIVING_ENTITY_ATTACK_BLACKLIST_TAG)
				&& chunk != null && !chunk.teamData.canAttackBlackListedEntity(player, FTBChunksTeamData.ALLOW_ATTACK_BLACKLISTED_ENTITIES)) {
			return ProtectionOverride.CHECK;
		}

		return ProtectionOverride.ALLOW;
	};

	// for use on Fabric
    Protection EDIT_AND_INTERACT_BLOCK = (player, pos, hand, chunk, entity) -> {
		BlockState blockState = player.level.getBlockState(pos);

		if (blockState.is(FTBChunksAPI.INTERACT_WHITELIST_TAG)) {
			return ProtectionOverride.ALLOW;
		}

		if (chunk != null && chunk.teamData.canUse(player, FTBChunksTeamData.BLOCK_EDIT_AND_INTERACT_MODE)) {
			return ProtectionOverride.ALLOW;
		}

		return ProtectionOverride.CHECK;
	};

	Protection BREAK_BLOCK_FABRIC = (player, pos, hand, chunk, entity) -> {
		BlockState blockState = player.level.getBlockState(pos);

		if (blockState.is(FTBChunksAPI.INTERACT_WHITELIST_TAG)) {
			return ProtectionOverride.ALLOW;
		}

		if (chunk != null && chunk.teamData.canBreak(player, FTBChunksTeamData.BLOCK_EDIT_AND_INTERACT_MODE, false, blockState)) {
			return ProtectionOverride.ALLOW;
		}

		return ProtectionOverride.CHECK;
	};

	Protection LEFT_CLICK_BLOCK_FABRIC = (player, pos, hand, chunk, entity) -> {
		BlockState blockState = player.level.getBlockState(pos);

		if (blockState.is(FTBChunksAPI.INTERACT_WHITELIST_TAG)) {
			return ProtectionOverride.ALLOW;
		}

		if (chunk != null && chunk.teamData.canBreak(player, FTBChunksTeamData.BLOCK_EDIT_AND_INTERACT_MODE, true, blockState)) {
			return ProtectionOverride.ALLOW;
		}

		return ProtectionOverride.CHECK;
	};

    ProtectionOverride override(ServerPlayer player, BlockPos pos, InteractionHand hand, @Nullable ClaimedChunk chunk, @Nullable Entity entity);
}
