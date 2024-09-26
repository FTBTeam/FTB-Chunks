package dev.ftb.mods.ftbchunks.client.mapicon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class EntityImageIcon extends Icon {
    @Nullable
    private final Slice mainSlice;
    private final List<ChildIconData> children;
    private final Icon mainIcon;
    private final List<Icon> childIcons;

    public EntityImageIcon(ResourceLocation mainTexture, @Nullable Slice mainSlice, List<ChildIconData> children) {
        this.mainSlice = mainSlice;
        this.children = children;

        mainIcon = createIcon(mainTexture, mainSlice);
        childIcons = children.stream().map(childIconData -> createIcon(childIconData.texture.orElse(mainTexture), childIconData.slice)).toList();
    }

    @Override
    public void draw(GuiGraphics graphics, int x, int y, int width, int height) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);

        float drawWidth = mainSlice == null ? width : mainSlice.width;
        float drawHeight = mainSlice == null ? height : mainSlice.height;

        float scaleX = width / drawWidth;
        float scaleY = height / drawHeight;

        pose.scale(scaleX, scaleY, 1);

        mainIcon.draw(graphics, 0, 0, (int) drawWidth, (int) drawHeight);

        for (int i = 0; i < children.size(); i++) {
            ChildIconData child = children.get(i);
            Icon icon = childIcons.get(i);

            pose.pushPose();
            child.offset.ifPresent(offset -> pose.translate(offset.x, offset.y, 0));
            icon.draw(graphics, 0, 0, child.slice.width, child.slice.height);
            pose.popPose();
        }

        pose.popPose();
    }


    public record Offset(int x, int y) {

        public static final Codec<Offset> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                Codec.INT.fieldOf("x").forGetter(o -> o.x),
                Codec.INT.fieldOf("y").forGetter(o -> o.y)
        ).apply(builder, Offset::new));

    }

    public record Slice(
            int x,
            int y,
            int width,
            int height) {

        public static final Codec<Slice> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                Offset.CODEC.fieldOf("offset").forGetter(s -> new Offset(s.x, s.y)),
                Codec.INT.fieldOf("width").forGetter(s -> s.width),
                Codec.INT.fieldOf("height").forGetter(s -> s.height)
        ).apply(builder, (offset, width, height) -> new Slice(offset.x(), offset.y(), width, height)));
    }

    public record ChildIconData(
            Optional<ResourceLocation> texture,
            Slice slice,
            Optional<Offset> offset) {

        public static final Codec<ChildIconData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.optionalFieldOf("texture").forGetter(entityImageIcon -> entityImageIcon.texture),
                Slice.CODEC.fieldOf("slice").forGetter(ChildIconData::slice),
                Offset.CODEC.optionalFieldOf("offset").forGetter(ChildIconData::offset)
        ).apply(instance, ChildIconData::new));

    }

    private Icon createIcon(ResourceLocation texture, @Nullable Slice slice) {
        SimpleTexture.TextureImage load = SimpleTexture.TextureImage.load(Minecraft.getInstance().getResourceManager(), texture);

        try {
            ImageIcon imageIcon = new ImageIcon(texture);
            if (slice != null) {
                int textureWidth = load.getImage().getWidth();
                int textureHeight = load.getImage().getHeight();
                return imageIcon.withUV(slice.x, slice.y, slice.width, slice.height, textureWidth, textureHeight);
            }
            return imageIcon;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load image: " + texture, e);
        }
    }
}
