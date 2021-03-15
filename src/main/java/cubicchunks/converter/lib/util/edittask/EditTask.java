package cubicchunks.converter.lib.util.edittask;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.ImmutablePair;
import cubicchunks.converter.lib.util.Vector3i;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public interface EditTask {
    /**
     * @param cube The cube to be modified. The {@link Vector3i} is in cube coordinates, not block
     * @return The modified cube/s. The {@link CompoundMap} can be null, if so the cube will be regenerated the next time it's loaded by the game
     */
    @Nonnull List<ImmutablePair<Vector3i, CompoundTag>> actOnCube(ImmutablePair<Vector3i, CompoundTag> cube);

    /**
     * @return A list of bounding boxes describing which cubes this task wants to recieve in {@link EditTask#actOnCube(ImmutablePair)}
     */
    @Nonnull List<BoundingBox> getSrcBoxes();

    /**
     * @return A list of bounding boxes describing which cubes this task will modify from {@link EditTask#actOnCube(ImmutablePair)}
     */
    @Nonnull List<BoundingBox> getDstBoxes();

    /**
     * @return A task/list of tasks that would entirely undo any operation done by this task from a backup world
     */
    @Nonnull default List<EditTask> getInverse() {
        List<EditTask> tasks = new ArrayList<>();
        getDstBoxes().forEach(box -> tasks.add(new MoveEditTask(box, new Vector3i(0, 0, 0))));
        return tasks;
    }

    /**
     * @return Whether this task requires the cube data at all. If this returns false {@link EditTask#actOnCube(ImmutablePair)}
     * will never be called. Only {@link EditTask#getSrcBoxes()}
     * This is used in tasks such as {@link KeepEditTask}, as no cube data is required.
     */
    default boolean readsCubeData() {
        return true;
    }

    default boolean isCubeSrc(int x, int y, int z) {
        for (BoundingBox box : getSrcBoxes()) {
            if (box.intersects(x, y, z))
                return true;
        }
        return false;
    }
    default boolean isCubeDst(int x, int y, int z) {
        for (BoundingBox box : getDstBoxes()) {
            if (box.intersects(x, y, z))
                return true;
        }
        return false;
    }

//    private boolean isCubeDstExclusive(int x, int y, int z) {
//        if (editTask.getOffset() != null)
//            return editTask.getSourceBox().add(editTask.getOffset()).intersects(x, y, z);
//        return false;
//    }

    static int nibbleGetAtIndex(byte[] arr, int index)
    {
        int i = index >> 1;
        return (index & 1) == 0 ? arr[i] & 0xf : arr[i] >> 4 & 0xf;
    }

    static void nibbleSetAtIndex(byte[] arr, int index, int value)
    {
        int i = index >> 1;

        if ((index & 1) == 0) {
            arr[i] = (byte)(arr[i] & 0xf0 | value & 0xf);
        }
        else {
            arr[i] = (byte)(arr[i] & 0xf | (value & 0xf) << 4);
        }
    }
}
