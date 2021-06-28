package cubicchunks.converter.lib.convert.io;

import cubicchunks.converter.lib.Dimension;
import cubicchunks.converter.lib.convert.ChunkDataWriter;
import cubicchunks.converter.lib.convert.data.PriorityCubicChunksColumnData;
import cubicchunks.converter.lib.util.*;
import cubicchunks.regionlib.impl.EntryLocation2D;
import cubicchunks.regionlib.impl.EntryLocation3D;
import cubicchunks.regionlib.impl.SaveCubeColumns;
import cubicchunks.regionlib.impl.save.SaveSection2D;
import cubicchunks.regionlib.impl.save.SaveSection3D;
import cubicchunks.regionlib.lib.ExtRegion;
import cubicchunks.regionlib.lib.provider.SimpleRegionProvider;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PriorityCubicChunkWriter implements ChunkDataWriter<PriorityCubicChunksColumnData> {

    private final Path dstPath;
    private final Map<Dimension, SaveCubeColumns> saves = new ConcurrentHashMap<>();

    public PriorityCubicChunkWriter(Path dstPath) {
        this.dstPath = dstPath;
    }

    private Map<Vector3i, Long> cubePriorities = new HashMap<>();

    @Override public void accept(PriorityCubicChunksColumnData data) throws IOException {
        SaveCubeColumns save = saves.computeIfAbsent(data.getDimension(), dim -> {
            try {
                Path path = dstPath.resolve(dim.getDirectory());

                Utils.createDirectories(path);

                Path part2d = path.resolve("region2d");
                Utils.createDirectories(part2d);

                Path part3d = path.resolve("region3d");
                Utils.createDirectories(part3d);

                SaveSection2D section2d = new SaveSection2D(
                        new RWLockingCachedRegionProvider<>(
                                new SimpleRegionProvider<>(new EntryLocation2D.Provider(), part2d, (keyProv, r) ->
                                        new MemoryWriteRegion.Builder<EntryLocation2D>()
                                                .setDirectory(part2d)
                                                .setRegionKey(r)
                                                .setKeyProvider(keyProv)
                                                .setSectorSize(512)
                                                .build(),
                                        (file, key) -> Files.exists(file)
                                )
                        ),
                        new RWLockingCachedRegionProvider<>(
                                new SimpleRegionProvider<>(new EntryLocation2D.Provider(), part2d,
                                        (keyProvider, regionKey) -> new ExtRegion<>(part2d, Collections.emptyList(), keyProvider, regionKey),
                                        (dir, key) -> Files.exists(dir.resolveSibling(key.getRegionKey().getName() + ".ext"))
                                )
                        ));
                SaveSection3D section3d = new SaveSection3D(
                        new RWLockingCachedRegionProvider<>(
                                new SimpleRegionProvider<>(new EntryLocation3D.Provider(), part3d, (keyProv, r) ->
                                        new MemoryWriteRegion.Builder<EntryLocation3D>()
                                                .setDirectory(part3d)
                                                .setRegionKey(r)
                                                .setKeyProvider(keyProv)
                                                .setSectorSize(512)
                                                .build(),
                                        (file, key) -> Files.exists(file)
                                )
                        ),
                        new RWLockingCachedRegionProvider<>(
                                new SimpleRegionProvider<>(new EntryLocation3D.Provider(), part3d,
                                        (keyProvider, regionKey) -> new ExtRegion<>(part3d, Collections.emptyList(), keyProvider, regionKey),
                                        (dir, key) -> Files.exists(dir.resolveSibling(key.getRegionKey().getName() + ".ext"))
                                )
                        ));

                return new SaveCubeColumns(section2d, section3d);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        EntryLocation2D pos = data.getPosition();
        if (data.getColumnData() != null) {
            save.save2d(pos, data.getColumnData());
        }
        EntryLocation2D entryPos = data.getPosition();
        for (Map.Entry<Integer, ImmutablePair<Long, ByteBuffer>> entry : data.getCubeData().entrySet()) {
            Vector3i cubePos = new Vector3i(entryPos.getEntryX(), entry.getKey(), entryPos.getEntryZ());
            Long priority = cubePriorities.get(cubePos);
            if(priority == null || entry.getValue().getFirst() > priority) {
                cubePriorities.put(cubePos, entry.getValue().getKey());
                save.save3d(new EntryLocation3D(pos.getEntryX(), entry.getKey(), pos.getEntryZ()), entry.getValue().getValue());
            }
        }
    }

    @Override public void discardData() throws IOException {
        Utils.rm(dstPath);
    }

    @Override public void close() throws Exception {
        boolean exception = false;
        for (SaveCubeColumns save : saves.values()) {
            try {
                save.close();
            } catch (IOException e) {
                e.printStackTrace();
                exception = true;
            }
        }
        if (exception) {
            throw new IOException();
        }
    }
}