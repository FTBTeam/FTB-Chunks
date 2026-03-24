package dev.ftb.mods.ftbchunks.util;

import dev.ftb.mods.ftbchunks.CustomMinYRegistry;
import dev.ftb.mods.ftbchunks.api.LevelMinYCalculator;
import dev.ftb.mods.ftbchunks.api.event.CustomMinYEvent;
import dev.ftb.mods.ftblibrary.platform.event.NativeEventPosting;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FTBCUtils {
	/// Used after various events have been canceled server-side; client may already have updated the held item for the
	/// player, but it needs to be brought back in sync with the server.
	///
	/// @param sp   the player
	/// @param hand the hand being used
	public static void forceHeldItemSync(ServerPlayer sp, InteractionHand hand) {
		if (sp.connection != null) {
			switch (hand) {
				case MAIN_HAND -> sp.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, sp.getInventory().getSelectedSlot(), sp.getItemInHand(hand)));
				case OFF_HAND -> sp.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, Inventory.SLOT_OFFHAND, sp.getItemInHand(hand)));
			}
		}
	}

    public static void postMinYEvent(boolean clientSide) {
        List<LevelMinYCalculator> list = new CopyOnWriteArrayList<>();
        NativeEventPosting.get().postEvent(new CustomMinYEvent.Data(list));
        CustomMinYRegistry.getInstance(clientSide).register(list);
    }
}