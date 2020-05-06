package com.feed_the_beast.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbchunks.client.map.ClientMapDimension;
import com.feed_the_beast.mods.ftbchunks.impl.map.XZ;
import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import com.feed_the_beast.mods.ftbguilibrary.widget.GuiHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.TextureUtil;

import java.nio.file.Files;

/**
 * @author LatvianModder
 */
public class RegionTextureData
{
	public final ClientMapDimension dimension;
	public final XZ pos;
	public int id;
	public boolean loaded;

	public RegionTextureData(LargeMapScreen l, XZ p)
	{
		dimension = l.dimension;
		pos = p;
		id = 0;
		loaded = false;
	}

	public void draw(int x, int y, int w, int h)
	{
		if (id == 0)
		{
			id = TextureUtil.generateTextureId();

			FTBChunksClient.taskQueue.addLast(() -> {
				try
				{
					final NativeImage image = NativeImage.read(Files.newInputStream(dimension.directory.resolve(pos.x + "," + pos.z + ",map.png")));

					for (int iy = 0; iy < 512; iy++)
					{
						for (int ix = 0; ix < 512; ix++)
						{
							if (image.getPixelRGBA(ix, iy) == 0xFF000000)
							{
								image.setPixelRGBA(ix, iy, 0);
							}
						}
					}

					Minecraft.getInstance().runAsync(() -> {
						if (id != 0)
						{
							TextureUtil.prepareImage(id, 512, 512);
							image.uploadTextureSub(0, 0, 0, false);
							loaded = true;
						}

						image.close();
					});
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			});
		}

		if (loaded)
		{
			RenderSystem.bindTexture(id);
			GuiHelper.drawTexturedRect(x, y, w, h, Color4I.WHITE, 0F, 0F, 1F, 1F);
		}
	}

	public void release()
	{
		if (loaded && id != 0)
		{
			TextureUtil.releaseTextureId(id);
			loaded = false;
			id = 0;
		}
	}
}