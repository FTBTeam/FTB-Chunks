package dev.ftb.mods.ftbchunks.client.mapicon;

import dev.ftb.mods.ftbchunks.api.client.icon.MapIcon;
import dev.ftb.mods.ftbchunks.api.client.icon.MapType;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftblibrary.client.gui.input.Key;
import dev.ftb.mods.ftblibrary.client.gui.input.MouseButton;
import dev.ftb.mods.ftblibrary.client.gui.widget.BaseScreen;
import dev.ftb.mods.ftblibrary.client.icon.IconHelper;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.EntityIconLoader;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;

public class EntityMapIcon implements MapIcon {
    private final Entity entity;
    private final Icon<?> icon;
    private final EntityIconLoader.WidthHeight widthHeight;

    public EntityMapIcon(Entity entity, Icon icon) {
        this.entity = entity;
        this.icon = icon;
        this.widthHeight = new EntityIconLoader.WidthHeight(16, 16);
    }

    public EntityMapIcon(Entity entity, Icon icon, EntityIconLoader.WidthHeight widthHeight) {
        this.entity = entity;
        this.icon = icon;
        this.widthHeight = widthHeight;
    }

    @Override
    public Vec3 getPos(float partialTick) {
        return partialTick >= 1F ? entity.position() : entity.getPosition(partialTick);
    }

    @Override
    public boolean isVisible(MapType mapType, double distanceToPlayer, boolean outsideVisibleArea) {
        return !mapType.isWorldIcon() && (entity instanceof AbstractClientPlayer || !outsideVisibleArea) && FTBChunksClientConfig.ENTITY_ICON.get().getOrDefault(entity.getType().arch$registryName().toString(), true);
    }

    @Override
    public double getIconScale(MapType mapType) {
        return (entity instanceof AbstractClientPlayer || !mapType.isMinimap() || FTBChunksClientConfig.MINIMAP_LARGE_ENTITIES.get() ? 1D : (1D / 1.5D)) * EntityIconLoader.getSettings(entity.getType()).map(EntityIconLoader.EntityIconSettings::scale).orElse(1D);
    }

    @Override
    public boolean isZoomDependant(MapType mapType) {
        return !(entity instanceof AbstractClientPlayer);
    }

    @Override
    public int getPriority() {
        return entity instanceof LocalPlayer ? 150 : entity instanceof AbstractClientPlayer ? 100 : 0;
    }

    @Override
    public void addTooltip(TooltipList list) {
        list.add(entity.getName());
    }

    @Override
    public boolean onMousePressed(BaseScreen screen, MouseButton button) {
        return false;
    }

    @Override
    public boolean onKeyPressed(BaseScreen screen, Key key) {
        return !(entity instanceof LocalPlayer) && StaticMapIcon.handleKeypress(this, screen, key);
    }

    @Override
    public void draw(MapType mapType, GuiGraphics graphics, int x, int y, int w, int h, boolean outsideVisibleArea, int iconAlpha) {
        if (!(entity instanceof AbstractClientPlayer) || mapType.isMinimap() || w < 4 || icon == EntityIconLoader.NORMAL || icon == EntityIconLoader.HOSTILE) {
            IconHelper.renderIcon(icon, graphics, x, y, w, h);
        } else {
            Matrix3x2fStack poseStack = graphics.pose();
            poseStack.pushMatrix();
            poseStack.translate(x, y);
            poseStack.scale(w / 18F, h / 18F);
            IconHelper.renderIcon(Color4I.BLACK, graphics, 0, 0, 18, 18);
            IconHelper.renderIcon(icon, graphics, 1, 1, widthHeight.width(), widthHeight.height());
            poseStack.popMatrix();
        }
    }
}
