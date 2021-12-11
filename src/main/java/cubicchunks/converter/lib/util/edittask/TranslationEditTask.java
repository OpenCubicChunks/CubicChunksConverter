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

import com.flowpowered.nbt.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class TranslationEditTask extends BaseEditTask {
    protected void inplaceMoveTileEntitiesBy(CompoundMap cubeLevel, int blockX, int blockY, int blockZ) {
        List<CompoundTag> tileEntities = ((ListTag<CompoundTag>) cubeLevel.get("TileEntities")).getValue();
        for (int idx = 0, size = tileEntities.size(); idx < size; idx++) {
            CompoundMap tileEntityData = tileEntities.get(idx).getValue();
            tileEntityData.put("x", new IntTag("x", ((IntTag)tileEntityData.get("x")).getValue() + blockX));
            tileEntityData.put("y", new IntTag("y", ((IntTag)tileEntityData.get("y")).getValue() + blockY));
            tileEntityData.put("z", new IntTag("z", ((IntTag)tileEntityData.get("z")).getValue() + blockZ));
        }
    }

    protected void inplaceMoveEntitiesBy(CompoundMap cubeLevel, int blockX, int blockY, int blockZ, boolean replaceUUIDs) {
        List<CompoundTag> entities = ((ListTag<CompoundTag>) cubeLevel.get("Entities")).getValue();
        for (int idx = 0, size = entities.size(); idx < size; idx++) {
            CompoundMap entityData = entities.get(idx).getValue();
            List<DoubleTag> pos = ((ListTag<DoubleTag>) entityData.get("Pos")).getValue();
            List<DoubleTag> newPos = new ArrayList<>();

            //they don't have names WHAT, ordered x, y, z
            newPos.add(new DoubleTag("", pos.get(0).getValue() + blockX));
            newPos.add(new DoubleTag("", pos.get(1).getValue() + blockY));
            newPos.add(new DoubleTag("", pos.get(2).getValue() + blockZ));

            entityData.put(new ListTag<>("Pos", DoubleTag.class, newPos));

            if(replaceUUIDs) {
                UUID uuid = UUID.randomUUID();
                entityData.put(new LongTag("UUIDLeast", uuid.getLeastSignificantBits()));
                entityData.put(new LongTag("UUIDMost", uuid.getMostSignificantBits()));
            }
        }
    }
}
