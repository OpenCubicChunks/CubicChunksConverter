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

import cubicchunks.converter.lib.conf.ConverterConfig;
import cubicchunks.converter.lib.convert.ChunkDataConverter;
import cubicchunks.converter.lib.convert.data.AnvilChunkData;
import cubicchunks.converter.lib.convert.data.CubicChunksColumnData;
import cubicchunks.converter.lib.util.Utils;
import cubicchunks.regionlib.impl.EntryLocation2D;
import net.kyori.nbt.*;

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
        CompoundTag level = new CompoundTag();
        CompoundTag srcLevel = tag.getCompound("Level");

        int[] srcHeightMap = fixHeightmap(srcLevel.getIntArray("HeightMap"));

        level.put("v", new IntTag(1));

        if(!srcLevel.containsAll(TagType.INT, "xPos", "zPos"))
            throw new RuntimeException("Anvil chunk does not contain xPos or zPos!");

        level.put("x", new IntTag(srcLevel.getInt("xPos")));
        level.put("z", new IntTag(srcLevel.getInt("zPos")));
        level.put("InhabitedTime", new IntTag(srcLevel.getInt("InhabitedTime", 0)));
        if(srcLevel.contains("Biomes")) {
            level.put("Biomes", srcLevel.get("Biomes"));
        }
        level.put("OpacityIndex", new ByteArrayTag(makeDummyOpacityIndex(srcHeightMap)));

        CompoundTag root = new CompoundTag();
        root.put("Level", level);
        if (tag.contains("DataVersion")) {
            root.put("DataVersion", tag.get("DataVersion"));
        }

        return root;
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
    private Map<Integer, CompoundTag> extractCubeData(CompoundTag srcRoot) {
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
        Map<Integer, CompoundTag> tags = new HashMap<>();
        CompoundTag srcLevel = srcRoot.getCompound("Level");

        if(!srcLevel.containsAll(TagType.INT, "xPos", "zPos"))
            throw new RuntimeException("Anvil chunk does not contain xPos or zPos!");

        int x = srcLevel.getInt("xPos");
        int z = srcLevel.getInt("zPos");
        for (Tag srcSectionTag : srcLevel.getList("Sections")) {
            if(srcSectionTag.type() != TagType.COMPOUND)
                continue;

            CompoundTag srcSection = ((CompoundTag) srcSectionTag);
            int y = srcSection.getByte("Y");

            CompoundTag root = new CompoundTag();
            {
                if (srcRoot.contains("DataVersion")) {
                    root.put("DataVersion", srcRoot.get("DataVersion"));
                }
                CompoundTag level = new CompoundTag();

                {
                    level.put("v", new ByteTag((byte) 1));
                    level.put("x", new IntTag(x));
                    level.put("y", new IntTag(y));
                    level.put("z", new IntTag(z));

                    byte populated = srcLevel.getByte("TerrainPopulated", (byte) 0);
                    level.put("populated", new ByteTag(populated));
                    level.put("fullyPopulated", new ByteTag(populated)); // TODO: handle this properly
                    level.put("isSurfaceTracked", new ByteTag((byte) 0)); // so that cubic chunks can re-make surface tracking data on it's own

                    byte lightPopulated = srcLevel.getByte("LightPopulated", (byte) 0);
                    level.put("initLightDone", new ByteTag(lightPopulated));

                    // the vanilla section has additional Y tag, it will be ignored by cubic chunks
                    ListTag sectionsTag = new ListTag(TagType.COMPOUND);
                    sectionsTag.add(fixSection(srcSection));
                    level.put("Sections", sectionsTag);

                    level.put("Entities", filterEntities(srcLevel.getList("Entities"), y));
                    ListTag tileEntities = filterTileEntities(srcLevel.getList("TileEntities"), y);
                    if (fixMissingTileEntities) {
                        tileEntities = addMissingTileEntities(x, y, z, tileEntities, srcSection);
                    }
                    level.put("TileEntities", tileEntities);
                    if (srcLevel.contains("TileTicks")) {
                        level.put("TileTicks", filterTileTicks(srcLevel.getList("TileTicks"), y));
                    }
                    level.put("LightingInfo", makeLightingInfo(srcLevel));
                }
                root.put("Level", level);
            }
            tags.put(y, root);
        }
        // make sure the 0-15 range is there because it's using vanilla generator which expects it to be the case
        for (int y = 0; y < 16; y++) {
            if (!tags.containsKey(y)) {
                tags.put(y, Utils.emptyCube(x, y, z));
            }
        }
        return tags;
    }

    private ListTag addMissingTileEntities(int cubeX, int cubeY, int cubeZ, ListTag tileEntities, CompoundTag section) {
        if (!section.contains("Blocks")) {
            return tileEntities;
        }

        byte[] blocks = section.getByteArray("Blocks");
        byte[] add = section.getByteArray("Add", null);
        byte[] add2neid = section.getByteArray("Add2", null);

        Map<Integer, CompoundTag> teMap = new HashMap<>();
        for (Tag tag : tileEntities) {
            CompoundTag te = (CompoundTag) tag;
            int x = te.getInt("x", 0);
            int y = te.getInt("y", 0);
            int z = te.getInt("z", 0);
            int idx = y & 0xF << 8 | z & 0xF << 4 | x & 0xF;
            teMap.put(idx, te);
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
                CompoundTag tag = new CompoundTag();
                tag.put("id", new StringTag(teId));
                tag.put("x", new IntTag(cubeX * 16 + x));
                tag.put("y", new IntTag(cubeY * 16 + y));
                tag.put("z", new IntTag(cubeZ * 16 + z));
                teMap.put(i, tag);
            }
        }
        ListTag tileEntitiesList = new ListTag(TagType.COMPOUND);
        tileEntitiesList.addAll(teMap.values());
        return tileEntitiesList;
    }

    private static int getNibble(byte[] array, int i) {
        byte v = array[i >> 1];
        int shiftedValue = (i & 1) == 0 ? v : (v >> 4);
        return shiftedValue & 0xF;
    }

    private CompoundTag fixSection(CompoundTag srcSection) {
        byte[] ids = srcSection.getByteArray("Blocks");
        // TODO: handle it the forge way
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] == 7) { // bedrock
                ids[i] = 1; // stone
            }
        }
        return srcSection;
    }

    private CompoundTag makeLightingInfo(CompoundTag srcLevel) {
        IntArrayTag heightmap = new IntArrayTag(srcLevel.getIntArray("HeightMap"));
        CompoundTag lightingInfoMap = new CompoundTag();
        lightingInfoMap.put("LastHeightMap", heightmap);
        return lightingInfoMap;
    }

    private ListTag filterEntities(ListTag entities, int cubeY) {
        double yMin = cubeY * 16;
        double yMax = yMin + 16;
        ListTag cubeEntities = new ListTag(TagType.COMPOUND);
        for (Tag entityTag : entities) {
            CompoundTag entity = (CompoundTag) entityTag;
            ListTag pos = entity.getList("Pos");
            double y = pos.getDouble(1);
            if (y >= yMin && y < yMax) {
                cubeEntities.add(entity);
            }
        }
        return cubeEntities;
    }

    private ListTag filterTileEntities(ListTag tileEntities, int cubeY) {
        // empty list is list of EndTags
        if (tileEntities.isEmpty()) {
            return tileEntities;
        }
        int yMin = cubeY * 16;
        int yMax = yMin + 16;
        ListTag cubeTEs = new ListTag(TagType.COMPOUND);
        for (Tag tileEntityTag : tileEntities) {
            CompoundTag teTag = (CompoundTag) tileEntityTag;
            int y = teTag.getInt("y");
            if (y >= yMin && y < yMax) {
                cubeTEs.add(teTag);
            }
        }
        return cubeTEs;
    }

    private ListTag filterTileTicks(ListTag tileTicks, int cubeY) {
        int yMin = cubeY * 16;
        int yMax = yMin + 16;
        ListTag cubeTicks = new ListTag(TagType.COMPOUND);
        for (Tag tileTickTag : tileTicks) {
            CompoundTag tileTick = (CompoundTag) tileTickTag;
            int y = tileTick.getInt("y");
            if (y >= yMin && y < yMax) {
                cubeTicks.add(tileTick);
            }
        }
        return cubeTicks;
    }

}
