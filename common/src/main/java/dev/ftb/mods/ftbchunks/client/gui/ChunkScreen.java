package dev.ftb.mods.ftbchunks.client.gui;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.net.SendGeneralDataPacket;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.ToggleableButton;
import dev.ftb.mods.ftblibrary.ui.misc.AbstractThreePanelScreen;
import dev.ftb.mods.ftblibrary.ui.misc.KeyReferenceScreen;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

public class ChunkScreen extends AbstractThreePanelScreen<ChunkScreenPanel> {

    private static MapDimension dimension;
    private static Team openedAs;
    private static ChunkScreenPanel chunkScreen;
    private SimpleButton largeMapButton;

    private ChunkScreen() {
        showCloseButton(true);
        showScrollBar(false);
    }

    @Override
    protected Panel createBottomPanel() {
        return new CustomBottomPanel();
    }

    @Override
    protected int getScrollbarWidth() {
        return -1;
    }

    @Override
    public boolean onInit() {
        int size = (int) (getScreen().getGuiScaledHeight() * 0.85f);

        setWidth(Math.min(size + 2, getScreen().getGuiScaledWidth() - 2));
        setHeight(Math.min(size + getTopPanelHeight() + BOTTOM_PANEL_H, getScreen().getGuiScaledHeight() - 2));

        return true;
    }


    @Override
    public void addWidgets() {
        super.addWidgets();

        add(largeMapButton = new SimpleButton(this, Component.translatable("ftbchunks.gui.large_map"), Icons.MAP,
                (simpleButton, mouseButton) -> LargeMapScreen.openMap()
        ));
    }

    @Override
    public void alignWidgets() {
        super.alignWidgets();

        largeMapButton.setPosAndSize(-getX() + 2, -getY() + 2, 16, 16);
    }

    public static void openChunkScreen(@Nullable Team openedAs) {
        MapDimension.getCurrent().ifPresentOrElse(
                mapDimension -> {
                    //Todo this better
                    ChunkScreen.dimension = mapDimension;
                    ChunkScreen.openedAs = openedAs;
                    new ChunkScreen().openGui();
                },
                () -> FTBChunks.LOGGER.warn("MapDimension data missing?? not opening chunk screen")
        );
    }

    public static void openChunkScreen() {
        openChunkScreen(null);
    }

    public ChunkScreenPanel getChunkScreen() {
        return chunkScreen;
    }

    @Override
    protected void doCancel() {

    }

    @Override
    protected void doAccept() {

    }

    @Override
    protected int getTopPanelHeight() {
        return 16;
    }

    @Override
    protected ChunkScreenPanel createMainPanel() {
        chunkScreen = new ChunkScreenPanel(this, dimension, openedAs);
        return chunkScreen;
    }

    @Override
    protected Panel createTopPanel() {
        return new CustomTopPanel();
    }

    protected class CustomTopPanel extends Panel {
        private final Button closeButton;
        private final Button removeAllClaims;
        private final Button adminButton;
        private final Button mouseReferenceButton;

        public CustomTopPanel() {
            super(ChunkScreen.this);

            closeButton = new SimpleButton(this, Component.translatable("gui.close"), Icons.CLOSE,
                    (btn, mb) -> doCancel())
                    .setForceButtonSize(false);

            mouseReferenceButton = new SimpleButton(this, Component.translatable("ftbchunks.gui.chunk_info"), Icons.INFO,
                    (btn, mb) -> new ChunkMouseReferenceScreen().openGui())
                    .setForceButtonSize(false);

            removeAllClaims = new SimpleButton(this, Component.translatable("ftbchunks.gui.unclaim_all"), Icons.BIN,
                    (btn, mb) -> chunkScreen.removeAllClaims())
                    .setForceButtonSize(false);


            adminButton = new AdminButton().setForceButtonSize(false);
        }

        @Override
        public void addWidgets() {
            add(closeButton);
            add(removeAllClaims);
            if (!Minecraft.getInstance().isSingleplayer() && ChunkScreen.this.getChunkScreen().openedAs == null && Minecraft.getInstance().player.hasPermissions(Commands.LEVEL_GAMEMASTERS)) {
                add(adminButton);
            }
            add(mouseReferenceButton);
        }

        @Override
        public void alignWidgets() {
            removeAllClaims.setPosAndSize(2, 2, 12, 12);
            adminButton.setPosAndSize(18, 2, 12, 12);

            closeButton.setPosAndSize(width - 16, 2, 12, 12);
            mouseReferenceButton.setPosAndSize(width - 32, 2, 12, 12);

        }

        @Override
        public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            theme.drawPanelBackground(graphics, x, y, w, h);
            Color4I.BLACK.withAlpha(80).draw(graphics, x, y + h - 1, w, 1);
        }

        private class AdminButton extends ToggleableButton {

            private static final Component DISABLED = Component.translatable("ftbchunks.gui.admin_mode_disabled");
            private static final Component ENABLED = Component.translatable("ftbchunks.gui.admin_mode_enabled");
            private static final Component MORE_INFO = Component.translatable("ftbchunks.gui.admin_mode_info", ChatFormatting.GRAY);

            public AdminButton() {
                super(CustomTopPanel.this, false, Icons.LOCK_OPEN, Icons.LOCK, (btn, newState) -> {
                    ChunkScreen.this.getChunkScreen().isAdminEnabled = newState;
                });
                setEnabledText(ENABLED);
                setDisabledText(DISABLED);
            }

            @Override
            public void addMouseOverText(TooltipList list) {
                super.addMouseOverText(list);
                list.add(MORE_INFO);
            }
        }
    }

    private static class ChunkMouseReferenceScreen extends KeyReferenceScreen {
        public ChunkMouseReferenceScreen() {
            super("ftbchunks.gui.chunk_info.text");
        }

        @Override
        public Component getTitle() {
            return Component.translatable("ftbchunks.gui.chunk_info");
        }
    }


    private class CustomBottomPanel extends Panel {

        public CustomBottomPanel() {
            super(ChunkScreen.this);
        }

        @Override
        public void addWidgets() {
        }

        @Override
        public void alignWidgets() {

        }

        @Override
        public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            theme.drawPanelBackground(graphics, x, y, w, h);
            Color4I.GRAY.withAlpha(64).draw(graphics, x, y, w, 1);
            SendGeneralDataPacket.GeneralChunkData generalChunkData = FTBChunksClient.INSTANCE.getGeneralChunkData();

            int claimed = generalChunkData.claimed();
            int maxClaim = generalChunkData.maxClaimChunks();

            MutableComponent append1 = Component.translatable("ftbchunks.gui.claimed").append(Component.literal(": ")
                    .append(Component.literal(claimed + " / " + maxClaim)
                            .withStyle(claimed > maxClaim ? ChatFormatting.RED : claimed == maxClaim ? ChatFormatting.YELLOW : ChatFormatting.GREEN)));
            theme.drawString(graphics, append1, x + 4, y + 4);

            int loaded = generalChunkData.loaded();
            int maxLoaded = generalChunkData.maxForceLoadChunks();
            MutableComponent append = Component.translatable("ftbchunks.gui.force_loaded").append(Component.literal(": "))
                    .append(Component.literal(loaded + " / " + maxLoaded)
                                    .withStyle(loaded > maxLoaded ? ChatFormatting.RED : loaded == maxLoaded ? ChatFormatting.YELLOW : ChatFormatting.GREEN));

            theme.drawString(graphics, append, x + 4, y + 6 + Minecraft.getInstance().font.lineHeight);

            if (openedAs != null) {
                String openAsMessage = Component.translatable("ftbchunks.gui.opened_as", openedAs.getName()).getString();
                //from right side
                int sx = x + w - theme.getStringWidth(openAsMessage) - 4;
                theme.drawString(graphics, openAsMessage, sx, y + h - theme.getFontHeight() - 2, Color4I.WHITE, Theme.SHADOW);
//                theme.drawString(graphics, openAsMessage, sx + 2, sy + FTBChunks.MINIMAP_SIZE - theme.getFontHeight() - 2, Color4I.WHITE, Theme.SHADOW);
            }
        }
    }
}
