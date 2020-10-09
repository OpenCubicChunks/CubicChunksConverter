package cubicchunks.converter.lib.util;

public class BlockEditTask extends EditTask {

    private final Byte inBlockId;
    private final Byte inBlockMeta;

    private final byte outBlockId;
    private final byte outBlockMeta;

    public BlockEditTask(BoundingBox src, Vector3i offset, Type type, byte id, byte meta) {
        super(src, offset, type);

        inBlockId = null;
        inBlockMeta = null;

        outBlockId = id;
        outBlockMeta = meta;
    }

    public BlockEditTask(BoundingBox src, Vector3i offset, Type type, byte inId, byte inMeta, byte outId, byte outMeta) {
        super(src, offset, type);

        inBlockId = inId;
        inBlockMeta = inMeta;

        outBlockId = outId;
        outBlockMeta = outMeta;
    }

    public Byte getInBlockId() {
        return inBlockId;
    }

    public Byte getInBlockMeta() {
        return inBlockMeta;
    }

    public byte getOutBlockId() {
        return outBlockId;
    }

    public byte getOutBlockMeta() {
        return outBlockMeta;
    }
}
