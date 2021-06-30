package dev.ftb.mods.ftbchunks.client.map;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * @author LatvianModder
 */
public class PlayerHeadTexture extends SimpleTexture {
	private final String imageUrl;
	@Nullable
	private CompletableFuture<?> future;
	private boolean textureUploaded;

	public PlayerHeadTexture(String imageUrlIn, ResourceLocation textureResourceLocation) {
		super(textureResourceLocation);
		this.imageUrl = imageUrlIn;
	}

	private void upload(NativeImage imageIn) {
		TextureUtil.prepareImage(getId(), imageIn.getWidth(), imageIn.getHeight());
		imageIn.upload(0, 0, 0, true);
	}

	@Override
	public void load(ResourceManager manager) {
		Minecraft.getInstance().execute(() -> {
			if (!this.textureUploaded) {
				try {
					super.load(manager);
				} catch (IOException ioexception) {
				}

				this.textureUploaded = true;
			}

		});
		if (this.future == null) {
			this.future = CompletableFuture.runAsync(() -> {

				try {
					HttpURLConnection httpurlconnection = (HttpURLConnection) (new URL(this.imageUrl)).openConnection(Minecraft.getInstance().getProxy());
					httpurlconnection.setDoInput(true);
					httpurlconnection.setDoOutput(false);
					httpurlconnection.connect();
					if (httpurlconnection.getResponseCode() / 100 == 2) {
						InputStream inputstream = httpurlconnection.getInputStream();

						Minecraft.getInstance().execute(() -> {
							try {
								NativeImage img = NativeImage.read(inputstream);

								if (img != null) {
									this.textureUploaded = true;
									if (!RenderSystem.isOnRenderThread()) {
										RenderSystem.recordRenderCall(() -> upload(img));
									} else {
										upload(img);
									}
									httpurlconnection.disconnect();
								}
							} catch (Exception ex) {
								ex.printStackTrace();
							}
						});
					}
				} catch (Exception exception) {
				}
			}, Util.backgroundExecutor());
		}
	}
}