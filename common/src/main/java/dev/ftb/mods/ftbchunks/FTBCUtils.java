package dev.ftb.mods.ftbchunks;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.block.Block;

public class FTBCUtils {
	@ExpectPlatform
	public static boolean isRail(Block block) {
		throw new AssertionError();
	}

	public static boolean isBeneficialPotion(ItemStack stack) {
		return stack.getItem() instanceof PotionItem && PotionUtils.getMobEffects(stack).stream()
				.noneMatch(effect -> effect.getEffect().getCategory() == MobEffectCategory.HARMFUL);
	}
}
