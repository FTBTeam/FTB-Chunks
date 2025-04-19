package dev.ftb.mods.ftbchunks;

import com.mojang.datafixers.util.Pair;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

public class FTBCUtils {
	@ExpectPlatform
	public static boolean isRail(Block block) {
		throw new AssertionError();
	}

	public static boolean isBeneficialPotion(ItemStack stack) {
		return stack.getItem() instanceof PotionItem && PotionUtils.getMobEffects(stack).stream()
				.noneMatch(effect -> effect.getEffect().getCategory() == MobEffectCategory.HARMFUL);
	}

	/**
	 * Used after various events have been cancelled server-side; client may already have updated the held item for the
	 * player, but it needs to be brought back in sync with the server.
	 * @param sp the player
	 * @param hand the hand being used
	 */
	public static void forceHeldItemSync(ServerPlayer sp, InteractionHand hand) {
		if (sp.connection == null) return;
		if (hand == InteractionHand.MAIN_HAND) {
			sp.connection.send(new ClientboundContainerSetSlotPacket(-2, 0,
					sp.getInventory().selected, sp.getItemInHand(hand)));
		} else if (hand == InteractionHand.OFF_HAND) {
			List<Pair<EquipmentSlot, ItemStack>> slots = new ArrayList<>();
			slots.add(new Pair<>(EquipmentSlot.OFFHAND, sp.getInventory().offhand.get(0)));
			sp.connection.send(new ClientboundSetEquipmentPacket(sp.getId(), slots));
		}
	}
}
