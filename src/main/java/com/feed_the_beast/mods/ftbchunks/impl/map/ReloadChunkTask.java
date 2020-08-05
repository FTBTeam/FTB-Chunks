package com.feed_the_beast.mods.ftbchunks.impl.map;

import com.feed_the_beast.mods.ftbchunks.api.ChunkDimPos;
import com.feed_the_beast.mods.ftbchunks.impl.FTBChunksAPIImpl;
import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import net.minecraft.block.BlockState;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.block.GrassBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;

import java.util.function.Predicate;

/**
 * @author LatvianModder
 */
public class ReloadChunkTask implements MapTask
{
	public interface Callback
	{
		void accept(ReloadChunkTask task, boolean changed);
	}

	public static int reduce(int c)
	{
		float f = (c & 0xFF) / 255F;
		int c1 = (int) (f * 64F);
		float f1 = c1 / 64F;
		return (int) (f1 * 255F);
	}

	public final World world;
	public final XZ chunkPosition;
	private final Callback callback;

	public ReloadChunkTask(World w, XZ pos, Callback c)
	{
		world = w;
		chunkPosition = pos;
		callback = c;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void run()
	{
		String dimId = ChunkDimPos.getID(world);

		if (dimId.isEmpty())
		{
			return;
		}

		MapChunk data = FTBChunksAPIImpl.manager.map.getChunk(dimId, chunkPosition);
		XZ c = data.getActualPos();
		IChunk ichunk = world.getChunk(c.x, c.z, ChunkStatus.FULL, true);

		if (!(ichunk instanceof Chunk))
		{
			return;
		}

		Chunk chunk = (Chunk) ichunk;

		BlockPos.Mutable currentBlockPos = new BlockPos.Mutable(0, 0, 0);
		int x = c.x << 4;
		int z = c.z << 4;
		boolean changed = false;

		try
		{
			int topY = world.func_234938_ad_() + 1;

			for (int wi = 0; wi < 256; wi++)
			{
				int wx = wi % 16;
				int wz = wi / 16;
				int by = MapChunk.getHeight(chunk, currentBlockPos, x + wx, z + wz, topY);

				if (by == -1)
				{
					data.setHRGB(wx, wz, 0);
					continue;
				}

				BlockState state = chunk.getBlockState(currentBlockPos);

				Color4I color = null;

				if ((state.getBlock() instanceof FlowingFluidBlock && ((FlowingFluidBlock) state.getBlock()).getFluid().isEquivalentTo(Fluids.WATER)) || FTBChunksAPIImpl.MAP_IGNORE_IN_WATER_TAG.contains(state.getBlock()))
				{
					for (int depth = 1; depth < 50; depth++)
					{
						currentBlockPos.setPos(x + wx, by - depth, z + wz);
						BlockState state1 = chunk.getBlockState(currentBlockPos);

						if (state1.getBlock() instanceof FlowingFluidBlock || FTBChunksAPIImpl.MAP_IGNORE_IN_WATER_TAG.contains(state1.getBlock()))
						{
							continue;
						}

						Color4I fluidColor = Color4I.rgb(ColorBlend.WATER.blend(world, currentBlockPos, 2)).withAlpha(220);

						color = FTBChunksAPIImpl.COLOR_MAP.get(state1.getBlock());

						if (color == null)
						{
							color = Color4I.rgb(state1.getMaterialColor(world, currentBlockPos).colorValue);
						}

						color = color.withTint(fluidColor);
						break;
					}
				}
				else
				{
					if (state.getBlock() instanceof GrassBlock)
					{
						color = Color4I.rgb(ColorBlend.GRASS.blend(world, currentBlockPos, 2)).withTint(Color4I.BLACK.withAlpha(50));
					}
					else if (state.getBlock() instanceof LeavesBlock || state.getBlock() instanceof VineBlock)
					{
						color = Color4I.rgb(ColorBlend.FOLIAGE.blend(world, currentBlockPos, 2)).withTint(Color4I.BLACK.withAlpha(50));
					}
					else if (state.getBlock() instanceof RedstoneWireBlock)
					{
						color = Color4I.rgb(redstoneColor(state.get(RedstoneWireBlock.POWER)));
					}
				}

				if (color == null)
				{
					color = FTBChunksAPIImpl.COLOR_MAP.get(state.getBlock());
				}

				if (color == null)
				{
					MaterialColor materialColor = state.getMaterialColor(world, currentBlockPos);
					color = materialColor == null ? Color4I.RED : Color4I.rgb(materialColor.colorValue);
				}

				if (data.setHRGB(wx, wz, (by << 24) | color.rgb()))
				{
					changed = true;
				}
			}

			// FTBChunks.LOGGER.debug("Reloaded chunk " + data.pos + " in " + data.region.pos + " (" + FTBChunksAPIImpl.manager.map.taskQueue.size() + " tasks left)");
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}

		callback.accept(this, changed);
	}

	private static int redstoneColor(int power)
	{
		float f = power / 15F;
		float f1 = f * 0.6F + 0.4F;
		if (power == 0)
		{
			f1 = 0.3F;
		}

		float f2 = f * f * 0.7F - 0.5F;
		float f3 = f * f * 0.6F - 0.7F;

		if (f2 < 0F)
		{
			f2 = 0F;
		}

		if (f3 < 0F)
		{
			f3 = 0F;
		}

		int i = MathHelper.clamp((int) (f1 * 255F), 0, 255);
		int j = MathHelper.clamp((int) (f2 * 255F), 0, 255);
		int k = MathHelper.clamp((int) (f3 * 255F), 0, 255);
		return 0xFF000000 | i << 16 | j << 8 | k;
	}

	public void send(Predicate<ServerPlayerEntity> predicate)
	{
		FTBChunksAPIImpl.manager.map.queueSend(world, chunkPosition, predicate);
	}
}