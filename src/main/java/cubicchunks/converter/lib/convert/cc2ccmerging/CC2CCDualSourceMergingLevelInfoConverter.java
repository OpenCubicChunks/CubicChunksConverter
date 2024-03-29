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

import cubicchunks.converter.lib.Dimensions;
import cubicchunks.converter.lib.convert.LevelInfoConverter;
import cubicchunks.converter.lib.convert.data.CubicChunksColumnData;
import cubicchunks.converter.lib.convert.data.DualSourceCubicChunksColumnData;
import cubicchunks.converter.lib.util.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CC2CCDualSourceMergingLevelInfoConverter implements LevelInfoConverter<DualSourceCubicChunksColumnData, CubicChunksColumnData> {

    private final Path prioritySrcDir;
    private final Path fallbackSrcDir;
    private final Path dstDir;

    public CC2CCDualSourceMergingLevelInfoConverter(Path prioritySrcDir, Path fallbackSrcDir, Path dstDir) {
        this.prioritySrcDir = prioritySrcDir;
        this.fallbackSrcDir = fallbackSrcDir;
        this.dstDir = dstDir;
    }

    @Override public void convert() throws IOException {
        Utils.createDirectories(dstDir);
        if(Files.exists(prioritySrcDir)) {
            Utils.copyEverythingExcept(prioritySrcDir, prioritySrcDir, dstDir, file ->
                    Dimensions.getDimensions().stream().anyMatch(dim ->
                        prioritySrcDir.resolve(dim.getDirectory()).resolve("region2d").equals(file) ||
                            prioritySrcDir.resolve(dim.getDirectory()).resolve("region3d").equals(file)
                    ),
                f -> {
                } // TODO: counting files
            );
        } else {
            Utils.copyEverythingExcept(fallbackSrcDir, fallbackSrcDir, dstDir, file ->
                    Dimensions.getDimensions().stream().anyMatch(dim ->
                        fallbackSrcDir.resolve(dim.getDirectory()).resolve("region2d").equals(file) ||
                            fallbackSrcDir.resolve(dim.getDirectory()).resolve("region3d").equals(file)
                    ),
                f -> {
                } // TODO: counting files
            );
        }
    }
}
