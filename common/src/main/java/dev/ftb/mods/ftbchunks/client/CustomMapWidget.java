package dev.ftb.mods.ftbchunks.client;

import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Widget;
import net.minecraft.world.phys.Vec3;

public abstract class CustomMapWidget extends Widget {

	public CustomMapWidget(Panel panel) {
		super(panel);
	}

	public abstract Vec3 getPos();

}
