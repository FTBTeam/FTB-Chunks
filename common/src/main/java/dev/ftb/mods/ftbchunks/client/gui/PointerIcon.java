package dev.ftb.mods.ftbchunks.client.gui;

import com.mojang.math.Axis;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.icon.MapIcon;
import dev.ftb.mods.ftbchunks.api.client.icon.MapType;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class PointerIcon implements MapIcon {

    private static final Icon POINTER = Icon.getIcon(FTBChunksAPI.rl("textures/player.png"));
    
    @Override
    public Vec3 getPos(float partialTick) {
        Player player = Minecraft.getInstance().player;
        return partialTick >= 1F ? player.position() : player.getPosition(partialTick);
    }

    @Override
    public boolean onMousePressed(BaseScreen screen, MouseButton button) {
        return false;
    }

    @Override
    public boolean onKeyPressed(BaseScreen screen, Key key) {
        return false;
    }

    @Override
    public void draw(MapType mapType, GuiGraphics graphics, int x, int y, int w, int h, boolean outsideVisibleArea, int iconAlpha) {
        Player player = Minecraft.getInstance().player;
        graphics.pose().pushPose();
        graphics.pose().translate(x + w / 2f, y + h / 2f, 0F);
        float scale = mapType == MapType.LARGE_MAP ? 2.5F : 2F;
        graphics.pose().scale(scale, scale, scale);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(player.getYRot() + 180F));
        POINTER.draw(graphics, - w / 2, -h / 2, w, h);
        graphics.pose().popPose();
    }
}
