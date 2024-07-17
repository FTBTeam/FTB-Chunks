package dev.ftb.mods.ftbchunks.api.client.minimap;

import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * An entry point for developers to create custom minimap info components.
 */
public interface MinimapInfoComponent {
    /**
     * The ID of this component.
     */
    ResourceLocation id();

    /**
     * Render your component here, the {@link com.mojang.blaze3d.vertex.PoseStack} will already be scaled and
     * translated to the correct position (centered by default). We do not provide an X and Y position as
     * 0, 0 is the center of the correct location. Use 0, 0 as the center of the component and {@link #height(MinimapContext)}
     * to allocate the correct height for the component.
     *
     * @param context The minimap context
     * @param graphics The graphics object see {@link GuiGraphics}
     * @param font The font object
     */
    void render(MinimapContext context, GuiGraphics graphics, Font font);

    /**
     * Whether this component should be included in the minimap during the minimaps initial setup
     */
    default boolean enabled() {
        return true;
    }

    /**
     * The height of the component is used to allocate the correct space for the component. Failure to return the correct
     * height will result in the component overlapping with other components.
     *
     * @param context The minimap context
     * @return The height of the component
     */
    default int height(MinimapContext context) {
        return computeLineHeight(context.minecraft(), 1) + 1;
    }

    /**
     * Checked on each render frame to determine if the height for the component should be allocated
     */
    default boolean shouldRender(MinimapContext context) {
        return true;
    }

    /**
     * Sort order for the component see {@link ListPriority}
     */
    default ListPriority priority() {
        return ListPriority.defaultOrder();
    }

    /**
     * Helper method to compute the height of a text component whilst taking into account the font scale
     */
    default int computeLineHeight(Minecraft minecraft, int lines) {
        final float fontScale = FTBChunksClientConfig.MINIMAP_FONT_SCALE.get().floatValue();
        return (int) ((minecraft.font.lineHeight + 2) * lines * fontScale);
    }

    /**
     * Helper method to draw centered text without the faff of calculating the width of the text
     */
    default void drawCenteredText(Font font, GuiGraphics graphics, Component text, int y) {
        int textWidth = font.width(text.getVisualOrderText());
        graphics.drawString(font, text, -textWidth / 2, y, 0xFFFFFFFF, true);
    }
}
