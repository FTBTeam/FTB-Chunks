package dev.ftb.mods.ftbchunks.client.gui;

import dev.ftb.mods.ftbchunks.client.map.MapManager;
import dev.ftb.mods.ftblibrary.config.ColorConfig;
import dev.ftb.mods.ftblibrary.config.ConfigCallback;
import dev.ftb.mods.ftblibrary.config.ConfigFromString;
import dev.ftb.mods.ftblibrary.config.ui.EditStringConfigOverlay;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.ColorSelectorPanel;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftblibrary.ui.DropDownMenu;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import dev.ftb.mods.ftblibrary.ui.IntTextBox;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleButton;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.TextBox;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TextComponentUtils;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static dev.ftb.mods.ftblibrary.util.TextComponentUtils.hotkeyTooltip;

public class AddWaypointOverlay extends EditStringConfigOverlay<String> {
    private final ColorButton colorButton;
    private final TextBox dimension;
    private final IntTextBox x, y, z;
    private final Button buttonAccept, buttonCancel;
    private final Button dropDownButton;
    private final GlobalPosConfig pos;

    public AddWaypointOverlay(Panel panel, Component title, GlobalPosConfig pos, ConfigFromString<String> config, ColorConfig colorConfig, ConfigCallback callback) {
        super(panel, config, callback, title);
        this.pos = pos;
        colorButton = new ColorButton(colorConfig.getValue(), (btn, mb) -> {
            ColorSelectorPanel.popupAtMouse(getGui(), colorConfig, accepted -> {
                if (accepted) {
                    btn.setIcon(colorConfig.getValue());
                } else {
                    colorConfig.setValue(((ColorButton) btn).getIcon());
                }
            });
        });
        this.dimension = new TextBox(this) {
            @Override
            public boolean allowInput() {
                return false;
            }
        };
        List<ContextMenuItem> contextMenuItems = createDimContextItems(key -> {
            dimension.setText(key.location().toString());
            getGui().closeContextMenu();
        });
        this.dropDownButton = new SimpleButton(this, Component.empty(), Icons.DROPDOWN_OUT, (widget, mouseButton) -> {
            DropDownMenu dropDownMenu = getGui().openDropdownMenu(contextMenuItems);
            dropDownMenu.setPos(dropDownMenu.getPosX() + 3 + (dimension.getX() - getMouseX()), dropDownMenu.getPosY() + 3 + (dimension.getY() + dimension.getHeight() - getMouseY()));
        }) {
            @Override
            public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                theme.drawButton(graphics, x, y, w, h, getWidgetType());
            }
        };
        this.x = new AddWaypointIntBox(this);
        this.y = new AddWaypointIntBox(this);
        this.z = new AddWaypointIntBox(this);

        this.textBox.setLabel(Component.translatable("ftbchunks.gui.label.name"));
        this.dimension.setLabel(Component.translatable("ftbchunks.gui.label.dimension"));
        this.x.setLabel(Component.literal("X"));
        this.y.setLabel(Component.literal("Y"));
        this.z.setLabel(Component.literal("Z"));

        this.dimension.setText(pos.getValue().dimension().location().toString());
        this.x.setAmount(pos.getValue().pos().getX());
        this.y.setAmount(pos.getValue().pos().getY());
        this.z.setAmount(pos.getValue().pos().getZ());

        this.x.setMinMax(Integer.MIN_VALUE, Integer.MAX_VALUE);
        this.y.setMinMax(Integer.MIN_VALUE, Integer.MAX_VALUE);
        this.z.setMinMax(Integer.MIN_VALUE, Integer.MAX_VALUE);


        buttonAccept = new SimpleTextButton(this, Component.translatable("gui.accept"), Icons.ACCEPT) {
            @Override
            public void onClicked(MouseButton button) {
                handleAccepted(button);
            }

            @Override
            public void addMouseOverText(TooltipList list) {
                if (isNameValid()) {
                    list.add(hotkeyTooltip("⇧ + Enter"));
                } else {
                    list.add(Component.translatable("ftbchunks.gui.waypoint.no_name"));
                }
            }

            @Override
            public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                buttonAccept.setIcon(isNameValid() ? Icons.ACCEPT : Icons.ACCEPT_GRAY);
                super.draw(graphics, theme, x, y, w, h);
            }
        };
        SimpleTextButton.accept(this, this::handleAccepted, hotkeyTooltip("⇧ + Enter"));
        buttonCancel = SimpleTextButton.cancel(this, this::handleCancelled, hotkeyTooltip("ESC"));

        setAddAcceptCancelButtons(false);

    }

    private boolean isNameValid() {
        return !textBox.getText().isBlank();
    }

    private void handleAccepted(MouseButton mouseButton) {
        if (!isNameValid()) {
            return;
        }
        playClickSound();
        onAccepted(buttonAccept, mouseButton);
    }

    private void handleCancelled(MouseButton mouseButton) {
        playClickSound();
        onCancelled(buttonCancel, mouseButton);
    }

    private void updatePos() {
        ResourceLocation dimension = ResourceLocation.parse(this.dimension.getText());
        ResourceKey<Level> resourceKey = ResourceKey.create(Registries.DIMENSION, dimension);
        if (x.getText().isBlank() || y.getText().isBlank() || z.getText().isBlank()) {
            return;
        }
        BlockPos blockPos = new BlockPos(x.getIntValue(), y.getIntValue(), z.getIntValue());
        this.pos.setValue(GlobalPos.of(resourceKey, blockPos));
    }

    @Override
    public void addWidgets() {
        super.addWidgets();

        add(colorButton);
        add(dimension);
        add(x);
        add(y);
        add(z);
        add(buttonAccept);
        add(buttonCancel);
        add(dropDownButton);
    }

    @Override
    public void alignWidgets() {
        super.alignWidgets();

        int widgetH = getGui().getTheme().getFontHeight() + 4;
        textBox.setPos(2, widgetH + 6);
        textBox.setWidth(width - widgetH - 4 - 4);
        colorButton.setPosAndSize(width - widgetH - 4, widgetH + 6, widgetH + 1, widgetH + 1);
        int w = (width - 3) / 3;
        int y1 = textBox.posY + textBox.getHeight() + widgetH / 2 + 2;
        dimension.setPosAndSize(2, y1, width - widgetH - 4 - 4, widgetH);
        dropDownButton.setPosAndSize(width - widgetH - 4, y1, widgetH + 1, widgetH);
        int xyzHeight = dimension.posY + dimension.getHeight() + widgetH / 2 + 2;
        x.setPosAndSize(2, xyzHeight, w - 2, widgetH);
        y.setPosAndSize(2 + w, xyzHeight, w - 2, widgetH);
        z.setPosAndSize(2 + w * 2, xyzHeight, w - 2, widgetH);

        int buttonW = w + w / 2;
        int buttonHeight = x.posY + x.getHeight() + 5;
        buttonAccept.setPosAndSize(width - buttonW * 2 - 2, buttonHeight, buttonW, 20);
        buttonCancel.setPosAndSize(width - buttonW, buttonHeight, buttonW - 3, 20);

        height += 28 + 14 + 30;
    }

    @Override
    public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
        super.drawBackground(graphics, theme, x, y, w, h);
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

    public static List<ContextMenuItem> createDimContextItems(Consumer<ResourceKey<Level>> onSelected) {
        List<ContextMenuItem> contextMenuItems = new ArrayList<>();
        MapManager manager = MapManager.getInstance().orElseThrow();
        manager.getDimensions().forEach((key, value) ->
                contextMenuItems.add(new ContextMenuItem(TextComponentUtils.translatedDimension(key), Icon.empty(), (button) -> onSelected.accept(key))));
        return contextMenuItems;
    }

    public class AddWaypointIntBox extends IntTextBox {

        public AddWaypointIntBox(Panel panel) {
            super(panel);
        }

        @Override
        public void onTextChanged() {
            super.onTextChanged();
            updatePos();
        }
    }

    public static class GlobalPosConfig extends ConfigFromString<GlobalPos> {
        @Override
        public boolean parse(@Nullable Consumer<GlobalPos> callback, String string) {
            return false;
        }
    }
}
