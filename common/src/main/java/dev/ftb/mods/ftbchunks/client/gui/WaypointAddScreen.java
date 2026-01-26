package dev.ftb.mods.ftbchunks.client.gui;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.waypoint.Waypoint;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftblibrary.client.config.editable.EditableColor;
import dev.ftb.mods.ftblibrary.client.config.editable.EditableString;
import dev.ftb.mods.ftblibrary.client.gui.theme.Theme;
import dev.ftb.mods.ftblibrary.client.gui.widget.BaseScreen;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class WaypointAddScreen extends BaseScreen {
    private final EditableString name;
    private final GlobalPos waypointLocation;
    private final EditableColor color;
    private final boolean override;

    public WaypointAddScreen(EditableString name, GlobalPos waypointLocation, Color4I color, boolean override) {
        super();
        this.name = name;
        this.waypointLocation = waypointLocation;
        this.setHeight(35);
        this.color = new EditableColor();
        this.color.setValue(color);
        this.override = override;
    }

    public WaypointAddScreen(EditableString name, GlobalPos waypointLocation) {
        this(name, waypointLocation, Color4I.hsb(MathUtils.RAND.nextFloat(), 1F, 1F), false);
    }

    public WaypointAddScreen(EditableString name, Player player) {
        this(name, new GlobalPos(player.level().dimension(), player.blockPosition()));
    }

    @Override
    public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
    }

    @Override
    public void addWidgets() {
        AddWaypointOverlay.GlobalPosConfig globalPosConfig = new AddWaypointOverlay.GlobalPosConfig();
        globalPosConfig.setValue(waypointLocation);
        Component title = Component.translatable("ftbchunks.gui." + (override ? "edit_waypoint" : "add_waypoint"));
        AddWaypointOverlay overlay = new AddWaypointOverlay(this, title, globalPosConfig, name, color, accepted -> {
            if (accepted && !name.getValue().isEmpty()) {
                if (override) {
                    FTBChunksAPI.clientApi().getWaypointManager(waypointLocation.dimension())
                            .ifPresent(mgr -> mgr.removeWaypointAt(waypointLocation.pos()));
                }
                Waypoint wp = FTBChunksClient.addWaypoint(name.getValue(), globalPosConfig.getValue(), color.getValue().rgba());
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("ftbchunks.waypoint_added",
                                wp.getDisplayName().copy().withStyle(ChatFormatting.YELLOW)
                        ), true);
            }
        }) {
            @Override
            public void onClosed() {
                closeGui();
            }
        };
        overlay.setWidth(this.width);
        pushModalPanel(overlay);
    }
}
