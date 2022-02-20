/*
 *  This file is part of CubicChunksConverter, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2017-2021 contributors
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
package cubicchunks.converter.lib.convert.anvil2cc;

import com.flowpowered.nbt.*;
import cubicchunks.converter.lib.conf.ConverterConfig;
import cubicchunks.converter.lib.convert.ChunkDataConverter;
import cubicchunks.converter.lib.convert.data.AnvilChunkData;
import cubicchunks.converter.lib.convert.data.CubicChunksColumnData;
import cubicchunks.converter.lib.util.Utils;
import cubicchunks.regionlib.impl.EntryLocation2D;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

// TODO: use kyori NBT
public class Anvil2CCDataConverter implements ChunkDataConverter<AnvilChunkData, CubicChunksColumnData> {

    private static final Map<Integer, String> TE_REGISTRY = new HashMap<>();

    static {
        TE_REGISTRY.put(61, "furnace");
        TE_REGISTRY.put(62, "furnace");
        TE_REGISTRY.put(54, "chest");
        TE_REGISTRY.put(146, "chest");
        TE_REGISTRY.put(130, "ender_chest");
        TE_REGISTRY.put(84, "jukebox");
        TE_REGISTRY.put(23, "dispenser");
        TE_REGISTRY.put(158, "dropper");
        TE_REGISTRY.put(63, "sign");
        TE_REGISTRY.put(68, "sign");
        TE_REGISTRY.put(52, "mob_spawner");
        TE_REGISTRY.put(25, "noteblock");
        // TE_REGISTRY.put(, "piston");
        TE_REGISTRY.put(117, "brewing_stand");
        TE_REGISTRY.put(116, "enchanting_table");
        TE_REGISTRY.put(119, "end_portal");
        TE_REGISTRY.put(138, "beacon");
        TE_REGISTRY.put(144, "skull");
        TE_REGISTRY.put(151, "daylight_detector");
        TE_REGISTRY.put(178, "daylight_detector");
        TE_REGISTRY.put(154, "hopper");
        TE_REGISTRY.put(149, "comparator");
        TE_REGISTRY.put(150, "comparator");
        TE_REGISTRY.put(140, "flower_pot");
        TE_REGISTRY.put(176, "banner");
        TE_REGISTRY.put(177, "banner");
        TE_REGISTRY.put(255, "structure_block");
        TE_REGISTRY.put(209, "end_gateway");
        TE_REGISTRY.put(137, "command_block");
        TE_REGISTRY.put(210, "command_block");
        TE_REGISTRY.put(211, "command_block");
        TE_REGISTRY.put(219, "shulker_box");
        TE_REGISTRY.put(220, "shulker_box");
        TE_REGISTRY.put(221, "shulker_box");
        TE_REGISTRY.put(222, "shulker_box");
        TE_REGISTRY.put(223, "shulker_box");
        TE_REGISTRY.put(224, "shulker_box");
        TE_REGISTRY.put(225, "shulker_box");
        TE_REGISTRY.put(226, "shulker_box");
        TE_REGISTRY.put(227, "shulker_box");
        TE_REGISTRY.put(228, "shulker_box");
        TE_REGISTRY.put(229, "shulker_box");
        TE_REGISTRY.put(230, "shulker_box");
        TE_REGISTRY.put(231, "shulker_box");
        TE_REGISTRY.put(232, "shulker_box");
        TE_REGISTRY.put(233, "shulker_box");
        TE_REGISTRY.put(234, "shulker_box");
        TE_REGISTRY.put(26, "bed");
    }
    private final boolean fixMissingTileEntities;

    public Anvil2CCDataConverter(ConverterConfig config) {
        fixMissingTileEntities = config.getBool("fixMissingTileEntities");
    }

    public static ConverterConfig loadConfig(Consumer<Throwable> errorHandler) {
        ConverterConfig conf = new ConverterConfig(new HashMap<>());
        conf.set("fixMissingTileEntities", true);
        return conf;
    }

    public Set<CubicChunksColumnData> convert(AnvilChunkData input) {
        try {
            Map<Integer, ByteBuffer> cubes = extractCubeData(input.getData());
            ByteBuffer column = extractColumnData(input.getData());
            EntryLocation2D location = new EntryLocation2D(input.getPosition().getEntryX(), input.getPosition().getEntryZ());
            return Collections.singleton(new CubicChunksColumnData(input.getDimension(), location, column, cubes));
        } catch (IOException impossible) {
            throw new Error("ByteArrayInputStream doesn't throw IOException", impossible);
        }
    }


    private ByteBuffer extractColumnData(ByteBuffer vanillaData) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(vanillaData.array());
        CompoundTag tag = Utils.readCompressed(in);
        CompoundTag columnTag = extractColumnData(tag);
        return Utils.writeCompressed(columnTag, false);
    }

    private CompoundTag extractColumnData(CompoundTag tag) throws IOException {
        /*
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
         * CubicChunks Column format:
         *
         * ROOT
         * |- DataVersion
         * |- Level
         *  |- v
         *  |- x
         *  |- z
         *  |- InhabitedTime
         *  |- Biomes
         *  |- OpacityIndex
         */
        CompoundMap levelMap = new CompoundMap();
        CompoundMap srcLevel = (CompoundMap) tag.getValue().get("Level").getValue();

        int[] srcHeightMap = fixHeightmap((int[]) srcLevel.get("HeightMap").getValue());

        levelMap.put(new IntTag("v", 1));
        levelMap.put(new IntTag("x", (Integer) srcLevel.get("xPos").getValue()));
        levelMap.put(new IntTag("z", (Integer) srcLevel.get("zPos").getValue()));
        levelMap.put(srcLevel.getOrDefault("InhabitedTime", new IntTag("InhabitedTime", 0)));
        levelMap.put(srcLevel.get("Biomes"));
        levelMap.put(new ByteArrayTag("OpacityIndex", makeDummyOpacityIndex(srcHeightMap)));

        CompoundMap rootMap = new CompoundMap();
        rootMap.put(new CompoundTag("Level", levelMap));
        if (tag.getValue().containsKey("DataVersion")) {
            rootMap.put(tag.getValue().get("DataVersion"));
        }

        return new CompoundTag("", rootMap);
    }

    private int[] fixHeightmap(int[] heights) {
        for (int i = 0; i < heights.length; i++) {
            heights[i]--; // vanilla = 1 above top, data = top block
        }
        return heights;
    }

    private byte[] makeDummyOpacityIndex(int[] heightMap) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(buf);

        for (int i = 0; i < 256; i++) { // 256 segment arrays
            out.writeInt(0); // minY
            out.writeInt(heightMap[i]); // maxY
            out.writeShort(0); // no segments - write zero
        }

        out.close();
        return buf.toByteArray();
    }

    private Map<Integer, ByteBuffer> extractCubeData(ByteBuffer vanillaData) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(vanillaData.array());
        Map<Integer, CompoundTag> tags = extractCubeData(Utils.readCompressed(in));
        Map<Integer, ByteBuffer> bytes = new HashMap<>();
        for (Integer y : tags.keySet()) {
            bytes.put(y, Utils.writeCompressed(tags.get(y), false));
        }
        return bytes;
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, CompoundTag> extractCubeData(CompoundTag srcRootTag) {
        /*
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
         *  |- isSurfaceTracked
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
        Map<Integer, CompoundTag> tags = new HashMap<>();
        CompoundMap srcLevel = ((CompoundTag) srcRoot.get("Level")).getValue();
        int x = (Integer) srcLevel.get("xPos").getValue();
        int z = (Integer) srcLevel.get("zPos").getValue();
        //noinspection unchecked
        for (CompoundTag srcSection : ((ListTag<CompoundTag>) srcLevel.get("Sections")).getValue()) {
            int y = ((ByteTag) srcSection.getValue().get("Y")).getValue();

            CompoundMap root = new CompoundMap();
            {
                if (srcRoot.containsKey("DataVersion")) {
                    root.put(srcRoot.get("DataVersion"));
                }
                CompoundMap level = new CompoundMap();

                {
                    level.put(new ByteTag("v", (byte) 1));
                    level.put(new IntTag("x", x));
                    level.put(new IntTag("y", y));
                    level.put(new IntTag("z", z));

                    ByteTag populated = (ByteTag) srcLevel.get("TerrainPopulated");
                    level.put(new ByteTag("populated", populated == null ? 0 : populated.getValue()));
                    level.put(new ByteTag("fullyPopulated", populated == null ? 0 : populated.getValue())); // TODO: handle this properly
                    level.put(new ByteTag("isSurfaceTracked", (byte) 0)); // so that cubic chunks can re-make surface tracking data on it's own

                    ByteTag lightPopulated = (ByteTag) srcLevel.get("LightPopulated");
                    level.put(new ByteTag("initLightDone", lightPopulated == null ? 0 : lightPopulated.getValue()));

                    // the vanilla section has additional Y tag, it will be ignored by cubic chunks
                    level.put(new ListTag<>("Sections", CompoundTag.class, singletonList(fixSection(srcSection))));

                    level.put(filterEntities((ListTag<CompoundTag>) srcLevel.get("Entities"), y));
                    ListTag<?> tileEntities = filterTileEntities((ListTag<?>) srcLevel.get("TileEntities"), y);
                    if (fixMissingTileEntities) {
                        tileEntities = addMissingTileEntities(x, y, z, (ListTag<CompoundTag>) tileEntities, srcSection);
                    }
                    level.put(tileEntities);
                    if (srcLevel.containsKey("TileTicks")) {
                        level.put(filterTileTicks((ListTag<CompoundTag>) srcLevel.get("TileTicks"), y));
                    }
                    level.put(makeLightingInfo(srcLevel));
                }
                root.put(new CompoundTag("Level", level));
            }
            tags.put(y, new CompoundTag("", root));
        }
        // make sure the 0-15 range is there because it's using vanilla generator which expects it to be the case
        for (int y = 0; y < 16; y++) {
            if (!tags.containsKey(y)) {
                tags.put(y, Utils.emptyCube(x, y, z));
            }
        }
        return tags;
    }

    private ListTag<CompoundTag> addMissingTileEntities(int cubeX, int cubeY, int cubeZ, ListTag<CompoundTag> tileEntities, CompoundTag srcSection) {
        CompoundMap section = srcSection.getValue();
        if (!section.containsKey("Blocks")) {
            return tileEntities;
        }
        final IntTag zeroTag = new IntTag("", 0);

        byte[] blocks = ((ByteArrayTag) section.get("Blocks")).getValue();
        byte[] add = section.containsKey("Add") ? ((ByteArrayTag) section.get("Add")).getValue() : null;
        byte[] add2neid = section.containsKey("Add2") ? ((ByteArrayTag) section.get("Add2")).getValue() : null;

        Map<Integer, CompoundTag> teMap = new HashMap<>();
        for (CompoundTag tag : tileEntities.getValue()) {
            CompoundMap te = tag.getValue();
            int x = ((Number) te.getOrDefault("x", zeroTag).getValue()).intValue();
            int y = ((Number) te.getOrDefault("y", zeroTag).getValue()).intValue();
            int z = ((Number) te.getOrDefault("z", zeroTag).getValue()).intValue();
            int idx = y & 0xF << 8 | z & 0xF << 4 | x & 0xF;
            teMap.put(idx, tag);
        }
        for (int i = 0; i < 4096; i++) {
            int x = i & 15;
            int y = i >> 8 & 15;
            int z = i >> 4 & 15;

            int toAdd = add == null ? 0 : getNibble(add, i);
            toAdd = (toAdd & 0xF) | (add2neid == null ? 0 : getNibble(add2neid, i) << 4);
            int id = (toAdd << 8) | (blocks[i] & 0xFF);
            String teId = TE_REGISTRY.get(id);
            if (teId != null && !teMap.containsKey(i)) {
                CompoundMap map = new CompoundMap();
                map.put(new StringTag("id", teId));
                map.put(new IntTag("x", cubeX * 16 + x));
                map.put(new IntTag("y", cubeY * 16 + y));
                map.put(new IntTag("z", cubeZ * 16 + z));
                CompoundTag tag = new CompoundTag("", map);
                teMap.put(i, tag);
            }
        }
        return new ListTag<>(tileEntities.getName(), CompoundTag.class, new ArrayList<>(teMap.values()));
    }

    private static int getNibble(byte[] array, int i) {
        byte v = array[i >> 1];
        int shiftedValue = (i & 1) == 0 ? v : (v >> 4);
        return shiftedValue & 0xF;
    }

    private CompoundTag fixSection(CompoundTag srcSection) {
        ByteArrayTag data = (ByteArrayTag) srcSection.getValue().get("Blocks");
        byte[] ids = data.getValue();
        // TODO: handle it the forge way
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] == 7) { // bedrock
                ids[i] = 1; // stone
            }
        }
        return srcSection;
    }

    private CompoundTag makeLightingInfo(CompoundMap srcLevel) {
        IntArrayTag heightmap = new IntArrayTag("LastHeightMap", (int[]) srcLevel.get("HeightMap").getValue());
        CompoundMap lightingInfoMap = new CompoundMap();
        lightingInfoMap.put(heightmap);
        return new CompoundTag("LightingInfo", lightingInfoMap);
    }

    @SuppressWarnings("unchecked")
    private ListTag<CompoundTag> filterEntities(ListTag<CompoundTag> entities, int cubeY) {
        double yMin = cubeY * 16;
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

    @SuppressWarnings("unchecked")
    private ListTag<?> filterTileEntities(ListTag<?> tileEntities, int cubeY) {
        // empty list is list of EndTags
        if (tileEntities.getValue().isEmpty()) {
            return tileEntities;
        }
        int yMin = cubeY * 16;
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
        int yMin = cubeY * 16;
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

}
