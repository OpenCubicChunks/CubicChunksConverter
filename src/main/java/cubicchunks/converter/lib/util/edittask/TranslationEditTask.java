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

import net.kyori.nbt.*;

import java.util.UUID;

public abstract class TranslationEditTask extends BaseEditTask {
    protected void inplaceMoveTileEntitiesBy(CompoundTag cubeLevel, int blockX, int blockY, int blockZ) {
        ListTag tileEntities = cubeLevel.getList("TileEntities");
        if(tileEntities.type() != TagType.COMPOUND)
            return;

        for (Tag tileEntity : tileEntities) {
            CompoundTag tileEntityData = (CompoundTag) tileEntity;
            if(!(tileEntityData.containsAll(TagType.INT, "x", "y", "z")))
                throw new RuntimeException("TileEntity data did not contain INT tags x, y, z");

            tileEntityData.put("x", new IntTag((tileEntityData.getInt("x") + blockX)));
            tileEntityData.put("y", new IntTag((tileEntityData.getInt("y") + blockY)));
            tileEntityData.put("z", new IntTag((tileEntityData.getInt("z") + blockZ)));
        }
    }

    protected void inplaceMoveEntitiesBy(CompoundTag cubeLevel, int blockX, int blockY, int blockZ, boolean replaceUUIDs) {
        ListTag entities = cubeLevel.getList("Entities");
        if(entities.type() != TagType.COMPOUND)
            return;

        for (int idx = 0, size = entities.size(); idx < size; idx++) {
            CompoundTag entityData = entities.getCompound(idx);
            ListTag pos = entityData.getList("Pos", TagType.DOUBLE);
            if(pos.size() < 3)
                throw new RuntimeException("TileEntity data did not contain INT tags x, y, z");

            ListTag newPos = new ListTag(TagType.DOUBLE);

            newPos.add(new DoubleTag(pos.getDouble(0) + blockX));
            newPos.add(new DoubleTag(pos.getDouble(1) + blockY));
            newPos.add(new DoubleTag(pos.getDouble(2) + blockZ));

            entityData.put("Pos", newPos);

            if(replaceUUIDs) {
                UUID uuid = UUID.randomUUID();
                entityData.put("UUIDLeast", new LongTag(uuid.getLeastSignificantBits()));
                entityData.put("UUIDMost", new LongTag(uuid.getMostSignificantBits()));
            }
        }
    }
}
