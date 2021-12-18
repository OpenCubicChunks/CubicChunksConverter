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
import cubicchunks.converter.lib.util.*;
import net.kyori.nbt.CompoundTag;
import net.kyori.nbt.ListTag;
import net.kyori.nbt.TagType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

public class SchematicTask extends BaseEditTask {

    private final Schematic schematic;
    private final Matrix4d transformInverse;
    private final List<BoundingBox> workingArea;
    private final boolean skipAir;
    private final String dimension;

    public SchematicTask(Schematic schematic, Matrix4d transform, boolean skipAir, String dimension) {
        this.schematic = schematic;
        this.transformInverse = transform.inverse();
        this.skipAir = skipAir;
        this.dimension = dimension;
        Vector3i p000 = transform.transformVec3i(new Vector3i(0, 0, 0));
        Vector3i p001 = transform.transformVec3i(new Vector3i(0, 0, schematic.getZSize()));
        Vector3i p010 = transform.transformVec3i(new Vector3i(0, schematic.getYSize(), 0));
        Vector3i p011 = transform.transformVec3i(new Vector3i(0, schematic.getYSize(), schematic.getZSize()));

        Vector3i p100 = transform.transformVec3i(new Vector3i(schematic.getXSize(), 0, 0));
        Vector3i p101 = transform.transformVec3i(new Vector3i(schematic.getXSize(), 0, schematic.getZSize()));
        Vector3i p110 = transform.transformVec3i(new Vector3i(schematic.getXSize(), schematic.getYSize(), 0));
        Vector3i p111 = transform.transformVec3i(new Vector3i(schematic.getXSize(), schematic.getYSize(), schematic.getZSize()));

        int minX = MathUtil.min(
                p000.getX(),
                p001.getX(),
                p010.getX(),
                p011.getX(),
                p100.getX(),
                p101.getX(),
                p110.getX(),
                p111.getX()
        );
        int minY = MathUtil.min(
                p000.getY(),
                p001.getY(),
                p010.getY(),
                p011.getY(),
                p100.getY(),
                p101.getY(),
                p110.getY(),
                p111.getY()
        );
        int minZ = MathUtil.min(
                p000.getZ(),
                p001.getZ(),
                p010.getZ(),
                p011.getZ(),
                p100.getZ(),
                p101.getZ(),
                p110.getZ(),
                p111.getZ()
        );

        int maxX = MathUtil.max(
                p000.getX(),
                p001.getX(),
                p010.getX(),
                p011.getX(),
                p100.getX(),
                p101.getX(),
                p110.getX(),
                p111.getX()
        );
        int maxY = MathUtil.max(
                p000.getY(),
                p001.getY(),
                p010.getY(),
                p011.getY(),
                p100.getY(),
                p101.getY(),
                p110.getY(),
                p111.getY()
        );
        int maxZ = MathUtil.max(
                p000.getZ(),
                p001.getZ(),
                p010.getZ(),
                p011.getZ(),
                p100.getZ(),
                p101.getZ(),
                p110.getZ(),
                p111.getZ()
        );

        this.workingArea = Collections.singletonList(new BoundingBox(
            minX >> 4, minY >> 4, minZ >> 4,
            (maxX >> 4) + 1, (maxY >> 4) + 1, (maxZ >> 4) + 1
        ));
    }

    @Override
    public boolean handlesDimension(String dirName) {
        return dimension.equals(dirName);
    }

    @Nonnull @Override
    public List<ImmutablePair<Vector3i, ImmutablePair<Long, CompoundTag>>> actOnCube(Vector3i cubePos, EditTaskContext.EditTaskConfig config,
                                                                                     CompoundTag cubeTag, long inCubePriority) {
        List<ImmutablePair<Vector3i, ImmutablePair<Long, CompoundTag>>> outCubes = new ArrayList<>();

        CompoundTag level = cubeTag.getCompound("Level");
        if(config.shouldRelightDst()) {
            this.markCubeForLightUpdates(level);
        }
        this.markCubePopulated(level);

        CompoundTag sectionDetails;
        try {
            if (!level.contains("Sections")) {
                ListTag tags = new ListTag(TagType.COMPOUND);
                tags.add(Utils.createEmptySectionTag());
                level.put("Sections", tags);
            }
            sectionDetails = ((CompoundTag)((List<?>) (level).get("Sections")).get(0)); //POSSIBLE ARRAY OUT OF BOUNDS EXCEPTION ON A MALFORMED CUBE
        } catch(NullPointerException | ArrayIndexOutOfBoundsException e) {
            LOGGER.warning("Malformed cube at position (" + cubePos.getX() + ", " + cubePos.getY() + ", " + cubePos.getZ() + "), skipping!");
            return outCubes;
        }

        byte[] blocks = sectionDetails.getByteArray("Blocks");
        byte[] meta = sectionDetails.getByteArray("Data");

        int baseX = cubePos.getX() * 16;
        int baseY = cubePos.getY() * 16;
        int baseZ = cubePos.getZ() * 16;

        for (int i = 0; i < 4096; i++) {
            int x = baseX + (i & 15);
            int y = baseY + (i >> 8 & 15);
            int z = baseZ + (i >> 4 & 15);

            int schematicX = transformInverse.transformX(x, y, z);
            int schematicY = transformInverse.transformY(x, y, z);
            int schematicZ = transformInverse.transformZ(x, y, z);

            if (schematicX < 0 || schematicX >= schematic.getXSize() ||
                    schematicY < 0 || schematicY >= schematic.getYSize() ||
                    schematicZ < 0 || schematicZ >= schematic.getZSize()) {
                continue;
            }
            int idx = schematic.index(schematicX, schematicY, schematicZ);
            int id = schematic.id(idx);
            if (skipAir && id == 0) {
                continue;
            }
            blocks[i] = (byte) id;
            EditTask.nibbleSetAtIndex(meta, i, schematic.meta(idx));
            // TODO: id MSB
        }

        outCubes.add(new ImmutablePair<>(cubePos, new ImmutablePair<>(inCubePriority+1, cubeTag)));
        return outCubes;
    }

    @Nonnull @Override public List<BoundingBox> getSrcBoxes() {
        return this.workingArea;
    }

    @Nonnull @Override public List<BoundingBox> getDstBoxes() {
        return this.workingArea;
    }

    @Override
    public boolean createSrcCubesIfMissing() {
        return true;
    }
}
