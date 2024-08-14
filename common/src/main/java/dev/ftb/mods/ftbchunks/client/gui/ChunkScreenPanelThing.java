package dev.ftb.mods.ftbchunks.client.gui;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.net.SendGeneralDataPacket;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.misc.AbstractThreePanelScreen;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

import static dev.ftb.mods.ftblibrary.util.TextComponentUtils.hotkeyTooltip;

public class ChunkScreenPanelThing extends AbstractThreePanelScreen<ChunkScreen> {

    private static MapDimension dimension;
    @Nullable
    private final Team openedAs;
    public ChunkScreen.ChunkUpdateInfo updateInfo;


    public ChunkScreenPanelThing(MapDimension dimension, @Nullable Team openedAs) {
        this.openedAs = openedAs;
        showCloseButton(true);
        //use reflection to change BottomPanel
        try {
            Field bottomPanelField = AbstractThreePanelScreen.class.getDeclaredField("bottomPanel");
            bottomPanelField.setAccessible(true);
            bottomPanelField.set(this, new CustomBottomPanel());
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

//        MapManager.getInstance().ifPresent(m -> m.updateAllRegions(false));
        setFullscreen();
    }

    @Override
    protected int getScrollbarWidth() {
        return -1;
    }

    @Override
    public boolean onInit() {
        setWidth(FTBChunks.MINIMAP_SIZE + 2);
        setHeight(FTBChunks.MINIMAP_SIZE + 2);
//        setHeight((int) (getScreen().getGuiScaledHeight() * 0.95f));
        return true;
//        return setSizeProportional(0.40f, 0.9f);
    }

    public static void openChunkScreen(@Nullable Team openedAs) {
        MapDimension.getCurrent().ifPresentOrElse(
                mapDimension -> {
                    //Temp cheat fix api in Lib
                    ChunkScreenPanelThing.dimension = mapDimension;
                    ChunkScreenPanelThing chunkScreenPanelThing = new ChunkScreenPanelThing(mapDimension, openedAs);
                    chunkScreenPanelThing.openGui();
                },
                () -> FTBChunks.LOGGER.warn("MapDimension data missing?? not opening chunk screen")
        );
    }

    public static void openChunkScreen() {
        openChunkScreen(null);
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
        return new ChunkScreen(this, dimension, openedAs);
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
