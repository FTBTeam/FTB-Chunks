package com.feed_the_beast.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbguilibrary.config.ConfigGroup;
import com.feed_the_beast.mods.ftbguilibrary.config.NameMap;
import com.feed_the_beast.mods.ftbguilibrary.config.gui.GuiEditConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @author LatvianModder
 */
public class FTBChunksClientConfig
{
	public static MinimapPosition minimap;
	public static double minimapScale;
	public static boolean minimapWaypoints;
	public static boolean minimapPlayerHeads;

	private static Pair<ClientConfig, ForgeConfigSpec> client;

	public static void init()
	{
		FMLJavaModLoadingContext.get().getModEventBus().register(FTBChunksClientConfig.class);

		client = new ForgeConfigSpec.Builder().configure(ClientConfig::new);

		ModLoadingContext modLoadingContext = ModLoadingContext.get();
		modLoadingContext.registerConfig(ModConfig.Type.CLIENT, client.getRight());
	}

	@SubscribeEvent
	public static void reload(ModConfig.ModConfigEvent event)
	{
		ModConfig config = event.getConfig();

		if (config.getSpec() == client.getRight())
		{
			ClientConfig c = client.getLeft();
			minimap = c.minimap.get();
			minimapScale = c.minimapScale.get();
			minimapWaypoints = c.minimapWaypoints.get();
			minimapPlayerHeads = c.minimapPlayerHeads.get();
		}
	}

	private static class ClientConfig
	{
		private final ForgeConfigSpec.EnumValue<MinimapPosition> minimap;
		public final ForgeConfigSpec.DoubleValue minimapScale;
		public final ForgeConfigSpec.BooleanValue minimapWaypoints;
		public final ForgeConfigSpec.BooleanValue minimapPlayerHeads;

		private ClientConfig(ForgeConfigSpec.Builder builder)
		{
			minimap = builder
					.comment("Enables minimap to show up in corner")
					.translation("ftbchunks.minimap")
					.defineEnum("minimap", MinimapPosition.TOP_RIGHT);

			minimapScale = builder
					.comment("Scale of minimap")
					.translation("ftbchunks.minimap_scale")
					.defineInRange("minimap_scale", 1D, 0.125D, 4D);

			minimapWaypoints = builder
					.comment("Scale of minimap")
					.translation("ftbchunks.minimap_waypoints")
					.define("minimap_waypoints", true);

			minimapPlayerHeads = builder
					.comment("Scale of minimap")
					.translation("ftbchunks.minimap_player_heads")
					.define("minimap_player_heads", true);
		}
	}

	public static void openSettings()
	{
		ConfigGroup group = new ConfigGroup("ftbchunks");

		group.addEnum("minimap", minimap, v -> {
			client.getLeft().minimap.set(v);
			minimap = v;
		}, NameMap.of(MinimapPosition.TOP_RIGHT, MinimapPosition.values()).create());

		group.addDouble("minimap_scale", minimapScale, v -> {
			client.getLeft().minimapScale.set(v);
			minimapScale = v;
		}, 1D, 0.125D, 4D);

		group.addBool("minimap_waypoints", minimapWaypoints, v -> {
			client.getLeft().minimapWaypoints.set(v);
			minimapWaypoints = v;
		}, true);

		group.addBool("minimap_player_heads", minimapPlayerHeads, v -> {
			client.getLeft().minimapPlayerHeads.set(v);
			minimapPlayerHeads = v;
		}, true);

		GuiEditConfig gui = new GuiEditConfig(group);
		group.savedCallback = b -> {
			if (b)
			{
				client.getRight().save();
			}

			gui.closeGui(false);
		};

		gui.openGui();
	}
}
