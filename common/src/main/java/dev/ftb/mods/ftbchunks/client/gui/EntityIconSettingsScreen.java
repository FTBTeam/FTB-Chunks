package dev.ftb.mods.ftbchunks.client.gui;

import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftbchunks.client.mapicon.EntityIcons;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleButton;
import dev.ftb.mods.ftblibrary.ui.TextField;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.ToggleableButton;
import dev.ftb.mods.ftblibrary.ui.misc.AbstractGroupedButtonListScreen;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.ArrayList;
import java.util.List;

public class EntityIconSettingsScreen extends AbstractGroupedButtonListScreen<MobCategory, EntityType<?>> {
    private final boolean showCreationButton;

    public EntityIconSettingsScreen(boolean showCreationButton) {
        super(Component.translatable("ftbchunks.gui.entity_icon_settings"));
        this.showCreationButton = showCreationButton;
    }

    @Override
    protected List<GroupData<MobCategory, EntityType<?>>> getGroups() {
        List<GroupData<MobCategory, EntityType<?>>> groups = new ArrayList<>();
        for (MobCategory mobCategory : MobCategory.values()) {
            List<EntityType<?>> entityTypes = new ArrayList<>();
            for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
                if (entityType.getCategory() == mobCategory && EntityIcons.canTypeRenderer(entityType)) {
                    entityTypes.add(entityType);
                }
            }
            groups.add(new GroupData<>(mobCategory, false, Component.translatable("mob_category." + mobCategory.getName()), entityTypes));
        }
        return groups;
    }

    @Override
    protected AbstractGroupedButtonListScreen<MobCategory, EntityType<?>>.RowPanel createRowPanel(Panel panel, EntityType<?> value) {
        return new RowPanel(panel, value);
    }

    private class RowPanel extends AbstractGroupedButtonListScreen<MobCategory, EntityType<?>>.RowPanel {

        private TextField nameField;
        private SimpleButton hideButton;
        private SimpleButton createButton;
        private final Icon icon;
        private final ResourceKey<EntityType<?>> resourceKey;

        public RowPanel(Panel panel, EntityType<?> entityType) {
            super(panel, entityType);
            this.icon = EntityIcons.getIcon(entityType);
            this.resourceKey = ResourceKey.create(Registries.ENTITY_TYPE, BuiltInRegistries.ENTITY_TYPE.getKey(entityType));
            setHeight(18);
        }

        @Override
        public void addWidgets() {
            boolean hiddenState = FTBChunksClientConfig.ENTITY_ICON.get().getOrDefault(resourceKey, true);
            add(hideButton = new ToggleableButton(this, hiddenState, Icons.ACCEPT, Icons.ACCEPT_GRAY, (hideButton, hidden) -> {
                FTBChunksClientConfig.ENTITY_ICON.get().put(resourceKey, hidden);
                FTBChunksClientConfig.saveConfig();
            }));

            if (showCreationButton) {
                boolean isDynamicTexture = EntityIcons.USE_NEW_TEXT.getOrDefault(value, false);
                Icon icon = isDynamicTexture ? Icons.BOOK_RED : Icons.BOOK;
                add(createButton = new SimpleButton(this, Component.translatable("ftbchunks.gui.open_creation_gui"), icon, (widget, button) -> new SliceCreationGUI(value).openGui()));
            }

            EntityIcons.EntityIconSettings entityIconSettings = EntityIcons.getSettings(value).orElseThrow();
            add(nameField = new TextField(this) {
                @Override
                public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                    graphics.pose().pushPose();
                    graphics.pose().translate(x - 16, y - 2, 0);
                    graphics.pose().scale(0.75F, 0.75F, 0);
                    icon.draw(graphics, 0, 0, entityIconSettings.widthHeight().width(), entityIconSettings.widthHeight().height());
                    graphics.pose().popPose();
                    super.draw(graphics, theme, x, y, w, h);
                }

                @Override
                public void addMouseOverText(TooltipList list) {
                    list.add(Component.literal(value.arch$registryName().toString()));
                }
            }.setTrim().addFlags(Theme.SHADOW));

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

                if (showCreationButton) {
                    createButton.setPosAndSize(farRight - 8 - (int) size, 1, (int) size, (int) size);
                }

                nameField.setPos(5 + 12, yOff);
                nameField.setText(Component.translatable(value.getDescriptionId()));
                nameField.setHeight(getTheme().getFontHeight() + 2);
            }
        }
    }
}
