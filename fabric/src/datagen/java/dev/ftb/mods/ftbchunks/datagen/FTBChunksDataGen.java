package dev.ftb.mods.ftbchunks.datagen;

import com.mojang.logging.LogUtils;
import dev.ftb.mods.ftbchunks.datagen.tags.BlockTagGenerator;
import dev.ftb.mods.ftbchunks.datagen.tags.EntityTagGenerator;
import dev.ftb.mods.ftbchunks.datagen.tags.ItemTagGenerator;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import org.slf4j.Logger;

public class FTBChunksDataGen implements DataGeneratorEntrypoint {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        LOGGER.info("FTB Chunks DataGen Initialized");
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider(BlockTagGenerator::new);
        pack.addProvider(EntityTagGenerator::new);
        pack.addProvider(ItemTagGenerator::new);
        pack.addProvider(LangGenerator::new);
    }
}
