package dev.ftb.mods.ftbchunks.client.gui;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.net.SendGeneralDataPacket;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleButton;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.misc.AbstractThreePanelScreen;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

import static dev.ftb.mods.ftblibrary.util.TextComponentUtils.hotkeyTooltip;

public class ChunkScreenPanelThing extends AbstractThreePanelScreen<ChunkScreen> {

    private static MapDimension dimension;
    private static Team openedAs;
    private static ChunkScreen chunkScreen;

    private ChunkScreenPanelThing() {
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
        setWidth(FTBChunks.MINIMAP_SIZE + 2);
        int extraHeight = getTopPanelHeight() + BOTTOM_PANEL_H;
        setHeight(FTBChunks.MINIMAP_SIZE + 2 + extraHeight);
        return true;
    }

    public static void openChunkScreen(@Nullable Team openedAs) {
        MapDimension.getCurrent().ifPresentOrElse(
                mapDimension -> {
                    //Todo this better
                    ChunkScreenPanelThing.dimension = mapDimension;
                    ChunkScreenPanelThing.openedAs = openedAs;
                    new ChunkScreenPanelThing().openGui();
                },
                () -> FTBChunks.LOGGER.warn("MapDimension data missing?? not opening chunk screen")
        );
    }

    public static void openChunkScreen() {
        openChunkScreen(null);
    }

    public ChunkScreen getChunkScreen() {
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
    protected ChunkScreen createMainPanel() {
        chunkScreen = new ChunkScreen(this, dimension, openedAs);
        return chunkScreen;
    }

    @Override
    protected Panel createTopPanel() {
        return new CustomTopPanel();
    }

    protected class CustomTopPanel extends Panel {
        private final SimpleButton closeButton;
        private final SimpleButton removeAllClaims;
        private final SimpleButton adminButton;

        public CustomTopPanel() {
            super(ChunkScreenPanelThing.this);

            closeButton = new SimpleButton(this, Component.translatable("gui.close"),
                    Icons.CLOSE, (btn, mb) -> doCancel());

            removeAllClaims = new SimpleButton(this, Component.literal("FIX ME"),
                    Icons.BIN, (btn, mb) -> {});

            adminButton = new AdminButton();
        }

        @Override
        public void addWidgets() {
            add(closeButton);
            add(removeAllClaims);
            if (!Minecraft.getInstance().isSingleplayer() && ChunkScreenPanelThing.this.getChunkScreen().openedAs == null && Minecraft.getInstance().player.hasPermissions(Commands.LEVEL_GAMEMASTERS)) {
                add(adminButton);
            }
        }

        @Override
        public void alignWidgets() {
            removeAllClaims.setPosAndSize(2, 1, 14, 14);
            closeButton.setPosAndSize(width - 16, 1, 14, 14);
            adminButton.setPosAndSize(width - 32, 1, 16, 16);

        }

        @Override
        public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            theme.drawPanelBackground(graphics, x, y, w, h);
            Color4I.BLACK.withAlpha(80).draw(graphics, x, y + h - 1, w, 1);
        }

        private class AdminButton extends SimpleButton {

            private boolean enabled = false;
            private static final Component DISABLED = Component.translatable("ftbchunks.gui.admin_mode_disabled");
            private static final Component ENABLED = Component.translatable("ftbchunks.gui.admin_mode_enabled");
            private static final Component MORE_INFO = Component.translatable("ftbchunks.gui.admin_mode_info", ChatFormatting.GRAY);

            public AdminButton() {
                super(CustomTopPanel.this, DISABLED, Icons.LOCK, (btn, mb) -> {
                    ChunkScreenPanelThing.this.getChunkScreen().isAdminEnabled = !ChunkScreenPanelThing.this.getChunkScreen().isAdminEnabled;
                    btn.setIcon(ChunkScreenPanelThing.this.getChunkScreen().isAdminEnabled ? Icons.LOCK_OPEN : Icons.LOCK);
                    btn.setTitle(ChunkScreenPanelThing.this.getChunkScreen().isAdminEnabled ? ENABLED : DISABLED);
                });
            }

            @Override
            public void addMouseOverText(TooltipList list) {
                super.addMouseOverText(list);
                list.add(MORE_INFO);
            }
        }
    }

    private class CustomBottomPanel extends Panel {

        public CustomBottomPanel() {
            super(ChunkScreenPanelThing.this);
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
        }
    }
}
