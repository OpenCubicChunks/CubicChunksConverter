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
package cubicchunks.converter.lib.convert.cc2ccstacked;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.IntTag;
import com.flowpowered.nbt.Tag;
import cubicchunks.converter.lib.convert.ChunkDataConverter;
import cubicchunks.converter.lib.convert.data.CubicChunksColumnData;
import cubicchunks.regionlib.impl.EntryLocation2D;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static cubicchunks.converter.lib.util.Utils.readCompressedCC;
import static cubicchunks.converter.lib.util.Utils.writeCompressed;

public class CC2CCStackedDataConverter implements ChunkDataConverter<CubicChunksColumnData, CubicChunksColumnData> {

    List<List<BoundingBox>> tilePositionsList;
    int[] dstTileIdx = { 1, 1 };

    private final int[][] boundingBoxOffsets;

    public CC2CCStackedDataConverter() {
        try {
            this.loadDataFromFile("stackedConfig.txt");
        } catch (IOException e) {
            throw new UncheckedIOException("stackedConfig.txt doesnt exist!", e);
        }

        boundingBoxOffsets = new int[tilePositionsList.size()][];
        for (int k = 0; k < tilePositionsList.size(); k++) {
            boundingBoxOffsets[k] = new int[tilePositionsList.get(k).size()];
            for (int i = 0; i < boundingBoxOffsets[k].length; i++) {
                int offset = 0;
                for (int j = 0; j < i; j++) {
                    offset += (tilePositionsList.get(k).get(j).maxY - tilePositionsList.get(k).get(j).minY) + 1;
                }
                boundingBoxOffsets[k][i] = offset;
            }
            int dstOffset = boundingBoxOffsets[k][dstTileIdx[k]];
            for (int i = 0; i < boundingBoxOffsets[k].length; i++) {
                boundingBoxOffsets[k][i] -= dstOffset;
                boundingBoxOffsets[k][i] += tilePositionsList.get(k).get(dstTileIdx[k]).minY;
            }
        }
    }


    void loadDataFromFile(String filename) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filename));

        Map<String, Integer> mainIndices = new HashMap<>();
        Map<String, List<BoundingBox>> boxes = new HashMap<>();

        int cutPasteIdx = 0;
        for (String line : lines) {

            String[] split = line.split(" ");

            if(split.length <= 1) continue;

            //This is the original stacking format
            if(split[1].equals("stack") || split[1].equals("origin")) {
                boxes.computeIfAbsent(split[0], list->new ArrayList<>()).add(new BoundingBox(
                        Integer.parseInt(split[2]),
                        Integer.parseInt(split[3]),
                        Integer.parseInt(split[4]),
                        Integer.parseInt(split[5]),
                        Integer.parseInt(split[6]),
                        Integer.parseInt(split[7])
                ));
                if(split[1].equals("origin")) {
                    mainIndices.computeIfAbsent(split[0], size->boxes.containsKey(split[0]) ? boxes.get(split[0]).size() : 0);
                }
            }

            //This is the cut and paste format
            //example: `move 0 0 0 3 3 3 to 3 0 0`
            //example: `move 0 0 0 3 3 3 by 3 0 0`
            else if(split[0].equals("move")) {
                String boxName = "move" + cutPasteIdx;

                BoundingBox moveBox = new BoundingBox(
                        Integer.parseInt(split[1]),
                        Integer.parseInt(split[2]),
                        Integer.parseInt(split[3]),
                        Integer.parseInt(split[4]),
                        Integer.parseInt(split[5]),
                        Integer.parseInt(split[6])
                );
                boxes.computeIfAbsent(boxName, list -> new ArrayList<>()).add(moveBox);

                if (split[7].equals("to")) {
                    boxes.computeIfAbsent(boxName, list -> new ArrayList<>()).add(new BoundingBox(
                            Integer.parseInt(split[8]),
                            Integer.parseInt(split[9]),
                            Integer.parseInt(split[10]),
                            Integer.parseInt(split[8]) + moveBox.getSizeX(),
                            Integer.parseInt(split[9]) + moveBox.getSizeY(),
                            Integer.parseInt(split[10]) + moveBox.getSizeZ()
                    ));
                } else if (split[8].equals("by")) {
                    boxes.computeIfAbsent(boxName, list -> new ArrayList<>()).add(new BoundingBox(
                            moveBox.minX + Integer.parseInt(split[8]),
                            moveBox.minY + Integer.parseInt(split[9]),
                            moveBox.minZ + Integer.parseInt(split[10]),
                            moveBox.minX + Integer.parseInt(split[8]) + moveBox.getSizeX(),
                            moveBox.minY + Integer.parseInt(split[9]) + moveBox.getSizeY(),
                            moveBox.minZ + Integer.parseInt(split[10]) + moveBox.getSizeZ()
                    ));
                }
                mainIndices.putIfAbsent(boxName, 1);
            }
        }

        this.dstTileIdx = new int[mainIndices.values().size()];
        this.tilePositionsList = new ArrayList<>();

        int i = 0;
        for (String key : mainIndices.keySet()) {
            this.dstTileIdx[i] = mainIndices.get(key);
            this.tilePositionsList.add(boxes.get(key));
            i++;
        }
    }


    @Override public CubicChunksColumnData convert(CubicChunksColumnData input) {
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
            int[] columnPos = new int[2];
            Map<Integer, ByteBuffer> cubeData = compressCubeData(stackCubeData(oldCubeTags, columnPos));
            EntryLocation2D inPos = input.getPosition();
            ByteBuffer column = columnPos[0] != inPos.getEntryX() || columnPos[1] != inPos.getEntryZ() ? null : input.getColumnData();

            EntryLocation2D location = new EntryLocation2D(columnPos[0], columnPos[1]);
            return new CubicChunksColumnData(input.getDimension(), location, column, cubeData);

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

    Map<Integer, CompoundTag> stackCubeData(Map<Integer, CompoundTag> cubeDataOld, int[] columnPosOut) {

        Map<Integer, CompoundTag> tags = new HashMap<>();

        for(Map.Entry<Integer, CompoundTag> entry : cubeDataOld.entrySet()) {
            CompoundMap root = new CompoundMap();
            CompoundMap level = (CompoundMap)entry.getValue().getValue().get("Level").getValue();

            int cubeX = (Integer) level.get("x").getValue();
            int cubeY = (Integer) level.get("y").getValue();
            int cubeZ = (Integer) level.get("z").getValue();

            if (this.isColumnInCopyLoc(cubeX, cubeZ)) {
                if (!this.isCubeInCopyLoc(cubeX, cubeY, cubeZ)) {
                    continue;
                }
            }

            int dstX = cubeX;
            int dstY = cubeY;
            int dstZ = cubeZ;
            boolean successful = false;
            for (int tileListIdx = 0, positionsSize = tilePositionsList.size(); tileListIdx < positionsSize; tileListIdx++) {
                List<BoundingBox> tilePositions = tilePositionsList.get(tileListIdx);

                for (int i = 0, tilePositionsSize = tilePositions.size(); i < tilePositionsSize; i++) {
                    BoundingBox tilePosition = tilePositions.get(i);
                    if (tilePosition.intersects(cubeX, cubeY, cubeZ)) {
                        dstX = cubeX - tilePosition.minX + tilePositions.get(dstTileIdx[tileListIdx]).minX;
                        dstY = cubeY  - tilePosition.minY + boundingBoxOffsets[tileListIdx][i];
                        dstZ = cubeZ - tilePosition.minZ + tilePositions.get(dstTileIdx[tileListIdx]).minZ;

                        level.put(new IntTag("x", dstX));
                        level.put(new IntTag("y", dstY));
                        level.put(new IntTag("z", dstZ));
                        //System.out.printf("Index = %d, (%d, %d, %d) -> (%d, %d, %d)%n", i, cubeX, cubeY, cubeZ, dstX, dstY, dstZ);
                        successful = true;
                    }
                }
                if(successful) break;
            }
            root.put(new CompoundTag("Level", level));
            tags.put(dstY, new CompoundTag("", root));
            columnPosOut[0] = dstX;
            columnPosOut[1] = dstZ;
        }
        return tags;
    }

    private static CompoundMap cloneMap(CompoundMap map) {
        if (map == null) {
            return null;
        }

        CompoundMap newMap = new CompoundMap();
        for (Map.Entry<String, Tag<?>> entry : map.entrySet()) {
            newMap.put(entry.getKey(), entry.getValue().clone());
        }
        return newMap;
    }

    //Returns true if cube data is going to be used for copy
    private boolean isCubeInCopyLoc(int x, int y, int z) {
        for(List<BoundingBox> boxes : this.tilePositionsList) {
            for (BoundingBox box : boxes) {
                if (box.intersects(x, y, z)) return true;
            }
        }
        return false;
    }
    private boolean isColumnInCopyLoc(int x, int z) {
        for(List<BoundingBox> boxes : this.tilePositionsList) {
            for (BoundingBox box : boxes) {
                if (box.columnIntersects(x, z)) return true;
            }
        }
        return false;
    }

    private boolean isCubeInPasteLoc(int idx, int x, int y, int z) {
        return this.tilePositionsList.get(idx).get(dstTileIdx[idx]).intersects(x, y, z);
    }
    private boolean isColumnInPasteLoc(int idx, int x, int z) {
        return this.tilePositionsList.get(idx).get(dstTileIdx[idx]).columnIntersects(x, z);
    }

    private static class BoundingBox {
        final int minX, minY, minZ;
        final int maxX, maxY, maxZ;

        private BoundingBox(int[] values) {
            this.minX = values[0];
            this.minY = values[1];
            this.minZ = values[2];
            this.maxX = values[3];
            this.maxY = values[4];
            this.maxZ = values[5];
        }

        private BoundingBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        @Override public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            BoundingBox that = (BoundingBox) o;
            return minX == that.minX &&
                    minY == that.minY &&
                    minZ == that.minZ &&
                    maxX == that.maxX &&
                    maxY == that.maxY &&
                    maxZ == that.maxZ;
        }

        @Override public int hashCode() {
            return Objects.hash(minX, minY, minZ, maxX, maxY, maxZ);
        }

        public boolean intersects(int x, int y, int z) {
            return x >= minX && x <= maxX &&
                    y >= minY && y <= maxY &&
                    z >= minZ && z <= maxZ;
        }
        public boolean columnIntersects(int x, int z) {
            return x >= minX && x <= maxX &&
                    z >= minZ && z <= maxZ;
        }

        public int getSizeX() { return this.maxX - this.minX + 1; }
        public int getSizeY() { return this.maxY - this.minY + 1; }
        public int getSizeZ() { return this.maxZ - this.minZ + 1; }

    }
}
