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
package cubicchunks.converter.lib.convert.data;

import cubicchunks.converter.lib.Dimension;
import cubicchunks.regionlib.impl.EntryLocation2D;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;

public class DualSourceCubicChunksColumnData {

    private final Dimension dimension;
    private final EntryLocation2D position;
    private final ByteBuffer columnData;
    private final Map<Integer, ByteBuffer> priorityCubeData;
    private final Map<Integer, ByteBuffer> fallbackCubeData;

    public DualSourceCubicChunksColumnData(Dimension dimension,
                                           EntryLocation2D position,
                                           @Nullable ByteBuffer priorityColumnData,
                                           @Nullable Map<Integer, ByteBuffer> priorityCubeData,
                                           Map<Integer, ByteBuffer> fallbackCubeData) {
        this.dimension = dimension;
        this.position = position;
        this.columnData = priorityColumnData;
        this.priorityCubeData = priorityCubeData;
        this.fallbackCubeData = fallbackCubeData;
    }

    public Dimension getDimension() {
        return dimension;
    }

    public EntryLocation2D getPosition() {
        return position;
    }

    @Nullable
    public ByteBuffer getColumnData() {
        return columnData;
    }

    @Nullable
    public Map<Integer, ByteBuffer> getPriorityCubeData() {
        return priorityCubeData;
    }


    public Map<Integer, ByteBuffer> getFallbackCubeData() {
        return fallbackCubeData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DualSourceCubicChunksColumnData that = (DualSourceCubicChunksColumnData) o;
        return Objects.equals(dimension, that.dimension) && Objects.equals(position, that.position) && Objects.equals(columnData, that.columnData) && Objects.equals(priorityCubeData, that.priorityCubeData) && Objects.equals(fallbackCubeData, that.fallbackCubeData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimension, position, columnData, priorityCubeData, fallbackCubeData);
    }

    @Override
    public String toString() {
        return "DualSourceCubicChunksColumnData{" +
            "dimension=" + dimension +
            ", position=" + position +
            ", columnData=" + columnData +
            ", priorityCubeData=" + priorityCubeData +
            ", fallbackCubeData=" + fallbackCubeData +
            '}';
    }
}
