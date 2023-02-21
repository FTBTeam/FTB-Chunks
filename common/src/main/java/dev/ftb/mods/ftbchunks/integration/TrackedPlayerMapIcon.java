package dev.ftb.mods.ftbchunks.integration;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ftb.mods.ftbchunks.client.MapType;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.FaceIcon;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

public class TrackedPlayerMapIcon extends MapIcon {
    private Vec3 pos;
    private final FaceIcon faceIcon;
    private final Component name;

    public TrackedPlayerMapIcon(GameProfile profile, Vec3 pos, FaceIcon faceIcon) {
        this.pos = pos;
        this.faceIcon = faceIcon;
        this.name = Component.literal(profile.getName());
    }

    @Override
    public Vec3 getPos(float delta) {
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
    public void draw(MapType mapType, PoseStack stack, int x, int y, int w, int h, boolean outsideVisibleArea, int iconAlpha) {
        if (mapType.isMinimap() || w < 4) {
            faceIcon.draw(stack, x, y, w, h);
        } else {
            stack.pushPose();
            stack.translate(x, y, 0F);
            stack.scale(w / 18F, h / 18F, 1F);
            Color4I.BLACK.draw(stack, 0, 0, 18, 18);
            faceIcon.draw(stack, 1, 1, 16, 16);
            stack.popPose();
        }
    }
}
