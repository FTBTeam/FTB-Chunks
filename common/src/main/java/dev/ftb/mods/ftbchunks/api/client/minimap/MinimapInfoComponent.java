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

    void render(MinimapContext context, GuiGraphics graphics, Font font);

    /**
     * Whether this component should be included in the minimap during the minimaps initial setup
     */
    default boolean enabled() {
        return true;
    }

    int height(MinimapContext context);

    /**
     * Checked on each render frame to determine if the height for the component should be allocated
     */
    default boolean shouldRender(MinimapContext context) {
        return true;
    }

    default ListPriority priority() {
        return ListPriority.defaultOrder();
    }

    default int computeLineHeight(Minecraft minecraft, int lines) {
        final float fontScale = FTBChunksClientConfig.MINIMAP_FONT_SCALE.get().floatValue();
        return (int) ((minecraft.font.lineHeight + 2) * lines * fontScale);
    }

    default void drawCenteredText(Font font, GuiGraphics graphics, Component text, int y) {
        int textWidth = font.width(text.getVisualOrderText());
        graphics.drawString(font, text, -textWidth / 2, y, 0xFFFFFFFF, true);
    }
}
