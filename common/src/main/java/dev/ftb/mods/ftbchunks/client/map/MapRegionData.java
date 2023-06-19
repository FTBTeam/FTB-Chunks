package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.data.HeightUtils;
import dev.ftb.mods.ftblibrary.math.XZ;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author LatvianModder
 */
public class MapRegionData {
	// WLLLLBBB BBBBBBBB - waterLightAndBiome
	// W - Water (x & 1) << 15
	// L - Light (x & 15) << 11
	// B - Biome (x & 0b111_11111111)

	public final MapRegion region;
	public final short[] height = new short[512 * 512];
	public final short[] waterLightAndBiome = new short[512 * 512];
	public final int[] foliage = new int[512 * 512];
	public final int[] grass = new int[512 * 512];
	public final int[] water = new int[512 * 512];

	public MapRegionData(MapRegion r) {
		region = r;
		Arrays.fill(height, (short) HeightUtils.UNKNOWN);
		FTBChunks.LOGGER.debug("constructing map region data for {} / {}", region, region.pos);
	}

	public int getBlockIndex(int index) {
		int f = (foliage[index] >> 24) & 0xFF;
		int g = (grass[index] >> 24) & 0xFF;
		int w = (water[index] >> 24) & 0xFF;
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
							int s = stream.readShort();

							for (int i = 0; i < s; i++) {
								int v = version >= 2 ? stream.readByte() : 0;
								int x = stream.readByte();
								int z = stream.readByte();
								long m = stream.readLong();

								MapChunk mapChunk = Objects.requireNonNullElse(region.getMapChunk(XZ.of(x, z)), new MapChunk(region, XZ.of(x, z)));
								mapChunk.version = v;
								mapChunk.modified = m;
								synchronized (region.dimension.manager.lock) {
									region.addMapChunk(mapChunk);
								}
							}
						}
						case "data.png" -> dataImage = ImageIO.read(zis);
						case "foliage.png" -> foliageImage = ImageIO.read(zis);
						case "grass.png" -> grassImage = ImageIO.read(zis);
						case "water.png" -> waterImage = ImageIO.read(zis);
						case "blocks.png" -> blocksImage = ImageIO.read(zis);
						default -> FTBChunks.LOGGER.warn("Unknown file " + ze.getName() + " in " + file.toAbsolutePath());
					}
				}

				for (int y = 0; y < 512; y++) {
					for (int x = 0; x < 512; x++) {
						int index = x + y * 512;
						height[index] = (short) ((dataImage.getRGB(x, y) >> 16) & 0xFFFF);

						if (version < 3 && height[index] == 0) {
							height[index] = (short) HeightUtils.UNKNOWN;
							region.saveData = true;
						}

						waterLightAndBiome[index] = (short) (dataImage.getRGB(x, y) & 0xFFFF);
						foliage[index] = foliageImage.getRGB(x, y);
						grass[index] = grassImage.getRGB(x, y);
						water[index] = waterImage.getRGB(x, y);
						setBlockIndex(index, blocksImage.getRGB(x, y));
					}
				}
			}
		}
	}

	public void write() throws IOException {
		Collection<MapChunk> chunkList;
		synchronized (region.dimension.manager.lock) {
			chunkList = region.getModifiedChunks();
		}

		if (chunkList.isEmpty()) {
			return;
		}

		BufferedImage dataImage = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
		BufferedImage foliageImage = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
		BufferedImage grassImage = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
		BufferedImage waterImage = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
		BufferedImage blocksImage = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);

		for (int y = 0; y < 512; y++) {
			for (int x = 0; x < 512; x++) {
				int index = x + y * 512;
				dataImage.setRGB(x, y, (((int) height[index] & 0xFFFF) << 16) | ((int) waterLightAndBiome[index] & 0xFFFF));
				foliageImage.setRGB(x, y, 0xFF000000 | (foliage[index] & 0xFFFFFF));
				grassImage.setRGB(x, y, 0xFF000000 | (grass[index] & 0xFFFFFF));
				waterImage.setRGB(x, y, 0xFF000000 | (water[index] & 0xFFFFFF));
				blocksImage.setRGB(x, y, 0xFF000000 | getBlockIndex(index));
			}
		}

		FTBChunks.EXECUTOR.execute(() -> {
			try {
				writeData(chunkList, dataImage, foliageImage, grassImage, waterImage, blocksImage);
			} catch (Exception ex) {
				FTBChunks.LOGGER.error("Failed to write map region " + region.dimension + ":" + region + ":");
				ex.printStackTrace();
			}
		});
	}

	private void writeData(Collection<MapChunk> chunkList, BufferedImage dataImage, BufferedImage foliageImage, BufferedImage grassImage, BufferedImage waterImage, BufferedImage blocksImage) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(baos);
			 ZipOutputStream out = new ZipOutputStream(bufferedOutputStream)) {
			DataOutputStream stream = new DataOutputStream(out);
			out.putNextEntry(new ZipEntry("chunks.dat"));

			stream.writeByte(0);
			stream.writeByte(3);
			stream.writeShort(chunkList.size());

			for (MapChunk chunk : chunkList) {
				stream.writeByte(chunk.version);
				stream.writeByte(chunk.pos.x());
				stream.writeByte(chunk.pos.z());
				stream.writeLong(chunk.modified);
			}

			out.closeEntry();

			out.putNextEntry(new ZipEntry("data.png"));
			ImageIO.write(dataImage, "PNG", out);
			out.closeEntry();

			out.putNextEntry(new ZipEntry("foliage.png"));
			ImageIO.write(foliageImage, "PNG", out);
			out.closeEntry();

			out.putNextEntry(new ZipEntry("grass.png"));
			ImageIO.write(grassImage, "PNG", out);
			out.closeEntry();

			out.putNextEntry(new ZipEntry("water.png"));
			ImageIO.write(waterImage, "PNG", out);
			out.closeEntry();

			out.putNextEntry(new ZipEntry("blocks.png"));
			ImageIO.write(blocksImage, "PNG", out);
			out.closeEntry();
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

		synchronized (region.dimension.manager.lock) {
			return region.getOrCreateMapChunk(effectivePos);
		}
	}
}