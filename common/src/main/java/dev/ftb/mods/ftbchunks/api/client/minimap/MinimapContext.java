package dev.ftb.mods.ftbchunks.api.client.minimap;

import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public record MinimapContext(
   Minecraft minecraft,
   LocalPlayer player,
   MapDimension mapDimension,
   int mapChunkPosX,
   int mapChunkPosZ,
   double mapPlayerX,
   double mapPlayerY,
   double mapPlayerZ
) {}
