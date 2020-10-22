package cubicchunks.converter.lib.convert.cc2ccmerging;

import cubicchunks.converter.lib.convert.ChunkDataConverter;
import cubicchunks.converter.lib.convert.data.CubicChunksColumnData;
import cubicchunks.converter.lib.convert.data.DualSourceCubicChunksColumnData;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CC2CCDualSourceMergingDataConverter implements ChunkDataConverter<DualSourceCubicChunksColumnData, CubicChunksColumnData> {
    @Override public Set<CubicChunksColumnData> convert(DualSourceCubicChunksColumnData input) {
        Map<Integer, ByteBuffer> priorityCubes = input.getPriorityCubeData();
        Map<Integer, ByteBuffer> fallbackCubes = input.getFallbackCubeData();
        Map<Integer, ByteBuffer> cubes = new HashMap<>();

        if(priorityCubes != null)
            priorityCubes.forEach(cubes::put);
        fallbackCubes.forEach((y, cube) -> {
            if(!cubes.containsKey(y))
                cubes.put(y, cube);
        });

        Set<CubicChunksColumnData> columnData = new HashSet<>();
        columnData.add(new CubicChunksColumnData(input.getDimension(), input.getPosition(), input.getColumnData(), cubes));
        return columnData;
    }
}