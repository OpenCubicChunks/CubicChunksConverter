package cubicchunks.converter.lib.convert.data;

import cubicchunks.converter.lib.Dimension;
import cubicchunks.converter.lib.util.ImmutablePair;
import cubicchunks.regionlib.impl.EntryLocation2D;

import java.nio.ByteBuffer;
import java.util.Map;

public class PriorityCubicChunksColumnData {
    private final Dimension dimension;
    private final EntryLocation2D position;
    private final ByteBuffer columnData;
    private final Map<Integer, ImmutablePair<Long, ByteBuffer>> cubeData;

    private final boolean isCompressed;

    public PriorityCubicChunksColumnData(Dimension dimension, EntryLocation2D position, ByteBuffer columnData,
                                         Map<Integer, ImmutablePair<Long, ByteBuffer>> cubeData, boolean isCompressed) {
        this.dimension = dimension;
        this.position = position;
        this.columnData = columnData;
        this.cubeData = cubeData;
        this.isCompressed = isCompressed;
    }

    public Dimension getDimension() {
        return dimension;
    }

    public EntryLocation2D getPosition() {
        return position;
    }

    public ByteBuffer getColumnData() {
        return columnData;
    }

    public Map<Integer, ImmutablePair<Long, ByteBuffer>> getCubeData() {
        return cubeData;
    }

    public boolean isCompressed() {
        return isCompressed;
    }
}
