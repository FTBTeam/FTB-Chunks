package dev.ftb.mods.ftbchunks.client.gui;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbchunks.client.map.MapManager;
import dev.ftb.mods.ftbchunks.client.map.WaypointImpl;
import dev.ftb.mods.ftbchunks.net.TeleportFromMapPacket;
import dev.ftb.mods.ftblibrary.client.config.editable.EditableColor;
import dev.ftb.mods.ftblibrary.client.config.editable.EditableString;
import dev.ftb.mods.ftblibrary.client.gui.input.Key;
import dev.ftb.mods.ftblibrary.client.gui.input.MouseButton;
import dev.ftb.mods.ftblibrary.client.gui.screens.AbstractGroupedButtonListScreen;
import dev.ftb.mods.ftblibrary.client.gui.theme.Theme;
import dev.ftb.mods.ftblibrary.client.gui.widget.*;
import dev.ftb.mods.ftblibrary.client.icon.IconHelper;
import dev.ftb.mods.ftblibrary.client.util.ClientTextComponentUtils;
import dev.ftb.mods.ftblibrary.client.util.ClientUtils;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.util.TextComponentUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class WaypointEditorScreen extends AbstractGroupedButtonListScreen<ResourceKey<Level>, WaypointImpl> {
    private int widestWaypoint = 0;

    public WaypointEditorScreen() {
        super(Component.translatable("ftbchunks.gui.waypoints"));
    }

    @Override
    protected List<GroupData<ResourceKey<Level>, WaypointImpl>> buildGroupData() {
        List<GroupData<ResourceKey<Level>, WaypointImpl>> res = new ArrayList<>();

        Player player = ClientUtils.getClientPlayer();
        MapManager manager = MapManager.getInstance().orElseThrow();
        manager.getDimensions().values().stream()
                .filter(dim -> !dim.getWaypointManager().isEmpty())
                .sorted()
                .forEach(dim -> {
                    var waypoints = dim.getWaypointManager().stream()
                            .sorted(Comparator.comparingDouble(wp -> wp.getDistanceSq(player)))
                            .toList();
                    Component title = TextComponentUtils.translatedDimension(dim.dimension).copy()
                            .withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(Component.literal(dim.dimension.identifier().toString()))));
                    res.add(new GroupData<>(dim.dimension, false, title, waypoints));
                });

        return res;
    }

    @Override
    protected AbstractGroupedButtonListScreen<ResourceKey<Level>, WaypointImpl>.RowPanel createRowPanel(Panel panel, WaypointImpl value) {
        return new RowPanel(panel, value);
    }

    @Override
    public boolean onInit() {
        computeWaypointTextWidth();

        setWidth(Mth.clamp(widestWaypoint + 80, 220, getWindow().getGuiScaledWidth() * 4 / 5));
        setHeight(getWindow().getGuiScaledHeight() * 4 / 5);

        return true;
    }

    @Override
    public void alignWidgets() {
        super.alignWidgets();

        mainPanel.getWidgets().forEach(widget -> {
            if (widget instanceof RowPanel row) {
                row.alignWidgets();
            }
        });
    }

    private void computeWaypointTextWidth() {
        widestWaypoint = 0;

        getGroupData().stream()
                .flatMap(grp -> grp.values().stream())
                .forEach(wp -> widestWaypoint = Math.max(widestWaypoint, getTheme().getStringWidth(wp.getName())));
    }

    private class RowPanel extends AbstractGroupedButtonListScreen<ResourceKey<Level>, WaypointImpl>.RowPanel {
        private static final Component DELETE = Component.translatable("ftbchunks.gui.delete");
        private static final Component QUICK_DELETE = Component.translatable("ftbchunks.gui.quick_delete");

        private final TextField nameField;
        private final TextField distField;
        private final SimpleButton hideButton;
        private final SimpleButton deleteButton;

        public RowPanel(Panel parent, WaypointImpl waypoint) {
            super(parent, waypoint);

            hideButton = new ToggleableButton(this, !value.isHidden(), (widget, newState) -> value.setHidden(!newState));
            nameField = new TextField(this).setTrim().setColor(Color4I.rgb(value.getColor())).addFlags(Theme.SHADOW);
            String distStr = ClientUtils.getClientLevel().dimension().equals(value.getDimension()) ?
                    String.format("%.1fm", Math.sqrt(value.getDistanceSq(ClientUtils.getClientPlayer()))) : "";
            distField = new TextField(this).setText(distStr).setColor(Color4I.WHITE);
            deleteButton = new SimpleButton(this, DELETE, Icons.BIN, (w, mb) -> deleteWaypoint(!isShiftKeyDown())) {
                @Override
                public Component getTitle() {
                    return isShiftKeyDown() ? QUICK_DELETE : DELETE;
                }
            };
        }

        @Override
        public void addWidgets() {
            add(hideButton);
            add(nameField);
            add(distField);
            add(deleteButton);
        }

        @Override
        public void alignWidgets() {
            int farRight = width - 8;

            int yPos = (this.height - getTheme().getFontHeight()) / 2 + 1;

            hideButton.setPosAndSize(farRight - 8 - 16, 3, 12, 12);
            deleteButton.setPosAndSize(farRight - 8, 3, 12, 12);

            distField.setPos(hideButton.getPosX() - 5 - distField.width, yPos);

            nameField.setPos(5, yPos);
            nameField.setText(ClientTextComponentUtils.ellipsize(getTheme().getFont(), value.getDisplayName(), distField.getPosX() - 5).getString());
            nameField.setHeight(getTheme().getFontHeight() + 2);
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            super.draw(graphics, theme, x, y, w, h);

            var mouseOver = getMouseY() >= 20 && isMouseOver();

            if (mouseOver) {
                IconHelper.renderIcon(Color4I.WHITE.withAlpha(33), graphics, x, y, w, h);
            }
        }
        @Override
        public boolean mouseDoubleClicked(MouseButton button) {
            if (isMouseOver()) {
                openWaypointEditPanel();
                return true;
            }
            return false;
        }

        private void openWaypointEditPanel() {
            EditableString configName = new EditableString();
            configName.setValue(value.getName());
            new WaypointAddScreen(configName, GlobalPos.of(value.getDimension(), value.getPos()), Color4I.rgb(value.getColor()), true).openGui();
        }

        @Override
        public boolean mousePressed(MouseButton button) {
            if (isMouseOver() && button.isRight()) {
                List<ContextMenuItem> list = new ArrayList<>();
                list.add(makeTitleMenuItem());
                list.add(ContextMenuItem.SEPARATOR);

                list.add(new ContextMenuItem(Component.translatable("ftbchunks.gui.edit"), Icons.SETTINGS, b -> openWaypointEditPanel()));

                WaypointShareMenu.build(ClientUtils.getClientPlayer(), value).ifPresent(list::add);

                list.add(new ContextMenuItem(Component.translatable("gui.rename"), ItemIcon.ofItem(Items.WRITABLE_BOOK), btn -> {
                    EditableString config = new EditableString();
                    config.setDefaultValue("");
                    config.setValue(value.getName());
                    config.onClicked(btn, MouseButton.LEFT, accepted -> {
                        if (accepted) {
                            value.setName(config.getValue());
                        }
                        computeWaypointTextWidth();
                        openGui();
                    });
                }));
                if (value.getType().canChangeColor()) {
                    list.add(new ContextMenuItem(Component.translatable("ftbchunks.gui.change_color"), Icons.COLOR_RGB, btn -> {
                        EditableColor col = new EditableColor();
                        col.setValue(Color4I.rgb(value.getColor()));
                        ColorSelectorPanel.popupAtMouse(btn.getGui(), col, accepted -> {
                            if (accepted) {
                                value.setColor(col.getValue().rgba());
                                value.refreshIcon();
                                if (widgets.get(1) instanceof TextField tf) {
                                    tf.setColor(Color4I.rgb(value.getColor()));
                                }
                            }
                        });
                    }));
                }
                if (ClientUtils.getClientPlayer().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {  // permissions are checked again on server!
                    list.add(new ContextMenuItem(Component.translatable("ftbchunks.gui.teleport"), ItemIcon.ofItem(Items.ENDER_PEARL), btn -> {
                        NetworkManager.sendToServer(new TeleportFromMapPacket(value.getPos().above(), false, value.getDimension()));
                        closeGui(false);
                    }));
                }
                list.add(new ContextMenuItem(Component.translatable("gui.remove"), Icons.REMOVE, btn -> deleteWaypoint(true)));

                getGui().openContextMenu(list);
                return true;
            }
            return super.mousePressed(button);
        }

        @Override
        public boolean keyPressed(Key key) {
            if (key.is(GLFW.GLFW_KEY_DELETE)) {
                deleteWaypoint(!isShiftKeyDown());
                return true;
            } else {
                return super.keyPressed(key);
            }
        }

        private void deleteWaypoint(boolean confirm) {
            if (confirm) {
                getGui().openYesNo(Component.translatable("ftbchunks.gui.delete_waypoint", value.getDisplayName().copy()
                        .withStyle(Style.EMPTY.withColor(value.getColor()))), Component.empty(), () -> {
                    value.removeFromManager();
                    WaypointEditorScreen.this.rebuildGroupData();
                    getGui().refreshWidgets();
                });
            } else {
                value.removeFromManager();
                WaypointEditorScreen.this.rebuildGroupData();
                getGui().refreshWidgets();
            }
        }

        private ContextMenuItem makeTitleMenuItem() {
            return new ContextMenuItem(value.getDisplayName(), Icon.empty(), null) {
                @Override
                public Icon<?> getIcon() {
                    return value.getType().getIcon().withTint(Color4I.rgb(value.getColor()));
                }
            };
        }
    }
}
