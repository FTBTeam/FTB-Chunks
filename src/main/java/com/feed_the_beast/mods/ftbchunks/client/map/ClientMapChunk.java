package com.feed_the_beast.mods.ftbchunks.client.map;

import com.feed_the_beast.mods.ftbchunks.impl.map.XZ;
import net.minecraft.client.renderer.texture.NativeImage;

import java.util.Date;

/**
 * @author LatvianModder
 */
public class ClientMapChunk
{
	public final ClientMapRegion region;
	public final XZ pos;

	public long relativeTimeClaimed;
	public long relativeTimeForceLoaded;
	public Date claimedDate;
	public Date forceLoadedDate;
	public int color;
	public String formattedOwner;

	public ClientMapChunk(ClientMapRegion r, XZ p)
	{
		region = r;
		pos = p;

		relativeTimeClaimed = 0L;
		relativeTimeForceLoaded = 0L;
		claimedDate = null;
		forceLoadedDate = null;
		color = 0;
		formattedOwner = "";
	}

	public int getRGB(int x, int z)
	{
		int c = region.getImage().getPixelRGBA(pos.x * 32 + x, pos.z * 32 + z);
		int r = NativeImage.getRed(c);
		int g = NativeImage.getGreen(c);
		int b = NativeImage.getBlue(c);
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}
}