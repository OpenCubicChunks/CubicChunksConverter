package cubicchunks.converter.lib.util.edittask;

import com.flowpowered.nbt.ByteTag;
import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.ImmutablePair;
import cubicchunks.converter.lib.util.Vector2i;
import cubicchunks.converter.lib.util.Vector3i;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SetEditTask extends BaseEditTask {
    private final byte blockID;
    private final byte blockMeta;

    public SetEditTask(BoundingBox srcBox, byte blockID, byte blockMeta) {
        srcBoxes.add(srcBox);
        dstBoxes.add(srcBox);
        this.blockID = blockID;
        this.blockMeta = blockMeta;
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

        CompoundMap sectionDetails;
        try {
            sectionDetails = ((CompoundTag)((List<?>) (entryLevel).get("Sections").getValue()).get(0)).getValue(); //POSSIBLE ARRAY OUT OF BOUNDS EXCEPTION ON A MALFORMED CUBE
        } catch(NullPointerException | ArrayIndexOutOfBoundsException e) {
            LOGGER.warning("Malformed cube at position (" + cubeX + ", " + cubeY + ", " + cubeZ + "), skipping!");
            return outCubes;
        }
        Arrays.fill((byte[]) sectionDetails.get("Blocks").getValue(), blockID);
        Arrays.fill((byte[]) sectionDetails.get("Data").getValue(), (byte) (blockMeta | blockMeta << 4));

        outCubes.add(new ImmutablePair<>(cube.getKey(), cube.getValue()));
        return outCubes;
    }
}
