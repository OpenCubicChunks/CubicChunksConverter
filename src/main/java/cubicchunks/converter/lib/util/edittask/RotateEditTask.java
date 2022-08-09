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

import com.flowpowered.nbt.*;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.flowpowered.nbt.stream.NBTOutputStream;
import cubicchunks.converter.lib.conf.command.EditTaskContext;
import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.ImmutablePair;
import cubicchunks.converter.lib.util.Vector3i;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RotateEditTask extends TranslationEditTask {
    private final Vector3i origin;
    private final int degrees;

    public RotateEditTask(Vector3i origin, int degrees) {
        ///TODO check if degrees is invalid
        this.origin = origin;
        this.degrees = degrees;
    }

    @Nonnull @Override public List<ImmutablePair<Vector3i, ImmutablePair<Long, CompoundTag>>> actOnCube(Vector3i cubePos, EditTaskContext.EditTaskConfig config, CompoundTag cubeTag, long inCubePriority) {
        Vector3i dstOffset;
        //TOdo this only works if you assume origin is x=0 and z=0
        if (this.degrees == 90){
            dstOffset = new Vector3i(cubePos.getX(), cubePos.getY(), cubePos.getZ() * -1);
        }
        else if (this.degrees == 180){
            dstOffset = new Vector3i(cubePos.getX() * -1, cubePos.getY(), cubePos.getZ() * -1);
        }
        else if (this.degrees == 270){
            dstOffset = new Vector3i(cubePos.getX() * -1, cubePos.getY(), cubePos.getZ());
        }
        else{
            dstOffset = new Vector3i(cubePos.getX(), cubePos.getY(), cubePos.getZ());
        }
        BoundingBox srcBox = new BoundingBox(cubePos.getX(), cubePos.getY(), cubePos.getZ(), cubePos.getX(), cubePos.getY(), cubePos.getZ());

        MoveEditTask task = new MoveEditTask(srcBox, dstOffset);
        return task.actOnCube(cubePos, config, cubeTag, inCubePriority);
    }
}
