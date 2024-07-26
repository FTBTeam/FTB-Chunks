package dev.ftb.mods.ftbchunks.client.gui;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapInfoComponent;
import dev.ftb.mods.ftbchunks.api.client.minimap.TranslatedOption;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.misc.AbstractThreePanelScreen;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftblibrary.util.client.ClientTextComponentUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class MinimapInfoSortScreen extends AbstractThreePanelScreen<MinimapInfoSortScreen.MinimapInfoSortEntry> {

    private List<ResourceLocation> infoSortList = new LinkedList<>();

    public MinimapInfoSortScreen() {
        super();
        setHeight(200);
        setWidth(200);
        FTBChunksClientConfig.MINIMAP_INFO_ORDER.get().forEach(s -> infoSortList.add(new ResourceLocation(s)));
        showBottomPanel(false);
    }

    @Override
    protected void doCancel() {

    }

    @Override
    protected void doAccept() {

    }

    @Override
    protected int getTopPanelHeight() {
        return 22;
    }

    @Override
    protected MinimapInfoSortEntry createMainPanel() {
        return new MinimapInfoSortEntry(this);
    }

    @Override
    protected Panel createTopPanel() {
        return new CustomTopPanel();
    }

    protected class CustomTopPanel extends TopPanel {
        private final TextField titleLabel = new TextField(this);

        @Override
        public void addWidgets() {
            titleLabel.setText(Component.translatable("ftbchunks.gui.sort_minimap_info"));
            titleLabel.addFlags(Theme.CENTERED_V);
            add(titleLabel);
        }

        @Override
        public void alignWidgets() {
            titleLabel.setPosAndSize(4, 0, titleLabel.width, height);
        }
    }

    public class MinimapInfoSortEntry extends Panel {

        private final Map<ResourceLocation, InfoEntry> entryMap = new HashMap<>();

        public MinimapInfoSortEntry(Panel panel) {
            super(panel);
            for (MinimapInfoComponent minimapComponent : FTBChunksAPI.clientApi().getMinimapComponents()) {
                entryMap.put(minimapComponent.id(), new InfoEntry(minimapComponent, MinimapInfoSortEntry.this));
            }
        }

        @Override
        public void addWidgets() {
            addAll(entryMap.values());
        }

        @Override
        public void alignWidgets() {
            int height = 0;
            for (ResourceLocation id : infoSortList) {
                InfoEntry infoEntry = entryMap.get(id);
                if (infoEntry != null) {
                    infoEntry.setPosAndSize(0, height, width, 24);
                    infoEntry.alignWidgets();
                    height += 24;
                }
            }
        }
    }

    public class InfoEntry extends Panel {

        private final Button down;
        private final Button up;
        private final SimpleButton hideButton;
        private final TextField field;
        private final MinimapInfoComponent infoComponent;
        private final SimpleButton configButton;

        public InfoEntry(MinimapInfoComponent infoComponent, Panel panel) {
            super(panel);
            hideButton = ToggleVisibilityButton.create(this, !isComponentDisabled(infoComponent), (enabled) -> setComponentEnabled(infoComponent, enabled));
            down = new SortScreenButton(InfoEntry.this, Component.translatable("ftbchunks.gui.move_up"), Icons.UP, (widget, button) -> move(false, isShiftKeyDown()));
            up = new SortScreenButton(InfoEntry.this, Component.translatable("ftbchunks.gui.move_down"), Icons.DOWN, (widget, button) -> move(true, isShiftKeyDown()));
            configButton = new SimpleButton(InfoEntry.this, Component.translatable("gui.settings"), Icons.SETTINGS, (widget, button) -> {
                List<ContextMenuItem> items = new ArrayList<>();
                for (TranslatedOption translatedOption : infoComponent.getConfigComponents()) {
                    ContextMenuItem item = new ContextMenuItem(Component.translatable(translatedOption.translationKey()), isTranslatedOptionEnabled(infoComponent, translatedOption) ? Icons.ACCEPT : Icons.REMOVE_GRAY, (button1) -> setTranslatedOptionEnabled(infoComponent, translatedOption));
                    items.add(item);
                }
                openContextMenu(items);
                playClickSound();
            }) {
                @Override
                public void drawIcon(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                    super.drawIcon(graphics, theme, x, y, 12, 12);
                }
            };
            field = new TextField(InfoEntry.this) {
                @Override
                public void addMouseOverText(TooltipList list) {
                    list.add(infoComponent.description());
                }
            };
            this.infoComponent = infoComponent;
        }


        @Override
        public void addWidgets() {
            int listIndex = infoSortList.indexOf(infoComponent.id());
            if (listIndex != 0) {
                add(down);
            }
            if (listIndex != infoSortList.size() - 1) {
                add(up);
            }
            add(hideButton);
            add(field);
            if(!infoComponent.getConfigComponents().isEmpty()) {
                add(configButton);
            }
        }

        @Override
        public void alignWidgets() {
            down.setPosAndSize(6, height / 6 + 1, 6, 8);
            up.setPosAndSize(6, height / 6 + 11, 6, 8);
            hideButton.setPosAndSize(width - 18, height / 6, 12, 12);
            field.setPosAndSize(16, 8, width - 37, height);
            if(!infoComponent.getConfigComponents().isEmpty()) {
                configButton.setPos(width - 18 - 14, height / 6 + 2);
            }
            field.setText(ClientTextComponentUtils.ellipsize(getTheme().getFont(), infoComponent.displayName(), hideButton.getPosX() - 14 - 5).getString());
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            theme.drawWidget(graphics, x, y, w, h, getWidgetType());
            if (isMouseOver()) {
                Color4I.WHITE.withAlpha(33).draw(graphics, x, y, w, h);
            }
            super.draw(graphics, theme, x, y, w, h);
        }

        /**
         * moves the current MiniMapInfoComponent in our ordered list and saves the new order for rendering
         * @param forward true if the current value `down` the list and false if `up` the list
         * @param end true if the current value is the last value in the list and false if it is not
         */
        private void move(boolean forward, boolean end) {
            List<ResourceLocation> list = new LinkedList<>(infoSortList);
            if (end) {
                if (forward) {
                    list.remove(infoComponent.id());
                    list.add(infoComponent.id());
                } else {
                    list.remove(infoComponent.id());
                    list.add(0, infoComponent.id());
                }
            } else {
                int index = list.indexOf(infoComponent.id());
                if (index == -1) {
                    return;
                }
                if (forward) {
                    if (index == list.size() - 1) {
                        return;
                    }
                    list.remove(index);
                    list.add(index + 1, infoComponent.id());
                } else {
                    if (index == 0) {
                        return;
                    }
                    list.remove(index);
                    list.add(index - 1, infoComponent.id());
                }
            }
            infoSortList = list;
            saveConfig();
            parent.refreshWidgets();
        }
    }

    public static class SortScreenButton extends SimpleButton {

        public SortScreenButton(Panel panel, Component text, Icon icon, Callback c) {
            super(panel, text, icon, c);
        }

        @Override
        public void drawIcon(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            super.drawIcon(graphics, theme, x - 1, y - 2, 6, 8);
        }
    }


    private void saveConfig() {
        List<String> list = new LinkedList<>();
        infoSortList.forEach(resourceLocation -> list.add(resourceLocation.toString()));
        FTBChunksClientConfig.MINIMAP_INFO_ORDER.set(list);
        FTBChunksClientConfig.saveConfig();
        FTBChunksClient.INSTANCE.setupComponents();
    }

    private boolean isComponentDisabled(MinimapInfoComponent component) {
        return !FTBChunksAPI.clientApi().isMinimapComponentEnabled(component);
    }

    private void setComponentEnabled(MinimapInfoComponent component, boolean enabled) {
        FTBChunksAPI.clientApi().setMinimapComponentEnabled(component, enabled);
    }

    private boolean isTranslatedOptionEnabled(MinimapInfoComponent component, TranslatedOption option) {
        Map<String, String> stringStringMap = FTBChunksClientConfig.MINIMAP_SETTINGS.get();
        if (!stringStringMap.containsKey(component.id().toString())) {
            stringStringMap.put(component.id().toString(), option.optionName());
        }
        return stringStringMap.get(component.id().toString()).equals(option.optionName());
    }

    private void setTranslatedOptionEnabled(MinimapInfoComponent component, TranslatedOption option) {
        Map<String, String> stringStringMap = FTBChunksClientConfig.MINIMAP_SETTINGS.get();
        stringStringMap.put(component.id().toString(), option.optionName());
        FTBChunksClientConfig.MINIMAP_SETTINGS.set(stringStringMap);
        FTBChunksClientConfig.saveConfig();
    }

}
