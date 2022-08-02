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
    private final Vector3i offset;

    private final BoundingBox exclusiveDstBox;

    public RotateEditTask(Vector3i origin, int degrees) {
        ///TODO get this to actually work
        offset = null;
        exclusiveDstBox = null;
    }

    @Nonnull @Override public List<ImmutablePair<Vector3i, ImmutablePair<Long, CompoundTag>>> actOnCube(Vector3i cubePos, EditTaskContext.EditTaskConfig config, CompoundTag cubeTag, long inCubePriority) {
        //TODO get this to actually work
        List<ImmutablePair<Vector3i, ImmutablePair<Long, CompoundTag>>> outCubes = new ArrayList<>();

        int cubeX = cubePos.getX();
        int cubeY = cubePos.getY();
        int cubeZ = cubePos.getZ();

        try {
            //clearing data from old cube
            if(offset == null || !exclusiveDstBox.intersects(cubeX, cubeY, cubeZ)) {
                //this is just doing a deep copy of the tag by writing to byte array then back again
                ByteArrayOutputStream bout = new ByteArrayOutputStream(1024);
                NBTOutputStream out = new NBTOutputStream(bout, false);
                out.writeTag(cubeTag);

                NBTInputStream is = new NBTInputStream(new ByteArrayInputStream(bout.toByteArray()), false);
                CompoundTag tag = (CompoundTag) is.readTag();
                //copy done here ^
                CompoundMap srcLevel = (CompoundMap) (tag).getValue().get("Level").getValue();
                ListTag<?> sectionsTag = (ListTag<?>) srcLevel.get("Sections");

                //handle edge-case where cube exists but sections array is null
                CompoundMap sectionDetails = sectionsTag == null || sectionsTag.getValue().isEmpty() ? null :
                        ((CompoundTag)sectionsTag.getValue().get(0)).getValue();

                if(sectionDetails != null) {
                    //remove optional "Additional" block data array
                    sectionDetails.remove("Add");

                    ByteArrayTag emptyArray = new ByteArrayTag("", new byte[0]);
                    Arrays.fill((byte[]) sectionDetails.getOrDefault("Blocks", emptyArray).getValue(), (byte) 0);
                    Arrays.fill((byte[]) sectionDetails.getOrDefault("Data", emptyArray).getValue(), (byte) 0);
                    Arrays.fill((byte[]) sectionDetails.getOrDefault("BlockLight", emptyArray).getValue(), (byte) 0);
                    Arrays.fill((byte[]) sectionDetails.getOrDefault("SkyLight", emptyArray).getValue(), (byte) 0);
                }

                if(config.shouldRelightSrc()) {
                    this.markCubeForLightUpdates(srcLevel);
                }
                this.markCubePopulated(srcLevel);

                srcLevel.put(new ListTag<>("TileTicks", CompoundTag.class, new ArrayList<>()));
                srcLevel.put(new ListTag<>("Entities", CompoundTag.class, new ArrayList<>()));
                srcLevel.put(new ListTag<>("TileEntities", CompoundTag.class, new ArrayList<>()));

                outCubes.add(new ImmutablePair<>(new Vector3i(cubeX, cubeY, cubeZ), new ImmutablePair<>(inCubePriority+1, tag)));
            }

            // adjusting new cube data to be valid
            CompoundMap level = (CompoundMap)cubeTag.getValue().get("Level").getValue();
            if (offset != null && !offset.equals(new Vector3i(0, 0, 0))) {
                int dstX = cubeX + offset.getX();
                int dstY = cubeY + offset.getY();
                int dstZ = cubeZ + offset.getZ();
                level.put(new IntTag("x", dstX));
                level.put(new IntTag("y", dstY));
                level.put(new IntTag("z", dstZ));

                if(config.shouldRelightDst()) {
                    this.markCubeForLightUpdates(level);
                }
                this.markCubePopulated(level);

                this.inplaceMoveTileEntitiesBy(level, offset.getX() << 4, offset.getY() << 4, offset.getZ() << 4);
                this.inplaceMoveEntitiesBy(level, offset.getX() << 4, offset.getY() << 4, offset.getZ() << 4, false);

                outCubes.add(new ImmutablePair<>(new Vector3i(dstX, dstY, dstZ), new ImmutablePair<>(inCubePriority+1, cubeTag)));
            }
        } catch(IOException e) {
            e.printStackTrace();
            throw new UncheckedIOException(e);
        }
        return outCubes;
    }
}
