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
import cubicchunks.converter.lib.conf.command.EditTaskContext;
import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.ImmutablePair;
import cubicchunks.converter.lib.util.Vector3i;

import javax.annotation.Nonnull;
import java.util.List;

public class RotateEditTask extends TranslationEditTask {
    private final Vector3i origin;
    private final int degrees;
    public RotateEditTask(BoundingBox srcBox, Vector3i origin, int degrees){
        srcBoxes.add(srcBox);
        dstBoxes.add(srcBox);
        this.origin = origin;
        if (degrees % 90 != 0) throw new IllegalArgumentException("Degrees must be divisible by 90");
        this.degrees = degrees;
    }

    public Vector3i rotateDst90Degrees(Vector3i dstOffset){
        int newX = dstOffset.getX();
        int newZ = dstOffset.getZ();

        //Subtract origin from points
        newX-=this.origin.getX();
        newZ-=this.origin.getZ();

        //Swap X and Y
        int temp = newZ;
        newZ = newX;
        newX = temp;
        if(newX>0 && newZ > 0) newZ*=-1;
        else if (newX > 0 && newZ < 0) newX*=-1;
        else if (newX < 0 && newZ < 0) newZ*=-1;
        else if (newX < 0 && newZ > 0) newX*=-1;

        //Add origin to points
        newX+=this.origin.getX();
        newZ+=this.origin.getZ();

        return new Vector3i(newX, dstOffset.getY(), newZ);
    }

    public Vector3i calculateDstOffset(Vector3i cubePos, Vector3i dst){
        return new Vector3i(dst.getX()-cubePos.getX(), dst.getY()-cubePos.getY(), dst.getZ()-cubePos.getZ());
    }

    @Nonnull public List<ImmutablePair<Vector3i, ImmutablePair<Long, CompoundTag>>> actOnCube(Vector3i cubePos, EditTaskContext.EditTaskConfig config, CompoundTag cubeTag, long inCubePriority) {
        Vector3i dstOffset;
        Vector3i dst = cubePos;
        int degree = this.degrees;
        while ((degree/=90) > 0){
            dst = this.rotateDst90Degrees(dst);
        }
        dstOffset = this.calculateDstOffset(cubePos, dst);
        BoundingBox srcBox = new BoundingBox(cubePos.getX(), cubePos.getY(), cubePos.getZ(), cubePos.getX(), cubePos.getY(), cubePos.getZ());

        CutEditTask task = new CutEditTask(srcBox, dstOffset);
        System.out.println(String.format("Rotator: Cutting %d %d %d to %d %d %d", cubePos.getX(), cubePos.getY(), cubePos.getZ(), dst.getX(), dst.getY(), dst.getZ()));
        return task.actOnCube(cubePos, config, cubeTag, inCubePriority);
    }
}
