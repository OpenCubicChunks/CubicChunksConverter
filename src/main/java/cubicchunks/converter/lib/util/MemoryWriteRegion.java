package cubicchunks.converter.lib.util;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

import cubicchunks.regionlib.api.region.IRegion;
import cubicchunks.regionlib.api.region.IRegionProvider;
import cubicchunks.regionlib.api.region.key.IKey;
import cubicchunks.regionlib.api.region.key.IKeyProvider;
import cubicchunks.regionlib.api.region.key.RegionKey;
import cubicchunks.regionlib.lib.Region;
import cubicchunks.regionlib.lib.RegionEntryLocation;
import cubicchunks.regionlib.util.CheckedConsumer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

public class MemoryWriteRegion<K extends IKey<K>> implements IRegion<K> {

    private static final int SIZE_BITS = 8;
    private static final int OFFSET_BITS = Integer.SIZE - SIZE_BITS;
    private static final int SIZE_MASK = (1 << SIZE_BITS) - 1;
    private static final int MAX_SIZE = SIZE_MASK;
    private static final int OFFSET_MASK = (1 << OFFSET_BITS) - 1;
    private static final int MAX_OFFSET = OFFSET_MASK;

    private final SeekableByteChannel file;
    private final int sectorSize;
    private final int keyCount;
    private WriteEntry[] writeEntries;

    private MemoryWriteRegion(SeekableByteChannel file,
            RegionKey regionKey,
            IKeyProvider<K> keyProvider,
            int sectorSize) throws IOException {
        this.keyCount = keyProvider.getKeyCount(regionKey);
        this.file = file;
        this.sectorSize = sectorSize;
    }

    @Override public synchronized void writeValue(K key, ByteBuffer value) throws IOException {
        if (value == null) {
            return;
        }
        if (writeEntries == null) {
            writeEntries = new WriteEntry[keyCount];
            if (file.size() >= keyCount * Integer.BYTES) {
                ByteBuffer fileBuffer = ByteBuffer.allocate((int) file.size());

                file.position(0);
                file.read(fileBuffer);

                fileBuffer.reset();

                for (int i = 0; i < keyCount; i++) {
                    int loc = fileBuffer.getInt();
                    int sizeBytes = unpackSize(loc) * sectorSize;
                    ByteBuffer data = ByteBuffer.allocate(sizeBytes);
                    int offsetBytes = unpackOffset(loc) * sectorSize;
                    fileBuffer.limit(offsetBytes + sizeBytes);
                    fileBuffer.position(offsetBytes);
                    data.put(fileBuffer);

                    writeEntries[i] = new WriteEntry(data);
                }
            }
        }
        int size = value.remaining();
        int sizeWithSizeInfo = size + Integer.BYTES;
        int numSectors = getSectorNumber(sizeWithSizeInfo);

        ByteBuffer data = ByteBuffer.allocate(numSectors * sectorSize);
        data.putInt(size);
        data.put(value);
        writeEntries[key.getId()] = new WriteEntry(data);
    }

    @Override public void writeSpecial(K key, Object marker) throws IOException {
        throw new UnsupportedOperationException("writeSpecial not supported");
    }

    @Override public synchronized Optional<ByteBuffer> readValue(K key) throws IOException {
        throw new UnsupportedOperationException("readValue not supported");
    }

    /**
     * Returns true if something was stored there before within this region.
     */
    @Override public synchronized boolean hasValue(K key) {
        throw new UnsupportedOperationException("hasValue not supported");
    }

    @Override public void forEachKey(CheckedConsumer<? super K, IOException> cons) throws IOException {
        throw new UnsupportedOperationException("forEachKey not supported");
    }


    private int getSectorNumber(int bytes) {
        return ceilDiv(bytes, sectorSize);
    }

    @Override public void close() throws IOException {
        ByteBuffer header = ByteBuffer.allocate(keyCount * Integer.BYTES);
        int writePos = ceilDiv(keyCount * Integer.BYTES, sectorSize);
        for (WriteEntry writeEntry : writeEntries) {
            if (writeEntry == null) {
                header.putInt(0);
                continue;
            }
            int sectorCount = ceilDiv(writeEntry.buffer.remaining(), sectorSize);
            header.putInt(packed(new RegionEntryLocation(writePos, sectorCount)));
            writePos += sectorCount;
        }
        file.position(0);
        file.write(header);
        for (WriteEntry writeEntry : writeEntries) {
            if (writeEntry == null) {
                continue;
            }
            file.write(writeEntry.buffer);
        }
        Arrays.fill(writeEntries, null);
        this.file.close();
    }

    private static int ceilDiv(int x, int y) {
        return -Math.floorDiv(-x, y);
    }

    public static <L extends IKey<L>> Region.Builder<L> builder() {
        return new Region.Builder<>();
    }

    private static int unpackOffset(int sectorLocation) {
        return sectorLocation >>> SIZE_BITS;
    }

    private static int unpackSize(int sectorLocation) {
        return sectorLocation & SIZE_MASK;
    }

    private static int packed(RegionEntryLocation location) {
        if ((location.getSize() & SIZE_MASK) != location.getSize()) {
            throw new IllegalArgumentException("Supported entry size range is 0 to " + MAX_SIZE + ", but got " + location.getSize());
        }
        if ((location.getOffset() & OFFSET_MASK) != location.getOffset()) {
            throw new IllegalArgumentException("Supported entry offset range is 0 to " + MAX_OFFSET + ", but got " + location.getOffset());
        }
        return location.getSize() | (location.getOffset() << SIZE_BITS);
    }

    private static class WriteEntry {

        final ByteBuffer buffer;

        private WriteEntry(ByteBuffer buffer) {
            this.buffer = buffer;
        }
    }
    /**
     * Internal Region builder. Using it is very unsafe, there are no safeguards against using it improperly. Should only be used by
     * {@link IRegionProvider} implementations.
     */
    // TODO: make a safer to use builder
    public static class Builder<K extends IKey<K>> {

        private Path directory;
        private int sectorSize = 512;
        private RegionKey regionKey;
        private IKeyProvider<K> keyProvider;

        public MemoryWriteRegion.Builder<K> setDirectory(Path path) {
            this.directory = path;
            return this;
        }

        public MemoryWriteRegion.Builder<K> setRegionKey(RegionKey key) {
            this.regionKey = key;
            return this;
        }

        public MemoryWriteRegion.Builder<K> setKeyProvider(IKeyProvider<K> keyProvider) {
            this.keyProvider = keyProvider;
            return this;
        }

        public MemoryWriteRegion.Builder<K> setSectorSize(int sectorSize) {
            this.sectorSize = sectorSize;
            return this;
        }

        public MemoryWriteRegion<K> build() throws IOException {
            SeekableByteChannel file = Files.newByteChannel(directory.resolve(regionKey.getName()), CREATE, READ, WRITE);
            return new MemoryWriteRegion<>(file, this.regionKey, keyProvider, this.sectorSize);
        }
    }
}
