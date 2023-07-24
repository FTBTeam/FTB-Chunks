package dev.ftb.mods.ftbchunks.integration.kubejs;

import dev.ftb.mods.ftbchunks.data.ClaimResult;
import dev.ftb.mods.ftbchunks.data.ClaimedChunk;
import net.minecraft.commands.CommandSourceStack;

public class BeforeEventJS extends AfterEventJS {
	private ClaimResult result;

	public BeforeEventJS(CommandSourceStack source, ClaimedChunk chunk) {
		super(source, chunk);
		result = chunk;
	}

	public ClaimResult getResult() {
		return result;
	}

	public void setResult(ClaimResult r) {
		cancel();
		result = r;
	}

	public void setCustomResult(String messageKey) {
		cancel();
		result = new ClaimResult() {
			@Override
			public String claimResultName() {
				return messageKey;
			}

			@Override
			public String getTranslationKey() {
				return messageKey;
			}
		};
	}
}
