package dev.ftb.mods.ftbchunks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ftb.mods.ftblibrary.icon.FaceIcon;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;

/**
 * @author LatvianModder
 */
public class PlayerButton extends Widget {
	public final Component name;
	public final Icon icon;
	public final double playerX, playerZ;
	public final float rotation;

	public PlayerButton(Panel panel, AbstractClientPlayer e) {
		super(panel);
		name = e.getDisplayName();
		icon = FaceIcon.getFace(e.getGameProfile());
		playerX = e.getX();
		playerZ = e.getZ();
		rotation = e.getYHeadRot();
	}

	@Override
	public void addMouseOverText(TooltipList list) {
		list.add(name);
	}

	@Override
	public void draw(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
		icon.draw(matrixStack, x, y, w, h);
	}
}