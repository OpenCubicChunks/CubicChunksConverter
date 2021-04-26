package cubicchunks.converter.lib.convert.io;

import static cubicchunks.converter.lib.util.Utils.interruptibleConsumer;

import cubicchunks.converter.lib.Dimension;
import cubicchunks.converter.lib.convert.data.CubicChunksBigCube112Data;
import cubicchunks.converter.lib.util.BigCubeCoords;
import cubicchunks.converter.lib.util.MemoryReadRegion;
import cubicchunks.converter.lib.util.RWLockingCachedRegionProvider;
import cubicchunks.converter.lib.util.UncheckedInterruptedException;
import cubicchunks.converter.lib.util.Utils;
import cubicchunks.regionlib.api.region.IRegionProvider;
import cubicchunks.regionlib.impl.EntryLocation2D;
import cubicchunks.regionlib.impl.EntryLocation3D;
import cubicchunks.regionlib.impl.SaveCubeColumns;
import cubicchunks.regionlib.impl.save.SaveSection2D;
import cubicchunks.regionlib.impl.save.SaveSection3D;
import cubicchunks.regionlib.lib.ExtRegion;
import cubicchunks.regionlib.lib.provider.SimpleRegionProvider;
import cubicchunks.regionlib.util.CheckedConsumer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class CubicChunksBigCube112Reader extends BaseMinecraftReader<CubicChunksBigCube112Data, SaveCubeColumns> {

    private final CompletableFuture<ChunkList> chunkList = new CompletableFuture<>();
    private final Thread loadThread;


    public CubicChunksBigCube112Reader(Path srcDir) {
        super(srcDir, (dim, path) -> Files.exists(getDimensionPath(dim, path)) ? createSave(getDimensionPath(dim, path)) : null);
        loadThread = Thread.currentThread();
    }

    @Override public void countInputChunks(Runnable increment) throws IOException, InterruptedException {
        try {
            Map<Dimension, List<Map.Entry<EntryLocation3D, Integer>>> dimensions = doCountChunks(increment);
            chunkList.complete(new ChunkList(dimensions));
        } catch (UncheckedInterruptedException ex) {
            chunkList.complete(null);
        }
    }

    private Map<Dimension, List<Map.Entry<EntryLocation3D, Integer>>> doCountChunks(Runnable increment) {
        Map<Dimension, List<Map.Entry<EntryLocation3D, Integer>>> dimensions = new HashMap<>();
        for (Map.Entry<Dimension, SaveCubeColumns> entry : saves.entrySet()) {
            SaveCubeColumns save = entry.getValue();
            Dimension dim = entry.getKey();
            Map<EntryLocation3D, Integer> chunksMap = new ConcurrentHashMap<>();

            CheckedConsumer<EntryLocation3D, IOException> addPos = interruptibleConsumer(loc -> {
                int index = BigCubeCoords.sectionToIndex32(loc.getEntryX(), loc.getEntryY(), loc.getEntryZ());
                chunksMap.merge(new EntryLocation3D(
                                BigCubeCoords.sectionToCube(loc.getEntryX()),
                                BigCubeCoords.sectionToCube(loc.getEntryY()),
                                BigCubeCoords.sectionToCube(loc.getEntryZ())),
                        1 << index, (a, b) -> a | b);
            });
            try {
                save.getSaveSection3D().forAllKeys(addPos);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            List<Map.Entry<EntryLocation3D, Integer>> chunks = dimensions.computeIfAbsent(dim, p -> new ArrayList<>());
            chunksMap.forEach((k, v) -> chunks.add(new AbstractMap.SimpleEntry<>(k, v)));
        }
        return dimensions;
    }

    @Override public void loadChunks(Consumer<? super CubicChunksBigCube112Data> accept, Predicate<Throwable> errorHandler)
            throws InterruptedException {
        try {
            ChunkList list = chunkList.get();
            if (list == null) {
                return; // counting interrupted
            }
            doLoadChunks(accept, list, errorHandler);
        } catch (UncheckedInterruptedException ex) {
            // interrupted, do nothing
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void doLoadChunks(Consumer<? super CubicChunksBigCube112Data> consumer, ChunkList list, Predicate<Throwable> errorHandler) {
        for (Map.Entry<Dimension, List<Map.Entry<EntryLocation3D, Integer>>> dimEntry : list.getChunks().entrySet()) {
            if (Thread.interrupted()) {
                return;
            }
            Dimension dim = dimEntry.getKey();
            SaveCubeColumns save = saves.get(dim);
            dimEntry.getValue().parallelStream().forEach(chunksEntry -> {
                if (Thread.interrupted()) {
                    return;
                }
                EntryLocation3D pos = chunksEntry.getKey();
                int presentSections = chunksEntry.getValue();
                if (presentSections != 0xFF) {
                    System.out.println("Skipping incomplete cube at " + pos + " sections = " + Integer.toBinaryString(presentSections));
                    return;
                }
                ByteBuffer[] cubes = new ByteBuffer[8];
                for (int i = 0; i < 8; i++) {
                    if (Thread.interrupted()) {
                        return;
                    }
                    if ((presentSections & (1 << i)) == 0) {
                        continue;
                    }
                    int dx = BigCubeCoords.indexToX(i);
                    int dy = BigCubeCoords.indexToY(i);
                    int dz = BigCubeCoords.indexToZ(i);
                    EntryLocation3D sectionPos = new EntryLocation3D(
                            BigCubeCoords.cubeToSection(pos.getEntryX(), dx),
                            BigCubeCoords.cubeToSection(pos.getEntryY(), dy),
                            BigCubeCoords.cubeToSection(pos.getEntryZ(), dz)
                    );
                    ByteBuffer cube;
                    try {
                        cube = save.load(sectionPos, true).orElseThrow(
                                () -> new IllegalStateException("Expected cube (section) at " + sectionPos + " in dimension " + dim));
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (!errorHandler.test(e)) {
                            throw new UncheckedInterruptedException();
                        }
                        continue;
                    }
                    cubes[i] = cube;
                }
                CubicChunksBigCube112Data data = new CubicChunksBigCube112Data(dim, pos, null, cubes);
                consumer.accept(data);
            });
        }
    }

    @Override public void stop() {
        loadThread.interrupt();
    }


    private static Path getDimensionPath(Dimension d, Path worldDir) {
        if (!d.getDirectory().isEmpty()) {
            worldDir = worldDir.resolve(d.getDirectory());
        }
        return worldDir;
    }

    private static SaveCubeColumns createSave(Path path) {
        try {
            Utils.createDirectories(path);

            Path part2d = path.resolve("region2d");
            Utils.createDirectories(part2d);

            Path part3d = path.resolve("region3d");
            Utils.createDirectories(part3d);

            EntryLocation2D.Provider keyProv2d = new EntryLocation2D.Provider();
            EntryLocation3D.Provider keyProv3d = new EntryLocation3D.Provider();

            IRegionProvider<EntryLocation2D> prov2d1, prov2d2;
            IRegionProvider<EntryLocation3D> prov3d1, prov3d2;
            SaveSection2D section2d = new SaveSection2D(
                    prov2d1 = new RWLockingCachedRegionProvider<>(
                            new SimpleRegionProvider<>(keyProv2d, part2d, (keyProv, r) ->
                                    new MemoryReadRegion.Builder<EntryLocation2D>()
                                            .setDirectory(part2d)
                                            .setRegionKey(r)
                                            .setKeyProvider(keyProv2d)
                                            .setSectorSize(512)
                                            .build(),
                                    (file, key) -> Files.exists(file)
                            )
                    ),
                    prov2d2 = new RWLockingCachedRegionProvider<>(
                            new SimpleRegionProvider<>(new EntryLocation2D.Provider(), part2d,
                                    (keyProvider, regionKey) -> new ExtRegion<>(part2d, Collections.emptyList(), keyProvider, regionKey),
                                    (file, key) -> Files.exists(file.resolveSibling(key.getRegionKey().getName() + ".ext"))
                            )
                    ));
            SaveSection3D section3d = new SaveSection3D(
                    prov3d1 = new RWLockingCachedRegionProvider<>(
                            new SimpleRegionProvider<>(keyProv3d, part3d, (keyProv, r) ->
                                    new MemoryReadRegion.Builder<EntryLocation3D>()
                                            .setDirectory(part3d)
                                            .setRegionKey(r)
                                            .setKeyProvider(keyProv3d)
                                            .setSectorSize(512)
                                            .build(),
                                    (file, key) -> Files.exists(file)
                            )
                    ),
                    prov3d2 = new RWLockingCachedRegionProvider<>(
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

    private static class ChunkList {

        private final Map<Dimension, List<Map.Entry<EntryLocation3D, Integer>>> chunks;

        private ChunkList(Map<Dimension, List<Map.Entry<EntryLocation3D, Integer>>> chunks) {
            this.chunks = chunks;
        }

        Map<Dimension, List<Map.Entry<EntryLocation3D, Integer>>> getChunks() {
            return chunks;
        }
    }

}
