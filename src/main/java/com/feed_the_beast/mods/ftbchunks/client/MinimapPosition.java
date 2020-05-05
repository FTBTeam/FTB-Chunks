package com.feed_the_beast.mods.ftbchunks.client;

/**
 * @author LatvianModder
 */
public enum MinimapPosition
{
	DISABLED(-1, -1),
	BOTTOM_LEFT(0, 2),
	LEFT(0, 1),
	TOP_LEFT(0, 0),
	TOP_RIGHT(1, 0),
	RIGHT(1, 1),
	BOTTOM_RIGHT(1, 2);

	public final int posX;
	public final int posY;

	MinimapPosition(int x, int y)
	{
		posX = x;
		posY = y;
	}

	public int getX(int w, int s)
	{
		if (posX == 0)
		{
			return 3;
		}

		return w - s - 3;
	}

	public int getY(int h, int s)
	{
		if (posY == 0)
		{
			return 3;
		}
		else if (posY == 1)
		{
			return (h - s) / 2;
		}

		return h - s - 3;
	}
}