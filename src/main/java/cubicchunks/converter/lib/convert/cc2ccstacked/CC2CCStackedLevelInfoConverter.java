package cubicchunks.converter.lib.convert.cc2ccstacked;

import cubicchunks.converter.lib.Dimensions;
import cubicchunks.converter.lib.convert.LevelInfoConverter;
import cubicchunks.converter.lib.convert.data.CubicChunksColumnData;
import cubicchunks.converter.lib.util.Utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CC2CCStackedLevelInfoConverter implements LevelInfoConverter<CubicChunksColumnData, CubicChunksColumnData> {

    private final Path srcDir;
    private final Path dstDir;

    public CC2CCStackedLevelInfoConverter(Path srcDir, Path dstDir) {
        this.srcDir = srcDir;
        this.dstDir = dstDir;
    }

    @Override public void convert() throws IOException {
        Utils.createDirectories(dstDir);
        Utils.copyEverythingExcept(srcDir, srcDir, dstDir, file ->
                        Dimensions.getDimensions().stream().anyMatch(dim ->
                                srcDir.resolve(dim.getDirectory()).resolve("region2d").equals(file) ||
                                        srcDir.resolve(dim.getDirectory()).resolve("region3d").equals(file)
                        ),
                f -> {
                } // TODO: counting files
        );
    }
}
