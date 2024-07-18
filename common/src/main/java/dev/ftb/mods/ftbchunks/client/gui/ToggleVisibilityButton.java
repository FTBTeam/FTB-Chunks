package dev.ftb.mods.ftbchunks.client.gui;

import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class ToggleVisibilityButton extends SimpleButton {

    private static final Component TOGGLE_ON = Component.translatable("ftbchunks.gui.toggle_visibility_on");
    private static final Component TOGGLE_OFF = Component.translatable("ftbchunks.gui.toggle_visibility_off");

    private boolean visible;

    private ToggleVisibilityButton(Panel panel, boolean defaultState, Callback c) {
        super(panel, Component.empty(), defaultState ? Icons.ACCEPT : Icons.REMOVE_GRAY, c);
        visible = defaultState;
    }

    public static ToggleVisibilityButton create(Panel panel, boolean defaultState, Consumer<Boolean> newState) {
        return new ToggleVisibilityButton(panel, defaultState, (widget, button) -> {
            ToggleVisibilityButton visibilityButton = (ToggleVisibilityButton) widget;
            visibilityButton.visible = !visibilityButton.visible;
            widget.setIcon(visibilityButton.visible ? Icons.ACCEPT : Icons.REMOVE_GRAY);
            newState.accept(visibilityButton.visible);
        });
    }

    public boolean isVisible() {
        return visible;
    }

    @Override
    public Component getTitle() {
        return visible ? TOGGLE_ON : TOGGLE_OFF;
    }


    @Override
    public void drawIcon(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
        super.drawIcon(graphics, theme, x, y, width, height);
    }
}
