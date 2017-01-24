/*
 *  This file is part of CubicChunksConverter, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2017 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package cubicchunks.converter.lib;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Utils {


	public static int countFiles(Path f) throws IOException {
		try {
			return countFiles_do(f);
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}

	private static int countFiles_do(Path f) {
		if (Files.isRegularFile(f)) {
			return 1;
		} else if (!Files.isDirectory(f)) {
			throw new UnsupportedOperationException();
		}

		try {
			return Files.list(f).map(p -> countFiles_do(p)).reduce((x, y) -> x + y).orElse(0);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static void copyEverythingExcept(Path file, Path srcDir, Path dstDir, Predicate<Path> excluded, Consumer<Path> onCopy) throws IOException {
		try {
			Files.list(file).forEach(f -> {
				if (!excluded.test(f)) {
					try {
						copyFile(f, srcDir, dstDir);
						if (Files.isRegularFile(f)) {
							onCopy.accept(f);
						}
						if (Files.isDirectory(f)) {
							Files.createDirectories(f);
							copyEverythingExcept(f, srcDir, dstDir, excluded, onCopy);
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

	private enum OS {
		WINDOWS, MACOS, SOLARIS, LINUX, UNKNOWN;
	}

	private static OS getPlatform() {
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.contains("win")) {
			return OS.WINDOWS;
		}
		if (osName.contains("mac")) {
			return OS.MACOS;
		}
		if (osName.contains("linux")) {
			return OS.LINUX;
		}
		if (osName.contains("unix")) {
			return OS.LINUX;
		}
		return OS.UNKNOWN;
	}

	public static Path getApplicationDirectory() {
		String userHome = System.getProperty("user.home", ".");
		Path workingDirectory;
		switch (getPlatform()) {
			case LINUX:
			case SOLARIS:
				workingDirectory = Paths.get(userHome, ".minecraft/");
				break;
			case WINDOWS:
				String applicationData = System.getenv("APPDATA");
				String folder = applicationData != null ? applicationData : userHome;

				workingDirectory = Paths.get(folder, ".minecraft/");
				break;
			case MACOS:
				workingDirectory = Paths.get(userHome, "Library/Application Support/minecraft");
				break;
			default:
				workingDirectory = Paths.get(userHome, "minecraft/");
		}
		return workingDirectory;
	}
}
