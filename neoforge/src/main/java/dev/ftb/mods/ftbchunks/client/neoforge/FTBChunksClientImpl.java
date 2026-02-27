package dev.ftb.mods.ftbchunks.client.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
import org.jspecify.annotations.Nullable;

// arch ExpectPlatform
@SuppressWarnings("unused")
public class FTBChunksClientImpl {
    public static boolean doesKeybindMatch(@Nullable KeyMapping keyMapping, KeyEvent event) {
		if (keyMapping != null && keyMapping.matches(event)) {
			return switch (keyMapping.getKeyModifier()) {
                case SHIFT -> (event.modifiers() & InputConstants.MOD_SHIFT) != 0;
				case CONTROL -> (event.modifiers() & InputConstants.MOD_CONTROL) != 0;
				case ALT ->  (event.modifiers() & InputConstants.MOD_ALT) != 0;
				default -> event.modifiers() == 0;
			};
		}
		return false;
	}
}
