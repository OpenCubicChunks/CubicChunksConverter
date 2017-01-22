package cubicchunks.converter.lib;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.IntTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.stream.NBTInputStream;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.zip.GZIPInputStream;

import cubicchunks.regionlib.SaveSection;
import cubicchunks.regionlib.impl.EntryLocation2D;
import cubicchunks.regionlib.impl.EntryLocation3D;
import cubicchunks.regionlib.impl.RegionLocation2D;
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


	@Override public void convert(IProgress progress, Path srcDir, Path dstDir) throws IOException {
		int step = 0;
		int maxSteps = Dimensions.getDimensions().size();
		for (Dimension d : Dimensions.getDimensions()) {
			convertDimension(progress, LOCATION_FUNC_SRC.apply(d, srcDir), LOCATION_FUNC_DST.apply(d, dstDir), step, maxSteps);
			step++;
		}
	}

	private void convertDimension(IProgress progress, Path srcRegions, Path dstParent, int step, int maxSteps) throws IOException {
		SaveSection<RegionLocation2D, EntryLocation2D> vanillaSave = new SaveSection<>(
			new CachedRegionProvider<>(
				new SimpleRegionProvider<>(srcRegions, (path, key) ->
					Region.<RegionLocation2D, EntryLocation2D>builder()
						.setPath(path)
						.setSectorSize(4096)
						.setEntriesPerRegion(key.getKeyCount())
						.addHeaderEntry(new TimestampHeaderEntryProvider<>(TimeUnit.MILLISECONDS))
						.build(),
					RegionLocation2D::fromName
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
	}

	private void convertRegion(IProgress progress, RegionLocation2D loc, SaveSection<RegionLocation2D, EntryLocation2D> vanillaSave, SaveCubeColumns saveCubic) throws IOException {
		int baseX = loc.getX() << EntryLocation2D.LOC_BITS;
		int baseZ = loc.getX() << EntryLocation2D.LOC_BITS;
		for (int dx = 0; dx < 32; dx++) {
			for (int dz = 0; dz < 32; dz++) {
				EntryLocation2D entryLoc = new EntryLocation2D(baseX + dx, baseZ + dz);
				ByteBuffer vanillaData = vanillaSave.load(entryLoc).orElse(null);
				if (vanillaData == null) {
					continue;
				}
				ByteBuffer[] cubes = extractCubeData(vanillaData);
				ByteBuffer column = extractColumnData(vanillaData);
				for (int y = 0; y < cubes.length; y++) {
					EntryLocation3D l = new EntryLocation3D(baseX + dx, y, baseZ + dz);
					saveCubic.save3d(l, cubes[y]);
				}
				saveCubic.save2d(entryLoc, column);
			}
		}
	}

	private ByteBuffer extractColumnData(ByteBuffer vanillaData) {

	}

	private ByteBuffer[] extractCubeData(ByteBuffer vanillaData) throws IOException {
		CompoundTag[] tags = extractCubeData(readCompressed(new ByteArrayInputStream(vanillaData.array())));
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
					level.put(srcLevel.get("v"));
					level.put("x", srcLevel.get("xPos"));
					level.put(new IntTag("y", y));
					level.put("z", srcLevel.get("zPos"));

					level.put("populated", srcLevel.get("TerrainPopulated"));
					level.put("fullyPopulated", srcLevel.get("TerrainPopulated")); // TODO: handle this properly

					level.put("initLightDone", srcLevel.get("LightPopulated"));

					level.put("initLightDone", srcLevel.get("LightPopulated"));

					// the vanilla section has additional Y tag, it will be ignored by cubic chunks
					level.put(new ListTag<>("Sections", CompoundTag.class, Arrays.asList(srcSection)));

					level.put(filterEntities((CompoundTag) srcLevel.get("Entities")));
					level.put(filterTileEntities((CompoundTag) srcLevel.get("TileEntities")));
					level.put(filterTileTicks((CompoundTag) srcLevel.get("TileTicks")));
				}
				root.put(new CompoundTag("Level", level));
			}
			tags[y] = new CompoundTag("", root);
		}

	}

	private CompoundTag getSection(CompoundMap srcLevel, int y) {

	}

	private CompoundTag filterEntities(CompoundTag entities) {

	}

	private CompoundTag filterTileEntities(CompoundTag tileEntities) {

	}

	private CompoundTag filterTileTicks(CompoundTag tileTicks) {

	}

	private static CompoundTag readCompressed(InputStream is) throws IOException {
		BufferedInputStream data = new BufferedInputStream(new GZIPInputStream(is));
		return (CompoundTag) new NBTInputStream(data).readTag();
	}
}
