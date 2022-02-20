/*
 *  This file is part of CubicChunksConverter, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2017-2021 contributors
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
package cubicchunks.converter.lib.util;

import com.flowpowered.nbt.ByteArrayTag;
import com.flowpowered.nbt.ByteTag;
import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.IntArrayTag;
import com.flowpowered.nbt.IntTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.flowpowered.nbt.stream.NBTOutputStream;
import cubicchunks.regionlib.impl.EntryLocation3D;
import cubicchunks.regionlib.util.CheckedConsumer;
import cubicchunks.regionlib.util.CheckedFunction;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

import static java.util.Collections.emptyList;

public class Utils {

    /**
     * Returns a consumer that checks for interruption, and throws {@link UncheckedInterruptedException}
     * if thread is interrupted.
     */
    public static <T, E extends Throwable> CheckedConsumer<T, E> interruptibleConsumer(CheckedConsumer<T, E> cons) {
        return x -> {
            if (Thread.interrupted()) {
                throw new UncheckedInterruptedException();
            }
            cons.accept(x);
        };
    }

    // from one of the replies in https://stackoverflow.com/questions/27644361/how-can-i-throw-checked-exceptions-from-inside-java-8-streams
    public static <IN, OUT> Function<IN, OUT> propagateExceptions(CheckedFunction<IN, OUT, ?> func) {
        return x -> {
            try {
                return func.apply(x);
            } catch (Throwable t) {
                return throwUnchecked(t);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static <T, E extends Throwable> T throwUnchecked(Throwable t) throws E {
        throw (E) t;
    }

    // Files.createDirectories doesn't handle symlinks
    public static void createDirectories(Path dir) throws IOException {
        if (Files.isDirectory(dir)) {
            return;
        }
        createDirectories(dir.getParent());
        try {
            Files.createDirectory(dir);
        } catch (FileAlreadyExistsException ex) {}
    }

    public static boolean isValidPath(String text) {
        try {
            Files.exists(Paths.get(text));
            return true;
        } catch (InvalidPathException e) {
            return false;
        }
    }

    public static boolean fileExists(String text) {
        try {
            return Files.exists(Paths.get(text));
        } catch (InvalidPathException e) {
            return false;
        }
    }

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

        try (Stream<Path> stream = Files.list(f)) {
            return stream.mapToInt(Utils::countFiles_do).sum();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void copyEverythingExcept(Path file, Path srcDir, Path dstDir, Predicate<Path> excluded, Consumer<Path> onCopy) throws IOException {
        // workaround for WSL: windows filesystem mounted on WSL doesn't support copying file attributes
        String testFileName = "__CC_CONVERTER_TEST_FILE_NAME_786432879129048";
        Path testPath1 = dstDir.resolve(testFileName);
        Path testPath2 = dstDir.resolve(testFileName + "_OUT");
        Files.write(testPath1, new byte[0]);
        boolean canCopyFileAttributes;
        try {
            Files.copy(testPath1, testPath2, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            canCopyFileAttributes = true;
        } catch (IOException ignore) {
            canCopyFileAttributes = false;
        } finally {
            Files.delete(testPath1);
            Files.delete(testPath2);
        }
        copyEverythingExceptInternal(file, srcDir, dstDir, excluded, onCopy, canCopyFileAttributes);
    }
    private static void copyEverythingExceptInternal(Path file, Path srcDir, Path dstDir, Predicate<Path> excluded, Consumer<Path> onCopy, boolean canCopyAttributes) throws IOException{
        try (Stream<Path> stream = Files.list(file)) {
            stream.forEach(f -> {
                if (!excluded.test(f)) {
                    try {
                        copyFile(f, srcDir, dstDir, canCopyAttributes);
                        if (Files.isRegularFile(f)) {
                            onCopy.accept(f);
                        }
                        if (Files.isDirectory(f)) {
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

    public static void copyFile(Path srcFile, Path srcDir, Path dstDir, boolean canCopyFileAttributes) throws IOException {
        Path relative = srcDir.relativize(srcFile);//
        Path dstFile = dstDir.resolve(relative);

        // TODO: handle symlinks
        // workaround for WSL: Windows filesystem mounted on WSL doesn't allow copying directories
        Files.createDirectories(dstFile.getParent());
        if (Files.isDirectory(srcFile)) {
            if(!Files.exists(dstFile)) {
                Files.createDirectories(dstFile);
            }
            return;
        }
        if (canCopyFileAttributes) {
            Files.copy(srcFile, dstFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        } else {
            Files.copy(srcFile, dstFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static CompoundTag readCompressed(InputStream is) throws IOException {
        int i = is.read();
        BufferedInputStream data;
        if (i == 1) {
            data = new BufferedInputStream(new GZIPInputStream(is));
        } else if (i == 2) {
            data = new BufferedInputStream(new InflaterInputStream(is));
        } else {
            throw new UnsupportedOperationException();
        }

        return (CompoundTag) new NBTInputStream(data, false).readTag();
    }

    public static CompoundTag readCompressedCC(InputStream is) throws IOException {
        BufferedInputStream data = new BufferedInputStream(new GZIPInputStream(is));
        return (CompoundTag) new NBTInputStream(data, false).readTag();
    }

    public static ByteBuffer writeCompressed(CompoundTag tag, boolean prefixFormat) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        if (prefixFormat) {
            bytes.write(1); // mark as GZIP
        }
        NBTOutputStream nbtOut = new NBTOutputStream(new BufferedOutputStream(new GZIPOutputStream(bytes)), false);
        nbtOut.writeTag(tag);
        nbtOut.close();
        bytes.flush();
        return ByteBuffer.wrap(bytes.toByteArray());
    }

    public static ByteBuffer createAirCubeBuffer(EntryLocation3D loc) {
        CompoundTag compoundTag = Utils.emptyCube(loc.getEntryX(), loc.getEntryY(), loc.getEntryZ());
        try {
            return Utils.writeCompressed(compoundTag, false);
        } catch(IOException e) {
            throw new Error("Writing known NBT to ByteBuffer shouldn't throw IOException", e);
        }
    }

    public static CompoundTag emptyCube(int x, int y, int z) {
        CompoundMap root = new CompoundMap();
        {
            CompoundMap level = new CompoundMap();

            {
                level.put(new ByteTag("v", (byte) 1));
                level.put(new IntTag("x", x));
                level.put(new IntTag("y", y));
                level.put(new IntTag("z", z));

                level.put(new ByteTag("populated", true));
                level.put(new ByteTag("fullyPopulated", true));
                level.put(new ByteTag("isSurfaceTracked", true)); // it's empty, no need to re-track


                level.put(new ListTag<>("Sections", CompoundTag.class, Collections.singletonList(createEmptySectionTag())));

                level.put(new ByteTag("initLightDone", false));

                level.put(new ListTag<>("Entities", CompoundTag.class, emptyList()));
                level.put(new ListTag<>("TileEntities", CompoundTag.class, emptyList()));

                level.put(makeEmptyLightingInfo());
            }
            root.put(new CompoundTag("Level", level));
        }
        return new CompoundTag("", root);
    }

    public static CompoundTag createEmptySectionTag() {
        CompoundMap sectionData = new CompoundMap();
        sectionData.put("Blocks", new ByteArrayTag("Blocks", new byte[4096]));
        sectionData.put("Data", new ByteArrayTag("Data", new byte[2048]));
        sectionData.put("BlockLight", new ByteArrayTag("BlockLight", new byte[2048]));
        sectionData.put("SkyLight", new ByteArrayTag("SkyLight", new byte[2048]));

        return new CompoundTag("", sectionData);
    }
    private static CompoundTag makeEmptyLightingInfo() {
        IntArrayTag heightmap = new IntArrayTag("LastHeightMap", new int[256]);
        CompoundMap lightingInfoMap = new CompoundMap();
        lightingInfoMap.put(heightmap);
        return new CompoundTag("LightingInfo", lightingInfoMap);
    }

    /**
     * Deletes the specified file or directory, recursively
     */
    public static void rm(Path toDelete) throws IOException {
        if (Files.isDirectory(toDelete)) {
            try (Stream<Path> files = Files.list(toDelete)) {
                for (Path path : files.collect(Collectors.toList())) {
                    rm(path);
                }
            }
            Files.delete(toDelete);
        } else {
            Files.delete(toDelete);
        }
    }

    public static boolean isEmpty(final Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return true;
        }
        try(DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
            return !dirStream.iterator().hasNext();
        }
    }

    public static <E extends Throwable> void forEachDirectory(Path directory, CheckedConsumer<Path, E> consumer) throws E, IOException {
        try(DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
            for (Path path : dirStream) {
                if (Files.isDirectory(path)) {
                    consumer.accept(path);
                }
            }
        }
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
