package dev.ftb.mods.ftbchunks.api.client.minimap;

import dev.ftb.mods.ftbchunks.config.FTBChunksClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Set;

/// An entry point for developers to create custom minimap info components.
///
/// Instances of this can be registered via [dev.ftb.mods.ftbchunks.api.client.FTBChunksClientAPI#registerMinimapComponent(dev.ftb.mods.ftbchunks.api.client.minimap.MinimapInfoComponent)]
public interface MinimapInfoComponent {
    /// The ID of this component.
    Identifier id();

    /// Render your component here, the [com.mojang.blaze3d.vertex.PoseStack] will already be scaled and
    /// translated to the correct position (centered by default). We do not provide an X and Y position as
    /// 0, 0 is the center of the correct location. Use 0, 0 as the center of the component and [#height(MinimapComponentContext)]
    /// to allocate the correct height for the component.
    ///
    /// @param context The minimap context
    /// @param graphics The graphics object see [GuiGraphicsExtractor]
    /// @param font The font object
    void render(MinimapComponentContext context, GuiGraphicsExtractor graphics, Font font);

    /// Set of Info [TranslatedOption] that are used to configure options for rendering the waypoint
    /// this is exposed in the right click action of Minimap Info GUI
    /// @return the set of [TranslatedOption].
    default Set<TranslatedOption> getConfigComponents() {
        return Set.of();
    }

    /// The height of the component is used to allocate the correct space for the component. Failure to return the correct
    /// height will result in the component overlapping with other components.
    ///
    /// @param context The minimap context
    /// @return The height of the component
    default int height(MinimapComponentContext context) {
        return computeLineHeight(context.minecraft(), 1);
    }

    /// Checked on each render frame to determine if the height for the component should be allocated
    default boolean shouldRender(MinimapComponentContext context) {
        return true;
    }

    /// Helper method to compute the height of a text component whilst taking into account the font scale
    default int computeLineHeight(Minecraft minecraft, int lines) {
        final float fontScale = FTBChunksClientConfig.MINIMAP_FONT_SCALE.get().floatValue();
        return (int) ((minecraft.font.lineHeight + 1) * lines * fontScale);
    }

    /// Helper method to draw centered text without the faff of calculating the width of the text
    default void drawCenteredText(Font font, GuiGraphicsExtractor graphics, Component text, int y) {
        int textWidth = font.width(text.getVisualOrderText());
        graphics.text(font, text, -textWidth / 2, y, 0xFFFFFFFF, true);
    }

    /// @return display name render in the Minimap Info Settings GUI
    default Component displayName() {
        return Component.translatable("minimap.info." + id().getNamespace() + "." + id().getPath() + ".title");
    }

    /// @return hover texted displayed render in the Minimap Info Settings GUI
    default Component description() {
        return Component.translatable("minimap.info." + id().getNamespace() + "." + id().getPath() + ".description");
    }
}
