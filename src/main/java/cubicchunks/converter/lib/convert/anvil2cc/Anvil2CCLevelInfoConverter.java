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
package cubicchunks.converter.lib.convert.anvil2cc;

import cubicchunks.converter.lib.Dimensions;
import cubicchunks.converter.lib.util.Utils;
import cubicchunks.converter.lib.convert.data.AnvilChunkData;
import cubicchunks.converter.lib.convert.data.CubicChunksColumnData;
import cubicchunks.converter.lib.convert.LevelInfoConverter;
import net.kyori.nbt.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Anvil2CCLevelInfoConverter implements LevelInfoConverter<AnvilChunkData, CubicChunksColumnData> {

    private final Path srcDir;
    private final Path dstDir;

    public Anvil2CCLevelInfoConverter(Path srcDir, Path dstDir) {
        this.srcDir = srcDir;
        this.dstDir = dstDir;
    }

    @Override public void convert() throws IOException {
        Utils.createDirectories(dstDir);

        CompoundTag root = TagIO.readCompressedPath(TagTypeMaps.MINECRAFT, srcDir.resolve("level.dat"));

        root.getCompound("Data").put("isCubicWorld", new ByteTag((byte) 1));

        String value = root.getCompound("Data").getString("generatorName");
        if (value.equalsIgnoreCase("default")) {
            root.getCompound("Data").put("generatorName", new StringTag("VanillaCubic"));
        }

        Files.createDirectories(dstDir);

        TagIO.writeCompressedPath(TagTypeMaps.MINECRAFT, root, dstDir.resolve("level.dat"));

        Utils.copyEverythingExcept(srcDir, srcDir, dstDir,
                file -> file.toString().contains("level.dat") ||
                        file.toString().contains("cubicChunksData.dat") ||
                        Dimensions.getDimensions().stream().anyMatch(dim ->
                                srcDir.resolve(dim.getDirectory()).resolve("region").equals(file)
                        ),
                f -> {
                } // TODO: counting files
        );
    }
}
