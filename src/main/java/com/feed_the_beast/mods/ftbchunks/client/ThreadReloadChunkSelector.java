package com.feed_the_beast.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import com.feed_the_beast.mods.ftbguilibrary.icon.Icon;
import com.feed_the_beast.mods.ftbguilibrary.utils.MathUtils;
import com.feed_the_beast.mods.ftbguilibrary.utils.PixelBuffer;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author LatvianModder
 */
public class ThreadReloadChunkSelector extends Thread
{
	private static ByteBuffer pixelBuffer = null;
	private static final int PIXEL_SIZE = FTBChunks.TILE_SIZE * 16;
	private static final PixelBuffer PIXELS = new PixelBuffer(PIXEL_SIZE, PIXEL_SIZE);
	private static Map<Block, Color4I> COLOR_CACHE = null;
	private static final BlockPos.Mutable CURRENT_BLOCK_POS = new BlockPos.Mutable(0, 0, 0);
	private static World world = null;
	private static ThreadReloadChunkSelector instance;
	private static int textureID = -1;
	private static final int[] HEIGHT_MAP = new int[PIXEL_SIZE * PIXEL_SIZE];

	static int getTextureId()
	{
		if (textureID == -1)
		{
			textureID = TextureUtil.generateTextureId();
		}

		return textureID;
	}

	static void updateTexture()
	{
		if (pixelBuffer != null)
		{
			//boolean hasBlur = false;
			//int filter = hasBlur ? GL11.GL_LINEAR : GL11.GL_NEAREST;
			RenderSystem.bindTexture(getTextureId());
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, FTBChunks.TILE_SIZE * 16, FTBChunks.TILE_SIZE * 16, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixelBuffer);
			pixelBuffer = null;
		}
	}

	static void reloadArea(World w, int sx, int sz)
	{
		if (instance != null)
		{
			instance.cancelled = true;
			instance = null;
		}

		//COLOR_CACHE = null;
		instance = new ThreadReloadChunkSelector(w, sx, sz);
		instance.cancelled = false;
		instance.start();
	}

	private final int startX, startZ;
	private boolean cancelled = false;

	private ThreadReloadChunkSelector(World w, int sx, int sz)
	{
		super("ChunkSelectorAreaReloader");
		setDaemon(true);
		world = w;
		startX = sx;
		startZ = sz;
	}

	private static int getHeight(int x, int z)
	{
		int index = x + z * PIXEL_SIZE;
		return index < 0 || index >= HEIGHT_MAP.length ? -1 : HEIGHT_MAP[index];
	}

	@Override
	public void run()
	{
		//Arrays.fill(PIXELS.getPixels(), Color4I.rgb(world.getSkyColor(Minecraft.getInstance().player, 0)).rgba());
		Arrays.fill(PIXELS.getPixels(), 0xFF000000);
		Arrays.fill(HEIGHT_MAP, -1);
		pixelBuffer = PIXELS.toByteBuffer(false);

		Chunk chunk;
		int x, z, wi, wx, wz, by, topY;
		Color4I color;
		BlockState state;

		int startY = Minecraft.getInstance().player.getPosition().getY();

		try
		{
			for (int index = 0; index < FTBChunks.TILES * FTBChunks.TILES + 1; index++)
			{
				World w = world;

				if (w == null)
				{
					break;
				}

				ChunkPos pos = MathUtils.getSpiralPoint(index);
				int cx = pos.x + FTBChunks.TILE_OFFSET;
				int cz = pos.z + FTBChunks.TILE_OFFSET;

				chunk = w.getChunkProvider().getChunk(startX + cx, startZ + cz, false);

				if (chunk != null)
				{
					x = (startX + cx) << 4;
					z = (startZ + cz) << 4;
					topY = w.getDimension().getType() == DimensionType.THE_NETHER ? startY + 5 : Math.max(w.getActualHeight(), chunk.getTopFilledSegment() + 15);

					for (wi = 0; wi < 256; wi++)
					{
						wx = wi % 16;
						wz = wi / 16;

						for (by = topY; by > 0; --by)
						{
							if (cancelled)
							{
								return;
							}

							CURRENT_BLOCK_POS.setPos(x + wx, by, z + wz);
							//state = chunk.getBlockState(wx, by, wz);
							state = chunk.getBlockState(CURRENT_BLOCK_POS);

							if (state.getBlock() != Blocks.GRASS && state.getBlock() != Blocks.TALL_GRASS && !state.getBlock().isAir(state, w, CURRENT_BLOCK_POS))
							{
								HEIGHT_MAP[(cx * 16 + wx) + (cz * 16 + wz) * PIXEL_SIZE] = by;
								break;
							}
						}
					}
				}
			}

			for (int index = 0; index < FTBChunks.TILES * FTBChunks.TILES + 1; index++)
			{
				World w = world;

				if (w == null)
				{
					break;
				}

				ChunkPos pos = MathUtils.getSpiralPoint(index);
				int cx = pos.x + FTBChunks.TILE_OFFSET;
				int cz = pos.z + FTBChunks.TILE_OFFSET;

				chunk = w.getChunkProvider().getChunk(startX + cx, startZ + cz, false);

				if (chunk == null)
				{
					continue;
				}

				x = (startX + cx) << 4;
				z = (startZ + cz) << 4;

				for (wi = 0; wi < 256; wi++)
				{
					wx = wi % 16;
					wz = wi / 16;
					by = getHeight(cx * 16 + wx, cz * 16 + wz);

					if (by < 0)
					{
						continue;
					}

					CURRENT_BLOCK_POS.setPos(x + wx, by, z + wz);
					//state = chunk.getBlockState(wx, by, wz);
					state = chunk.getBlockState(CURRENT_BLOCK_POS);

					color = getBlockColor0(state.getBlock());

					if (color.isEmpty())
					{
						color = Color4I.rgb(state.getMaterialColor(world, CURRENT_BLOCK_POS).colorValue);
					}

					int bn = getHeight(cx * 16 + wx, cz * 16 + wz - 1);
					int bw = getHeight(cx * 16 + wx - 1, cz * 16 + wz);

					if (by > bn && bn != -1 || by > bw && bw != -1)
					{
						color = color.addBrightness(0.1F);
					}

					if (by < bn && bn != -1 || by < bw && bw != -1)
					{
						color = color.addBrightness(-0.1F);
					}

					PIXELS.setRGB(cx * 16 + wx, cz * 16 + wz, color.addBrightness(MathUtils.RAND.nextFloat() * 0.04F).rgba());
				}

				pixelBuffer = PIXELS.toByteBuffer(false);
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}

		pixelBuffer = PIXELS.toByteBuffer(false);
		world = null;
		instance = null;
	}

	private Color4I getBlockColor0(Block block)
	{
		if (COLOR_CACHE == null)
		{
			COLOR_CACHE = new HashMap<>();

			try (Reader reader = new BufferedReader(new InputStreamReader(ThreadReloadChunkSelector.class.getResourceAsStream("/data/ftbchunks/ftbchunks_colors.json"))))
			{
				for (Map.Entry<String, JsonElement> entry : new Gson().fromJson(reader, JsonObject.class).entrySet())
				{
					Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(entry.getKey()));

					if (b != null && b != Blocks.AIR)
					{
						COLOR_CACHE.put(b, Color4I.fromJson(entry.getValue()));
					}
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}

		return COLOR_CACHE.getOrDefault(block, Icon.EMPTY);
	}
}