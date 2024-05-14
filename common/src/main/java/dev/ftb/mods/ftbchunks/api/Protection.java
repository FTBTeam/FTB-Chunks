package dev.ftb.mods.ftbchunks.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.stream.StreamSupport;

@FunctionalInterface
public interface Protection {
	/**
	 * Get the protection policy for a specific action
	 *
	 * @param player player doing the action
	 * @param pos blockpos at which the action occurs
	 * @param hand the hand the player is using
	 * @param chunk the claimed chunk in which the action is occurring, or null if not in a claimed chunk
	 * @param entity the entity being acted on, if any
	 *
	 * @return the protection policy
	 */
	ProtectionPolicy getProtectionPolicy(ServerPlayer player, BlockPos pos, InteractionHand hand, @Nullable ClaimedChunk chunk, @Nullable Entity entity);

	Protection EDIT_BLOCK = (player, pos, hand, chunk, entity) -> {
		BlockState blockState = player.level().getBlockState(pos);

		if (blockState.is(FTBChunksTags.Blocks.EDIT_WHITELIST_TAG)) {
			return ProtectionPolicy.ALLOW;
		}

		if (chunk != null && chunk.getTeamData().canPlayerUse(player, FTBChunksProperties.BLOCK_EDIT_MODE)) {
			return ProtectionPolicy.ALLOW;
		}

		return ProtectionPolicy.CHECK;
	};

	Protection INTERACT_BLOCK = (player, pos, hand, chunk, entity) -> {
		BlockState blockState = player.level().getBlockState(pos);

		if (blockState.is(FTBChunksTags.Blocks.INTERACT_WHITELIST_TAG)) {
			return ProtectionPolicy.ALLOW;
		}

		if (chunk != null && chunk.getTeamData().canPlayerUse(player, FTBChunksProperties.BLOCK_INTERACT_MODE)) {
			return ProtectionPolicy.ALLOW;
		}

		return ProtectionPolicy.CHECK;
	};

	Protection RIGHT_CLICK_ITEM = (player, pos, hand, chunk, entity) -> {
		ItemStack stack = player.getItemInHand(hand);

		if (isFood(stack) || isBeneficialPotion(stack) || stack.is(FTBChunksTags.Items.RIGHT_CLICK_WHITELIST_TAG)) {
			return ProtectionPolicy.ALLOW;
		} else if (chunk != null && chunk.getTeamData().canPlayerUse(player, FTBChunksProperties.BLOCK_INTERACT_MODE)) {
			return ProtectionPolicy.ALLOW;
		} else if (chunk != null && stack.is(FTBChunksTags.Items.RIGHT_CLICK_BLACKLIST_TAG)) {
			return ProtectionPolicy.DENY;
		}

		return ProtectionPolicy.ALLOW;
	};

	static boolean isFood(ItemStack stack) {
        //noinspection DataFlowIssue
        return stack.has(DataComponents.FOOD) && stack.get(DataComponents.FOOD).nutrition() > 0;
	}

	static boolean isBeneficialPotion(ItemStack stack) {
		if (stack.has(DataComponents.POTION_CONTENTS)) {
			return StreamSupport.stream(stack.get(DataComponents.POTION_CONTENTS).getAllEffects().spliterator(), false)
					.noneMatch(effect -> effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL);
		}
		return false;
	}

	Protection EDIT_FLUID = (player, pos, hand, chunk, entity) -> {
		if (chunk != null && chunk.getTeamData().canPlayerUse(player, FTBChunksProperties.BLOCK_EDIT_MODE)) {
			return ProtectionPolicy.ALLOW;
		}

		return ProtectionPolicy.CHECK;
	};

	Protection INTERACT_ENTITY = (player, pos, hand, chunk, entity) -> {
		if (entity != null && entity.getType().is(FTBChunksTags.Entities.ENTITY_INTERACT_WHITELIST_TAG)) {
			return ProtectionPolicy.ALLOW;
		} else if (chunk != null && chunk.getTeamData().canPlayerUse(player, FTBChunksProperties.ENTITY_INTERACT_MODE)) {
			return ProtectionPolicy.ALLOW;
		}

		return ProtectionPolicy.CHECK;
	};

	Protection ATTACK_NONLIVING_ENTITY = (player, pos, hand, chunk, entity) -> {
		if (entity != null && entity.getType().is(FTBChunksTags.Entities.NONLIVING_ENTITY_ATTACK_WHITELIST_TAG)) {
			return ProtectionPolicy.ALLOW;
		} else if (chunk != null && chunk.getTeamData().canPlayerUse(player, FTBChunksProperties.NONLIVING_ENTITY_ATTACK_MODE)) {
			return ProtectionPolicy.ALLOW;
		}

		return ProtectionPolicy.CHECK;
	};

	// for use on Fabric
	Protection EDIT_AND_INTERACT_BLOCK = (player, pos, hand, chunk, entity) -> {
		BlockState blockState = player.level().getBlockState(pos);

		if (blockState.is(FTBChunksTags.Blocks.INTERACT_WHITELIST_TAG)) {
			return ProtectionPolicy.ALLOW;
		}

		if (chunk != null && chunk.getTeamData().canPlayerUse(player, FTBChunksProperties.BLOCK_EDIT_AND_INTERACT_MODE)) {
			return ProtectionPolicy.ALLOW;
		}

		return ProtectionPolicy.CHECK;
	};
}
