package cubicchunks.converter.lib.convert.cc2ccmerging;

import cubicchunks.converter.lib.conf.ConverterConfig;
import cubicchunks.converter.lib.convert.ChunkDataConverter;
import cubicchunks.converter.lib.convert.data.CubicChunksColumnData;
import cubicchunks.converter.lib.convert.data.DualSourceCubicChunksColumnData;
import cubicchunks.converter.lib.util.edittask.EditTask;

import java.nio.ByteBuffer;
import java.util.*;

public class CC2CCDualSourceMergingDataConverter implements ChunkDataConverter<DualSourceCubicChunksColumnData, CubicChunksColumnData> {
    private final List<EditTask> relocateTasks;

    @SuppressWarnings("unchecked")
    public CC2CCDualSourceMergingDataConverter(ConverterConfig config) {
        relocateTasks = (List<EditTask>) config.getValue("relocations");
    }

    @Override public Set<CubicChunksColumnData> convert(DualSourceCubicChunksColumnData input) {
        Map<Integer, ByteBuffer> priorityCubes = input.getPriorityCubeData();
        Map<Integer, ByteBuffer> fallbackCubes = input.getFallbackCubeData();
        Map<Integer, ByteBuffer> inCubes = new HashMap<>();

        if(priorityCubes != null)
            priorityCubes.forEach(inCubes::put);
        fallbackCubes.forEach((y, cube) -> {
            if(!inCubes.containsKey(y))
                inCubes.put(y, cube);
        });

        Map<Integer, ByteBuffer> outCubes = new HashMap<>();
        inCubes.forEach((y, cube) -> {
            for (EditTask relocateTask : relocateTasks) {
                if(relocateTask.getSourceBox().intersects(input.getPosition().getEntryX(), y, input.getPosition().getEntryZ())) {
                    outCubes.put(y, cube);
                    break;
                }
            }
        });

        Set<CubicChunksColumnData> columnData = new HashSet<>();
        columnData.add(new CubicChunksColumnData(input.getDimension(), input.getPosition(), input.getColumnData(), outCubes));
        return columnData;
    }
}