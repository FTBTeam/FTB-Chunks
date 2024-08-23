package dev.ftb.mods.ftbchunks.client.mapicon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.injection.Slice;

import java.util.Optional;

public class EntityImageIcon extends ImageIcon {

    private final Slice slice;
    private final Optional<Offset> offset;
    private final int textureWidth;
    private final int textureHeight;

    public static final Codec<EntityImageIcon> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("texture").forGetter(entityImageIcon -> entityImageIcon.texture),
            Slice.CODEC.fieldOf("slice").forGetter(EntityImageIcon::getSlice),
            Offset.CODEC.optionalFieldOf("offset").forGetter(EntityImageIcon::getOffset)
    ).apply(instance, EntityImageIcon::new));


    public EntityImageIcon(ResourceLocation tex, Slice slice, Optional<Offset> offset) {
        super(tex);
        this.slice = slice;
        this.offset = offset;

        SimpleTexture.TextureImage load = SimpleTexture.TextureImage.load(Minecraft.getInstance().getResourceManager(), tex);


        try {
            this.textureWidth = load.getImage().getWidth();
            this.textureHeight = load.getImage().getHeight();
            minU = slice.x / textureWidth;
            minV = slice.y / textureHeight;
            maxU = (slice.x + slice.width) / textureWidth;
            maxV = (slice.y + slice.height) / textureHeight;
        }catch (Exception e) {
            throw new RuntimeException("Failed to load image: " + tex, e);
        }
    }

    @Override
    public void draw(GuiGraphics graphics, int x, int y, int w, int h) {
        //Todo scaling
//        float scaleX = (float) w / slice.width;
//        float scaleY = (float) h / slice.height;
        w = (int) slice.width;
        h = (int) slice.height;
        graphics.pose().pushPose();
//        graphics.pose().translate(scaleX, scaleY, 0);
        if(offset.isPresent()) {

            Offset offset1 = offset.get();
            Color4I.GREEN.draw(graphics, x + offset1.x, y + offset1.y, 1, 1);

            for (int i = 0; i < 16; i++) {
                Color4I.get256( i * 16).draw(graphics, x -1, y + i, 1, 1);
//                Color4I.GREEN.draw(graphics, 0, i, 1, 1);

            }
            graphics.pose().translate(x + offset.get().x, y + offset.get().y, 0);
            super.draw(graphics, 0, 0, w, h);


//            graphics.pose().scale(scaleX, scaleY, 1);






        }else {
            super.draw(graphics, x, y, w, h);
        }
        graphics.pose().popPose();
    }

    public Slice getSlice() {
        return slice;
    }

    public Optional<Offset> getOffset() {
        return offset;
    }

    public record Offset(
            int x,
            int y) {

        public static final Codec<Offset> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                Codec.INT.fieldOf("x").forGetter(o -> o.x),
                Codec.INT.fieldOf("y").forGetter(o -> o.y)
        ).apply(builder, Offset::new));

    }

    public record Slice(
            float x,
            float y,
            float width,
            float height) {

        public static final Codec<Slice> CODEC = RecordCodecBuilder.create(builder -> builder.group(
                Codec.FLOAT.fieldOf("x").forGetter(s -> s.x),
                Codec.FLOAT.fieldOf("y").forGetter(s -> s.y),
                Codec.FLOAT.fieldOf("width").forGetter(s -> s.width),
                Codec.FLOAT.fieldOf("height").forGetter(s -> s.height)
        ).apply(builder, Slice::new));
    }
}
