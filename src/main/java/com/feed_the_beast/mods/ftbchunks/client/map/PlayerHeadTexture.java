package com.feed_the_beast.mods.ftbchunks.client.map;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * @author LatvianModder
 */
public class PlayerHeadTexture extends SimpleTexture
{
	private final String imageUrl;
	@Nullable
	private CompletableFuture<?> future;
	private boolean textureUploaded;

	public PlayerHeadTexture(String imageUrlIn, ResourceLocation textureResourceLocation)
	{
		super(textureResourceLocation);
		this.imageUrl = imageUrlIn;
	}

	private void upload(NativeImage imageIn)
	{
		TextureUtil.prepareImage(this.getGlTextureId(), imageIn.getWidth(), imageIn.getHeight());
		imageIn.uploadTextureSub(0, 0, 0, true);
	}

	public void loadTexture(IResourceManager manager)
	{
		Minecraft.getInstance().execute(() -> {
			if (!this.textureUploaded)
			{
				try
				{
					super.loadTexture(manager);
				}
				catch (IOException ioexception)
				{
				}

				this.textureUploaded = true;
			}

		});
		if (this.future == null)
		{
			this.future = CompletableFuture.runAsync(() -> {

				try
				{
					HttpURLConnection httpurlconnection = (HttpURLConnection) (new URL(this.imageUrl)).openConnection(Minecraft.getInstance().getProxy());
					httpurlconnection.setDoInput(true);
					httpurlconnection.setDoOutput(false);
					httpurlconnection.connect();
					if (httpurlconnection.getResponseCode() / 100 == 2)
					{
						InputStream inputstream = httpurlconnection.getInputStream();

						Minecraft.getInstance().execute(() -> {
							try
							{
								NativeImage img = NativeImage.read(inputstream);

								if (img != null)
								{
									this.textureUploaded = true;
									if (!RenderSystem.isOnRenderThread())
									{
										RenderSystem.recordRenderCall(() -> upload(img));
									}
									else
									{
										upload(img);
									}
									httpurlconnection.disconnect();
								}
							}
							catch (Exception ex)
							{
								ex.printStackTrace();
							}
						});
					}
				}
				catch (Exception exception)
				{
				}
			}, Util.getServerExecutor());
		}
	}
}