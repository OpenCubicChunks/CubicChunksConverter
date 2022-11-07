package cubicchunks.converter.lib.util.edittask;

import com.google.common.collect.Lists;
import cubicchunks.converter.lib.conf.command.EditTaskContext;
import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.ImmutablePair;
import cubicchunks.converter.lib.util.Vector3i;
import net.kyori.nbt.CompoundTag;

import javax.annotation.Nonnull;
import java.util.List;

public class ReTrackEditTask extends BaseEditTask {
    public ReTrackEditTask(BoundingBox affectedBox) {
        srcBoxes.add(affectedBox);
        dstBoxes.add(affectedBox);
    }

    @Nonnull
    @Override
    public List<ImmutablePair<Vector3i, ImmutablePair<Long, CompoundTag>>> actOnCube(Vector3i cubePos, EditTaskContext.EditTaskConfig config, CompoundTag cubeTag, long inCubePriority) {
        CompoundTag level = cubeTag.getCompound("Level");

        this.markCubeForReSurfaceTrack(level);

        return Lists.newArrayList(new ImmutablePair<>(cubePos, new ImmutablePair<>(inCubePriority+1, cubeTag)));
    }
}
