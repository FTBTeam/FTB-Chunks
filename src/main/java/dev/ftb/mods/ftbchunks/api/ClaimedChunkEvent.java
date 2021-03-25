package dev.ftb.mods.ftbchunks.api;

import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;

/**
 * @author LatvianModder
 */
public class ClaimedChunkEvent extends Event {
	private final CommandSourceStack source;
	private final ClaimedChunk chunk;
	private final boolean checking;
	private ClaimResult claimResult;

	public ClaimedChunkEvent(CommandSourceStack s, ClaimedChunk c, boolean p) {
		source = s;
		chunk = c;
		checking = p;
		claimResult = c;
	}

	public CommandSourceStack getSource() {
		return source;
	}

	public ClaimedChunk getChunk() {
		return chunk;
	}

	@Override
	public boolean isCancelable() {
		return checking;
	}

	public void setClaimResult(ClaimResult r) {
		claimResult = r;
	}

	@Override
	public void setCanceled(boolean cancel) {
		super.setCanceled(cancel);

		if (cancel && claimResult.isSuccess()) {
			claimResult = ClaimResults.OTHER;
		}
	}

	public ClaimResult postAndGetResult() {
		MinecraftForge.EVENT_BUS.post(this);
		return claimResult;
	}

	public static class Claim extends ClaimedChunkEvent {
		private Claim(CommandSourceStack s, ClaimedChunk a, boolean p) {
			super(s, a, p);
		}

		public static class Check extends Claim {
			public Check(CommandSourceStack s, ClaimedChunk a) {
				super(s, a, true);
			}
		}

		public static class Done extends Claim {
			public Done(CommandSourceStack s, ClaimedChunk a) {
				super(s, a, false);
			}
		}
	}

	public static class Unclaim extends ClaimedChunkEvent {
		private Unclaim(CommandSourceStack s, ClaimedChunk a, boolean p) {
			super(s, a, p);
		}

		public static class Check extends Unclaim {
			public Check(CommandSourceStack s, ClaimedChunk a) {
				super(s, a, true);
			}
		}

		public static class Done extends Unclaim {
			public Done(CommandSourceStack s, ClaimedChunk a) {
				super(s, a, false);
			}
		}
	}

	public static class Load extends ClaimedChunkEvent {
		private Load(CommandSourceStack s, ClaimedChunk a, boolean p) {
			super(s, a, p);
		}

		public static class Check extends Load {
			public Check(CommandSourceStack s, ClaimedChunk a) {
				super(s, a, true);
			}
		}

		public static class Done extends Load {
			public Done(CommandSourceStack s, ClaimedChunk a) {
				super(s, a, false);
			}
		}
	}

	public static class Unload extends ClaimedChunkEvent {
		private Unload(CommandSourceStack s, ClaimedChunk a, boolean p) {
			super(s, a, p);
		}

		public static class Check extends Unload {
			public Check(CommandSourceStack s, ClaimedChunk a) {
				super(s, a, true);
			}
		}

		public static class Done extends Unload {
			public Done(CommandSourceStack s, ClaimedChunk a) {
				super(s, a, false);
			}
		}
	}
}