package dev.ftb.mods.ftbchunks.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.OptionalDouble;

/**
 * @author LatvianModder
 */
public class FTBChunksRenderTypes extends RenderStateShard {
	public static final ResourceLocation WAYPOINT_BEAM = new ResourceLocation("ftbchunks:textures/waypoint_beam.png");

	public static final RenderType WAYPOINTS_DEPTH = RenderType.create("ftbchunks_waypoints_depth", DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 256, false, false, RenderType.CompositeState.builder()
			.setLineState(new LineStateShard(OptionalDouble.empty()))
			.setLayeringState(NO_LAYERING)
			.setTextureState(new TextureStateShard(WAYPOINT_BEAM, true, false))
			.setShaderState(RENDERTYPE_BEACON_BEAM_SHADER)
			.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
			.setWriteMaskState(COLOR_WRITE)
			.setCullState(CULL)
			.createCompositeState(false));

	private FTBChunksRenderTypes(String s, Runnable r0, Runnable r1) {
		super(s, r0, r1);
	}
}