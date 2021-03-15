package cubicchunks.converter.lib.util.edittask;

import cubicchunks.converter.lib.convert.cc2ccrelocating.CC2CCRelocatingDataConverter;
import cubicchunks.converter.lib.util.BoundingBox;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public abstract class BaseEditTask implements EditTask {
    protected final List<BoundingBox> srcBoxes = new ArrayList<>();
    protected final List<BoundingBox> dstBoxes = new ArrayList<>();

    protected static final Logger LOGGER = Logger.getLogger(CC2CCRelocatingDataConverter.class.getSimpleName());

    @Nonnull @Override public List<BoundingBox> getSrcBoxes() {
        return srcBoxes;
    }

    @Nonnull @Override public List<BoundingBox> getDstBoxes() {
        return dstBoxes;
    }
}
