package cubicchunks.converter.lib;

import java.io.IOException;
import java.nio.file.Path;

public interface ISaveConverter {
	void convert(IProgress progress, Path srcDir, Path dstDir) throws IOException;
}
