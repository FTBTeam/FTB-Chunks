package dev.ftb.mods.ftbchunks.client.mapicon;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbchunks.api.client.icon.MapIcon;
import dev.ftb.mods.ftbchunks.api.client.icon.MapType;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.FaceIcon;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

public class TrackedPlayerMapIcon implements MapIcon {
    private Vec3 pos;
    private final FaceIcon faceIcon;
    private final Component name;

    public TrackedPlayerMapIcon(GameProfile profile, Vec3 pos, FaceIcon faceIcon) {
        this.pos = pos;
        this.faceIcon = faceIcon;
        this.name = Component.literal(profile.getName());
    }

    @Override
    public Vec3 getPos(float partialTick) {
        return pos;
    }

    public void setPos(Vec3 pos) {
        this.pos = pos;
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public void addTooltip(TooltipList list) {
        list.add(name);
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
        if (mapType.isMinimap() || w < 4) {
            faceIcon.draw(graphics, x, y, w, h);
        } else {
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0F);
            graphics.pose().scale(w / 18F, h / 18F, 1F);
            Color4I.BLACK.draw(graphics, 0, 0, 18, 18);
            faceIcon.draw(graphics, 1, 1, 16, 16);
            graphics.pose().popPose();
        }
    }
}
