package dev.ftb.mods.ftbchunks.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftbchunks.client.map.WaypointType;
import dev.ftb.mods.ftblibrary.config.manager.ConfigManagerClient;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.ui.misc.KeyReferenceScreen;
import dev.ftb.mods.ftblibrary.util.TextComponentUtils;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.List;

class MapScreenButtonPanel extends Panel {
    private static final Icon MINIMAP_INFO = Icon.getIcon(FTBChunksAPI.rl("textures/minimap_info.png"));

    private final LargeMapScreen largeMapScreen;
    private final Button claimChunksButton;
    private final Button dimensionButton;
    private final Button waypointManagerButton;
    private final Button infoButton;
    private final Button settingsButton;
    private final Button serverSettingsButton;
    private final Button clearDeathpointsButton;
    private final Button infoSortScreen;

    MapScreenButtonPanel(LargeMapScreen largeMapScreen) {
        super(largeMapScreen);

        this.largeMapScreen = largeMapScreen;

        claimChunksButton = new SimpleTooltipButton(this, Component.translatable("ftbchunks.gui.claimed_chunks"), Icons.MAP,
                (b, mb) -> ChunkScreen.openChunkScreen(),
                TextComponentUtils.hotkeyTooltip("C"));

        Component tooltip = Component.literal("[")
                .append(FTBChunksClient.INSTANCE.waypointManagerKey.getTranslatedKeyMessage())
                .append(Component.literal("]")).withStyle(ChatFormatting.GRAY);
        waypointManagerButton = new SimpleTooltipButton(this, Component.translatable("ftbchunks.gui.waypoints"), Icons.COMPASS,
                (b, mb) -> new WaypointEditorScreen().openGui(), tooltip);
        infoButton = new SimpleButton(this, Component.translatable("ftbchunks.gui.large_map_info"), Icons.INFO,
                (b, mb) -> new MapKeyReferenceScreen().openGui());

        clearDeathpointsButton = new ClearDeathPointButton(this);

        List<ContextMenuItem> dimItems = AddWaypointOverlay.createDimContextItems(largeMapScreen::switchToDimension);
        dimensionButton = new SimpleButton(this, List.of(Component.empty()), Icons.GLOBE, (b, mb) -> {
            DropDownMenu dropDownMenu = getGui().openDropdownMenu(dimItems);
            dropDownMenu.setPos(b.getX() + b.width, b.getY() + b.height - dropDownMenu.height);
        }) {
            @Override
            public void addMouseOverText(TooltipList list) {
                list.add(Component.translatable("ftbchunks.gui.label.dimension"));
                list.add(TextComponentUtils.translatedDimension(largeMapScreen.dimension.dimension).copy().withStyle(ChatFormatting.GRAY));
            }
        };

        infoSortScreen = new SimpleTooltipButton(this, Component.translatable("ftbchunks.gui.sort_minimap_info"), MINIMAP_INFO,
                (b, mb) -> new MinimapInfoSortScreen().openGui(), tooltip);

        settingsButton = new SimpleTooltipButton(this, Component.translatable("ftbchunks.config.client"),
                Icons.SETTINGS,
                (b, mb) -> ConfigManagerClient.editConfig(FTBChunksClientConfig.KEY),
                TextComponentUtils.hotkeyTooltip("S"));

        boolean adminPlayer = Minecraft.getInstance().player.hasPermissions(Commands.LEVEL_GAMEMASTERS);
        serverSettingsButton = new SimpleTooltipButton(this, Component.translatable("ftbchunks.config.server"),
                Icons.SETTINGS.withTint(Color4I.rgb(0xA040FF)),
                (b, mb) -> ConfigManagerClient.editConfig(FTBChunksWorldConfig.KEY, !adminPlayer),
                TextComponentUtils.hotkeyTooltip("Ctrl + S"));
    }

    @Override
    public void addWidgets() {
        add(claimChunksButton);
        add(waypointManagerButton);
        add(infoButton);
        add(clearDeathpointsButton);
        add(infoSortScreen);
        add(dimensionButton);
        add(settingsButton);
        if (Minecraft.getInstance().player.hasPermissions(Commands.LEVEL_GAMEMASTERS)) {
            add(serverSettingsButton);
        }
    }

    @Override
    public void alignWidgets() {
        // upper buttons
        claimChunksButton.setPosAndSize(1, 1, 16, 16);
        waypointManagerButton.setPosAndSize(1, 19, 16, 16);
        infoButton.setPosAndSize(1, 37, 16, 16);
        dimensionButton.setPosAndSize(1, 55, 16, 16);
        clearDeathpointsButton.setPosAndSize(1, 73, 16, 16);
        // lower buttons
        int yOff = Minecraft.getInstance().player.hasPermissions(Commands.LEVEL_GAMEMASTERS) ? 18 : 0;
        infoSortScreen.setPosAndSize(1, height - 36 - yOff, 16, 16);
        settingsButton.setPosAndSize(1, height - 18 - yOff, 16, 16);
        serverSettingsButton.setPosAndSize(1, height - 18, 16, 16);
    }

    @Override
    public boolean keyPressed(Key key) {
        if (key.is(InputConstants.KEY_C)) {
            claimChunksButton.onClicked(MouseButton.LEFT);
            return true;
        } else if (key.is(InputConstants.KEY_S)) {
            if (Screen.hasControlDown()) {
                if (Minecraft.getInstance().player.hasPermissions(Commands.LEVEL_GAMEMASTERS)) {
                    serverSettingsButton.onClicked(MouseButton.LEFT);
                }
            } else {
                settingsButton.onClicked(MouseButton.LEFT);
            }
            return true;
        } else if (FTBChunksClient.INSTANCE.waypointManagerKey.isDown()) {
            waypointManagerButton.onClicked(MouseButton.LEFT);
            return true;
        }

        return false;
    }

    @Override
    public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, 0x30000000);
        graphics.vLine(x + w, y - 1, y + h, 0x50000000);
    }

    private static class SimpleTooltipButton extends SimpleButton {
        private final List<Component> tooltipLines;

        public SimpleTooltipButton(Panel panel, Component text, Icon icon, Callback c, Component tooltipLine) {
            super(panel, text, icon, c);
            this.tooltipLines = List.of(tooltipLine);
        }

        @Override
        public void addMouseOverText(TooltipList list) {
            super.addMouseOverText(list);
            tooltipLines.forEach(list::add);
        }
    }

    private class ClearDeathPointButton extends SimpleButton {
        public ClearDeathPointButton(Panel panel) {
            super(panel, Component.translatable("ftbchunks.gui.clear_deathpoints"), Icons.CANCEL, (b, mb) -> {
                if (largeMapScreen.getWaypointManager().removeIf(wp -> wp.getType() == WaypointType.DEATH)) {
                    refreshWidgets();
                }
            });
        }

        @Override
        public boolean shouldDraw() {
            return super.shouldDraw() && largeMapScreen.getWaypointManager().hasDeathpoint();
        }

        @Override
        public boolean isEnabled() {
            return shouldDraw();
        }
    }

    private static class MapKeyReferenceScreen extends KeyReferenceScreen {
        public MapKeyReferenceScreen() {
            super("ftbchunks.gui.large_map_info.text");
        }

        @Override
        public Component getTitle() {
            return Component.translatable("ftbchunks.gui.large_map_info");
        }
    }
}
