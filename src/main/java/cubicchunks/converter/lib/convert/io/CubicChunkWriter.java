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
package cubicchunks.converter.lib.convert.io;

import cubicchunks.converter.lib.Dimension;
import cubicchunks.converter.lib.convert.data.CubicChunksColumnData;
import cubicchunks.converter.lib.convert.ChunkDataWriter;
import cubicchunks.converter.lib.util.Utils;
import cubicchunks.regionlib.impl.EntryLocation2D;
import cubicchunks.regionlib.impl.EntryLocation3D;
import cubicchunks.regionlib.impl.SaveCubeColumns;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CubicChunkWriter implements ChunkDataWriter<CubicChunksColumnData> {

    private Path dstPath;
    private Map<Dimension, SaveCubeColumns> saves = new ConcurrentHashMap<>();

    public CubicChunkWriter(Path dstPath) {
        this.dstPath = dstPath;
    }

    @Override public void accept(CubicChunksColumnData data) throws IOException {
        SaveCubeColumns save = saves.computeIfAbsent(data.getDimension(), dim -> {
            try {
                return SaveCubeColumns.create(dstPath.resolve(dim.getDirectory()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        EntryLocation2D pos = data.getPosition();
        if (data.getColumnData() != null) {
            save.save2d(pos, data.getColumnData());
        }
        for (Map.Entry<Integer, ByteBuffer> entry : data.getCubeData().entrySet()) {
            save.save3d(new EntryLocation3D(pos.getEntryX(), entry.getKey(), pos.getEntryZ()), entry.getValue());
        }
    }

    @Override public void discardData() throws IOException {
        Utils.rm(dstPath);
    }

    @Override public void close() throws Exception {
        boolean exception = false;
        for (SaveCubeColumns save : saves.values()) {
            try {
                save.close();
            } catch (IOException e) {
                e.printStackTrace();
                exception = true;
            }
        }
        if (exception) {
            throw new IOException();
        }
    }
}
