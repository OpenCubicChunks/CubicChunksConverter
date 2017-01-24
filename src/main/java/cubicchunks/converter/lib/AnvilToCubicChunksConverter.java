/*
 *  This file is part of CubicChunksConverter, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2017 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package cubicchunks.converter.lib;

import com.flowpowered.nbt.ByteTag;
import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.DoubleTag;
import com.flowpowered.nbt.IntArrayTag;
import com.flowpowered.nbt.IntTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.StringTag;
import com.flowpowered.nbt.Tag;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.flowpowered.nbt.stream.NBTOutputStream;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

import cubicchunks.regionlib.SaveSection;
import cubicchunks.regionlib.impl.EntryLocation2D;
import cubicchunks.regionlib.impl.EntryLocation3D;
import cubicchunks.regionlib.impl.MinecraftChunkLocation;
import cubicchunks.regionlib.impl.MinecraftRegionLocation;
import cubicchunks.regionlib.impl.SaveCubeColumns;
import cubicchunks.regionlib.region.Region;
import cubicchunks.regionlib.region.header.TimestampHeaderEntryProvider;
import cubicchunks.regionlib.region.provider.CachedRegionProvider;
import cubicchunks.regionlib.region.provider.SimpleRegionProvider;
import cubicchunks.regionlib.util.WrappedException;

public class AnvilToCubicChunksConverter implements ISaveConverter {

	private static final BiFunction<Dimension, Path, Path> LOCATION_FUNC_SRC = (d, p) -> {
		if (!d.getDirectory().isEmpty()) {
			p = p.resolve(d.getDirectory());
		}
		return p.resolve("region");
	};

	private static final BiFunction<Dimension, Path, Path> LOCATION_FUNC_DST = (d, p) -> {
		if (!d.getDirectory().isEmpty()) {
			p = p.resolve(d.getDirectory());
		}
		return p;
	};


	@Override
	public void convert(IProgress progress, Path srcDir, Path dstDir) throws IOException {
		convertLevelInfo(progress, srcDir, dstDir);
		convertChunkData(progress, srcDir, dstDir);
		copyAllOtherData(progress, srcDir, dstDir);
	}

	private void convertLevelInfo(IProgress progress, Path srcDir, Path dstDir) throws IOException {
		NBTInputStream nbtIn = new NBTInputStream(new FileInputStream(srcDir.resolve("level.dat").toFile()));
		CompoundTag root = (CompoundTag) nbtIn.readTag();
		CompoundMap newRoot = new CompoundMap();
		for (Tag<?> tag : root.getValue()) {
			if (tag.getName().equals("Data")) {
				CompoundMap data = ((CompoundTag) root.getValue().get("Data")).getValue();
				CompoundMap newData = new CompoundMap();
				for (Tag<?> dataTag : data) {
					if (dataTag.getName().equals("generatorName")) {
						String value = (String) dataTag.getValue();
						String newValue;
						if (value.equals("default")) {
							newValue = "VanillaCubic";
						} else if (value.equals("flat")) {
							newValue = "FlatCubic";
						} else {
							throw new UnsupportedOperationException("Unsupported world type " + value);
						}
						newData.put(new StringTag(dataTag.getName(), newValue));
					} else {
						newData.put(dataTag);
					}
				}
				newRoot.put(new CompoundTag(tag.getName(), newData));
			} else {
				newRoot.put(tag);
			}
		}

		NBTOutputStream nbtOut = new NBTOutputStream(new FileOutputStream(dstDir.resolve("level.dat").toFile()));
		nbtOut.writeTag(new CompoundTag(root.getName(), newRoot));
		nbtOut.close();
	}

	private void convertChunkData(IProgress progress, Path srcDir, Path dstDir) throws IOException {
		int step = 0;
		int maxSteps = Dimensions.getDimensions().size();
		for (Dimension d : Dimensions.getDimensions()) {
			Path srcLoc = LOCATION_FUNC_SRC.apply(d, srcDir);
			if (!Files.exists(srcLoc)) {
				continue;
			}
			convertDimension(progress, srcLoc, LOCATION_FUNC_DST.apply(d, dstDir), step, maxSteps);
			step++;
		}
	}

	private void copyAllOtherData(IProgress progress, Path srcDir, Path dstDir) throws IOException {
		Utils.copyEverythingExcept(srcDir, srcDir, dstDir, file ->
			file.toString().contains("level.dat") ||
				Dimensions.getDimensions().stream().anyMatch(dim ->
					srcDir.resolve(dim.getDirectory()).resolve("region").equals(file)
				)
		);
	}

	private void convertDimension(IProgress progress, Path srcRegions, Path dstParent, int step, int maxSteps) throws IOException {
		SaveSection<MinecraftRegionLocation, MinecraftChunkLocation> vanillaSave = new SaveSection<>(
			new CachedRegionProvider<>(
				new SimpleRegionProvider<>(srcRegions, (path, key) ->
					Region.<MinecraftRegionLocation, MinecraftChunkLocation>builder()
						.setPath(path)
						.setSectorSize(4096)
						.setEntriesPerRegion(key.getKeyCount())
						.addHeaderEntry(new TimestampHeaderEntryProvider<>(TimeUnit.MILLISECONDS))
						.build(),
					MinecraftRegionLocation::fromName
				), 128
			)
		);

		SaveCubeColumns saveCubic = SaveCubeColumns.create(dstParent);

		try {
			vanillaSave.allRegions().forEachRemaining(loc -> {
				try {
					this.convertRegion(progress, loc, vanillaSave, saveCubic);
				} catch (IOException e) {
					throw new WrappedException(e);
				}
			});
		} catch (WrappedException e) {
			throw (IOException) e.get();
		}
		saveCubic.close();
	}

	private void convertRegion(IProgress progress, MinecraftRegionLocation loc,
	                           SaveSection<MinecraftRegionLocation, MinecraftChunkLocation> vanillaSave,
	                           SaveCubeColumns saveCubic) throws IOException {
		int baseX = loc.getX() << EntryLocation2D.LOC_BITS;
		int baseZ = loc.getZ() << EntryLocation2D.LOC_BITS;
		for (int dx = 0; dx < 32; dx++) {
			for (int dz = 0; dz < 32; dz++) {
				MinecraftChunkLocation entryLoc = new MinecraftChunkLocation(baseX + dx, baseZ + dz);
				ByteBuffer vanillaData = vanillaSave.load(entryLoc).orElse(null);
				if (vanillaData == null) {
					continue;
				}
				ByteBuffer[] cubes = extractCubeData(vanillaData);
				ByteBuffer column = extractColumnData(vanillaData);
				for (int y = 0; y < cubes.length; y++) {
					if (cubes[y] == null) {
						continue;
					}
					EntryLocation3D l = new EntryLocation3D(baseX + dx, y, baseZ + dz);
					saveCubic.save3d(l, cubes[y]);
				}
				if (column != null) {
					saveCubic.save2d(new EntryLocation2D(entryLoc.getEntryX(), entryLoc.getEntryZ()), column);
				}
			}
		}
	}

	private ByteBuffer extractColumnData(ByteBuffer vanillaData) {
		return null;
	}

	private ByteBuffer[] extractCubeData(ByteBuffer vanillaData) throws IOException {
		ByteArrayInputStream in = new ByteArrayInputStream(vanillaData.array());
		CompoundTag[] tags = extractCubeData(readCompressed(in));
		ByteBuffer[] buffers = new ByteBuffer[tags.length];
		for (int i = 0; i < tags.length; i++) {
			CompoundTag tag = tags[i];
			if (tag == null) {
				continue;
			}
			buffers[i] = writeCompressed(tag);
		}
		return buffers;
	}

	private CompoundTag[] extractCubeData(CompoundTag srcRootTag) {
		/**
		 *
		 * Vanilla Chunk NBT structure:
		 *
		 * ROOT
		 * |- DataVersion
		 * |- Level
		 *  |- v
		 *  |- xPos
		 *  |- zPos
		 *  |- LastUpdate
		 *  |- TerrainPopulated
		 *  |- LightPopulated
		 *  |- InhabitedTime
		 *  |- Biomes
		 *  |- HeightMap
		 *  |- Sections
		 *  ||* Section list:
		 *  | |- Y
		 *  | |- Blocks
		 *  | |- Data
		 *  | |- Add
		 *  | |- BlockLight
		 *  | |- SkyLight
		 *  |- Entities
		 *  |- TileEntities
		 *  |- TileTicks
		 *
		 * CubicChunks Cube NBT structure:
		 *
		 * ROOT
		 * |- DataVersion
		 * |- Level
		 *  |- v
		 *  |- x
		 *  |- y
		 *  |- z
		 *  |- populated
		 *  |- fullyPopulated
		 *  |- initLightDone
		 *  |- Sections
		 *  ||* A single section
		 *  | |- Blocks
		 *  | |- Data
		 *  | |- Add
		 *  | |- BlockLight
		 *  | |- SkyLight
		 *  |- Entities
		 *  |- TileEntities
		 *  |- TileTicks
		 *  |- LightingInfo
		 *   |- LastHeightMap
		 */
		CompoundMap srcRoot = srcRootTag.getValue();
		CompoundTag[] tags = new CompoundTag[16];
		for (int y = 0; y < 16; y++) {
			CompoundMap srcLevel = ((CompoundTag) srcRoot.get("Level")).getValue();
			CompoundTag srcSection = getSection(srcLevel, y);
			if (srcSection == null) {
				continue;
			}
			CompoundMap root = new CompoundMap();
			{
				root.put(srcRoot.get("DataVersion"));
				CompoundMap level = new CompoundMap();

				{
					// level.put(srcLevel.get("v"));
					level.put("x", srcLevel.get("xPos"));
					level.put(new IntTag("y", y));
					level.put("z", srcLevel.get("zPos"));

					level.put("populated", srcLevel.get("TerrainPopulated"));
					level.put("fullyPopulated", srcLevel.get("TerrainPopulated")); // TODO: handle this properly

					level.put("initLightDone", srcLevel.get("LightPopulated"));

					level.put("initLightDone", srcLevel.get("LightPopulated"));

					// the vanilla section has additional Y tag, it will be ignored by cubic chunks
					level.put(new ListTag<>("Sections", CompoundTag.class, Arrays.asList(srcSection)));

					level.put(filterEntities((ListTag<CompoundTag>) srcLevel.get("Entities"), y));
					level.put(filterTileEntities((ListTag<?>) srcLevel.get("TileEntities"), y));
					if (srcLevel.containsKey("TileTicks")) {
						level.put(filterTileTicks((ListTag<CompoundTag>) srcLevel.get("TileTicks"), y));
					}
					level.put(makeLightingInfo(srcLevel));
				}
				root.put(new CompoundTag("Level", level));
			}
			tags[y] = new CompoundTag("", root);
		}
		return tags;
	}

	private CompoundTag makeLightingInfo(CompoundMap srcLevel) {
		IntArrayTag heightmap = new IntArrayTag("LastHeightMap", (int[]) srcLevel.get("HeightMap").getValue());
		CompoundMap lightingInfoMap = new CompoundMap();
		lightingInfoMap.put(heightmap);
		CompoundTag lightingInfo = new CompoundTag("LightingInfo", lightingInfoMap);
		return lightingInfo;
	}

	private CompoundTag getSection(CompoundMap srcLevel, int y) {
		ListTag<CompoundTag> sections = (ListTag<CompoundTag>) srcLevel.get("Sections");
		for (CompoundTag tag : sections.getValue()) {
			if (((ByteTag) tag.getValue().get("Y")).getValue().equals((byte) (y))) {
				return tag;
			}
		}
		return null;
	}

	private ListTag<CompoundTag> filterEntities(ListTag<CompoundTag> entities, int cubeY) {
		double yMin = cubeY*16;
		double yMax = yMin + 16;
		List<CompoundTag> cubeEntities = new ArrayList<>();
		for (CompoundTag entityTag : entities.getValue()) {
			List<DoubleTag> pos = ((ListTag<DoubleTag>) entityTag.getValue().get("Pos")).getValue();
			double y = pos.get(1).getValue();
			if (y >= yMin && y < yMax) {
				cubeEntities.add(entityTag);
			}
		}
		return new ListTag<>(entities.getName(), CompoundTag.class, cubeEntities);
	}

	private ListTag<?> filterTileEntities(ListTag<?> tileEntities, int cubeY) {
		// empty list is list of EndTags
		if (tileEntities.getValue().isEmpty()) {
			return tileEntities;
		}
		int yMin = cubeY*16;
		int yMax = yMin + 16;
		List<CompoundTag> cubeTEs = new ArrayList<>();
		for (CompoundTag teTag : ((ListTag<CompoundTag>) tileEntities).getValue()) {
			int y = ((IntTag) teTag.getValue().get("y")).getValue();
			if (y >= yMin && y < yMax) {
				cubeTEs.add(teTag);
			}
		}
		return new ListTag<>(tileEntities.getName(), CompoundTag.class, cubeTEs);
	}

	private ListTag<CompoundTag> filterTileTicks(ListTag<CompoundTag> tileTicks, int cubeY) {
		int yMin = cubeY*16;
		int yMax = yMin + 16;
		List<CompoundTag> cubeTicks = new ArrayList<>();
		for (CompoundTag tileTick : tileTicks.getValue()) {
			int y = ((IntTag) tileTick.getValue().get("y")).getValue();
			if (y >= yMin && y < yMax) {
				cubeTicks.add(tileTick);
			}
		}
		return new ListTag<>(tileTicks.getName(), CompoundTag.class, cubeTicks);
	}

	private static CompoundTag readCompressed(InputStream is) throws IOException {
		int i = is.read();
		BufferedInputStream data;
		if (i == 1) {
			data = new BufferedInputStream(new GZIPInputStream(is));
		} else if (i == 2) {
			data = new BufferedInputStream(new InflaterInputStream(is));
		} else {
			throw new UnsupportedOperationException();
		}

		return (CompoundTag) new NBTInputStream(data, false).readTag();
	}

	private static ByteBuffer writeCompressed(CompoundTag tag) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		NBTOutputStream nbtOut = new NBTOutputStream(new GZIPOutputStream(bytes));
		nbtOut.writeTag(tag);
		nbtOut.close();
		return ByteBuffer.wrap(bytes.toByteArray());
	}
}
