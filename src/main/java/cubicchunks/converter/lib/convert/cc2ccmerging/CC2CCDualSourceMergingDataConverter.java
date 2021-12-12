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
package cubicchunks.converter.lib.convert.cc2ccmerging;

import cubicchunks.converter.lib.conf.ConverterConfig;
import cubicchunks.converter.lib.convert.ChunkDataConverter;
import cubicchunks.converter.lib.convert.data.CubicChunksColumnData;
import cubicchunks.converter.lib.convert.data.DualSourceCubicChunksColumnData;
import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.edittask.EditTask;

import java.nio.ByteBuffer;
import java.util.*;

public class CC2CCDualSourceMergingDataConverter implements ChunkDataConverter<DualSourceCubicChunksColumnData, CubicChunksColumnData> {
    private final List<EditTask> relocateTasks;

    @SuppressWarnings("unchecked")
    public CC2CCDualSourceMergingDataConverter(ConverterConfig config) {
        relocateTasks = (List<EditTask>) config.getValue("relocations");
    }

    @Override public Set<CubicChunksColumnData> convert(DualSourceCubicChunksColumnData input) {
        Map<Integer, ByteBuffer> priorityCubes = input.getPriorityCubeData();
        Map<Integer, ByteBuffer> fallbackCubes = input.getFallbackCubeData();
        Map<Integer, ByteBuffer> inCubes = new HashMap<>();

        if(priorityCubes != null)
            priorityCubes.forEach(inCubes::put);
        fallbackCubes.forEach((y, cube) -> {
            if(!inCubes.containsKey(y))
                inCubes.put(y, cube);
        });

        Map<Integer, ByteBuffer> outCubes = new HashMap<>();
        inCubes.forEach((y, cube) -> {
            for (EditTask relocateTask : relocateTasks) {
                List<BoundingBox> srcBoxes = relocateTask.getSrcBoxes();
                boolean foundIntersect = false;
                for (BoundingBox box : srcBoxes) {
                    if (box.intersects(input.getPosition().getEntryX(), y, input.getPosition().getEntryZ())) {
                        outCubes.put(y, cube);
                        foundIntersect = true;
                        break;
                    }
                }
                if(foundIntersect)
                    break;
            }
        });

        Set<CubicChunksColumnData> columnData = new HashSet<>();
        columnData.add(new CubicChunksColumnData(input.getDimension(), input.getPosition(), input.getColumnData(), outCubes));
        return columnData;
    }
}