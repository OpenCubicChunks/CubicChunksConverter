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
package cubicchunks.converter.lib.anvil2cc;

import static cubicchunks.regionlib.impl.save.MinecraftSaveSection.MinecraftRegionType.MCA;

import cubicchunks.converter.lib.Dimension;
import cubicchunks.converter.lib.Dimensions;
import cubicchunks.converter.lib.convert.ChunkDataReader;
import cubicchunks.regionlib.impl.save.MinecraftSaveSection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class AnvilChunkReader implements ChunkDataReader<AnvilChunkData> {

    private static final BiFunction<Dimension, Path, Path> LOCATION_FUNC_SRC = (d, p) -> {
        if (!d.getDirectory().isEmpty()) {
            p = p.resolve(d.getDirectory());
        }
        return p.resolve("region");
    };

    private final Map<Dimension, MinecraftSaveSection> saves = new ConcurrentHashMap<>();
    private final Path srcDir;

    public AnvilChunkReader(Path srcDir) {
        this.srcDir = srcDir;
        for (Dimension d : Dimensions.getDimensions()) {
            Path srcLoc = LOCATION_FUNC_SRC.apply(d, srcDir);
            if (!Files.exists(srcLoc)) {
                continue;
            }

            MinecraftSaveSection vanillaSave = MinecraftSaveSection.createAt(LOCATION_FUNC_SRC.apply(d, srcDir), MCA);
            saves.put(d, vanillaSave);
        }
    }

    @Override public void countInputChunks(Runnable increment) throws IOException {
        for (MinecraftSaveSection save : saves.values()) {
            save.forAllKeys(loc -> increment.run());
        }
    }

    @Override public void loadChunks(Consumer<? super AnvilChunkData> consumer) throws IOException {
        for (Dimension d : Dimensions.getDimensions()) {
            Path srcLoc = LOCATION_FUNC_SRC.apply(d, srcDir);
            if (!Files.exists(srcLoc)) {
                continue;
            }

            MinecraftSaveSection vanillaSave = saves.get(d);
            vanillaSave.forAllKeys(mcPos -> consumer.accept(new AnvilChunkData(d, mcPos, vanillaSave.load(mcPos).orElse(null))));
        }
    }

    @Override public void close() throws Exception {
        boolean exception = false;
        for (MinecraftSaveSection save : saves.values()) {
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
