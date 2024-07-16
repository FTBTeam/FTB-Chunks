package dev.ftb.mods.ftbchunks.api.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.apache.logging.log4j.core.net.Priority;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MapInfoEvent
{
    public static final Event<Consumer<MapInfoEvent>> REGISTER = EventFactory.createLoop();


    private final Map<ResourceLocation, MapText> mapTexts;

    public MapInfoEvent() {
        this.mapTexts = new HashMap<>();
    }


    public void register(ResourceLocation id, Supplier<MapInfo> text) {
        mapTexts.put(id, new MapText(id, text, new Priority(null, null)));
    }

    public void register(ResourceLocation id, Supplier<MapInfo> text, Priority priority) {
        mapTexts.put(id, new MapText(id, text, priority));
    }


    public List<MapText> computeOrder() {
        List<MapText> list = new LinkedList<>(mapTexts.values());
        list.sort((o1, o2) -> o1.requirement.compare(o1.id, o2.id));
        return list;
    }


    public static record MapText(ResourceLocation id, Supplier<MapInfo> text, Priority requirement) {}

    public static interface MapInfo {
        int render(GuiGraphics graphics, int x, int y, int size, int scaledHeight, double halfSizeD, MapDimension dim);

        boolean shouldRender(Minecraft mc, double playerX, double playerY, double playerZ, MapDimension dim);
    }

    public interface MapTextInfo extends MapInfo {

        List<Component> getTexts(MapDimension dim);

        @Override
        default int render(GuiGraphics graphics, int x, int y, int size, int scaledHeight, double halfSizeD, MapDimension dim) {
            List<Component> textList = getTexts(dim);
            Minecraft mc = Minecraft.getInstance();
            PoseStack poseStack = graphics.pose();
            if(!textList.isEmpty()) {
                float fontScale = FTBChunksClientConfig.MINIMAP_FONT_SCALE.get().floatValue();
                float textHeight = (mc.font.lineHeight + 2) * textList.size() * fontScale;
                // draw text below minimap if there's room, above otherwise
                float yOff = y + size + textHeight >= scaledHeight ? -textHeight : size + 2f;
                poseStack.pushPose();
                poseStack.translate(x + halfSizeD, y + yOff, 0D);
                poseStack.scale(fontScale, fontScale, 1F);

                for (int i = 0; i < textList.size(); i++) {
                    FormattedCharSequence text = textList.get(i).getVisualOrderText();
                    int textWidth = mc.font.width(text);
                    graphics.drawString(mc.font, text, -textWidth / 2, i * (mc.font.lineHeight + 2), 0xFFFFFFFF, true);
                }
                poseStack.popPose();
                return (int) textHeight;
            }
            return 0;
        }
    }

    public interface SingleTextInfo extends MapTextInfo {
        Component getText(MapDimension dim);

        @Override
        default List<Component> getTexts(MapDimension dim) {
            return Collections.singletonList(getText(dim));
        }
    }


    public static record Priority(@Nullable ResourceLocation before, @Nullable ResourceLocation after) implements Comparator<ResourceLocation> {

        public static Priority before(ResourceLocation before) {
            return new Priority(before, null);
        }

        public static Priority after(ResourceLocation after) {
            return new Priority(null, after);
        }

        public static Priority beforeAndAfter(ResourceLocation before, ResourceLocation after) {
            return new Priority(before, after);
        }

        @Override
        public int compare(ResourceLocation one, ResourceLocation two) {
            if (before != null) {
                if (one.equals(before) && !two.equals(before)) {
                    return 1;
                } else if (!one.equals(before) && two.equals(before)) {
                    return -1;
                }
            }

            if (after != null) {
                if (one.equals(after) && !two.equals(after)) {
                    return -1;
                } else if (!one.equals(after) && two.equals(after)) {
                    return 1;
                }
            }

            // Default comparison logic by alphabetical order
            return one.toString().compareTo(two.toString());
        }
    }
}
