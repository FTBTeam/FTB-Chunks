package com.feed_the_beast.mods.ftbchunks.client;

import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.util.OptionalDouble;

/**
 * @author LatvianModder
 */
public class FTBChunksRenderTypes extends RenderState
{
	public static final RenderType WAYPOINTS = RenderType.makeType("ftbchunks_waypoints", DefaultVertexFormats.POSITION_COLOR, GL11.GL_QUADS, 256, RenderType.State.getBuilder()
			.line(new LineState(OptionalDouble.empty()))
			.layer(NO_LAYERING)
			.transparency(TRANSLUCENT_TRANSPARENCY)
			.writeMask(COLOR_DEPTH_WRITE)
			.cull(CULL_ENABLED)
			.depthTest(DEPTH_ALWAYS)
			.build(false));

	private FTBChunksRenderTypes(String s, Runnable r0, Runnable r1)
	{
		super(s, r0, r1);
	}
}