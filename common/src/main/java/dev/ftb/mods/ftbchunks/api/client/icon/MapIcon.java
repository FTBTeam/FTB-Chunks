package dev.ftb.mods.ftbchunks.api.client.icon;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public interface MapIcon {
    /**
     * Get this icon's position in the world
     * @param partialTick tick delta (0..1f) since last full tick
     * @return the icon's position
     */
    Vec3 getPos(float partialTick);

    /**
     * Check if this icon is visible and should be rendered
     *
     * @param mapType the map type
     * @param distanceToPlayer icon distance to the client player
     * @param outsideVisibleArea true if the icon is currently off the map's visible area
     * @return true if the icon should be considered visible
     */
    default boolean isVisible(MapType mapType, double distanceToPlayer, boolean outsideVisibleArea) {
        return !mapType.isWorldIcon();
    }

    /**
     * Get the icon's scaling factor (independent of zoom).
     *
     * @param mapType the map type
     * @return the scale factor
     */
    default double getIconScale(MapType mapType) {
        return 1D;
    }

    /**
     * Return true if the icon should be rendered on the edge of the map, even if it's off the map. An example of this
     * would be waypoint icons rendering on the edge of the minimap.
     *
     * @param mapType the map type
     * @param outsideVisibleArea true if the icon is off the map's visible area
     * @return true if the icon should be rendered on the map's edge
     */
    default boolean isIconOnEdge(MapType mapType, boolean outsideVisibleArea) {
        return false;
    }

    /**
     * Determines if the icon's rendered size is affected by the map's zoom factor
     *
     * @param mapType the map type
     * @return true if the map's zoom factor has an affect
     */
    default boolean isZoomDependant(MapType mapType) {
        return false;
    }

    /**
     * Determines the order in which map icons are rendered. Higher priority icons render in front of lower-priority
     * icons (even when further away, in 3d game context).
     *
     * @return the icon priority
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Add a tooltip for when the player mouses over the icon. The default implementation just shows the distance in
     * blocks (metres) from the icon to the client player's position.
     *
     * @param list a tooltip list to append to
     */
    default void addTooltip(TooltipList list) {
        Player player = Minecraft.getInstance().player;
        Vec3 pos = getPos(1F);
        list.styledString(String.format("%,d m", Mth.ceil(MathUtils.dist(player.getX(), player.getZ(), pos.x, pos.z))), ChatFormatting.GRAY);
    }

    /**
     * Called when the player clicks the mouse on the icon in GUI context
     * @param screen the currently open screen
     * @param button the clicked button
     * @return true if the click event was handled
     */
    boolean onMousePressed(BaseScreen screen, MouseButton button);

    /**
     * Called when the player presses a key while hovering over the icon in GUI context
     * @param screen the currently open screen
     * @param key the pressed key
     * @return true if the key event was handled
     */
    boolean onKeyPressed(BaseScreen screen, Key key);

    /**
     * Render the icon on-screen.
     *
     * @param mapType the map type
     * @param graphics the graphics context
     * @param x icon X position
     * @param y icon Y position
     * @param w icon width
     * @param h icon height
     * @param outsideVisibleArea true if the icon is currently off the map's visible area
     * @param iconAlpha the alpha factor (0..255) for the icon
     */
    void draw(MapType mapType, GuiGraphics graphics, int x, int y, int w, int h, boolean outsideVisibleArea, int iconAlpha);

    /**
     * A simple implementation of {@code MapIcon} with a static position and adjustable icon, which may be more convenient to
     * use (or extend) than implementing the interface directly.
     */
    class SimpleMapIcon implements MapIcon {
        protected final Vec3 pos;
        protected Icon icon;

        public SimpleMapIcon(Vec3 pos, Icon icon) {
            this.pos = pos;
            this.icon = icon;
        }

        public SimpleMapIcon(Vec3 pos) {
            this(pos, Color4I.empty());
        }

        @Override
        public Vec3 getPos(float partialTick) {
            return pos;
        }

        public Icon getIcon() {
            return icon;
        }

        public void setIcon(Icon icon) {
            this.icon = icon;
        }

        @Override
        public void draw(MapType mapType, GuiGraphics graphics, int x, int y, int w, int h, boolean outsideVisibleArea, int iconAlpha) {
            if (!icon.isEmpty()) {
                icon.draw(graphics, x, y, w, h);
            }
        }

        @Override
        public boolean onMousePressed(BaseScreen screen, MouseButton button) {
            return false;
        }

        @Override
        public boolean onKeyPressed(BaseScreen screen, Key key) {
            return false;
        }
    }
}
