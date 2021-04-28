package dev.ftb.mods.ftbchunks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ftb.mods.ftbguilibrary.icon.FaceIcon;
import dev.ftb.mods.ftbguilibrary.icon.Icon;
import dev.ftb.mods.ftbguilibrary.utils.TooltipList;
import dev.ftb.mods.ftbguilibrary.widget.Panel;
import dev.ftb.mods.ftbguilibrary.widget.Theme;
import dev.ftb.mods.ftbguilibrary.widget.Widget;
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