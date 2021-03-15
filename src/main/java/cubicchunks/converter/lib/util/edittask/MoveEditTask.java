package cubicchunks.converter.lib.util.edittask;

import com.flowpowered.nbt.CompoundTag;
import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.ImmutablePair;
import cubicchunks.converter.lib.util.Vector3i;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class MoveEditTask extends BaseEditTask {
    private final Vector3i offset;

    public MoveEditTask(BoundingBox srcBox, Vector3i dstOffset) {
        srcBoxes.add(srcBox);
        dstBoxes.add(srcBox.add(dstOffset));
        offset = dstOffset;
    }

    @Nonnull @Override public List<ImmutablePair<Vector3i, CompoundTag>> actOnCube(ImmutablePair<Vector3i, CompoundTag> cube) {
        List<ImmutablePair<Vector3i, CompoundTag>> outCubes = new ArrayList<>();
        outCubes.add(new ImmutablePair<>(cube.getKey().add(offset), cube.getValue()));
        return outCubes;
    }
}
