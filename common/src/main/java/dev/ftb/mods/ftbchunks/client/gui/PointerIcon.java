package dev.ftb.mods.ftbchunks.client.gui;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.icon.MapIcon;
import dev.ftb.mods.ftbchunks.api.client.icon.MapType;
import dev.ftb.mods.ftblibrary.client.gui.input.Key;
import dev.ftb.mods.ftblibrary.client.gui.input.MouseButton;
import dev.ftb.mods.ftblibrary.client.gui.widget.BaseScreen;
import dev.ftb.mods.ftblibrary.client.icon.IconHelper;
import dev.ftb.mods.ftblibrary.client.util.ClientUtils;
import dev.ftb.mods.ftblibrary.icon.Icon;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class PointerIcon implements MapIcon {
    public static final Icon<?> POINTER = Icon.getIcon(FTBChunksAPI.id("textures/player.png"));
    
    @Override
    public Vec3 getPos(float partialTick) {
        Player player = ClientUtils.getClientPlayer();
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
    public void draw(MapType mapType, GuiGraphicsExtractor graphics, int x, int y, int w, int h, boolean outsideVisibleArea, int iconAlpha) {
        Player player = ClientUtils.getClientPlayer();
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + w / 2f, y + h / 2f);
        float scale = mapType == MapType.LARGE_MAP ? 2.5F : 2F;
        graphics.pose().scale(scale, scale);
        graphics.pose().rotate((player.getYRot() + 180F) * Mth.DEG_TO_RAD);
        IconHelper.renderIcon(POINTER, graphics, - w / 2, -h / 2, w, h);
        graphics.pose().popMatrix();
    }
}
