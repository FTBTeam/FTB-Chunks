package dev.ftb.mods.ftbchunks.client.gui;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.minimap.InfoConfigComponent;
import dev.ftb.mods.ftbchunks.api.client.minimap.MinimapInfoComponent;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.ContextMenuItem;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleButton;
import dev.ftb.mods.ftblibrary.ui.TextField;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.ui.misc.AbstractThreePanelScreen;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MinimapInfoSortScreen extends AbstractThreePanelScreen<MinimapInfoSortScreen.MinimapInfoSortEntry> {


    private List<ResourceLocation> infoSortList = new LinkedList<>();

    public MinimapInfoSortScreen() {
        super();
        setHeight(200);
        setWidth(200);
        FTBChunksClientConfig.MINIMAP_INFO_ORDER.get().forEach(s -> infoSortList.add(ResourceLocation.parse(s)));
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

        public InfoEntry(MinimapInfoComponent infoComponent, Panel panel) {
            super(panel);
            hideButton = new SimpleButton(this, Component.empty(), isComponentDisabled(infoComponent) ? Icons.REMOVE_GRAY : Icons.ACCEPT, (w, mb) -> {
                setComponentEnabled(infoComponent, isComponentDisabled(infoComponent));;
                w.setIcon(isComponentDisabled(infoComponent) ? Icons.REMOVE_GRAY : Icons.ACCEPT);
            });
            down = new SimpleButton(InfoEntry.this, List.of(Component.translatable("gui.move_up")), Icons.UP,
                    (widget, button) -> move(false));
            up = new SimpleButton(InfoEntry.this, List.of(Component.translatable("gui.move_down")), Icons.DOWN,
                    (widget, button) -> move(true));
            field = new TextField(InfoEntry.this) {
                @Override
                public boolean mousePressed(MouseButton button) {
                    if(!infoComponent.getConfigComponents().isEmpty() && button == MouseButton.RIGHT) {
                        List<ContextMenuItem> items = new ArrayList<>();
                        for (InfoConfigComponent value : infoComponent.getConfigComponents()) {
                            ContextMenuItem item = new ContextMenuItem(value.displayName(), infoComponent.getActiveConfigComponent() == value ? Icons.ACCEPT : Icons.REMOVE_GRAY, (button1) -> infoComponent.setActiveConfigComponent(value));
                            items.add(item);
                        }
                        openContextMenu(items);
                        playClickSound();
                        return true;
                    }else {
                        return super.mousePressed(button);
                    }
                }

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
            if(listIndex != 0) {
                add(down);
            }
            if(listIndex != infoSortList.size() - 1) {
                add(up);
            }
            add(hideButton);
            add(field);
        }

        @Override
        public void alignWidgets() {
            down.setPosAndSize(2, height / 6, 15, 15);
            up.setPosAndSize(18, height / 6, 15, 15);
            hideButton.setPosAndSize(width - 18, height / 6, 16, 16);
            field.setPosAndSize(37, 8, width - 37, height);
            field.setText(WaypointEditorScreen.ellipsize(getTheme().getFont(), infoComponent.displayName(),hideButton.getPosX() - 37 - 5).getString());
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            theme.drawWidget(graphics, x, y, w, h, getWidgetType());
            if (isMouseOver()) {
                Color4I.WHITE.withAlpha(33).draw(graphics, x, y, w, h);
            }
            super.draw(graphics, theme, x, y, w, h);
        }

        private void move(boolean forward) {
            List<ResourceLocation> list = new LinkedList<>(infoSortList);
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
            infoSortList = list;
            saveConfig();
            parent.refreshWidgets();
        }

    }

    private void saveConfig() {
        List<String> list = new LinkedList<>();
        infoSortList.forEach(resourceLocation -> list.add(resourceLocation.toString()));
        FTBChunksClientConfig.MINIMAP_INFO_ORDER.set(list);
        FTBChunksClientConfig.saveConfig();
    }

    private boolean isComponentDisabled(MinimapInfoComponent component) {
        return !FTBChunksAPI.clientApi().isMinimapComponentEnabled(component);
    }

    private void setComponentEnabled(MinimapInfoComponent component, boolean enabled) {
        FTBChunksAPI.clientApi().setMinimapComponentEnabled(component, enabled);
    }

}
