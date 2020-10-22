package cubicchunks.converter.lib.convert.impl;

import cubicchunks.regionlib.impl.SaveCubeColumns;

import java.io.Closeable;
import java.io.IOException;

public class DualSourceSaveCubeColumns implements Closeable {

    private final SaveCubeColumns prioritySave;
    private final SaveCubeColumns fallbackSave;

    public DualSourceSaveCubeColumns(SaveCubeColumns _prioritySave, SaveCubeColumns _fallbackSave) {
        prioritySave = _prioritySave;
        fallbackSave = _fallbackSave;
    }

    public SaveCubeColumns getPrioritySave() {
        return prioritySave;
    }

    public SaveCubeColumns getFallbackSave() {
        return fallbackSave;
    }

    @Override public void close() throws IOException {
        prioritySave.close();
        fallbackSave.close();
    }
}
