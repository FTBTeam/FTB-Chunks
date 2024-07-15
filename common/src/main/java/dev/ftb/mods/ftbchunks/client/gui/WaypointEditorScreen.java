package dev.ftb.mods.ftbchunks.client.gui;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbchunks.client.map.MapManager;
import dev.ftb.mods.ftbchunks.client.map.WaypointImpl;
import dev.ftb.mods.ftbchunks.net.TeleportFromMapPacket;
import dev.ftb.mods.ftblibrary.config.ColorConfig;
import dev.ftb.mods.ftblibrary.config.StringConfig;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.ui.misc.AbstractButtonListScreen;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;

import java.util.*;

import static dev.ftb.mods.ftblibrary.util.TextComponentUtils.hotkeyTooltip;
import static net.minecraft.network.chat.CommonComponents.ELLIPSIS;

public class WaypointEditorScreen extends AbstractButtonListScreen {
    private final Map<ResourceKey<Level>, Boolean> collapsed = new HashMap<>();
    private final Map<ResourceKey<Level>, List<WaypointImpl>> waypoints = new HashMap<>();
    private final Button buttonCollapseAll, buttonExpandAll;

    public WaypointEditorScreen() {
        showBottomPanel(false);
        showCloseButton(true);

        for (Map.Entry<ResourceKey<Level>, List<WaypointImpl>> resourceKeyListEntry : collectWaypoints().entrySet()) {
            collapsed.put(resourceKeyListEntry.getKey(), false);
            waypoints.put(resourceKeyListEntry.getKey(), new ArrayList<>(resourceKeyListEntry.getValue()));
        }

        buttonExpandAll = new SimpleButton(topPanel, List.of(Component.translatable("gui.expand_all"), hotkeyTooltip("="), hotkeyTooltip("+")), Icons.UP,
                (widget, button) -> toggleAll(false));
        buttonCollapseAll = new SimpleButton(topPanel, List.of(Component.translatable("gui.collapse_all"), hotkeyTooltip("-")), Icons.DOWN,
                (widget, button) -> toggleAll(true));
    }

    private static FormattedText ellipsize(Font font, FormattedText text, int maxWidth) {
        final int strWidth = font.width(text);
        final int ellipsisWidth = font.width(ELLIPSIS);
        if (strWidth > maxWidth) {
            return ellipsisWidth >= maxWidth ?
                    font.substrByWidth(text, maxWidth) :
                    FormattedText.composite(font.substrByWidth(text, maxWidth - ellipsisWidth), ELLIPSIS);
        }
        return text;
    }

    private void toggleAll(boolean collapsed) {
        this.collapsed.keySet().forEach(levelResourceKey -> this.collapsed.put(levelResourceKey, collapsed));
        scrollBar.setValue(0);
        getGui().refreshWidgets();
    }

    @Override
    protected void doCancel() {
    }

    @Override
    protected void doAccept() {
    }

    @Override
    public boolean onInit() {
        setWidth(220);
        setHeight(getScreen().getGuiScaledHeight() * 4 / 5);
        return true;
    }

    @Override
    protected int getTopPanelHeight() {
        return 22;
    }

    @Override
    protected Panel createTopPanel() {
        return new CustomTopPanel();
    }

    @Override
    public void addButtons(Panel panel) {
        waypoints.forEach((key, value) -> {
            boolean startCollapsed = collapsed.get(key);
            GroupButton groupButton = new GroupButton(panel, key, startCollapsed, value);
            panel.add(groupButton);
            if (!startCollapsed) {
                panel.addAll(groupButton.collectPanels());
            }
        });
    }

    protected class CustomTopPanel extends TopPanel {
        private final TextField titleLabel = new TextField(this);

        @Override
        public void addWidgets() {
            titleLabel.setText(Component.translatable("ftbchunks.gui.waypoints"));
            titleLabel.addFlags(Theme.CENTERED_V);
            add(titleLabel);

            if (waypoints.size() > 1) {
                add(buttonExpandAll);
                add(buttonCollapseAll);
            }
        }

        @Override
        public void alignWidgets() {
            titleLabel.setPosAndSize(4, 0, titleLabel.width, height);
            if (waypoints.size() > 1) {
                buttonExpandAll.setPos(width - 18, 2);
                buttonCollapseAll.setPos(width - 38, 2);
            }
        }
    }

    private class GroupButton extends Button {
        private final Component titleText;
        private final List<RowPanel> rowPanels;
        private final ResourceKey<Level> dim;

        public GroupButton(Panel panel, ResourceKey<Level> dim, boolean startCollapsed, List<WaypointImpl> waypoints) {
            super(panel);

            this.dim = dim;
            //Todo move the to library or define / get some standard for dim names
            //Might need to show more with dim types / level ids
            this.titleText = Component.translatableWithFallback(dim.location().toLanguageKey("dimension"), dim.location().getPath())
                    .withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(dim.location().toString()))));
            setCollapsed(startCollapsed);
            this.rowPanels = new ArrayList<>();
            for (WaypointImpl waypoint : waypoints) {
                rowPanels.add(new RowPanel(panel, waypoint));
            }
        }

        public List<RowPanel> collectPanels() {
            return isCollapsed() ? List.of() : List.copyOf(rowPanels);
        }

        @Override
        public void onClicked(MouseButton button) {
            setCollapsed(!isCollapsed());
            parent.refreshWidgets();
            refreshWidgets();
            playClickSound();
        }

        public boolean isCollapsed() {
            return collapsed.get(dim);
        }

        public void setCollapsed(boolean collapsed) {
            WaypointEditorScreen.this.collapsed.put(dim, collapsed);
            boolean isCollapsed = isCollapsed();
            setTitle(Component.literal(isCollapsed ? "▶ " : "▼ ").withStyle(isCollapsed ? ChatFormatting.RED : ChatFormatting.GREEN).append(titleText));
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            theme.drawWidget(graphics, x, y, w, h, getWidgetType());
            theme.drawString(graphics, getTitle(), x + 3, y + 3);
            if (isMouseOver()) {
                Color4I.WHITE.withAlpha(33).draw(graphics, x, y, w, h);
            }
        }

        @Override
        public void addMouseOverText(TooltipList list) {
        }
    }

    private class RowPanel extends Panel {
        private static final Component DELETE = Component.translatable("ftbchunks.gui.delete");
        private static final Component QUICK_DELETE = Component.translatable("ftbchunks.gui.quick_delete");

        private final WaypointImpl wp;
        private TextField nameField;
        private TextField distField;
        private SimpleButton hideButton;
        private SimpleButton deleteButton;

        public RowPanel(Panel panel, WaypointImpl wp) {
            super(panel);

            this.wp = wp;
            setHeight(18);
        }

        @Override
        public void addWidgets() {
            add(hideButton = new SimpleButton(this, Component.empty(), wp.isHidden() ? Icons.REMOVE_GRAY : Icons.ACCEPT, (w, mb) -> {
                wp.setHidden(!wp.isHidden());
                w.setIcon(wp.isHidden() ? Icons.REMOVE_GRAY : Icons.ACCEPT);
            }));

            add(nameField = new TextField(this).setTrim().setColor(Color4I.rgb(wp.getColor())).addFlags(Theme.SHADOW));

            LocalPlayer player = Minecraft.getInstance().player;
            String distStr = player.level().dimension().equals(wp.getDimension()) ?
                    String.format("%.1fm", Math.sqrt(wp.getDistanceSq(player))) : "";
            add(distField = new TextField(this).setText(distStr).setColor(Color4I.WHITE));
            add(deleteButton = new SimpleButton(this, DELETE, Icons.BIN, (w, mb) -> deleteWaypoint(!isShiftKeyDown())) {
                @Override
                public Component getTitle() {
                    return isShiftKeyDown() ? QUICK_DELETE : DELETE;
                }
            });
        }

        @Override
        public void alignWidgets() {
        }

        @Override
        public void setWidth(int newWidth) {
            super.setWidth(newWidth);

            if (newWidth > 0) {
                int farRight = newWidth - 8;

                int yOff = (this.height - getTheme().getFontHeight()) / 2 + 1;

                hideButton.setPos(farRight - 8 - 16, 1);
                deleteButton.setPos(farRight - 8, 1);

                distField.setPos(hideButton.getPosX() - 5 - distField.width, yOff);

                nameField.setPos(5, yOff);
                nameField.setText(ellipsize(getTheme().getFont(), Component.literal(wp.getName()),distField.getPosX() - 5).getString());
                nameField.setHeight(getTheme().getFontHeight() + 2);
            }
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            super.draw(graphics, theme, x, y, w, h);

            var mouseOver = getMouseY() >= 20 && isMouseOver();

            if (mouseOver) {
                Color4I.WHITE.withAlpha(33).draw(graphics, x, y, w, h);
            }
        }

        @Override
        public boolean mousePressed(MouseButton button) {
            if (isMouseOver() && button.isRight()) {
                List<ContextMenuItem> list = new ArrayList<>();
                list.add(makeTitleMenuItem());
                list.add(ContextMenuItem.SEPARATOR);

                WaypointShareMenu.makeShareMenu(Minecraft.getInstance().player, wp).ifPresent(list::add);

                list.add(new ContextMenuItem(Component.translatable("gui.rename"), Icons.CHAT, btn -> {
                    StringConfig config = new StringConfig();
                    config.setDefaultValue("");
                    config.setValue(wp.getName());
                    config.onClicked(btn, MouseButton.LEFT, accepted -> {
                        if (accepted) {
                            wp.setName(config.getValue());
                        }
                        openGui();
                    });
                }));
                if (wp.getType().canChangeColor()) {
                    list.add(new ContextMenuItem(Component.translatable("ftbchunks.gui.change_color"), Icons.COLOR_RGB, btn -> {
                        ColorConfig col = new ColorConfig();
                        col.setValue(Color4I.rgb(wp.getColor()));
                        ColorSelectorPanel.popupAtMouse(btn.getGui(), col, accepted -> {
                            if (accepted) {
                                wp.setColor(col.getValue().rgba());
                                wp.refreshIcon();
                                if (widgets.get(1) instanceof TextField tf) {
                                    tf.setColor(Color4I.rgb(wp.getColor()));
                                }
                            }
                        });
                    }));
                }
                if (Minecraft.getInstance().player.hasPermissions(2)) {  // permissions are checked again on server!
                    list.add(new ContextMenuItem(Component.translatable("ftbchunks.gui.teleport"), ItemIcon.getItemIcon(Items.ENDER_PEARL), btn -> {
                        NetworkManager.sendToServer(new TeleportFromMapPacket(wp.getPos().above(), false, wp.getDimension()));
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

        private void deleteWaypoint(boolean gui) {
            if (gui) {
                getGui().openYesNo(Component.translatable("ftbchunks.gui.delete_waypoint", Component.literal(wp.getName())
                        .withStyle(Style.EMPTY.withColor(wp.getColor()))), Component.empty(), () -> {
                    wp.removeFromManager();
                    WaypointEditorScreen.this.waypoints.get(wp.getDimension()).remove(wp);
                    getGui().refreshWidgets();
                });
            } else {
                wp.removeFromManager();
                WaypointEditorScreen.this.waypoints.get(wp.getDimension()).remove(wp);
                getGui().refreshWidgets();
            }
        }

        private ContextMenuItem makeTitleMenuItem() {
            return new ContextMenuItem(Component.literal(wp.getName()), Icon.empty(), null) {
                @Override
                public Icon getIcon() {
                    return wp.getType().getIcon().withTint(Color4I.rgb(wp.getColor()));
                }
            };
        }
    }

    private static Map<ResourceKey<Level>, List<WaypointImpl>> collectWaypoints() {
        Map<ResourceKey<Level>, List<WaypointImpl>> res = new HashMap<>();

        Player player = Objects.requireNonNull(Minecraft.getInstance().player);
        MapManager manager = MapManager.getInstance().orElseThrow();
        manager.getDimensions().values().stream()
                .filter(dim -> !dim.getWaypointManager().isEmpty())
                .sorted((dim1, dim2) -> {
                    // put vanilla dimensions first
                    ResourceLocation dim1id = dim1.dimension.location();
                    ResourceLocation dim2id = dim2.dimension.location();
                    if (dim1id.getNamespace().equals("minecraft") && !dim2id.getNamespace().equals("minecraft")) {
                        return -1;
                    }
                    int i = dim1id.getNamespace().compareTo(dim2id.getNamespace());
                    return i == 0 ? dim1id.getPath().compareTo(dim2id.getPath()) : i;
                })
                .forEach(dim -> res.put(dim.dimension, dim.getWaypointManager().stream()
                        .sorted(Comparator.comparingDouble(wp -> wp.getDistanceSq(player)))
                        .toList())
                );

        return res;
    }
}
