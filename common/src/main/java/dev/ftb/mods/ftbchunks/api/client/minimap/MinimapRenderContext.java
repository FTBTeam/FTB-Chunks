package dev.ftb.mods.ftbchunks.api.client.minimap;

import dev.ftb.mods.ftbchunks.api.client.icon.MapIcon;
import dev.ftb.mods.ftblibrary.util.PanelPositioning;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

/**
 * Context object for all minimap layer renderers
 * @param mapPos position on screen of the top left of the minimap
 * @param size minimap width and height
 * @param scale minimap scale (based on client config settings and possibly current GUI scale)
 * @param zoom minimap zoom level
 * @param rotation minimap rotation, in radians
 * @param rotationLocked true if rotation is locked to north = up
 * @param playerPos the client player's current position
 * @param components info components rendered above or below the minimap
 * @param componentContext info components context
 * @param componentsHeight the total height of all info components
 * @param deltaTracker delta tracker for partial tick calculations
 * @param mapIcons all the icons which should be rendered on the map
 */
public record MinimapRenderContext(
        PanelPositioning.PanelPos mapPos,
        int size,
        float scale, float zoom,
        float rotation, boolean rotationLocked,
        Vec3 playerPos,
        Collection<MinimapInfoComponent> components,
        MinimapComponentContext componentContext,
        int componentsHeight,
        DeltaTracker deltaTracker,
        Collection<MapIcon> mapIcons
) {
}
