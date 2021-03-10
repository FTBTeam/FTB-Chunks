package com.feed_the_beast.mods.ftbchunks.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.OptionalDouble;

/**
 * @author LatvianModder
 */
public class FTBChunksRenderTypes extends RenderStateShard {
	public static final ResourceLocation WAYPOINT_BEAM = new ResourceLocation("ftbchunks:textures/waypoint_beam.png");

	public static final RenderType WAYPOINTS_DEPTH = RenderType.create("ftbchunks_waypoints_depth", DefaultVertexFormat.POSITION_COLOR_TEX, GL11.GL_QUADS, 256, RenderType.CompositeState.builder()
			.setLineState(new LineStateShard(OptionalDouble.empty()))
			.setLayeringState(NO_LAYERING)
			.setTextureState(new TextureStateShard(WAYPOINT_BEAM, true, false))
			.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
			.setAlphaState(DEFAULT_ALPHA)
			.setWriteMaskState(COLOR_WRITE)
			.setCullState(CULL)
			.setShadeModelState(SMOOTH_SHADE)
			.createCompositeState(false));

	private FTBChunksRenderTypes(String s, Runnable r0, Runnable r1) {
		super(s, r0, r1);
	}
}