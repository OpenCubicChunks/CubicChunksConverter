package cubicchunks.converter.lib.util.edittask;

import com.flowpowered.nbt.*;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.flowpowered.nbt.stream.NBTOutputStream;
import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.ImmutablePair;
import cubicchunks.converter.lib.util.Vector3i;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CopyEditTask extends BaseEditTask {
    private final Vector3i offset;

    public CopyEditTask(BoundingBox srcBox, Vector3i dstOffset) {
        srcBoxes.add(srcBox);
        dstBoxes.add(srcBox.add(dstOffset));
        offset = dstOffset;
    }

    @Nonnull @Override public List<ImmutablePair<Vector3i, ImmutablePair<Long, CompoundTag>>> actOnCube(Vector3i cubePos, CompoundTag cubeTag, long inCubePriority) {
        List<ImmutablePair<Vector3i, ImmutablePair<Long, CompoundTag>>> outCubes = new ArrayList<>();

        int cubeX = cubePos.getX();
        int cubeY = cubePos.getY();
        int cubeZ = cubePos.getZ();

        try {
            if(!dstBoxes.get(0).intersects(cubeX, cubeY, cubeZ)) {
                //this is just doing a deep copy of the tag by writing to byte array then back again
                ByteArrayOutputStream bout = new ByteArrayOutputStream(1024);
                NBTOutputStream out = new NBTOutputStream(bout, false);
                out.writeTag(cubeTag);

                NBTInputStream is = new NBTInputStream(new ByteArrayInputStream(bout.toByteArray()), false);
                CompoundTag tag = (CompoundTag) is.readTag();
                //copy done here ^

                outCubes.add(new ImmutablePair<>(new Vector3i(cubeX, cubeY, cubeZ), new ImmutablePair<>(inCubePriority+1, tag)));
            }

            CompoundMap level = (CompoundMap) cubeTag.getValue().get("Level").getValue();

            int dstX = cubeX + offset.getX();
            int dstY = cubeY + offset.getY();
            int dstZ = cubeZ + offset.getZ();
            level.put(new IntTag("x", dstX));
            level.put(new IntTag("y", dstY));
            level.put(new IntTag("z", dstZ));

            level.put(new ByteTag("isSurfaceTracked", (byte) 0));
            level.put(new ByteTag("initLightDone", (byte) 0));
            level.put(new ByteTag("populated", (byte) 1));
            level.put(new ByteTag("fullyPopulated", (byte) 1));

            outCubes.add(new ImmutablePair<>(new Vector3i(dstX, dstY, dstZ), new ImmutablePair<>(inCubePriority+1, cubeTag)));
        } catch (IOException ignored) {

        }
        return outCubes;
    }
}
