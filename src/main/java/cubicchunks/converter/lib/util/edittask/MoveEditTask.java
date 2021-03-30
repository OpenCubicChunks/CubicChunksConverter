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
package cubicchunks.converter.lib.util.edittask;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.IntTag;
import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.ImmutablePair;
import cubicchunks.converter.lib.util.Vector3i;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class MoveEditTask extends BaseEditTask {
    private final Vector3i offset;

    public MoveEditTask(BoundingBox srcBox, Vector3i dstOffset) {
        srcBoxes.add(srcBox);
        dstBoxes.add(srcBox.add(dstOffset));
        dstBoxes.add(srcBox);
        offset = dstOffset;
    }

    @Nonnull @Override public List<ImmutablePair<Vector3i, CompoundTag>> actOnCube(ImmutablePair<Vector3i, CompoundTag> cube) {
        List<ImmutablePair<Vector3i, CompoundTag>> outCubes = new ArrayList<>();

        Vector3i cubePos = cube.getKey();

        if(dstBoxes.get(0).intersects(cubePos.getX(), cubePos.getY(), cubePos.getZ())) {
            outCubes.add(new ImmutablePair<>(cubePos, null));
            return outCubes;
        }
        CompoundMap level = (CompoundMap) cube.getValue().getValue().get("Level").getValue();

        Vector3i dstPos = cubePos.add(offset);
        level.put(new IntTag("x", dstPos.getX()));
        level.put(new IntTag("y", dstPos.getY()));
        level.put(new IntTag("z", dstPos.getZ()));

        outCubes.add(new ImmutablePair<>(dstPos, cube.getValue()));
        outCubes.add(new ImmutablePair<>(cubePos, null));
        return outCubes;
    }
}
