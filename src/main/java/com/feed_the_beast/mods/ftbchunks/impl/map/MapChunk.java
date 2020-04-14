package com.feed_the_beast.mods.ftbchunks.impl.map;

/**
 * @author LatvianModder
 */
public class MapChunk
{
	public static int index(int x, int z)
	{
		return (x & 0xF) + (z & 0xF) * 16;
	}

	public final MapRegion region;
	public final XZ pos;
	public final byte[] height, red, green, blue;
	public boolean needsMapUpdate;
	public long lastUpdate;

	MapChunk(MapRegion r, XZ p)
	{
		region = r;
		pos = p;
		height = new byte[256];
		red = new byte[256];
		green = new byte[256];
		blue = new byte[256];
		needsMapUpdate = true;
	}

	public int getHeight(int x, int z)
	{
		return height[index(x, z)] & 0xFF;
	}

	public boolean setHeight(int x, int z, int h)
	{
		byte b = (byte) h;
		int i = index(x, z);

		if (height[i] != b)
		{
			height[i] = b;
			region.save = true;
			return true;
		}

		return false;
	}

	public int getRGB(int x, int z)
	{
		int i = index(x, z);
		int r = red[i] & 0xFF;
		int g = green[i] & 0xFF;
		int b = blue[i] & 0xFF;
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	public int getHRGB(int x, int z)
	{
		int i = index(x, z);
		int h = height[i] & 0xFF;
		int r = red[i] & 0xFF;
		int g = green[i] & 0xFF;
		int b = blue[i] & 0xFF;
		return (h << 24) | (r << 16) | (g << 8) | b;
	}

	public boolean setRGB(int x, int z, int rgb)
	{
		int i = index(x, z);
		byte r = (byte) (rgb >> 16);
		byte g = (byte) (rgb >> 8);
		byte b = (byte) rgb;

		if (red[i] != r || green[i] != g || blue[i] != b)
		{
			red[i] = r;
			green[i] = g;
			blue[i] = b;
			region.save = true;
			return true;
		}

		return false;
	}
}