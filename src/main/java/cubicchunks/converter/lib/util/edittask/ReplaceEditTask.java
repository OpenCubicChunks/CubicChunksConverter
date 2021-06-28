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

import com.flowpowered.nbt.ByteTag;
import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.ImmutablePair;
import cubicchunks.converter.lib.util.Vector3i;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ReplaceEditTask extends BaseEditTask {
    private final byte inBlockID;
    private final byte inBlockMeta;

    private final byte outBlockID;
    private final byte outBlockMeta;

    public ReplaceEditTask(BoundingBox srcBox, byte inBlockID, byte inBlockMeta, byte outBlockID, byte outBlockMeta) {
        srcBoxes.add(srcBox);
        dstBoxes.add(srcBox);
        this.inBlockID = inBlockID;
        this.inBlockMeta = inBlockMeta;
        this.outBlockID = outBlockID;
        this.outBlockMeta = outBlockMeta;
    }

    @Nonnull @Override public List<ImmutablePair<Vector3i, CompoundTag>> actOnCube(ImmutablePair<Vector3i, CompoundTag> cube) {
        List<ImmutablePair<Vector3i, CompoundTag>> outCubes = new ArrayList<>();

        Vector3i pos = cube.getKey();
        CompoundTag cubeTag = cube.getValue();

        int cubeX = pos.getX();
        int cubeY = pos.getY();
        int cubeZ = pos.getZ();

        CompoundMap entryLevel = (CompoundMap) cubeTag.getValue().get("Level").getValue();
        entryLevel.put(new ByteTag("isSurfaceTracked", (byte) 0));
        entryLevel.put(new ByteTag("initLightDone", (byte) 0));
        entryLevel.put(new ByteTag("populated", (byte) 1));
        entryLevel.put(new ByteTag("fullyPopulated", (byte) 1));

        CompoundMap sectionDetails;
        try {
            sectionDetails = ((CompoundTag)((List<?>) (entryLevel).get("Sections").getValue()).get(0)).getValue(); //POSSIBLE ARRAY OUT OF BOUNDS EXCEPTION ON A MALFORMED CUBE
        } catch(NullPointerException | ArrayIndexOutOfBoundsException e) {
            LOGGER.warning("Malformed cube at position (" + cubeX + ", " + cubeY + ", " + cubeZ + "), skipping!");
            return outCubes;
        }

        byte[] blocks = (byte[]) sectionDetails.get("Blocks").getValue();
        byte[] meta = (byte[]) sectionDetails.get("Data").getValue();

        for (int i = 0; i < 4096; i++) {
            if(blocks[i] == inBlockID && EditTask.nibbleGetAtIndex(meta, i) == inBlockMeta) {
                blocks[i] = outBlockID;
                EditTask.nibbleSetAtIndex(meta, i, outBlockMeta);
            }
        }

        outCubes.add(new ImmutablePair<>(cube.getKey(), cube.getValue()));
        return outCubes;
    }
}
