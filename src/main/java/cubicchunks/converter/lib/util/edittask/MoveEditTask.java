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
package cubicchunks.converter.lib.util.edittask;

import cubicchunks.converter.lib.conf.command.EditTaskContext;
import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.ImmutablePair;
import cubicchunks.converter.lib.util.Vector3i;
import net.kyori.nbt.CompoundTag;
import net.kyori.nbt.IntTag;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

public class MoveEditTask extends TranslationEditTask {
    private final Vector3i offset;

    public MoveEditTask(BoundingBox srcBox, Vector3i dstOffset) {
        srcBoxes.add(srcBox);
        dstBoxes.add(srcBox.add(dstOffset));
        dstBoxes.add(srcBox);
        offset = dstOffset;
    }

    @Nonnull @Override public List<ImmutablePair<Vector3i, ImmutablePair<Long, CompoundTag>>> actOnCube(Vector3i cubePos, EditTaskContext.EditTaskConfig config, CompoundTag cubeTag, long inCubePriority) {
        List<ImmutablePair<Vector3i, ImmutablePair<Long, CompoundTag>>> outCubes = new ArrayList<>();

        if(dstBoxes.get(0).intersects(cubePos.getX(), cubePos.getY(), cubePos.getZ())) {
            outCubes.add(new ImmutablePair<>(cubePos, new ImmutablePair<>(inCubePriority+1, null)));
            return outCubes;
        }
        CompoundTag level = cubeTag.getCompound("Level");

        Vector3i dstPos = cubePos.add(offset);
        level.put("x", new IntTag(dstPos.getX()));
        level.put("y", new IntTag(dstPos.getY()));
        level.put("z", new IntTag(dstPos.getZ()));

        if(config.shouldRelightDst()) {
            this.markCubeForLightUpdates(level);
        }
        this.markCubePopulated(level, true);

        this.inplaceMoveTileEntitiesBy(level, offset.getX() << 4, offset.getY() << 4, offset.getZ() << 4);
        this.inplaceMoveEntitiesBy(level, offset.getX() << 4, offset.getY() << 4, offset.getZ() << 4, false);

        outCubes.add(new ImmutablePair<>(dstPos, new ImmutablePair<>(inCubePriority+1, cubeTag)));
        outCubes.add(new ImmutablePair<>(cubePos, new ImmutablePair<>(inCubePriority+1, null)));
        return outCubes;
    }
}
