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
import cubicchunks.converter.lib.convert.data.PriorityCubicChunksColumnData;
import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.ImmutablePair;
import cubicchunks.converter.lib.util.Vector3i;
import cubicchunks.regionlib.impl.EntryLocation2D;

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
    public final int degrees;
    public RotateEditTask(BoundingBox srcBox, Vector3i origin, int degrees){
        srcBoxes.add(srcBox);
        dstBoxes.add(srcBox);
        this.origin = origin;
        if (degrees % 90 != 0) throw new IllegalArgumentException("Degrees must be divisible by 90");
        this.degrees = degrees;
    }

    public Vector3i rotateDst90Degrees(Vector3i dst){
        int newX = dst.getX();
        int newZ = dst.getZ();

        //Subtract origin from points
        newX-=this.origin.getX();
        newZ-=this.origin.getZ();

        //Swap X and Y
        int temp = newZ;
        newZ = newX;
        newX = temp;

        newZ*=-1;

        //Add origin to points
        newX+=this.origin.getX();
        newZ+=this.origin.getZ();

        return new Vector3i(newX, dst.getY(), newZ);
    }

    public Vector3i rotateDst(Vector3i dstPos, int degrees){
        int degree = degrees;
        while ((degree/=90) > 0){
            dstPos = this.rotateDst90Degrees(dstPos);
        }
        return dstPos;
    }

    public Vector3i calculateDstOffset(Vector3i cubePos, Vector3i dst){
        return new Vector3i(dst.getX()-cubePos.getX(), dst.getY()-cubePos.getY(), dst.getZ()-cubePos.getZ());
    }

    public EntryLocation2D rotateDst(EntryLocation2D dstPos, int degrees){
        Vector3i temp = new Vector3i(dstPos.getEntryX(), 0, dstPos.getEntryZ());
        temp = rotateDst(temp, this.degrees);
        return new EntryLocation2D(temp.getX(), temp.getZ());
    }

    public PriorityCubicChunksColumnData rotateColumn(PriorityCubicChunksColumnData input){
        EntryLocation2D position = this.rotateDst(input.getPosition(), this.degrees);
        return input;
    }

    @Nonnull public List<ImmutablePair<Vector3i, ImmutablePair<Long, CompoundTag>>> actOnCube(Vector3i cubePos, EditTaskContext.EditTaskConfig config, CompoundTag cubeTag, long inCubePriority) {
        List<ImmutablePair<Vector3i, ImmutablePair<Long, CompoundTag>>> outCubes = new ArrayList<>();

        // calculate offset
        Vector3i dstPos = this.rotateDst(cubePos, this.degrees);
        Vector3i offset = this.calculateDstOffset(cubePos, dstPos);

        // adjusting new cube data to be valid
        CompoundMap level = (CompoundMap) cubeTag.getValue().get("Level").getValue();

        level.put(new IntTag("x", dstPos.getX()));
        level.put(new IntTag("y", dstPos.getY()));
        level.put(new IntTag("z", dstPos.getZ()));

        if(config.shouldRelightDst()) {
            this.markCubeForLightUpdates(level);
        }
        this.markCubePopulated(level);

        this.inplaceMoveTileEntitiesBy(level, offset.getX() << 4, offset.getY() << 4, offset.getZ() << 4);
        this.inplaceMoveEntitiesBy(level, offset.getX() << 4, offset.getY() << 4, offset.getZ() << 4, false);

        outCubes.add(new ImmutablePair<>(dstPos, new ImmutablePair<>(inCubePriority+1, cubeTag)));
        return outCubes;
    }
}
