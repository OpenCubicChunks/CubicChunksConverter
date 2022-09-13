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
package cubicchunks.converter.lib.convert.cc2ccrelocating;

import com.flowpowered.nbt.*;
import cubicchunks.converter.lib.Dimension;
import cubicchunks.converter.lib.conf.ConverterConfig;
import cubicchunks.converter.lib.conf.command.EditTaskCommands;
import cubicchunks.converter.lib.conf.command.EditTaskContext;
import cubicchunks.converter.lib.convert.ChunkDataConverter;
import cubicchunks.converter.lib.convert.data.PriorityCubicChunksColumnData;
import cubicchunks.converter.lib.util.*;
import cubicchunks.converter.lib.util.edittask.EditTask;
import cubicchunks.regionlib.impl.EntryLocation2D;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static cubicchunks.converter.lib.util.Utils.readCompressedCC;
import static cubicchunks.converter.lib.util.Utils.writeCompressed;

public class CC2CCRelocatingDataConverter implements ChunkDataConverter<PriorityCubicChunksColumnData, PriorityCubicChunksColumnData> {

    private final List<EditTask> relocateTasks;
    private final EditTaskContext.EditTaskConfig config;

    private static final Logger LOGGER = Logger.getLogger(CC2CCRelocatingDataConverter.class.getSimpleName());

    @SuppressWarnings("unchecked")
    public CC2CCRelocatingDataConverter(ConverterConfig config) {
        this.relocateTasks = (List<EditTask>) config.getValue("relocations");
        this.config = new EditTaskContext.EditTaskConfig();
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

    @Override public Set<PriorityCubicChunksColumnData> convert(PriorityCubicChunksColumnData input) {
        Map<Integer, ImmutablePair<Long, ByteBuffer>> inCubes = input.getCubeData();
        Map<Integer, ImmutablePair<Long, ByteBuffer>> cubes = new HashMap<>();

        //Split out cubes that are only in a keep tasked bounding box
        Map<Integer, ImmutablePair<Long, ByteBuffer>> noReadCubes = new HashMap<>();
        EntryLocation2D inPosition = input.getPosition();
        for(Map.Entry<Integer, ImmutablePair<Long, ByteBuffer>> entry : inCubes.entrySet()) {
            boolean anyBoxNeedsData = false;
            boolean intersectsSrcBox = false;

            for(EditTask task : relocateTasks) {
                List<BoundingBox> srcBoxes = task.getSrcBoxes();
                for (BoundingBox srcBox : srcBoxes) {
                    if(srcBox.intersects(inPosition.getEntryX(), entry.getKey(), inPosition.getEntryZ())) {
                        intersectsSrcBox = true;
                        if(task.readsCubeData()) {
                            anyBoxNeedsData = true;
                            break;
                        }
                    }
                }
            }
            if(intersectsSrcBox) {
                if (anyBoxNeedsData)
                    cubes.put(entry.getKey(), entry.getValue());
                else
                    noReadCubes.put(entry.getKey(), entry.getValue());
            } else
                noReadCubes.put(entry.getKey(), entry.getValue());
        }

        Map<Integer, ImmutablePair<Long, CompoundTag>> inCubeData = new HashMap<>();
        cubes.forEach((key, value) -> {
            try {
                inCubeData.put(key, new ImmutablePair<>(value.getKey(), readCompressedCC(new ByteArrayInputStream(value.getValue().array()))));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        try {
            Map<Vector2i, Map<Integer, ImmutablePair<Long, CompoundTag>>> outCubeData = relocateCubeData(input.getDimension(), inCubeData, this.config);

            Set<PriorityCubicChunksColumnData> columnData = new HashSet<>();
            for (Map.Entry<Vector2i, Map<Integer, ImmutablePair<Long, CompoundTag>>> entry : outCubeData.entrySet()) {
                Vector2i key = entry.getKey();
                ByteBuffer column = key.getX() != inPosition.getEntryX() || key.getY() != inPosition.getEntryZ() ? null : input.getColumnData();

                EntryLocation2D location = new EntryLocation2D(key.getX(), key.getY());
                columnData.add(new PriorityCubicChunksColumnData(input.getDimension(), location, column, compressCubeData(entry.getValue()), true));
            }
            if (!noReadCubes.isEmpty()) {
                PriorityCubicChunksColumnData currentColumnData = columnData.stream()
                        .filter(column -> column.getPosition().equals(inPosition)).findAny()
                        .orElseGet(() -> new PriorityCubicChunksColumnData(input.getDimension(), inPosition, input.getColumnData(), new HashMap<>(), true));
                noReadCubes.forEach((yPos, buffer) -> currentColumnData.getCubeData().putIfAbsent(yPos, buffer));
                columnData.add(currentColumnData);
            }
            return columnData;

        } catch (IOException e) {
            throw new Error("Compressing cube data failed!", e);
        }
    }

    Map<Integer, ImmutablePair<Long, ByteBuffer>> compressCubeData(Map<Integer, ImmutablePair<Long, CompoundTag>> cubeData) throws IOException {
        Map<Integer, ImmutablePair<Long, ByteBuffer>> compressedData = new HashMap<>();
        for(Map.Entry<Integer, ImmutablePair<Long, CompoundTag>> entry : cubeData.entrySet()) {
            compressedData.put(entry.getKey(), new ImmutablePair<>(entry.getValue().getKey(), writeCompressed(entry.getValue().getValue(), false)));
        }
        return compressedData;
    }

    Map<Vector2i, Map<Integer, ImmutablePair<Long, CompoundTag>>> relocateCubeData(Dimension dimension, Map<Integer, ImmutablePair<Long, CompoundTag>> cubeDataOld, EditTaskContext.EditTaskConfig config) throws IOException {
        Map<Vector2i, Map<Integer, ImmutablePair<Long, CompoundTag>>> tagMap = new HashMap<>();

        for(Map.Entry<Integer, ImmutablePair<Long, CompoundTag>> entry : cubeDataOld.entrySet()) {
            CompoundMap level = (CompoundMap)entry.getValue().getValue().getValue().get("Level").getValue();

            int cubeX = (Integer) level.get("x").getValue();
            int cubeY = (Integer) level.get("y").getValue();
            int cubeZ = (Integer) level.get("z").getValue();

            for (EditTask task : this.relocateTasks) {
                if (!task.handlesDimension(dimension.getDirectory())) {
                    continue;
                }
                task.initialise(config);
                if(!task.readsCubeData()) {
                    continue;
                }

                boolean cubeIsSrc = false;
                for (BoundingBox sourceBox : task.getSrcBoxes()) {
                    if(sourceBox.intersects(cubeX, cubeY, cubeZ)) {
                        cubeIsSrc = true;
                        break;
                    }
                }
                if(!cubeIsSrc)
                    continue;

                List<ImmutablePair<Vector3i, ImmutablePair<Long, CompoundTag>>> outputCubes = task.actOnCube(new Vector3i(cubeX, cubeY, cubeZ), config, entry.getValue().getValue(), entry.getKey());

                outputCubes.forEach(positionTagPriority -> {
                    Vector3i cubePos = positionTagPriority.getKey();
                    ImmutablePair<Long, CompoundTag> tagPriority = positionTagPriority.getValue();
                    if(tagPriority.getValue() == null)
                        tagMap.computeIfAbsent(new Vector2i(cubePos.getX(), cubePos.getZ()), pos -> new HashMap<>()).remove(cubePos.getY());
                    else
                        tagMap.computeIfAbsent(new Vector2i(cubePos.getX(), cubePos.getZ()), pos -> new HashMap<>()).put(cubePos.getY(), tagPriority);
                });
            }
        }

        return tagMap;
    }
}
