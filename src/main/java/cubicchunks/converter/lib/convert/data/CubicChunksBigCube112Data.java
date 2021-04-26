package cubicchunks.converter.lib.convert.data;

import cubicchunks.converter.lib.Dimension;
import cubicchunks.regionlib.impl.EntryLocation3D;

import java.nio.ByteBuffer;
import java.util.Objects;

public class CubicChunksBigCube112Data {

    private final Dimension dimension;
    private final EntryLocation3D position;
    private final ByteBuffer columnData;
    private final ByteBuffer[] cubeData;

    public CubicChunksBigCube112Data(Dimension dimension, EntryLocation3D position, ByteBuffer columnData, ByteBuffer[] cubeData) {
        this.dimension = dimension;
        this.position = position;
        this.columnData = columnData;
        this.cubeData = cubeData;
    }

    public Dimension getDimension() {
        return dimension;
    }

    public EntryLocation3D getPosition() {
        return position;
    }

    public ByteBuffer getColumnData() {
        return columnData;
    }

    public ByteBuffer[] getCubeData() {
        return cubeData;
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CubicChunksBigCube112Data that = (CubicChunksBigCube112Data) o;
        return dimension.equals(that.dimension) &&
                position.equals(that.position) &&
                Objects.equals(columnData, that.columnData) &&
                Objects.equals(cubeData, that.cubeData);
    }

    @Override public int hashCode() {
        return Objects.hash(dimension, position, columnData, cubeData);
    }

    @Override public String toString() {
        return "CubicChunksBigCubeData{" +
                "dimension='" + dimension + '\'' +
                ", position=" + position +
                ", columnData=" + columnData +
                ", cubeData=" + cubeData +
                '}';
    }
}
