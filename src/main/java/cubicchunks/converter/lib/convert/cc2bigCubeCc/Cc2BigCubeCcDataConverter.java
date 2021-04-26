package cubicchunks.converter.lib.convert.cc2bigCubeCc;

import cubicchunks.converter.lib.convert.ChunkDataConverter;
import cubicchunks.converter.lib.convert.data.CubicChunksProtoBigCubeData;
import cubicchunks.converter.lib.convert.data.CubicChunksBigCube112Data;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

public class Cc2BigCubeCcDataConverter  implements ChunkDataConverter<CubicChunksBigCube112Data, CubicChunksProtoBigCubeData> {

    private final ByteBuffer prepend;

    public Cc2BigCubeCcDataConverter() {
        ByteArrayOutputStream dataOut = new ByteArrayOutputStream();
        try (OutputStream out = new BufferedOutputStream(new GZIPOutputStream(dataOut))){
            // NBT can't begin with byte 255, so this acts as a marker for
            // 1.12.2 "proto-big-cube" converted data
            out.write(255);
            // a second byte with value 0 for potential future extension
            out.write(0);
        } catch (IOException e) {
            throw new Error(e);
        }
        this.prepend = ByteBuffer.wrap(dataOut.toByteArray());
    }

    @Override public Set<CubicChunksProtoBigCubeData> convert(CubicChunksBigCube112Data input) {
        if (input.getColumnData() != null && input.getCubeData() == null) {
            return Collections.singleton(new CubicChunksProtoBigCubeData(input.getDimension(), input.getPosition(), input.getColumnData(), null));
        }
        ByteBuffer prepend = this.prepend.duplicate();
        int totalSize = prepend.capacity();
        for (ByteBuffer cube : input.getCubeData()) {
            totalSize += cube.capacity();
        }
        ByteBuffer buf = ByteBuffer.wrap(new byte[totalSize]);
        prepend.clear();
        buf.put(prepend);
        for (ByteBuffer cube : input.getCubeData()) {
            cube.clear();
            buf.put(cube);
        }
        buf.clear();
        return Collections.singleton(new CubicChunksProtoBigCubeData(input.getDimension(), input.getPosition(), null, buf));
    }
}
