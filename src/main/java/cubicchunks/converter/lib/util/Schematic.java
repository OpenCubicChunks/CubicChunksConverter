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
package cubicchunks.converter.lib.util;

import net.kyori.nbt.CompoundTag;
import net.kyori.nbt.TagIO;
import net.kyori.nbt.TagTypeMaps;

import java.io.IOException;
import java.nio.file.Path;

public class Schematic {
    // zSize = Length
    // xSize = Width
    private final int xSize, ySize, zSize;
    private final int xzFactor;
    private final byte[] blocks;
    private final byte[] add;
    private final byte[] data;

    public Schematic(int xSize, int ySize, int zSize, byte[] blocks, byte[] add, byte[] data) {
        this.xSize = xSize;
        this.ySize = ySize;
        this.zSize = zSize;
        this.xzFactor = xSize * zSize;
        this.blocks = blocks;
        this.add = add;
        this.data = data;
    }

    // TODO: entities, tile entities, tile ticks, biomes, id mappings, extended metadata, WorldEdit offset and origin
    // TODO: more efficient loading
    public static Schematic load(Path path) throws IOException {
        CompoundTag root = TagIO.readCompressedPath(TagTypeMaps.MINECRAFT, path);

        if (root.contains("Add")) {
            throw new UnsupportedOperationException("Legacy Add tag not implemented");
        }
        if (root.contains("SchematicaMapping") || root.contains("BlockIDs")) {
            throw new UnsupportedOperationException("ID mappings not implemented");
        }
        return new Schematic(root.getInt("Width"), root.getInt("Height"), root.getInt("Length"),
                root.getByteArray("Blocks"), root.getByteArray("AddBlocks"), root.getByteArray("Data"));
    }

    public int index(int x, int y, int z) {
        return y * xzFactor + z * xSize + x;
    }

    public int id(int index) {
        return blocks[index] & 0xFF;
    }

    public int idMSB(int index) {
        int i = index >> 1;
        int shift = (index & 1 ^ 1) << 2;
        return (add[i] >>> shift) & 0xF;
    }

    public int meta(int index) {
        return data[index] & 0xFF;
    }

    public int getXSize() {
        return xSize;
    }

    public int getYSize() {
        return ySize;
    }

    public int getZSize() {
        return zSize;
    }
}
