package dev.ftb.mods.ftbchunks.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbchunks.client.map.MapManager;
import dev.ftb.mods.ftbchunks.client.map.WaypointImpl;
import dev.ftb.mods.ftbchunks.net.TeleportFromMapPacket;
import dev.ftb.mods.ftblibrary.config.ColorConfig;
import dev.ftb.mods.ftblibrary.config.StringConfig;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class WaypointEditorScreen extends BaseScreen {
    public static final Color4I COLOR_BACKGROUND = Color4I.rgba(0xC0333333);
    private static final Icon PEARL_ICON = ImageIcon.getIcon(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/ender_pearl.png"));

    public static final Theme THEME = new Theme() {
        @Override
        public void drawScrollBarBackground(GuiGraphics graphics, int x, int y, int w, int h, WidgetType type) {
            Color4I.BLACK.withAlpha(70).draw(graphics, x, y, w, h);
        }

        @Override
        public void drawScrollBar(GuiGraphics graphics, int x, int y, int w, int h, WidgetType type, boolean vertical) {
            getContentColor(WidgetType.NORMAL).withAlpha(100).withBorder(Color4I.GRAY.withAlpha(100), false).draw(graphics, x, y, w, h);
        }
    };

    private static final Component TITLE = Component.translatable("ftbchunks.gui.waypoints").withStyle(ChatFormatting.BOLD);

    private final Panel waypointPanel;
    private final PanelScrollBar scrollbar;
    private final List<Widget> rows;
    private final Button buttonCollapseAll, buttonExpandAll;
    private int maxWidth;

    public WaypointEditorScreen() {
        waypointPanel = new WaypointPanel(this);
        scrollbar = new PanelScrollBar(this, waypointPanel) {
            @Override
            public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                if (getMaxValue() > parent.height) {
                    super.drawBackground(graphics, theme, x, y, w, h);
                }
            }
        };
        rows = new ArrayList<>();

        buttonCollapseAll = new ExpanderButton(this, false);
        buttonExpandAll = new ExpanderButton(this, true);

        for (DimWayPoints entry : collectWaypoints()) {
            rows.add(new VerticalSpaceWidget(waypointPanel,4));

            boolean thisDimension = Minecraft.getInstance().level.dimension().location().equals(entry.levelKey);
            GroupButton groupButton = new GroupButton(waypointPanel, Component.literal(entry.levelKey.toString()).withStyle(ChatFormatting.YELLOW), !thisDimension);
            rows.add(groupButton);

            for (WaypointImpl wp : entry.waypoints) {
                rows.add(new RowPanel(waypointPanel, groupButton, wp));
            }
        }
    }

    private List<DimWayPoints> collectWaypoints() {
        List<DimWayPoints> res = new ArrayList<>();

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
                .forEach(dim -> res.add(new DimWayPoints(dim.dimension.location(), dim.getWaypointManager().stream()
                        .sorted(Comparator.comparingDouble(wp -> wp.getDistanceSq(player)))
                        .toList()))
                );

        return res;
    }

    @Override
    public boolean onInit() {
        boolean r = setFullscreen();
        maxWidth = Math.min(maxWidth, getGui().width / 2);
        return r;
    }

    @Override
    public void addWidgets() {
        add(waypointPanel);
        add(scrollbar);

        add(buttonCollapseAll);
        add(buttonExpandAll);
    }

    @Override
    public void alignWidgets() {
        waypointPanel.setPosAndSize(0, 20, width, height - 20);
        waypointPanel.alignWidgets();
        scrollbar.setPosAndSize(width - 16, 20, 16, height - 20);
        buttonExpandAll.setPos(width - 18, 2);
        buttonCollapseAll.setPos(width - 38, 2);
    }

    @Override
    public Theme getTheme() {
        return super.getTheme();
    }

    @Override
    public Component getTitle() {
        return TITLE;
    }

    @Override
    public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
        Color4I.BLACK.withAlpha(128).draw(graphics, 0, 0, w, h);
        COLOR_BACKGROUND.draw(graphics, 0, 0, w, 20);
        theme.drawString(graphics, getTitle(), 6, 6, Theme.SHADOW);
    }

    @Override
    public boolean keyPressed(Key key) {
        if (key.esc() && getContextMenu().isPresent()) {
            closeContextMenu();
            return true;
        }
        return super.keyPressed(key);
    }

    private class WaypointPanel extends Panel {
        public WaypointPanel(Panel panel) {
            super(panel);
        }

        @Override
        public void addWidgets() {
            maxWidth = 0;
            for (var row : rows) {
                if (!(row instanceof RowPanel rowPanel) || !rowPanel.groupButton.collapsed && !rowPanel.deleted) {
                    add(row);
                    if (row instanceof RowPanel rp) maxWidth = Math.max(maxWidth, getTheme().getStringWidth(rp.wp.getName()));
                }
            }
            maxWidth = Math.min(maxWidth, getGui().width / 2);
        }

        @Override
        public void alignWidgets() {
            align(WidgetLayout.VERTICAL);
            int sbWidth = scrollbar.getMaxValue() > 0 ? 16 : 0;
            scrollbar.setWidth(sbWidth);
            widgets.forEach(w -> {
                int prevWidth = w.width;
                w.setWidth(width - sbWidth);
                if (w.width != prevWidth && w instanceof Panel p) p.alignWidgets();
            });
        }
    }

    private static class GroupButton extends Button {
        private boolean collapsed;
        private final Component titleText;

        public GroupButton(Panel panel, Component titleText, boolean startCollapsed) {
            super(panel);
            this.titleText = titleText;
            setCollapsed(startCollapsed);
        }

        @Override
        public void onClicked(MouseButton button) {
            setCollapsed(!collapsed);
            getGui().refreshWidgets();
        }

        void setCollapsed(boolean collapsed) {
            this.collapsed = collapsed;
            setTitle(Component.literal(collapsed ? "[>] " : "[v] ").withStyle(collapsed ? ChatFormatting.RED : ChatFormatting.GREEN).append(titleText));
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            COLOR_BACKGROUND.draw(graphics, x, y, w, h);
            theme.drawString(graphics, getTitle(), x + 3, y + 1 + (h - theme.getFontHeight()) / 2);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

            Color4I.GRAY.withAlpha(80).draw(graphics, 0, y, width, 1);
            Color4I.GRAY.withAlpha(80).draw(graphics, 0, y, 1, height);
            if (isMouseOver()) {
                Color4I.WHITE.withAlpha(33).draw(graphics, x, y, w, h);
            }
        }

        @Override
        public void addMouseOverText(TooltipList list) {
        }
    }

    private class RowPanel extends Panel {

        private static final Component DELETE = Component.literal("Delete");
        private static final Component QUICK_DELETE = Component.literal("Quick Delete");

        private final GroupButton groupButton;
        private final WaypointImpl wp;
        private boolean deleted = false;

        public RowPanel(Panel panel, GroupButton groupButton, WaypointImpl wp) {
            super(panel);
            this.groupButton = groupButton;
            this.wp = wp;
            setHeight(18);
        }

        @Override
        public void addWidgets() {
            add(new SimpleButton(this, Component.empty(), wp.isHidden() ? Icons.REMOVE_GRAY : Icons.ACCEPT, (w, mb) -> {
                wp.setHidden(!wp.isHidden());
                w.setIcon(wp.isHidden() ? Icons.REMOVE_GRAY : Icons.ACCEPT);
            }));

            add(new TextField(this).setMaxWidth(maxWidth).setTrim().setText(wp.getName()).setColor(Color4I.rgb(wp.getColor())).addFlags(Theme.SHADOW));

            LocalPlayer player = Minecraft.getInstance().player;
            String distStr = player.level().dimension().equals(wp.getDimension()) ?
                    String.format("%.1fm", Math.sqrt(wp.getDistanceSq(player))) : "";
            add(new TextField(this).setText(distStr).setColor(Color4I.WHITE));
            add(new SimpleButton(this, DELETE, Icons.BIN, (w, mb) -> deleteWaypoint(!isShiftKeyDown())) {

                @Override
                public Component getTitle() {
                    if(isShiftKeyDown()) {
                        return QUICK_DELETE;
                    }else {
                        return DELETE;
                    }
                }
            });
        }

        @Override
        public void alignWidgets() {
            if (widgets.size() == 4) {
                int yOff = (this.height - getTheme().getFontHeight()) / 2 + 1;
                widgets.get(0).setPos(10, 1);
                widgets.get(1).setPos(30, yOff);
                widgets.get(2).setPos(maxWidth + 40, yOff);
                widgets.get(3).setPos(maxWidth + 80, 1);
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
                    list.add(new ContextMenuItem(Component.translatable("ftbchunks.gui.teleport"), PEARL_ICON, btn -> {
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
            if(key.is(GLFW.GLFW_KEY_DELETE)) {
                deleteWaypoint(!isShiftKeyDown());
                return true;
            }else {
                return super.keyPressed(key);
            }
        }

        private void deleteWaypoint(boolean gui) {
            if(gui) {
                getGui().openYesNo(Component.translatable("ftbchunks.gui.delete_waypoint", Component.literal(wp.getName())
                        .withStyle(Style.EMPTY.withColor(wp.getColor()))), Component.empty(), () -> {
                    wp.removeFromManager();
                    deleted = true;
                    getGui().refreshWidgets();
                });
            }else {
                wp.removeFromManager();
                deleted = true;
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

    private class ExpanderButton extends SimpleButton {
        public ExpanderButton(Panel panel, boolean expand) {
            super(panel, Component.translatable(expand ? "gui.expand_all" : "gui.collapse_all"), expand ? Icons.DOWN : Icons.UP, (btn, mb) -> {
                rows.stream().filter(w -> w instanceof GroupButton).map(w -> (GroupButton) w).forEach(g -> g.setCollapsed(!expand));
                scrollbar.setValue(0);
                btn.getGui().refreshWidgets();
            });
        }
    }

    private record DimWayPoints(ResourceLocation levelKey, List<WaypointImpl> waypoints) {
    }
}
