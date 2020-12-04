package com.feed_the_beast.mods.ftbchunks.client.map;

import com.feed_the_beast.mods.ftbchunks.ColorMapLoader;
import com.feed_the_beast.mods.ftbchunks.core.BlockStateFTBC;
import com.feed_the_beast.mods.ftbchunks.impl.XZ;
import com.feed_the_beast.mods.ftbchunks.net.SendChunkPacket;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.chunk.IChunk;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class MapChunk
{
	public final MapRegion region;
	public final XZ pos;
	public long modified;

	public UUID ownerId;
	public Date claimedDate;
	public Date forceLoadedDate;
	public int color;
	public ITextComponent owner;

	public MapChunk(MapRegion r, XZ p)
	{
		region = r;
		pos = p;
		modified = 0L;

		ownerId = null;
		claimedDate = null;
		forceLoadedDate = null;
		color = 0;
		owner = StringTextComponent.EMPTY;
	}

	public int getBlock(int x, int z)
	{
		return region.getBlockImage().getPixelRGBA(pos.x * 16 + (x & 15), pos.z * 16 + (z & 15));
	}

	public int getData(int x, int z)
	{
		return region.getDataImage().getPixelRGBA(pos.x * 16 + (x & 15), pos.z * 16 + (z & 15));
	}

	public boolean connects(MapChunk chunk)
	{
		return (color & 0xFFFFFF) == (chunk.color & 0xFFFFFF) && Objects.equals(ownerId, chunk.ownerId);
	}

	public XZ getActualPos()
	{
		return XZ.of((region.pos.x << 5) + pos.x, (region.pos.z << 5) + pos.z);
	}

	public int getHeight(int x, int z)
	{
		return (getData(x, z) >> 16) & 0xFF;
	}

	public static boolean isWater(BlockState state)
	{
		return state instanceof BlockStateFTBC ? ((BlockStateFTBC) state).getFTBCIsWater() : state.getFluidState().getFluid().isEquivalentTo(Fluids.WATER);
	}

	public static boolean skipBlock(BlockState state)
	{
		return ColorMapLoader.getBlockColor(state.getBlock()).isIgnored();
	}

	public static BlockPos.Mutable setHeight(@Nullable IChunk chunk, BlockPos.Mutable pos, boolean[] flags)
	{
		int topY = pos.getY();

		if (topY == -1 || chunk == null || chunk.getWorldForge() == null)
		{
			pos.setY(-1);
			return pos;
		}

		for (int by = topY; by > 0; by--)
		{
			pos.setY(by);
			BlockState state = chunk.getBlockState(pos);

			if (by == topY || state.getBlock() == Blocks.BEDROCK)
			{
				for (; by > 0; by--)
				{
					pos.setY(by);
					state = chunk.getBlockState(pos);

					if (state.getBlock().isAir(state, chunk.getWorldForge(), pos))
					{
						break;
					}
				}
			}

			boolean water = isWater(state);
			flags[0] |= water;

			if (!water && !skipBlock(state))
			{
				pos.setY(by);
				return pos;
			}
		}

		pos.setY(-1);
		return pos;
	}

	public MapChunk created()
	{
		region.update(true);
		return this;
	}

	public MapChunk offset(int x, int z)
	{
		return region.dimension.getChunk(getActualPos().offset(x, z));
	}

	public void updateFrom(Date now, SendChunkPacket.SingleChunk packet)
	{
		ownerId = packet.ownerId;
		claimedDate = packet.owner == null ? null : new Date(now.getTime() - packet.relativeTimeClaimed);
		forceLoadedDate = packet.forceLoaded && claimedDate != null ? new Date(now.getTime() - packet.relativeTimeForceLoaded) : null;
		color = packet.color;
		owner = packet.owner == null ? StringTextComponent.EMPTY : packet.owner;
		region.update(false);
	}
}