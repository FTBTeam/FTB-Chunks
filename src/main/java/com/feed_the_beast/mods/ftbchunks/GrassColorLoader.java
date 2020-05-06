package com.feed_the_beast.mods.ftbchunks;

import net.minecraft.client.resources.ReloadListener;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.io.InputStream;

/**
 * @author LatvianModder
 */
public class GrassColorLoader extends ReloadListener<int[]>
{
	public static int[] map = new int[0];

	@Override
	protected int[] prepare(IResourceManager resourceManager, IProfiler profiler)
	{
		try (InputStream stream = resourceManager.getResource(new ResourceLocation("ftbchunks", "grass.png")).getInputStream())
		{
			return ImageIO.read(stream).getRGB(0, 0, 256, 256, null, 0, 256);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}

		return new int[0];
	}

	@Override
	protected void apply(int[] object, IResourceManager resourceManager, IProfiler profiler)
	{
		map = object;
	}
}