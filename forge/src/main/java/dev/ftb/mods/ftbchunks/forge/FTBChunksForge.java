package dev.ftb.mods.ftbchunks.forge;

import dev.ftb.mods.ftbchunks.FTBChunks;
import me.shedaniel.architectury.platform.forge.EventBuses;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FTBChunks.MOD_ID)
@Mod.EventBusSubscriber
public class FTBChunksForge {
	public FTBChunksForge() {
		EventBuses.registerModEventBus(FTBChunks.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
		FTBChunks.instance = new FTBChunks();
	}

	@SubscribeEvent
	public static void fillBucket(FillBucketEvent event) {
		InteractionResultHolder<ItemStack> result = FTBChunks.instance.fillBucket(event.getPlayer(), event.getEmptyBucket(), event.getTarget());

		if (result.getResult() != InteractionResult.PASS) {
			if (result.getResult().consumesAction()) {
				event.setFilledBucket(result.getObject());
			}

			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void farmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
		if (FTBChunks.instance.farmlandTrample(event.getEntity(), event.getPos(), event.getState(), event.getFallDistance()) != InteractionResult.PASS) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void chunkChange(EntityEvent.EnteringChunk event) {
		FTBChunks.instance.chunkChange(event.getEntity(), new ChunkPos(event.getOldChunkX(), event.getOldChunkZ()), new ChunkPos(event.getNewChunkX(), event.getNewChunkZ()));
	}

	@SubscribeEvent
	public static void mobSpawned(LivingSpawnEvent.CheckSpawn event) {
		InteractionResult result = FTBChunks.instance.mobSpawned(event.getWorld(), event.getEntityLiving(), event.getSpawner(), event.getSpawnReason(), new Vec3(event.getX(), event.getY(), event.getZ()));

		if (result.consumesAction()) {
			event.setResult(Event.Result.ALLOW);
		} else if (result == InteractionResult.FAIL) {
			event.setResult(Event.Result.DENY);
		}
	}
}
