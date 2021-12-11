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
