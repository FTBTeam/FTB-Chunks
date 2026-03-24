package dev.ftb.mods.ftbchunks.client.mapicon;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ftb.mods.ftbchunks.api.client.icon.MapIcon;
import dev.ftb.mods.ftbchunks.client.gui.map.LargeMapScreen;
import dev.ftb.mods.ftbchunks.net.TeleportFromMapPacket;
import dev.ftb.mods.ftblibrary.client.gui.input.Key;
import dev.ftb.mods.ftblibrary.client.gui.widget.BaseScreen;
import dev.ftb.mods.ftblibrary.platform.network.Play2ServerNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class StaticMapIcon extends MapIcon.SimpleMapIcon {
	public StaticMapIcon(BlockPos pos) {
		super(Vec3.atCenterOf(pos));
	}

	@Override
	public boolean onKeyPressed(BaseScreen screen, Key key) {
		return handleKeypress(this, screen, key);
	}

	static boolean handleKeypress(MapIcon icon, BaseScreen screen, Key key) {
		if (screen instanceof LargeMapScreen lms && key.is(InputConstants.KEY_T)) {
			Play2ServerNetworking.send(new TeleportFromMapPacket(BlockPos.containing(icon.getPos(1F)).above(), false, lms.currentDimension()));
			screen.closeGui(false);
			return true;
		}

		return false;
	}
}
