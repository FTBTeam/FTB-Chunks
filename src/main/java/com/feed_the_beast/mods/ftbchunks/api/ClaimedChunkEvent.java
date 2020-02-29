package com.feed_the_beast.mods.ftbchunks.api;

import net.minecraft.command.CommandSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;

/**
 * @author LatvianModder
 */
public class ClaimedChunkEvent extends Event
{
	private final CommandSource source;
	private final ClaimedChunk chunk;
	private final boolean checking;
	private ClaimResult claimResult;

	public ClaimedChunkEvent(CommandSource s, ClaimedChunk c, boolean p)
	{
		source = s;
		chunk = c;
		checking = p;
		claimResult = c;
	}

	public CommandSource getSource()
	{
		return source;
	}

	public ClaimedChunk getChunk()
	{
		return chunk;
	}

	@Override
	public boolean isCancelable()
	{
		return checking;
	}

	public void setClaimResult(ClaimResult r)
	{
		claimResult = r;
	}

	@Override
	public void setCanceled(boolean cancel)
	{
		super.setCanceled(cancel);

		if (cancel && claimResult.isSuccess())
		{
			claimResult = ClaimResults.OTHER;
		}
	}

	public ClaimResult postAndGetResult()
	{
		MinecraftForge.EVENT_BUS.post(this);
		return claimResult;
	}

	public static class Claim extends ClaimedChunkEvent
	{
		private Claim(CommandSource s, ClaimedChunk a, boolean p)
		{
			super(s, a, p);
		}

		public static class Check extends Unclaim
		{
			public Check(CommandSource s, ClaimedChunk a)
			{
				super(s, a, true);
			}
		}

		public static class Done extends Unclaim
		{
			public Done(CommandSource s, ClaimedChunk a)
			{
				super(s, a, false);
			}
		}
	}

	public static class Unclaim extends ClaimedChunkEvent
	{
		private Unclaim(CommandSource s, ClaimedChunk a, boolean p)
		{
			super(s, a, p);
		}

		public static class Check extends Unclaim
		{
			public Check(CommandSource s, ClaimedChunk a)
			{
				super(s, a, true);
			}
		}

		public static class Done extends Unclaim
		{
			public Done(CommandSource s, ClaimedChunk a)
			{
				super(s, a, false);
			}
		}
	}

	public static class Load extends ClaimedChunkEvent
	{
		private Load(CommandSource s, ClaimedChunk a, boolean p)
		{
			super(s, a, p);
		}

		public static class Check extends Load
		{
			public Check(CommandSource s, ClaimedChunk a)
			{
				super(s, a, true);
			}
		}

		public static class Done extends Load
		{
			public Done(CommandSource s, ClaimedChunk a)
			{
				super(s, a, false);
			}
		}
	}

	public static class Unload extends ClaimedChunkEvent
	{
		private Unload(CommandSource s, ClaimedChunk a, boolean p)
		{
			super(s, a, p);
		}

		public static class Check extends Unload
		{
			public Check(CommandSource s, ClaimedChunk a)
			{
				super(s, a, true);
			}
		}

		public static class Done extends Unload
		{
			public Done(CommandSource s, ClaimedChunk a)
			{
				super(s, a, false);
			}
		}
	}
}