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
import cubicchunks.converter.lib.convert.ChunkDataConverter;
import cubicchunks.converter.lib.convert.data.CubicChunksColumnData;
import cubicchunks.regionlib.impl.EntryLocation2D;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static cubicchunks.converter.lib.util.Utils.readCompressedCC;
import static cubicchunks.converter.lib.util.Utils.writeCompressed;

public class CC2CCRelocatingDataConverter implements ChunkDataConverter<CubicChunksColumnData, CubicChunksColumnData> {

    private final List<EditTask> relocateTasks;

    public CC2CCRelocatingDataConverter() {
        try {
            relocateTasks = this.loadDataFromFile("relocatingConfig.txt");
        } catch (IOException e) {
            throw new UncheckedIOException("relocatingConfig.txt doesn't exist!\n", e);
        }
    }

    List<EditTask> loadDataFromFile(String filename) throws IOException {
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
                case "ct":
                    type = EditTask.Type.CUT;
                    break;
                case "cp":
                    type = EditTask.Type.COPY;
                    break;
                case "mv":
                    type = EditTask.Type.MOVE;
                    break;
                case "rm":
                    type = EditTask.Type.REMOVE;
                    break;
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
                        Integer.parseInt(split[8]) - srcBox.minPos.getX(),
                        Integer.parseInt(split[9]) - srcBox.minPos.getY(),
                        Integer.parseInt(split[10]) - srcBox.minPos.getZ()
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
            } else if(type == EditTask.Type.REMOVE) {
                BoundingBox srcBox = new BoundingBox(
                        Integer.parseInt(split[1]),
                        Integer.parseInt(split[2]),
                        Integer.parseInt(split[3]),
                        Integer.parseInt(split[4]),
                        Integer.parseInt(split[5]),
                        Integer.parseInt(split[6])
                );

                tasks.add(new EditTask(srcBox, null, type));
            } else if(type == EditTask.Type.CUT) {
                BoundingBox srcBox = new BoundingBox(
                        Integer.parseInt(split[1]),
                        Integer.parseInt(split[2]),
                        Integer.parseInt(split[3]),
                        Integer.parseInt(split[4]),
                        Integer.parseInt(split[5]),
                        Integer.parseInt(split[6])
                );
                Vector3i offsetPos = null;
                if(split.length > 7) {
                    if (split[7].equals("to")) {
                        offsetPos = new Vector3i(
                                Integer.parseInt(split[8]) - srcBox.minPos.getX(),
                                Integer.parseInt(split[9]) - srcBox.minPos.getY(),
                                Integer.parseInt(split[10]) - srcBox.minPos.getZ()
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
        Map<Integer, ByteBuffer> cubes = input.getCubeData();

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
                else if(task.getType() == EditTask.Type.CUT) { //REPLACE EVERYTHING IN SECTIONS WITH 0
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
            if(!modified && !this.isCubeInCopyOrPasteLoc(cubeX, cubeY, cubeZ)) {
                tagMap.computeIfAbsent(new Vector2i(cubeX, cubeZ), key->new HashMap<>()).put(cubeY, entry.getValue());
            }
        }

        return tagMap;
    }

    //Returns true if cube data is going to be used for copy
    private boolean isCubeInCopyOrPasteLoc(int x, int y, int z) {
        for(EditTask task : this.relocateTasks) {
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

    private static final class EditTask {
        private final BoundingBox source;
        private final Vector3i offset;

        public enum Type { NONE, CUT, COPY, MOVE, REMOVE }
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

    private static final class Vector2i {
        private final int x;
        private final int y;

        public Vector2i(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }
        public int getY() {
            return y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Vector2i vector2i = (Vector2i) o;
            return x == vector2i.x &&
                    y == vector2i.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }

        @Override
        public String toString() {
            return "Vector2i{" +
                    "x=" + x +
                    ", y=" + y +
                    '}';
        }
    }
    private static final class Vector3i {
        private final int x;
        private final int y;
        private final int z;

        public Vector3i(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public int getX() {
            return x;
        }
        public int getY() {
            return y;
        }
        public int getZ() {
            return z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Vector3i vector3i = (Vector3i) o;
            return x == vector3i.x &&
                    y == vector3i.y &&
                    z == vector3i.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }

        @Override
        public String toString() {
            return "Vector3i{" +
                    "x=" + x +
                    ", y=" + y +
                    ", z=" + z +
                    '}';
        }
    }
    private static final class BoundingBox {
        private final Vector3i minPos;
        private final Vector3i maxPos;

        private BoundingBox(int[] values) {
            this(values[0], values[1], values[2], values[3], values[4], values[5]);
        }

        private BoundingBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            minPos = new Vector3i(minX, minY, minZ);
            maxPos = new Vector3i(maxX, maxY, maxZ);
        }

        @Override public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            BoundingBox that = (BoundingBox) o;
            return minPos.x == that.minPos.x &&
                    minPos.y == that.minPos.y &&
                    minPos.z == that.minPos.z &&
                    maxPos.x == that.maxPos.x &&
                    maxPos.y == that.maxPos.y &&
                    maxPos.z == that.maxPos.z;
        }

        @Override public int hashCode() {
            return Objects.hash(minPos.x, minPos.y, minPos.z, maxPos.x, maxPos.y, maxPos.z);
        }
        public boolean intersects(int x, int y, int z) {
            return x >= minPos.x && x <= maxPos.x &&
                    y >= minPos.y && y <= maxPos.y &&
                    z >= minPos.z && z <= maxPos.z;
        }

        public boolean columnIntersects(int x, int z) {
            return x >= minPos.x && x <= maxPos.x &&
                    z >= minPos.z && z <= maxPos.z;
        }

        public int getSizeX() { return this.maxPos.x - this.minPos.x + 1; }
        public int getSizeY() { return this.maxPos.y - this.minPos.y + 1; }
        public int getSizeZ() { return this.maxPos.z - this.minPos.z + 1; }

        public Vector3i getMinPos() {
            return minPos;
        }
        public Vector3i getMaxPos() {
            return maxPos;
        }

        @Override
        public String toString() {
            return "BoundingBox{" +
                    "minPos=" + minPos +
                    ", maxPos=" + maxPos +
                    '}';
        }
    }
}
