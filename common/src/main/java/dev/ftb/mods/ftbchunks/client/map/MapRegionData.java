package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.util.HeightUtils;
import dev.ftb.mods.ftblibrary.math.XZ;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class MapRegionData {
	public final MapRegion region;

	// a region is a 512x512 block area

	// 16 bits per block pos; the height of the highest non-air block
	public final short[] height = new short[512 * 512];

	// WLLLLBBB BBBBBBBB - waterLightAndBiome is packed into a 16-bit short
	// W - Water - 1 bit (water/no water) - (x & 1) << 15
	// L - Light - 4 bits (16 light levels) - (x & 15) << 11
	// B - Biome - 11 bits (2048 possible biomes) - (x & 0b111_11111111)
	public final short[] waterLightAndBiome = new short[512 * 512];

	// top 8 bits are bits 16-23 of the block index
	// bottom 24 bits are the biome-tinted RGB of foliage
	public final int[] foliage = new int[512 * 512];

	// top 8 bits are bits 8-15 of the block index
	// bottom 24 bits are the biome-tinted RGB of grass
	public final int[] grass = new int[512 * 512];

	// top 8 bits are bits 0-7 of the block index
	// bottom 24 bits are the biome-tinted RGB of water
	public final int[] water = new int[512 * 512];

	public MapRegionData(MapRegion r) {
		region = r;
		Arrays.fill(height, (short) HeightUtils.UNKNOWN);
		FTBChunks.LOGGER.debug("constructing map region data for {} / {}", region, region.pos);
	}

	public int getBlockIndex(int index) {
		return getBlockIndex(index, foliage, grass, water);
	}

	private static int getBlockIndex(int index, int[] foliage1, int[] grass1, int[] water1) {
		int f = (foliage1[index] >> 24) & 0xFF;
		int g = (grass1[index] >> 24) & 0xFF;
		int w = (water1[index] >> 24) & 0xFF;
		return (f << 16) | (g << 8) | w;
	}

	public void setBlockIndex(int index, int block) {
		foliage[index] = (foliage[index] & 0xFFFFFF) | (((block >> 16) & 0xFF) << 24);
		grass[index] = (grass[index] & 0xFFFFFF) | (((block >> 8) & 0xFF) << 24);
		water[index] = (water[index] & 0xFFFFFF) | ((block & 0xFF) << 24);
	}

	public void read() throws IOException {
		if (Files.notExists(region.dimension.directory)) {
			Files.createDirectories(region.dimension.directory);
		}

		Path file = region.dimension.directory.resolve(region.pos.toRegionString() + ".zip");

		if (Files.exists(file) && Files.isReadable(file)) {
			FTBChunks.LOGGER.debug("reading data from {} for {} / {}", file.getFileName(), region, region.pos);
			try (BufferedInputStream bufferedInputStream = new BufferedInputStream(Files.newInputStream(file));
				 ZipInputStream zis = new ZipInputStream(bufferedInputStream)) {
				ZipEntry ze;

				BufferedImage dataImage = null;
				BufferedImage foliageImage = null;
				BufferedImage grassImage = null;
				BufferedImage waterImage = null;
				BufferedImage blocksImage = null;
				int version = 0;

				while ((ze = zis.getNextEntry()) != null) {
					switch (ze.getName()) {
						case "chunks.dat" -> {
							DataInputStream stream = new DataInputStream(zis);
							stream.readByte();
							version = stream.readByte();
							int nMapChunks = stream.readShort();

							for (int i = 0; i < nMapChunks; i++) {
								int chunkVersion = version >= 2 ? stream.readByte() : 0;
								int x = stream.readByte();
								int z = stream.readByte();
								long lastModified = stream.readLong();

								MapChunk mapChunk = Objects.requireNonNullElse(region.getMapChunk(XZ.of(x, z)), new MapChunk(region, XZ.of(x, z)));
								mapChunk.setVersion(chunkVersion);
								mapChunk.setModified(lastModified);
								synchronized (region.dimension.getManager().lock) {
									region.addMapChunk(mapChunk);
								}
							}
						}
						case "data.png" -> dataImage = ImageIO.read(zis);
						case "foliage.png" -> foliageImage = ImageIO.read(zis);
						case "grass.png" -> grassImage = ImageIO.read(zis);
						case "water.png" -> waterImage = ImageIO.read(zis);
						case "blocks.png" -> blocksImage = ImageIO.read(zis);
						default -> FTBChunks.LOGGER.warn("Unexpected file {} in {}", ze.getName(), file.toAbsolutePath());
					}
				}

				if (dataImage != null && foliageImage != null && grassImage != null && waterImage != null && blocksImage != null) {
					for (int y = 0; y < 512; y++) {
						for (int x = 0; x < 512; x++) {
							int index = x + y * 512;
							height[index] = (short) ((dataImage.getRGB(x, y) >> 16) & 0xFFFF);

							if (version < 3 && height[index] == 0) {
								height[index] = (short) HeightUtils.UNKNOWN;
								region.markDirty();
							}

							waterLightAndBiome[index] = (short) (dataImage.getRGB(x, y) & 0xFFFF);
							foliage[index] = foliageImage.getRGB(x, y);
							grass[index] = grassImage.getRGB(x, y);
							water[index] = waterImage.getRGB(x, y);
							setBlockIndex(index, blocksImage.getRGB(x, y));
						}
					}
				} else {
					FTBChunks.LOGGER.warn("not all images could be read from {} - file damaged?", file);
				}
			}
		}
	}

	public void write() throws IOException {
		Collection<MapChunk> chunkList;
		synchronized (region.dimension.getManager().lock) {
			chunkList = region.getModifiedChunks();
		}

		if (chunkList.isEmpty()) {
			return;
		}

		// snapshot all the data arrays so we can safely construct images from them in a separate thread
		short[] heightCopy = Arrays.copyOf(height, height.length);
		short[] waterLightAndBiomeCopy = Arrays.copyOf(waterLightAndBiome, waterLightAndBiome.length);
		int[] foliageCopy = Arrays.copyOf(foliage, foliage.length);
		int[] grassCopy = Arrays.copyOf(grass, grass.length);
		int[] waterCopy = Arrays.copyOf(water, water.length);

		FTBChunksClient.MAP_EXECUTOR.execute(() -> {
			BufferedImage dataImage = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
			BufferedImage foliageImage = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
			BufferedImage grassImage = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
			BufferedImage waterImage = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
			BufferedImage blocksImage = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);

			for (int y = 0; y < 512; y++) {
				for (int x = 0; x < 512; x++) {
					int index = x + y * 512;
					dataImage.setRGB(x, y, (((int) heightCopy[index] & 0xFFFF) << 16) | ((int) waterLightAndBiomeCopy[index] & 0xFFFF));
					foliageImage.setRGB(x, y, 0xFF000000 | (foliageCopy[index] & 0xFFFFFF));
					grassImage.setRGB(x, y, 0xFF000000 | (grassCopy[index] & 0xFFFFFF));
					waterImage.setRGB(x, y, 0xFF000000 | (waterCopy[index] & 0xFFFFFF));
					blocksImage.setRGB(x, y, 0xFF000000 | getBlockIndex(index, foliageCopy, grassCopy, waterCopy));
				}
			}

			try {
				Map<String,BufferedImage> images = Map.of(
						"data.png", dataImage,
						"foliage.png", foliageImage,
						"grass.png", grassImage,
						"water.png", waterImage,
						"blocks.png", blocksImage
				);
				writeData(chunkList, images);
			} catch (Exception ex) {
                FTBChunks.LOGGER.error("Failed to write map region {}:{}: {}", region.dimension, region, ex.getMessage());
			}
		});
	}

	private void writeData(Collection<MapChunk> chunkList, Map<String,BufferedImage> images) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(baos);
			 ZipOutputStream out = new ZipOutputStream(bufferedOutputStream)) {
			DataOutputStream stream = new DataOutputStream(out);
			out.putNextEntry(new ZipEntry("chunks.dat"));

			stream.writeByte(0);
			stream.writeByte(3);
			stream.writeShort(chunkList.size());
			for (MapChunk chunk : chunkList) {
				chunk.writeData(stream);
			}

			out.closeEntry();

			for (Map.Entry<String, BufferedImage> entry : images.entrySet()) {
				out.putNextEntry(new ZipEntry(entry.getKey()));
				ImageIO.write(entry.getValue(), "PNG", out);
				out.closeEntry();
			}
		}

		if (Files.notExists(region.dimension.directory)) {
			Files.createDirectories(region.dimension.directory);
		}

		FTBChunks.LOGGER.debug("writing data to {} for {} / {}", region.pos.toRegionString() + ".zip", region, region.pos);
		Files.write(region.dimension.directory.resolve(region.pos.toRegionString() + ".zip"), baos.toByteArray());
	}

	public MapChunk getChunk(XZ pos) {
		XZ effectivePos = pos.x() != (pos.x() & 31) || pos.z() != (pos.z() & 31) ?
				XZ.of(pos.x() & 31, pos.z() & 31) :
				pos;

		synchronized (region.dimension.getManager().lock) {
			return region.getOrCreateMapChunk(effectivePos);
		}
	}
}