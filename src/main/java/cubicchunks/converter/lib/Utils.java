package cubicchunks.converter.lib;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Predicate;

public class Utils {
	public static void copyEverythingExcept(Path file, Path srcDir, Path dstDir, Predicate<Path> excluded) throws IOException {
		try {
			Files.list(file).forEach(f -> {
				if (!excluded.test(f)) {
					try {
						copyFile(f, srcDir, dstDir);
						if (Files.isDirectory(f)) {
							Files.createDirectories(f);
							copyEverythingExcept(f, srcDir, dstDir, excluded);
						} else if (!Files.isRegularFile(f)) {
							throw new UnsupportedOperationException();
						}
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}
			});
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}

	public static void copyFile(Path srcFile, Path srcDir, Path dstDir) throws IOException {
		Path relative = srcDir.relativize(srcFile);
		Path dstFile = dstDir.resolve(relative);

		if (Files.exists(dstFile) && Files.isDirectory(dstFile)) {
			return; // already exists, stop here to avoid DirectoryNotEmptyException
		}
		Files.copy(srcFile, dstFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
	}

}
