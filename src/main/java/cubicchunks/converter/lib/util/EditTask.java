package cubicchunks.converter.lib.util;

import cubicchunks.converter.lib.convert.cc2ccrelocating.CC2CCRelocatingDataConverter;

public final class EditTask {
    private final BoundingBox source;
    private final Vector3i offset;

    public enum Type {NONE, CUT, COPY, MOVE, REMOVE, KEEP}

    private final Type type;

    public EditTask(BoundingBox src, Vector3i offset, Type type) {
        this.source = src;
        this.offset = offset;
        this.type = type;
    }

    public BoundingBox getSourceBox() {
        return this.source;
    }

    public Vector3i getOffset() {
        return this.offset;
    }

    public Type getType() {
        return this.type;
    }
}
