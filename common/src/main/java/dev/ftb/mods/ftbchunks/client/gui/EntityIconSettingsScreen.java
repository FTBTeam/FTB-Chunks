package dev.ftb.mods.ftbchunks.client.gui;

import dev.ftb.mods.ftbchunks.client.mapicon.EntityIconUtils;
import dev.ftb.mods.ftbchunks.config.FTBChunksClientConfig;
import dev.ftb.mods.ftblibrary.client.gui.screens.AbstractGroupedButtonListScreen;
import dev.ftb.mods.ftblibrary.client.gui.theme.Theme;
import dev.ftb.mods.ftblibrary.client.gui.widget.Panel;
import dev.ftb.mods.ftblibrary.client.gui.widget.SimpleButton;
import dev.ftb.mods.ftblibrary.client.gui.widget.TextField;
import dev.ftb.mods.ftblibrary.client.gui.widget.ToggleableButton;
import dev.ftb.mods.ftblibrary.client.icon.IconHelper;
import dev.ftb.mods.ftblibrary.icon.EntityIconLoader;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EntityIconSettingsScreen extends AbstractGroupedButtonListScreen<MobCategory, EntityType<?>> {
    private final boolean showCreationButton;

    public EntityIconSettingsScreen(boolean showCreationButton) {
        super(Component.translatable("ftbchunks.gui.entity_icon_settings"));
        this.showCreationButton = showCreationButton;
    }

    @Override
    protected List<GroupData<MobCategory, EntityType<?>>> buildGroupData() {
        List<GroupData<MobCategory, EntityType<?>>> groups = new ArrayList<>();
        for (MobCategory mobCategory : MobCategory.values()) {
            List<EntityType<?>> entityTypes = new ArrayList<>();
            for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
                if (entityType.getCategory() == mobCategory && EntityIconUtils.canTypeRender(entityType)) {
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
        private final TextField nameField;
        private final SimpleButton hideButton;
        private final SimpleButton createButton;
        private final Icon<?> icon;
        private final ResourceKey<EntityType<?>> resourceKey;

        public RowPanel(Panel panel, EntityType<?> entityType) {
            super(panel, entityType);
            this.icon = EntityIconLoader.getIcon(entityType);
            this.resourceKey = ResourceKey.create(Registries.ENTITY_TYPE, BuiltInRegistries.ENTITY_TYPE.getKey(entityType));
            setHeight(18);

            boolean hiddenState = FTBChunksClientConfig.ENTITY_ICON.get().getOrDefault(resourceKey.identifier().toString(), true);
            hideButton = new ToggleableButton(this, hiddenState, Icons.ACCEPT, Icons.ACCEPT_GRAY, (hideButton, hidden) -> {
                FTBChunksClientConfig.ENTITY_ICON.get().put(resourceKey.identifier().toString(), hidden);
                FTBChunksClientConfig.saveConfig();
            });

            Icon<?> btnIcon = EntityIconLoader.isDynamicTexture(value) ? Icons.BOOK_RED : Icons.BOOK;
            createButton = new SimpleButton(this, Component.empty(), btnIcon, (widget, button) -> new SliceCreationGUI(value).openGui());
            var entityIconSettings = EntityIconLoader.getSettings(value).orElseThrow();

            nameField = new TextField(this) {
                @Override
                public void draw(GuiGraphicsExtractor graphics, Theme theme, int x, int y, int w, int h) {
                    graphics.pose().pushMatrix();
                    graphics.pose().translate(x - 16, y - 2);
                    graphics.pose().scale(0.75F, 0.75F);
                    IconHelper.renderIcon(icon, graphics, 0, 0, entityIconSettings.widthHeight().width(), entityIconSettings.widthHeight().height());
                    graphics.pose().popMatrix();
                    super.draw(graphics, theme, x, y, w, h);
                }

                @Override
                public void addMouseOverText(TooltipList list) {
                    list.add(Component.literal(Objects.requireNonNull(value.builtInRegistryHolder().key().identifier()).toString()));
                }
            }.setTrim().addFlags(Theme.SHADOW);
        }

        @Override
        public void addWidgets() {
            add(hideButton);
            if (showCreationButton) {
                add(createButton);
            }
            add(nameField);
        }

        @Override
        public void alignWidgets() {

        }

        @Override
        public void setWidth(int newWidth) {
            super.setWidth(newWidth);

            if (newWidth <= 0) {
                return;
            }
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
