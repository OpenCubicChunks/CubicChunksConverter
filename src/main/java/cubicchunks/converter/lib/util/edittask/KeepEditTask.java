package cubicchunks.converter.lib.util.edittask;

import com.flowpowered.nbt.CompoundTag;
import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.ImmutablePair;
import cubicchunks.converter.lib.util.Vector3i;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class KeepEditTask extends BaseEditTask {
    public KeepEditTask(BoundingBox srcBox) {
        srcBoxes.add(srcBox);
    }

    @Nonnull @Override public List<ImmutablePair<Vector3i, CompoundTag>> actOnCube(ImmutablePair<Vector3i, CompoundTag> cube) {
        List<ImmutablePair<Vector3i, CompoundTag>> cubes = new ArrayList<>(1);
        cubes.add(cube);
        return cubes;
    }

    @Override public boolean readsCubeData() {
        return false;
    }
}
