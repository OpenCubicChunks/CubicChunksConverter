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
package cubicchunks.converter.lib.convert.cc2ccrelocating;

import com.flowpowered.nbt.*;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.flowpowered.nbt.stream.NBTOutputStream;
import cubicchunks.converter.lib.conf.ConverterConfig;
import cubicchunks.converter.lib.conf.command.EditTaskCommands;
import cubicchunks.converter.lib.conf.command.EditTaskContext;
import cubicchunks.converter.lib.convert.ChunkDataConverter;
import cubicchunks.converter.lib.convert.data.CubicChunksColumnData;
import cubicchunks.converter.lib.util.*;
import cubicchunks.converter.lib.util.edittask.BlockEditTask;
import cubicchunks.converter.lib.util.edittask.EditTask;
import cubicchunks.regionlib.impl.EntryLocation2D;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static cubicchunks.converter.lib.util.Utils.readCompressedCC;
import static cubicchunks.converter.lib.util.Utils.writeCompressed;

public class CC2CCRelocatingDataConverter implements ChunkDataConverter<CubicChunksColumnData, CubicChunksColumnData> {

    private final List<EditTask> relocateTasks;

    private static final Logger LOGGER = Logger.getLogger(CC2CCRelocatingDataConverter.class.getSimpleName());

    @SuppressWarnings("unchecked")
    public CC2CCRelocatingDataConverter(ConverterConfig config) {
        relocateTasks = (List<EditTask>) config.getValue("relocations");
    }

    public static ConverterConfig loadConfig(Consumer<Throwable> throwableConsumer) {
        ConverterConfig conf = new ConverterConfig(new HashMap<>());
        try {
            conf.set("relocations", loadDataFromFile("relocatingConfig.txt"));
        } catch (IOException | RuntimeException e) {
            throwableConsumer.accept(e);
            return null;
        }
        return conf;
    }

    private static List<EditTask> loadDataFromFile(String filename) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filename));

        EditTaskContext context = new EditTaskContext();
        for(String line : lines) {
            line = line.trim();
            if(line.isEmpty() || line.startsWith("//") || line.startsWith("#")) continue;

            EditTaskCommands.handleCommand(context, line);
        }

        return context.getTasks();
    }

    @Override public Set<CubicChunksColumnData> convert(CubicChunksColumnData input) {
        Map<Integer, ByteBuffer> inCubes = input.getCubeData();
        Map<Integer, ByteBuffer> cubes = new HashMap<>();

        //Split out cubes that are only in a keep tasked bounding box
        Map<Integer, ByteBuffer> keepOnlyCubes = new HashMap<>();
        for(Map.Entry<Integer, ByteBuffer> entry : inCubes.entrySet()) {
            cubes.put(entry.getKey(), entry.getValue());
            boolean anyNonKeep = false;
            boolean anyTask = false;
            for(EditTask task : relocateTasks) {
                if(task.getSourceBox().intersects(input.getPosition().getEntryX(), entry.getKey(), input.getPosition().getEntryZ())) {
                    anyTask = true;
                    if(task.getType() != EditTask.Type.KEEP) {
                        anyNonKeep = true;
                        break;
                    }
                }
                if(task.getOffset() != null)
                    if(task.getSourceBox().add(task.getOffset()).intersects(input.getPosition().getEntryX(), entry.getKey(), input.getPosition().getEntryZ())) {
                        anyTask = true;
                        if(task.getType() != EditTask.Type.KEEP) {
                            anyNonKeep = true;
                            break;
                        }
                    }
            }
            if(anyTask) {
                if(!anyNonKeep) {
                    keepOnlyCubes.put(entry.getKey(), entry.getValue());
                    cubes.remove(entry.getKey());
                }
            }
        }

        Map<Integer, CompoundTag> oldCubeTags = new HashMap<>();
        cubes.forEach((key, value) ->
                {
                    try {
                        oldCubeTags.put(key, readCompressedCC(new ByteArrayInputStream(value.array())));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );

        try {
            Map<Vector2i, Map<Integer, CompoundTag>> uncompressedData = relocateCubeData(oldCubeTags);

            Set<CubicChunksColumnData> columnData = new HashSet<>();
            EntryLocation2D inPos = input.getPosition();
            for (Map.Entry<Vector2i, Map<Integer, CompoundTag>> entry : uncompressedData.entrySet()) {
                ByteBuffer column = entry.getKey().getX() != inPos.getEntryX() || entry.getKey().getY() != inPos.getEntryZ() ? null : input.getColumnData();

                EntryLocation2D location = new EntryLocation2D(entry.getKey().getX(), entry.getKey().getY());
                columnData.add(new CubicChunksColumnData(input.getDimension(), location, column, compressCubeData(entry.getValue())));
            }
            if (!keepOnlyCubes.isEmpty()) {
                CubicChunksColumnData currentColumnData = columnData.stream()
                        .filter(x -> x.getPosition().equals(input.getPosition()))
                        .findAny()
                        .orElseGet(() -> new CubicChunksColumnData(input.getDimension(), input.getPosition(), input.getColumnData(), new HashMap<>()));
                currentColumnData.getCubeData().putAll(keepOnlyCubes);
                columnData.add(currentColumnData);
            }
            return columnData;

        } catch (IOException e) {
            throw new Error("Compressing cube data failed!", e);
        }
    }

    Map<Integer, ByteBuffer> compressCubeData(Map<Integer, CompoundTag> cubeData) throws IOException {
        Map<Integer, ByteBuffer> compressedData = new HashMap<>();
        for(Map.Entry<Integer, CompoundTag> entry : cubeData.entrySet()) {
            compressedData.put(entry.getKey(), writeCompressed(entry.getValue(), false));
        }
        return compressedData;
    }

    Map<Vector2i, Map<Integer, CompoundTag>> relocateCubeData(Map<Integer, CompoundTag> cubeDataOld) throws IOException {
        Map<Vector2i, Map<Integer, CompoundTag>> tagMap = new HashMap<>();

        for(Map.Entry<Integer, CompoundTag> entry : cubeDataOld.entrySet()) {
            CompoundMap level = (CompoundMap)entry.getValue().getValue().get("Level").getValue();

            int cubeX = (Integer) level.get("x").getValue();
            int cubeY = (Integer) level.get("y").getValue();
            int cubeZ = (Integer) level.get("z").getValue();

            if(!isCubeSrc(this.relocateTasks, cubeX, cubeY, cubeZ) && !isCubeDst(this.relocateTasks, cubeX, cubeY, cubeZ)) {
                tagMap.computeIfAbsent(new Vector2i(cubeX, cubeZ), key->new HashMap<>()).put(cubeY, entry.getValue());
                continue;
            }

            for (EditTask task : this.relocateTasks) {
                if(task.getType() == EditTask.Type.KEEP) {
                    continue;
                }

                BoundingBox sourceBox = task.getSourceBox();
                Vector3i offset = task.getOffset();

                if (!sourceBox.intersects(cubeX, cubeY, cubeZ)) {
                    continue;
                }

                switch (task.getType()) {
                    case REMOVE: {
                        Vector2i vector2i = new Vector2i(cubeX, cubeZ);
                        Map<Integer, CompoundTag> column = tagMap.get(vector2i);
                        if(column != null) {
                            column.remove(cubeY);
                            if (column.isEmpty())
                                tagMap.remove(vector2i);
                        } else
                            tagMap.remove(vector2i);
                        break;
                    }
                    case COPY: {
                        if(!(isCubeDstExclusive(task, cubeX, cubeY, cubeZ) && isCubeSrc(task, cubeX, cubeY, cubeZ))) {
                            //this is just doing a deep copy of the tag by writing to byte array then back again
                            ByteArrayOutputStream bout = new ByteArrayOutputStream(1024);
                            NBTOutputStream out = new NBTOutputStream(bout, false);
                            out.writeTag(entry.getValue());

                            NBTInputStream is = new NBTInputStream(new ByteArrayInputStream(bout.toByteArray()), false);
                            Tag tag = is.readTag();
                            //copy done here ^

                            CompoundMap srcLevel = (CompoundMap) ((CompoundTag) tag).getValue().get("Level").getValue();

                            srcLevel.put(new ByteTag("isSurfaceTracked", (byte) 0));
                            srcLevel.put(new ByteTag("initLightDone", (byte) 0));

                            tagMap.computeIfAbsent(new Vector2i(cubeX, cubeZ), key -> new HashMap<>()).put(cubeY, (CompoundTag) tag);
                        }

                        int dstX = cubeX + offset.getX();
                        int dstY = cubeY + offset.getY();
                        int dstZ = cubeZ + offset.getZ();
                        level.put(new IntTag("x", dstX));
                        level.put(new IntTag("y", dstY));
                        level.put(new IntTag("z", dstZ));

                        tagMap.computeIfAbsent(new Vector2i(dstX, dstZ), key->new HashMap<>()).put(dstY, entry.getValue());

                        break;
                    }
                    case CUT: {
                        if(!(isCubeDstExclusive(task, cubeX, cubeY, cubeZ) && isCubeSrc(task, cubeX, cubeY, cubeZ))) {
                            //REPLACE EVERYTHING IN SECTIONS WITH 0
                            //this is just doing a deep copy of the tag by writing to byte array then back again
                            ByteArrayOutputStream bout = new ByteArrayOutputStream(1024);
                            NBTOutputStream out = new NBTOutputStream(bout, false);
                            out.writeTag(entry.getValue());

                            NBTInputStream is = new NBTInputStream(new ByteArrayInputStream(bout.toByteArray()), false);
                            Tag srcTag = is.readTag();
                            //copy done here ^

                            CompoundMap srcLevel = (CompoundMap) ((CompoundTag) srcTag).getValue().get("Level").getValue();
                            CompoundMap sectionDetails;
                            try {
                                sectionDetails = ((CompoundTag) ((List) srcLevel.get("Sections").getValue()).get(0)).getValue(); //POSSIBLE ARRAY OUT OF BOUNDS EXCEPTION ON A MALFORMED CUBE
                            } catch (NullPointerException e) {
                                LOGGER.warning("Null Sections array for cube at position (" + cubeX + ", " + cubeY + ", " + cubeZ + "), skipping!");
                                continue;
                            }
                            sectionDetails.putIfAbsent("Add", null);
                            sectionDetails.remove("Add");

                            Arrays.fill((byte[]) sectionDetails.get("Blocks").getValue(), (byte) 0);
                            Arrays.fill((byte[]) sectionDetails.get("Data").getValue(), (byte) 0);
                            Arrays.fill((byte[]) sectionDetails.get("BlockLight").getValue(), (byte) 0);
                            Arrays.fill((byte[]) sectionDetails.get("SkyLight").getValue(), (byte) 0);

                            srcLevel.put(new ByteTag("isSurfaceTracked", (byte) 0));
                            srcLevel.put(new ByteTag("initLightDone", (byte) 0));

                            tagMap.computeIfAbsent(new Vector2i(cubeX, cubeZ), key -> new HashMap<>()).put(cubeY, (CompoundTag) srcTag);
                            if (offset == null) continue;
                        }

                        int dstX = cubeX + offset.getX();
                        int dstY = cubeY + offset.getY();
                        int dstZ = cubeZ + offset.getZ();
                        level.put(new IntTag("x", dstX));
                        level.put(new IntTag("y", dstY));
                        level.put(new IntTag("z", dstZ));

                        tagMap.computeIfAbsent(new Vector2i(dstX, dstZ), key->new HashMap<>()).put(dstY, entry.getValue());

                        break;
                    }
                    case SET: {
                        CompoundMap entryLevel = (CompoundMap) entry.getValue().getValue().get("Level").getValue();
                        entryLevel.put(new ByteTag("isSurfaceTracked", (byte) 0));
                        entryLevel.put(new ByteTag("initLightDone", (byte) 0));

                        BlockEditTask blockTask = (BlockEditTask) task;

                        CompoundMap sectionDetails;
                        try {
                            sectionDetails = ((CompoundTag)((List) (entryLevel).get("Sections").getValue()).get(0)).getValue(); //POSSIBLE ARRAY OUT OF BOUNDS EXCEPTION ON A MALFORMED CUBE
                        } catch(NullPointerException | ArrayIndexOutOfBoundsException e) {
                            LOGGER.warning("Malformed cube at position (" + cubeX + ", " + cubeY + ", " + cubeZ + "), skipping!");
                            continue;
                        }
                        Arrays.fill((byte[]) sectionDetails.get("Blocks").getValue(), blockTask.getOutBlockId());
                        Arrays.fill((byte[]) sectionDetails.get("Data").getValue(), (byte) (blockTask.getOutBlockMeta() | blockTask.getOutBlockMeta() << 4));

                        tagMap.computeIfAbsent(new Vector2i(cubeX, cubeZ), key -> new HashMap<>()).put(cubeY, entry.getValue());

                        break;
                    }
                    case REPLACE: {
                        CompoundMap entryLevel = (CompoundMap) entry.getValue().getValue().get("Level").getValue();
                        entryLevel.put(new ByteTag("isSurfaceTracked", (byte) 0));
                        entryLevel.put(new ByteTag("initLightDone", (byte) 0));

                        BlockEditTask blockTask = (BlockEditTask) task;

                        CompoundMap sectionDetails;
                        try {
                            sectionDetails = ((CompoundTag) ((List)entryLevel.get("Sections").getValue()).get(0)).getValue(); //POSSIBLE ARRAY OUT OF BOUNDS EXCEPTION ON A MALFORMED CUBE
                        } catch(NullPointerException e) {
                            LOGGER.warning("Null Sections array for cube at position (" + cubeX + ", " + cubeY + ", " + cubeZ + "), skipping!");
                            continue;
                        }

                        byte[] blocks = (byte[]) sectionDetails.get("Blocks").getValue();
                        byte[] meta = (byte[]) sectionDetails.get("Data").getValue();

                        Byte inBlockId = blockTask.getInBlockId();
                        Byte inBlockMeta = blockTask.getInBlockMeta();
                        byte outBlockId = blockTask.getOutBlockId();
                        byte outBlockMeta = blockTask.getOutBlockMeta();

                        for (int i = 0; i < 4096; i++) {
                            if(blocks[i] == inBlockId && nibbleGetAtIndex(meta, i) == inBlockMeta) {
                                blocks[i] = outBlockId;
                                nibbleSetAtIndex(meta, i, outBlockMeta);
                            }
                        }
                        tagMap.computeIfAbsent(new Vector2i(cubeX, cubeZ), key -> new HashMap<>()).put(cubeY, entry.getValue());

                        break;
                    }
                }
            }
        }

        return tagMap;
    }

    private static boolean isCubeSrc(List<EditTask> tasks, int x, int y, int z) {
        for (EditTask editTask : tasks) {
            if (isCubeSrc(editTask, x, y, z))
                return true;
        }
        return false;
    }
    private static boolean isCubeDst(List<EditTask> tasks, int x, int y, int z) {
        for (EditTask editTask : tasks) {
            if(isCubeDst(editTask, x, y, z))
                return true;
        }
        return false;
    }
    private static boolean isCubeSrc(EditTask editTask, int x, int y, int z) {
        return editTask.getSourceBox().intersects(x, y, z);
    }
    private static boolean isCubeDst(EditTask editTask, int x, int y, int z) {
        if (editTask.getOffset() != null) {
            if (editTask.getSourceBox().add(editTask.getOffset()).intersects(x, y, z))
                return true;
        }

        if(editTask.getType() == EditTask.Type.CUT || editTask.getType() == EditTask.Type.REMOVE) {
            return editTask.getSourceBox().intersects(x, y, z);
        }
        return false;
    }
    private static boolean isCubeDstExclusive(EditTask editTask, int x, int y, int z) {
        if (editTask.getOffset() != null)
            return editTask.getSourceBox().add(editTask.getOffset()).intersects(x, y, z);
        return false;
    }

    public static boolean isRegionInCopyOrPasteLoc(List<EditTask> tasks, int x, int y, int z) {
        for(EditTask task : tasks) {
            if (task.getSourceBox().intersects(x, y, z)) {
                if (task.getOffset() == null) continue;
                if (task.getSourceBox().intersects(
                        x - task.getOffset().getX(),
                        y - task.getOffset().getY(),
                        z - task.getOffset().getZ())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int nibbleGetAtIndex(byte[] arr, int index)
    {
        int i = index >> 1;
        return (index & 1) == 0 ? arr[i] & 0xf : arr[i] >> 4 & 0xf;
    }

    private static void nibbleSetAtIndex(byte[] arr, int index, int value)
    {
        int i = index >> 1;

        if ((index & 1) == 0) {
            arr[i] = (byte)(arr[i] & 0xf0 | value & 0xf);
        }
        else {
            arr[i] = (byte)(arr[i] & 0xf | (value & 0xf) << 4);
        }
    }
}
