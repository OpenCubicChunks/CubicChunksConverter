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
package cubicchunks.converter.lib.convert.cc2bigCubeCc;

import cubicchunks.converter.lib.convert.ChunkDataConverter;
import cubicchunks.converter.lib.convert.data.CubicChunksProtoBigCubeData;
import cubicchunks.converter.lib.convert.data.CubicChunksBigCube112Data;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

public class Cc2BigCubeCcDataConverter  implements ChunkDataConverter<CubicChunksBigCube112Data, CubicChunksProtoBigCubeData> {

    private final ByteBuffer prepend;

    public Cc2BigCubeCcDataConverter() {
        ByteArrayOutputStream dataOut = new ByteArrayOutputStream();
        try (OutputStream out = new BufferedOutputStream(new GZIPOutputStream(dataOut))){
            // NBT can't begin with byte 255, so this acts as a marker for
            // 1.12.2 "proto-big-cube" converted data
            out.write(255);
            // a second byte with value 0 for potential future extension
            out.write(0);
        } catch (IOException e) {
            throw new Error(e);
        }
        this.prepend = ByteBuffer.wrap(dataOut.toByteArray());
    }

    @Override public Set<CubicChunksProtoBigCubeData> convert(CubicChunksBigCube112Data input) {
        if (input.getColumnData() != null && input.getCubeData() == null) {
            return Collections.singleton(new CubicChunksProtoBigCubeData(input.getDimension(), input.getPosition(), input.getColumnData(), null));
        }
        ByteBuffer prepend = this.prepend.duplicate();
        int totalSize = prepend.capacity();
        for (ByteBuffer cube : input.getCubeData()) {
            totalSize += cube.capacity();
        }
        ByteBuffer buf = ByteBuffer.wrap(new byte[totalSize]);
        prepend.clear();
        buf.put(prepend);
        for (ByteBuffer cube : input.getCubeData()) {
            cube.clear();
            buf.put(cube);
        }
        buf.clear();
        return Collections.singleton(new CubicChunksProtoBigCubeData(input.getDimension(), input.getPosition(), null, buf));
    }
}
