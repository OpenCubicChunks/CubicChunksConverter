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
import cubicchunks.converter.lib.convert.ChunkDataConverter;
import cubicchunks.converter.lib.convert.data.CubicChunksColumnData;
import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.Vector2i;
import cubicchunks.converter.lib.util.Vector3i;
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

        List<EditTask> tasks = new ArrayList<>();
        for (String line : lines) {

            String[] split = line.split(" ");

            if(split.length <= 1) continue;

            //This is the cut and paste format
            //example: `mv 0 0 0 3 3 3 to 3 0 0`
            //example: `cp 0 0 0 3 3 3 by 3 0 0`
            EditTask.Type type = EditTask.Type.NONE;

            switch (split[0]) {
                case "ct": case "cut":
                    type = EditTask.Type.CUT;
                    break;
                case "cp": case "copy":
                    type = EditTask.Type.COPY;
                    break;
                case "mv": case "move":
                    type = EditTask.Type.MOVE;
                    break;
                case "rm": case "remove":
                    type = EditTask.Type.REMOVE;
                    break;
                case "kp": case "keep":
                    type = EditTask.Type.KEEP;
                    break;
                default:
                    LOGGER.warning("Unknown command: \"" + split[0] + "\"");

            }
            if(type == EditTask.Type.MOVE || type == EditTask.Type.COPY) {
                BoundingBox srcBox = new BoundingBox(
                        Integer.parseInt(split[1]),
                        Integer.parseInt(split[2]),
                        Integer.parseInt(split[3]),
                        Integer.parseInt(split[4]),
                        Integer.parseInt(split[5]),
                        Integer.parseInt(split[6])
                );
                Vector3i offsetPos;
                if (split[7].equals("to")) {
                    offsetPos = new Vector3i(
                        Integer.parseInt(split[8]) - srcBox.getMinPos().getX(),
                        Integer.parseInt(split[9]) - srcBox.getMinPos().getY(),
                        Integer.parseInt(split[10]) - srcBox.getMinPos().getZ()
                    );
                } else if (split[7].equals("by")) {
                    offsetPos = new Vector3i(
                        Integer.parseInt(split[8]),
                        Integer.parseInt(split[9]),
                        Integer.parseInt(split[10])
                    );
                } else {
                    throw new UnsupportedOperationException("Please use commands \"to\" or \"by\", not \"" + split[7] + "\".");
                }
                tasks.add(new EditTask(srcBox, offsetPos, type));
            } else if(type == EditTask.Type.REMOVE || type == EditTask.Type.KEEP) {
                BoundingBox srcBox;
                if(split[1].equals("all")) {
                    srcBox = new BoundingBox(
                            Integer.MIN_VALUE,
                            Integer.MIN_VALUE,
                            Integer.MIN_VALUE,
                            Integer.MAX_VALUE,
                            Integer.MAX_VALUE,
                            Integer.MAX_VALUE
                    );
                } else {
                    srcBox = new BoundingBox(
                            Integer.parseInt(split[1]),
                            Integer.parseInt(split[2]),
                            Integer.parseInt(split[3]),
                            Integer.parseInt(split[4]),
                            Integer.parseInt(split[5]),
                            Integer.parseInt(split[6])
                    );
                }

                tasks.add(new EditTask(srcBox, type == EditTask.Type.KEEP ? new Vector3i(0, 0, 0) : null, type));
            } else if(type == EditTask.Type.CUT) {
                BoundingBox srcBox;
                if(split[1].equals("all")) {
                    srcBox = new BoundingBox(
                            Integer.MIN_VALUE,
                            Integer.MIN_VALUE,
                            Integer.MIN_VALUE,
                            Integer.MAX_VALUE,
                            Integer.MAX_VALUE,
                            Integer.MAX_VALUE
                    );
                } else {
                    srcBox = new BoundingBox(
                            Integer.parseInt(split[1]),
                            Integer.parseInt(split[2]),
                            Integer.parseInt(split[3]),
                            Integer.parseInt(split[4]),
                            Integer.parseInt(split[5]),
                            Integer.parseInt(split[6])
                    );
                }
                Vector3i offsetPos = null;
                if(split.length > 7) {
                    if (split[7].equals("to")) {
                        offsetPos = new Vector3i(
                                Integer.parseInt(split[8]) - srcBox.getMinPos().getX(),
                                Integer.parseInt(split[9]) - srcBox.getMinPos().getY(),
                                Integer.parseInt(split[10]) - srcBox.getMinPos().getZ()
                        );
                    } else if (split[7].equals("by")) {
                        offsetPos = new Vector3i(
                                Integer.parseInt(split[8]),
                                Integer.parseInt(split[9]),
                                Integer.parseInt(split[10])
                        );
                    } else {
                        throw new UnsupportedOperationException("Please use commands \"to\" or \"by\", not \"" + split[7] + "\".");
                    }
                }
                tasks.add(new EditTask(srcBox, offsetPos, type));
            }
        }
        return tasks;
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
            for (Map.Entry<Integer, ByteBuffer> entry : keepOnlyCubes.entrySet()) {
                Map<Integer, ByteBuffer> map = new HashMap<>();
                map.put(entry.getKey(), entry.getValue());

                columnData.add(new CubicChunksColumnData(input.getDimension(), input.getPosition(), input.getColumnData(), map));
            }

            return columnData;

        }catch (IOException e) {
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

            boolean modified = false;
            boolean deleted = false;
            for (EditTask task : this.relocateTasks) {
                if(task.getType() == EditTask.Type.KEEP) {
                    continue;
                }
                if (task.getType() == EditTask.Type.REMOVE && task.getSourceBox().intersects(cubeX, cubeY, cubeZ)) {
                    deleted = true;
                    continue;
                }

                BoundingBox sourceBox = task.getSourceBox();
                Vector3i offset = task.getOffset();

                if (!sourceBox.intersects(cubeX, cubeY, cubeZ)) {
                    continue;
                }
                modified = true;
                if(task.getType() == EditTask.Type.COPY) {
                    //this is just doing a deep copy of the tag by writing to byte array then back again
                    ByteArrayOutputStream bout = new ByteArrayOutputStream(1024);
                    NBTOutputStream out = new NBTOutputStream(bout, false);
                    out.writeTag(entry.getValue());

                    NBTInputStream is = new NBTInputStream(new ByteArrayInputStream(bout.toByteArray()), false);
                    Tag tag = is.readTag();
                    //copy done here ^
                    tagMap.computeIfAbsent(new Vector2i(cubeX, cubeZ), key->new HashMap<>()).put(cubeY, (CompoundTag)tag);
                }
                else if(task.getType() == EditTask.Type.CUT) {
                    //REPLACE EVERYTHING IN SECTIONS WITH 0
                    //this is just doing a deep copy of the tag by writing to byte array then back again
                    ByteArrayOutputStream bout = new ByteArrayOutputStream(1024);
                    NBTOutputStream out = new NBTOutputStream(bout, false);
                    out.writeTag(entry.getValue());

                    NBTInputStream is = new NBTInputStream(new ByteArrayInputStream(bout.toByteArray()), false);
                    Tag tag = is.readTag();
                    //copy done here ^

                    CompoundMap sectionDetails = null;
                    List sectionsList = (List)((CompoundMap)((CompoundTag)tag).getValue().get("Level").getValue()).get("Sections").getValue();
                    sectionDetails = ((CompoundTag)sectionsList.get(0)).getValue(); //POSSIBLE ARRAY OUT OF BOUNDS EXCEPTION ON A MALFORMED CUBE

                    sectionDetails.putIfAbsent("Add", null);
                    sectionDetails.remove("Add");

                    Arrays.fill((byte[])sectionDetails.get("Blocks").getValue(), (byte) 0);
                    Arrays.fill((byte[])sectionDetails.get("Data").getValue(), (byte) 0);
                    Arrays.fill((byte[])sectionDetails.get("BlockLight").getValue(), (byte) 0);
                    Arrays.fill((byte[])sectionDetails.get("SkyLight").getValue(), (byte) 0);

                    tagMap.computeIfAbsent(new Vector2i(cubeX, cubeZ), key->new HashMap<>()).put(cubeY, (CompoundTag)tag);
                    if(offset == null) continue;
                }

                int dstX = cubeX + offset.getX();
                int dstY = cubeY + offset.getY();
                int dstZ = cubeZ + offset.getZ();
                level.put(new IntTag("x", dstX));
                level.put(new IntTag("y", dstY));
                level.put(new IntTag("z", dstZ));

                tagMap.computeIfAbsent(new Vector2i(dstX, dstZ), key->new HashMap<>()).put(dstY, entry.getValue());
            }
            if(deleted) {
                Vector2i vector2i = new Vector2i(cubeX, cubeZ);
                Map<Integer, CompoundTag> column = tagMap.computeIfAbsent(vector2i, key -> new HashMap<>());
                column.remove(cubeY);
                if(column.isEmpty())
                    tagMap.remove(vector2i);
                continue;
            }
            if(!modified && !isCubeInCopyOrPasteLoc(this.relocateTasks, cubeX, cubeY, cubeZ)) {
                tagMap.computeIfAbsent(new Vector2i(cubeX, cubeZ), key->new HashMap<>()).put(cubeY, entry.getValue());
            }
        }

        return tagMap;
    }
    //Returns true if cube data is going to be used for copy
    private static boolean isCubeInCopyOrPasteLoc(List<EditTask> tasks, int x, int y, int z) {
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

    private boolean isColumnInCopyOrPasteLoc(int x, int z) {
        for(EditTask task : this.relocateTasks) {
            if (task.getSourceBox().columnIntersects(x, z) || task.getSourceBox().columnIntersects(
                    x - task.getOffset().getX(),
                    z - task.getOffset().getZ()))
                return true;
        }
        return false;
    }

    public static final class EditTask {
        private final BoundingBox source;
        private final Vector3i offset;

        public enum Type { NONE, CUT, COPY, MOVE, REMOVE, KEEP }
        private final Type type;

        public EditTask(BoundingBox src, Vector3i offset, Type type) {
            this.source = src;
            this.offset = offset;
            this.type = type;
        }

        public BoundingBox getSourceBox() {
            return this.source;
        }

        public Vector3i getOffset() {
            return this.offset;
        }

        public Type getType() {
            return this.type;
        }
    }

}
