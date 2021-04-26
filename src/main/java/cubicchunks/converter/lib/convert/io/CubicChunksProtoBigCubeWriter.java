package cubicchunks.converter.lib.convert.io;

import cubicchunks.converter.lib.Dimension;
import cubicchunks.converter.lib.convert.ChunkDataWriter;
import cubicchunks.converter.lib.convert.data.CubicChunksProtoBigCubeData;
import cubicchunks.converter.lib.util.MemoryWriteRegion;
import cubicchunks.converter.lib.util.RWLockingCachedRegionProvider;
import cubicchunks.converter.lib.util.Utils;
import cubicchunks.regionlib.impl.EntryLocation2D;
import cubicchunks.regionlib.impl.EntryLocation3D;
import cubicchunks.regionlib.impl.SaveCubeColumns;
import cubicchunks.regionlib.impl.save.SaveSection2D;
import cubicchunks.regionlib.impl.save.SaveSection3D;
import cubicchunks.regionlib.lib.ExtRegion;
import cubicchunks.regionlib.lib.provider.SimpleRegionProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CubicChunksProtoBigCubeWriter  implements ChunkDataWriter<CubicChunksProtoBigCubeData> {

    private final Path dstPath;
    private final Map<Dimension, SaveCubeColumns> saves = new ConcurrentHashMap<>();

    public CubicChunksProtoBigCubeWriter(Path dstPath) {
        this.dstPath = dstPath;
    }

    @Override public void accept(CubicChunksProtoBigCubeData data) throws IOException {
        SaveCubeColumns save = saves.computeIfAbsent(data.getDimension(), this::initSave);

        EntryLocation3D pos = data.getPosition();
        if (data.getColumnData() != null) {
            save.save2d(new EntryLocation2D(pos.getEntryX(), pos.getEntryZ()), data.getColumnData());
        }
        if (data.getCubeData() != null) {
            save.save3d(pos, data.getCubeData());
        }
    }

    private SaveCubeColumns initSave(Dimension dim) {
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
