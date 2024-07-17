package dev.ftb.mods.ftbchunks.api.client.minimap;

import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
     * Set of Info {@link InfoConfigComponent} that are used to configure options for rendering the waypoint
     * this is exposed in the right click action of Minimap Info GUI
     * @return the set of {@link InfoConfigComponent}.
     */
    default Set<InfoConfigComponent> getConfigComponents() {
        return Collections.emptySet();
    }

    /**
     * Sets the active {@link InfoConfigComponent} option should be overridden if {@link #getConfigComponents()} is not empty
     * @param component The {@link InfoConfigComponent} to set as active config
     */
    default void setActiveConfigComponent(InfoConfigComponent component) {

    }

    /**
     * Gets the active {@link InfoConfigComponent} should be overridden if {@link #getConfigComponents()} is not empty
     * @return The active {@link InfoConfigComponent} option, should only be null if {@link #getConfigComponents()} is empty
     */
    @Nullable
    default InfoConfigComponent getActiveConfigComponent() {
        return null;
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


    //Todo - this good?
    default Component displayName() {
        return Component.translatable("minimapinfo." + id().getNamespace() + "." + id().getPath() + ".title");
    }

    //Todo - this good?
    default Component description() {
        return Component.translatable("minimapinfo." + id().getNamespace() + "." + id().getPath() + ".description");
    }
}
