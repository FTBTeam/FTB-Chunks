package dev.ftb.mods.ftbchunks.client.gui;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import dev.architectury.platform.Platform;
import dev.ftb.mods.ftbchunks.client.mapicon.EntityIcons;
import dev.ftb.mods.ftbchunks.client.mapicon.EntityImageIcon;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.IntTextBox;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.ToggleableButton;
import dev.ftb.mods.ftblibrary.ui.misc.SimpleToast;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Not the most perfect gui but allows creation of the json files for image icons
// Uses the entity texture to render the entity icon
// Can create multiple slices and move them around.
public class SliceCreationGUI extends BaseScreen {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final EntityType<?> entityType;
    private final int textureWidth;
    private final int textureHeight;
    private final ResourceLocation texture;
    private final SliceControlBox sliceControlBox;
    private final SimpleButton exportButton;
    private final IntTextBox imageSizeX;
    private final IntTextBox imageSizeY;
    private final List<SliceControlBox> sliceControlBoxes;
    private int currentSlice = 0;
    private SliceControlBox activeSlice;
    private final SimpleButton addSlice;
    private final SimpleButton removeSlice;
    private final SimpleButton nextSlice;
    private final SimpleButton previousSlice;

    public SliceCreationGUI(EntityType<?> entityType) {
        super();
        this.entityType = entityType;
        this.sliceControlBoxes = new ArrayList<>();
        this.imageSizeX = new IntTextBox(this);
        this.imageSizeY = new IntTextBox(this);
        this.imageSizeX.setAmount(16);
        this.imageSizeY.setAmount(16);
        Entity entity = entityType.create(Minecraft.getInstance().level);
        this.texture = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity).getTextureLocation(entity);

        this.exportButton = createExportButton();

        Pair<Integer, Integer> textureSize = getTextureSize();
        this.textureWidth = textureSize.getFirst();
        this.textureHeight = textureSize.getSecond();
        this.sliceControlBox = new SliceControlBox(this, texture, textureWidth, textureHeight, false);

        // Create the button to move, create or remove slices
        this.addSlice = new SimpleButton(this, Component.literal("Add Slice"), Icons.ADD, (button1, mouseButton1) -> {
            SliceControlBox sliceControlBox = new SliceControlBox(this, texture, textureWidth, textureHeight, true);
            sliceControlBoxes.add(sliceControlBox);
            currentSlice = sliceControlBoxes.size() - 1;
            activeSlice = sliceControlBox;
            button1.getGui().refreshWidgets();
        });
        this.removeSlice = new SimpleButton(this, Component.literal("Remove Slice"), Icons.REMOVE, (button1, mouseButton1) -> {
            if (!sliceControlBoxes.isEmpty()) {
                sliceControlBoxes.remove(currentSlice);
                currentSlice = Math.max(0, Math.min(sliceControlBoxes.size() - 1, currentSlice));
                activeSlice = sliceControlBoxes.isEmpty() ? null : sliceControlBoxes.get(currentSlice);
                button1.getGui().refreshWidgets();
            }
        });
        this.nextSlice = new SimpleButton(this, Component.literal("Next Slice"), Icons.DOWN, (button1, mouseButton1) -> {
            if (!sliceControlBoxes.isEmpty()) {
                currentSlice = Math.max(0, Math.min(sliceControlBoxes.size() - 1, currentSlice + 1));
                activeSlice = sliceControlBoxes.get(currentSlice);
                button1.getGui().refreshWidgets();
            }
        });
        this.previousSlice = new SimpleButton(this, Component.literal("Previous Slice"), Icons.UP, (button1, mouseButton1) -> {
            if (!sliceControlBoxes.isEmpty()) {
                currentSlice = Math.max(0, Math.min(sliceControlBoxes.size() - 1, currentSlice - 1));
                activeSlice = sliceControlBoxes.get(currentSlice);
                button1.getGui().refreshWidgets();
            }
        });


        loadExistingSettings();
    }

    @Override
    public void addWidgets() {
        activeSlice = null;
        add(sliceControlBox);
        add(exportButton);
        add(imageSizeX);
        add(imageSizeY);
        add(addSlice);
        add(removeSlice);
        add(nextSlice);
        add(previousSlice);
        if (!sliceControlBoxes.isEmpty()) {
            int slice = Math.max(0, Math.min(sliceControlBoxes.size() - 1, currentSlice));
            SliceControlBox widget = sliceControlBoxes.get(slice);
            add(widget);
            activeSlice = widget;
        }
    }

    @Override
    public void alignWidgets() {
        setSizeProportional(1f, 1f);
        int oneFourth = width / 4;
        sliceControlBox.setPosAndSize(2, 60, oneFourth, 60);
        exportButton.setPosAndSize(28, 0, 20, 20);
        imageSizeX.setPosAndSize(2, 25, 20, 20);
        imageSizeY.setPosAndSize(27, 25, 20, 20);
        if (activeSlice != null) {
            activeSlice.setPosAndSize(2, 140, oneFourth, 60);
        }
        addSlice.setPosAndSize(0, 120, 8, 8);
        removeSlice.setPosAndSize(10, 120, 8, 8);
        nextSlice.setPosAndSize(20, 120, 8, 8);
        previousSlice.setPosAndSize(30, 120, 8, 8);
    }

    @Override
    public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
        super.drawBackground(graphics, theme, x, y, w, h);
        int oneFourth = w / 4;
        Color4I.BLACK.draw(graphics, oneFourth, y, 4, h);
        theme.drawHorizontalTab(graphics, x + 20, y, w, h, false);
        graphics.pose().pushPose();
        graphics.pose().scale(2, 2, 0);

        sliceControlBox.drawMainTexture(graphics, oneFourth + 5, y);
        sliceControlBox.drawOverlay(graphics, oneFourth + 5, y);

        for (SliceControlBox controlBox : sliceControlBoxes) {
            controlBox.drawOverlay(graphics, oneFourth + 5, y);
        }

        graphics.pose().popPose();
        List<EntityImageIcon.ChildIconData> slices = new ArrayList<>();
        for (SliceControlBox controlBox : sliceControlBoxes) {
            slices.add(new EntityImageIcon.ChildIconData(Optional.empty(), controlBox.createSlice(), Optional.of(new EntityImageIcon.Offset(controlBox.offsetXText.getIntValue(), controlBox.offsetYText.getIntValue()))));
        }
        new EntityImageIcon(sliceControlBox.texture, sliceControlBox.createSlice(), slices).draw(graphics, x + 2, y + 2, imageSizeX.getIntValue(), imageSizeY.getIntValue());
    }

    @Override
    public void drawForeground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
        super.drawForeground(graphics, theme, x, y, w, h);
    }

    // a "Control Box" that allows to create the overlays for the slices and move them around
    public static class SliceControlBox extends Panel {

        private final ImageIcon mainIcon;
        private final ResourceLocation texture;
        private final IntTextBox xText;
        private final IntTextBox yText;
        private final IntTextBox wText;
        private final IntTextBox hText;
        private final int imageWidth;
        private final int imageHeight;
        private final ColorButton colorButton;
        private final boolean offset;
        private final IntTextBox offsetXText;
        private final IntTextBox offsetYText;
        private final Button toggleOverlay;
        private boolean overlay = true;

        public SliceControlBox(Panel p, ResourceLocation texture, int imageWidth, int imageHeight, boolean offset) {
            super(p);
            this.colorButton = new ColorButton(this, Color4I.WHITE);
            this.mainIcon = new ImageIcon(texture);
            this.texture = texture;
            add(xText = new IntTextBox(this));
            xText.setAmount(0);
            add(yText = new IntTextBox(this));
            yText.setAmount(0);
            add(wText = new IntTextBox(this));
            add(hText = new IntTextBox(this));
            wText.setAmount(imageWidth);
            hText.setAmount(imageHeight);
            wText.setMinMax(1, imageWidth);
            hText.setMinMax(1, imageHeight);
            xText.setMinMax(0, imageWidth);
            yText.setMinMax(0, imageHeight);
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
            this.offset = offset;
            this.offsetXText = new IntTextBox(this);
            this.offsetYText = new IntTextBox(this);
            offsetXText.setAmount(0);
            offsetYText.setAmount(0);
            this.toggleOverlay = new ToggleableButton(this, overlay, Icons.ACCEPT, Icons.REMOVE, (button, newState) -> {
                overlay = newState;
            });
        }

        @Override
        public void addWidgets() {
            add(xText);
            add(yText);
            add(wText);
            add(hText);
            add(colorButton);
            add(toggleOverlay);
            if (offset) {
                add(offsetXText);
                add(offsetYText);
            }
        }

        @Override
        public void alignWidgets() {
            colorButton.setPosAndSize(50, 35, 20, 20);
            xText.setPosAndSize(0, 0, 20, 20);
            yText.setPosAndSize(25, 0, 20, 20);
            wText.setPosAndSize(50, 0, 20, 20);
            hText.setPosAndSize(75, 0, 20, 20);
            toggleOverlay.setPosAndSize(75, 35, 20, 20);
            if (offset) {
                offsetXText.setPosAndSize(0, 35, 20, 20);
                offsetYText.setPosAndSize(25, 35, 20, 20);
            }
        }

        @Override
        public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            super.drawBackground(graphics, theme, x, y, w, h);

            theme.drawString(graphics, "X", xText.getX() + 1, xText.getY() - theme.getFontHeight(), 0xFF000000);
            theme.drawString(graphics, "Y", yText.getX() + 1, yText.getY() - theme.getFontHeight(), 0xFF000000);
            theme.drawString(graphics, "W", wText.getX() + 1, wText.getY() - theme.getFontHeight(), 0xFF000000);
            theme.drawString(graphics, "H", hText.getX() + 1, hText.getY() - theme.getFontHeight(), 0xFF000000);

            if (offset) {
                theme.drawString(graphics, "OX", offsetXText.getX() + 1, offsetXText.getY() - theme.getFontHeight(), 0xFF000000);
                theme.drawString(graphics, "OY", offsetYText.getX() + 1, offsetYText.getY() - theme.getFontHeight(), 0xFF000000);
            }
        }

        public void drawMainTexture(GuiGraphics graphics, int x, int y) {
            mainIcon.draw(graphics, x, y, imageWidth, imageHeight);
        }

        public void drawOverlay(GuiGraphics graphics, int x, int y) {
            if (overlay) {
                colorButton.color4I.withAlpha(100).draw(graphics, x + xText.getIntValue(), y + yText.getIntValue(), wText.getIntValue(), hText.getIntValue());
            }
        }

        public EntityImageIcon.Slice createSlice() {
            return new EntityImageIcon.Slice(xText.getIntValue(), yText.getIntValue(), wText.getIntValue(), hText.getIntValue());
        }

        public int getW() {
            return wText.getIntValue();
        }

        public int getH() {
            return hText.getIntValue();
        }
    }


    private static class ColorButton extends SimpleButton {
        private Color4I color4I;

        public ColorButton(Panel panel, Color4I activeValue) {
            super(panel, Component.literal(""), Icons.COLOR_BLANK, null);
            color4I = activeValue;
            setConsumer((button, mouseButton) -> {
                int random = (int) Mth.randomBetween(Minecraft.getInstance().level.random, 0, 255);
                color4I = Color4I.get256(random);
            });
        }

        @Override
        public void drawIcon(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {

        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            super.draw(graphics, theme, x, y, w, h);
            theme.drawWidget(graphics, x, y, w, h, getWidgetType());
            Color4I.BLACK.withAlpha(125).draw(graphics, x + 2, y + 2, 12, 12);
            color4I.draw(graphics, x + 3, y + 3, 10, 10);
        }

    }

    private SimpleButton createExportButton() {
        return new SimpleButton(this, Component.literal("Export"), Icons.STAR, (button, mouseButton) -> {
            SliceCreationGUI bu = (SliceCreationGUI) button.getGui();
            EntityIcons.EntityIconSettings entityIconSettings = new EntityIcons.EntityIconSettings(
                    true,
                    Optional.empty(),
                    Optional.of(bu.sliceControlBox.createSlice()),
                    bu.sliceControlBoxes.stream().map(box -> new EntityImageIcon.ChildIconData(
                            Optional.empty(),
                            box.createSlice(),
                            Optional.of(new EntityImageIcon.Offset(box.offsetXText.getIntValue(), box.offsetYText.getIntValue())))).toList(),
                    EntityIcons.WidthHeight.DEFAULT,
                    1,
                    true);

            EntityIcons.EntityIconSettings.CODEC.encodeStart(JsonOps.INSTANCE, entityIconSettings).result().ifPresent(jsonElement -> {
                Path path = Platform.getGameFolder().resolve("export");
                try {
                    ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
                    String path1 = key.getNamespace() + "/" + key.getPath();
                    Path resolve = path.resolve(path1 + ".json");
                    if (!Files.exists(resolve.getParent())) {
                        Files.createDirectories(resolve.getParent());
                    }
                    Files.write(resolve, jsonElement.toString().getBytes());
                    SimpleToast.info(Component.literal("Saved File"), Component.empty());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

    private void loadExistingSettings() {
        EntityIcons.getSettings(entityType).ifPresent(settings -> {
            settings.mainSlice().ifPresent(slice -> {
                sliceControlBox.xText.setAmount(slice.x());
                sliceControlBox.yText.setAmount(slice.y());
                sliceControlBox.wText.setAmount(slice.width());
                sliceControlBox.hText.setAmount(slice.height());
            });
            settings.children().forEach(child -> {
                SliceControlBox sliceControlBox = new SliceControlBox(this, texture, textureWidth, textureHeight, true);
                EntityImageIcon.Slice slice1 = child.slice();
                sliceControlBox.xText.setAmount(slice1.x());
                sliceControlBox.yText.setAmount(slice1.y());
                sliceControlBox.wText.setAmount(slice1.width());
                sliceControlBox.hText.setAmount(slice1.height());
                child.offset().ifPresent(offset -> {
                    sliceControlBox.offsetXText.setAmount(offset.x());
                    sliceControlBox.offsetYText.setAmount(offset.y());
                });
                sliceControlBoxes.add(sliceControlBox);
                activeSlice = sliceControlBox;
            });
        });

    }

    private Pair<Integer, Integer> getTextureSize() {
        SimpleTexture.TextureImage textureImage = SimpleTexture.TextureImage.load(Minecraft.getInstance().getResourceManager(), texture);
        int imageWidth;
        int imageHeight;
        try {
            imageWidth = textureImage.getImage().getWidth();
            imageHeight = textureImage.getImage().getHeight();
            return Pair.of(imageWidth, imageHeight);
        } catch (Exception e) {
            LOGGER.error("Failed to get texture size for {}", texture, e);
            throw new RuntimeException(e);
        }
    }
}
