package dev.ftb.mods.ftbchunks.client.gui;

import dev.ftb.mods.ftblibrary.config.ColorConfig;
import dev.ftb.mods.ftblibrary.config.ConfigCallback;
import dev.ftb.mods.ftblibrary.config.ConfigFromString;
import dev.ftb.mods.ftblibrary.config.ui.EditStringConfigOverlay;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class AddWaypointOverlay extends EditStringConfigOverlay<String> {
    private final ColorButton colorButton;

    public AddWaypointOverlay(Panel panel, ConfigFromString<String> config, ColorConfig colorConfig, ConfigCallback callback) {
        super(panel, config, callback, Component.translatable("ftbchunks.gui.add_waypoint"));

        colorButton = new ColorButton(colorConfig.getValue(), (btn, mb) -> {
            ColorSelectorPanel.popupAtMouse(getGui(), colorConfig, accepted -> {
                if (accepted) {
                    btn.setIcon(colorConfig.getValue());
                } else {
                    colorConfig.setValue(((ColorButton) btn).getIcon());
                }
            });
        });
    }

    @Override
    public void addWidgets() {
        super.addWidgets();
        add(colorButton);
    }

    @Override
    public void alignWidgets() {
        super.alignWidgets();
        colorButton.setPosAndSize(width - 11, 1, 10, 10);
    }

    private class ColorButton extends SimpleButton {
        public ColorButton(Icon icon, Callback c) {
            super(AddWaypointOverlay.this, Component.empty(), icon, c);
        }

        Color4I getIcon() {
            return icon instanceof Color4I c ? c : Color4I.empty();
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            icon.draw(graphics, x, y, w, h);
            Color4I shade = getIcon().addBrightness(-0.15f);
            GuiHelper.drawHollowRect(graphics, x, y, w, h, shade, false);
        }
    }
}
