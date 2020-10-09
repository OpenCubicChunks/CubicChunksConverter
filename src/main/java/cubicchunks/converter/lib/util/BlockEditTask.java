package cubicchunks.converter.lib.util;

public class BlockEditTask extends EditTask {

    private final byte blockId;
    private final byte blockMeta;

    public BlockEditTask(BoundingBox src, Vector3i offset, Type type, byte id, byte meta) {
        super(src, offset, type);

        blockId = id;
        blockMeta = meta;
    }

    public byte getBlockId() {
        return blockId;
    }

    public byte getBlockMeta() {
        return blockMeta;
    }
}
