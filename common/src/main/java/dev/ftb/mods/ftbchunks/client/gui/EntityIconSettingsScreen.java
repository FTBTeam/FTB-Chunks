package dev.ftb.mods.ftbchunks.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ftb.mods.ftbchunks.api.FTBChunksTags;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftbchunks.client.mapicon.EntityIcons;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleButton;
import dev.ftb.mods.ftblibrary.ui.TextField;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.ToggleableButton;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.ui.misc.AbstractButtonListScreen;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.ftb.mods.ftblibrary.util.TextComponentUtils.hotkeyTooltip;

public class EntityIconSettingsScreen extends AbstractButtonListScreen {
    private final Map<MobCategory, Boolean> collapsed = new HashMap<>();
    private final Map<MobCategory, List<EntityType<?>>> entities = new HashMap<>();

    private final Button buttonCollapseAll, buttonExpandAll;

    public EntityIconSettingsScreen() {
        showBottomPanel(false);
        showCloseButton(true);

        for (MobCategory value : MobCategory.values()) {
            collapsed.put(value, false);
            entities.put(value, new ArrayList<>());
        }

        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
            if (entityType.getCategory() == MobCategory.MISC && !entityType.is(FTBChunksTags.Entities.MINIMAP_ALLOWED_MISC_MOBS)) {
                continue;
            }
            entities.get(entityType.getCategory()).add(entityType);
        }

        buttonExpandAll = new SimpleButton(topPanel, List.of(Component.translatable("gui.expand_all"), hotkeyTooltip("="), hotkeyTooltip("+")), Icons.UP,
                (widget, button) -> toggleAll(true));
        buttonCollapseAll = new SimpleButton(topPanel, List.of(Component.translatable("gui.collapse_all"), hotkeyTooltip("-")), Icons.DOWN,
                (widget, button) -> toggleAll(false));
    }

    private void toggleAll(boolean collapsed) {
        boolean allOpen = this.collapsed.values().stream().noneMatch(b -> b);
        // Don't try and re-render if everything is already open
        if (allOpen && !collapsed) {
            return;
        }
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
    public boolean keyPressed(Key key) {
        if (super.keyPressed(key)) {
            return true;
        } else if (key.is(InputConstants.KEY_ADD) || key.is(InputConstants.KEY_EQUALS)) {
            toggleAll(false);
        } else if (key.is(InputConstants.KEY_MINUS) || key.is(GLFW.GLFW_KEY_KP_SUBTRACT)) {
            toggleAll(true);
        }
        return false;
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
        entities.forEach((key, value) -> {
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

            if (entities.size() > 1) {
                add(buttonExpandAll);
                add(buttonCollapseAll);
            }
        }

        @Override
        public void alignWidgets() {
            titleLabel.setPosAndSize(4, 0, titleLabel.width, height);
            if (entities.size() > 1) {
                buttonExpandAll.setPos(width - 18, 2);
                buttonCollapseAll.setPos(width - 38, 2);
            }
        }
    }

    private class GroupButton extends Button {
        private final Component titleText;
        private final List<RowPanel> rowPanels;
        private final MobCategory mobCategory;

        public GroupButton(Panel panel, MobCategory mobCategory, boolean startCollapsed, List<EntityType<?>> entityTypes) {
            super(panel);

            this.mobCategory = mobCategory;
            this.titleText = Component.literal("mob_category." + mobCategory.getName());
            setCollapsed(startCollapsed);
            this.rowPanels = new ArrayList<>();
            for (EntityType<?> entityType : entityTypes) {
                rowPanels.add(new RowPanel(panel, entityType));
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
            return collapsed.get(mobCategory);
        }

        public void setCollapsed(boolean collapsed) {
            EntityIconSettingsScreen.this.collapsed.put(mobCategory, collapsed);
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
    }

    private class RowPanel extends Panel {
        private static final Component DELETE = Component.translatable("ftbchunks.gui.delete");
        private static final Component QUICK_DELETE = Component.translatable("ftbchunks.gui.quick_delete");

        private final EntityType<?> entityType;
        private TextField nameField;
        private SimpleButton hideButton;
        private Icon icon;

        public RowPanel(Panel panel, EntityType<?> entityType) {
            super(panel);
            this.entityType = entityType;
            this.icon = EntityIcons.getData(entityType).map(EntityIcons.EntityIconData::icon).orElse(Icons.DICE);
            setHeight(18);
        }

        @Override
        public void addWidgets() {
            Boolean orDefault = FTBChunksClientConfig.ENTITY_ICON.get().getOrDefault(entityType.arch$registryName().toString(), true);
            add(hideButton = new ToggleableButton(this, orDefault, Icons.ACCEPT, Icons.ACCEPT_GRAY, (hideButton, hidden) -> {
                FTBChunksClientConfig.ENTITY_ICON.get().put(entityType.arch$registryName().toString(), hidden);
                FTBChunksClientConfig.saveConfig();
            }));

            add(nameField = new TextField(this) {
                @Override
                public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                    icon.draw(graphics, x - 16, y - 2, 12, 12);
                    super.draw(graphics, theme, x, y, w, h);
                }

                @Override
                public void addMouseOverText(TooltipList list) {
                    list.add(Component.literal(entityType.arch$registryName().toString()));
                }
            }
            .setTrim().addFlags(Theme.SHADOW));

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

                double size = 16;
                hideButton.setForceButtonSize(false);
                hideButton.setPosAndSize(farRight - 8, 1, (int) size, (int) size);

                nameField.setPos(5 + 12, yOff);
                nameField.setText(Component.translatable(entityType.getDescriptionId()));
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

        }

    }

}
