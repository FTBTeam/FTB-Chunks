package com.feed_the_beast.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbchunks.impl.map.XZ;
import com.feed_the_beast.mods.ftbguilibrary.widget.Theme;
import com.feed_the_beast.mods.ftbguilibrary.widget.Widget;

/**
 * @author LatvianModder
 */
public class RegionMapButton extends Widget
{
	public final XZ pos;
	public final RegionTextureData tex;

	public RegionMapButton(RegionMapPanel pa, XZ ps)
	{
		super(pa);
		pos = ps;
		tex = pa.largeMap.regionTextures.computeIfAbsent(pos, p -> new RegionTextureData(pa.largeMap, p));
	}

	@Override
	public void draw(Theme theme, int x, int y, int w, int h)
	{
		tex.draw(x, y, w, h);
	}
}