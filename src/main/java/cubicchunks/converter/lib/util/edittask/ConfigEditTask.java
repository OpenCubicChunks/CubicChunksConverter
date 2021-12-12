package cubicchunks.converter.lib.util.edittask;

import com.flowpowered.nbt.CompoundTag;
import cubicchunks.converter.lib.conf.command.EditTaskContext;
import cubicchunks.converter.lib.util.ImmutablePair;
import cubicchunks.converter.lib.util.Vector3i;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

public class ConfigEditTask extends BaseEditTask {
    private final Consumer<EditTaskContext.EditTaskConfig> configConsumer;
    public ConfigEditTask(Consumer<EditTaskContext.EditTaskConfig> configConsumer) {
        this.configConsumer = configConsumer;
    }

    @Override public void initialise(EditTaskContext.EditTaskConfig config) {
        this.configConsumer.accept(config);
    }

    @Nonnull
    @Override public List<ImmutablePair<Vector3i, ImmutablePair<Long, CompoundTag>>> actOnCube(Vector3i cubePos, EditTaskContext.EditTaskConfig config, CompoundTag cubeTag, long inCubePriority) {
        throw new IllegalStateException("ConfigEditTask actOnCube should never be called as it doesn't request cube data");
    }

    @Override public boolean readsCubeData() {
        return false;
    }
}
