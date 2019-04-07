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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import cubicchunks.converter.lib.convert.ChunkDataConverter;
import cubicchunks.converter.lib.convert.ChunkDataReader;
import cubicchunks.converter.lib.convert.ChunkDataWriter;
import cubicchunks.converter.lib.convert.LevelInfoConverter;
import cubicchunks.converter.lib.convert.anvil2cc.Anvil2CCDataConverter;
import cubicchunks.converter.lib.convert.anvil2cc.Anvil2CCLevelInfoConverter;
import cubicchunks.converter.lib.convert.cc2anvil.CC2AnvilDataConverter;
import cubicchunks.converter.lib.convert.cc2anvil.CC2AnvilLevelInfoConverter;
import cubicchunks.converter.lib.convert.data.AnvilChunkData;
import cubicchunks.converter.lib.convert.data.CubicChunksColumnData;
import cubicchunks.converter.lib.convert.data.MultilayerAnvilChunkData;
import cubicchunks.converter.lib.convert.io.AnvilChunkReader;
import cubicchunks.converter.lib.convert.io.AnvilChunkWriter;
import cubicchunks.converter.lib.convert.io.CubicChunkReader;
import cubicchunks.converter.lib.convert.io.CubicChunkWriter;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class Registry {
    private static final BiMap<String, Function<Path, ? extends ChunkDataReader<?>>> readersByName = Maps.synchronizedBiMap(HashBiMap.create());
    private static final BiMap<Class<?>, Function<Path, ? extends ChunkDataReader<?>>> readersByClass = Maps.synchronizedBiMap(HashBiMap.create());

    private static final BiMap<String, Function<Path, ? extends ChunkDataWriter<?>>> writersByName = Maps.synchronizedBiMap(HashBiMap.create());
    private static final BiMap<Class<?>, Function<Path, ? extends ChunkDataWriter<?>>> writersByClass = Maps.synchronizedBiMap(HashBiMap.create());

    private static final BiMap<ClassPair<?, ?>, Supplier<? extends ChunkDataConverter<?, ?>>> convertersByClass = Maps.synchronizedBiMap(HashBiMap.create());
    private static final BiMap<ClassPair<?, ?>, BiFunction<Path, Path, ? extends LevelInfoConverter<?, ?>>> levelConvertersByClass = Maps.synchronizedBiMap(HashBiMap.create());

    static {
        registerReader("Anvil", AnvilChunkReader::new, AnvilChunkData.class);
        registerReader("CubicChunks", CubicChunkReader::new, CubicChunksColumnData.class);

        registerWriter("Anvil", AnvilChunkWriter::new, MultilayerAnvilChunkData.class);
        registerWriter("CubicChunks", CubicChunkWriter::new, CubicChunksColumnData.class);

        registerConverter(Anvil2CCDataConverter::new, Anvil2CCLevelInfoConverter::new, AnvilChunkData.class, CubicChunksColumnData.class);
        registerConverter(CC2AnvilDataConverter::new, CC2AnvilLevelInfoConverter::new, CubicChunksColumnData.class, MultilayerAnvilChunkData.class);
    }

    // can't have all named register because of type erasure

    public static <T> void registerReader(String name, Function<Path, ChunkDataReader<T>> reader, Class<T> clazz) {
        readersByName.put(name, reader);
        readersByClass.put(clazz, reader);
    }

    public static <T> void registerWriter(String name, Function<Path, ChunkDataWriter<T>> writer, Class<T> clazz) {
        writersByName.put(name, writer);
        writersByClass.put(clazz, writer);
    }

    public static <IN, OUT> void registerConverter(Supplier<ChunkDataConverter<IN, OUT>> conv,
        BiFunction<Path, Path, LevelInfoConverter<IN, OUT>> levelConv, Class<IN> in, Class<OUT> out) {

        convertersByClass.put(new ClassPair<>(in, out), conv);
        levelConvertersByClass.put(new ClassPair<>(in, out), levelConv);
    }

    public static Iterable<String> getWriters() {
        return writersByName.keySet();
    }

    public static Iterable<String> getReaders() {
        return readersByName.keySet();
    }

    public static Iterable<ClassPair<?, ?>> getConverters() {
        return convertersByClass.keySet();
    }

    @SuppressWarnings("unchecked")
    public static <T> Function<Path, ? extends ChunkDataReader<T>> getReader(String name) {
        return (Function<Path, ? extends ChunkDataReader<T>>) readersByName.get(name);
    }

    @SuppressWarnings("unchecked")
    public static <T> Function<Path, ? extends ChunkDataWriter<T>> getWriter(String name) {
        return (Function<Path, ? extends ChunkDataWriter<T>>) writersByName.get(name);
    }

    @SuppressWarnings("unchecked")
    public static String getReader(Class<?> clazz) {
        return readersByName.inverse().get(readersByClass.get(clazz));
    }

    @SuppressWarnings("unchecked")
    public static String getWriter(Class<?> clazz) {
        return writersByName.inverse().get(writersByClass.get(clazz));
    }

    @SuppressWarnings("unchecked")
    public static <IN, OUT> Supplier<ChunkDataConverter<IN, OUT>> getConverter(String inputName, String outputName) {
        ClassPair<IN, OUT> pair = new ClassPair<>(
            getReaderClass(inputName),
            getWriterClass(outputName)
        );
        return (Supplier<ChunkDataConverter<IN, OUT>>) convertersByClass.get(pair);
    }

    @SuppressWarnings("unchecked")
    public static <IN, OUT> Supplier<ChunkDataConverter<IN, OUT>> getConverter(ClassPair<IN, OUT> classes) {
        return (Supplier<ChunkDataConverter<IN, OUT>>) convertersByClass.get(classes);
    }

    @SuppressWarnings("unchecked")
    public static <IN, OUT> BiFunction<Path, Path, LevelInfoConverter<IN, OUT>> getLevelConverter(String inputName, String outputName) {
        ClassPair<IN, OUT> pair = new ClassPair<>(
            getReaderClass(inputName),
            getWriterClass(outputName)
        );
        return (BiFunction<Path, Path, LevelInfoConverter<IN, OUT>>) levelConvertersByClass.get(pair);
    }

    @SuppressWarnings("unchecked")
    public static <IN, OUT> BiFunction<Path, Path, LevelInfoConverter<IN, OUT>> getLevelConverter(ClassPair<IN, OUT> classes) {
        return (BiFunction<Path, Path, LevelInfoConverter<IN, OUT>>) levelConvertersByClass.get(classes);
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> getReaderClass(String name) {
        return (Class<T>) readersByClass.inverse().get(getReader(name));
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> getWriterClass(String writer) {
        return (Class<T>) writersByClass.inverse().get(getWriter(writer));
    }

    public static class ClassPair<X, Y> {
        private final Class<X> in;
        private final Class<Y> out;

        private ClassPair(Class<X> in, Class<Y> out) {
            this.in = in;
            this.out = out;
        }

        public Class<X> getIn() {
            return in;
        }

        public Class<Y> getOut() {
            return out;
        }

        @Override public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ClassPair<?, ?> classPair = (ClassPair<?, ?>) o;
            return in.equals(classPair.in) &&
                out.equals(classPair.out);
        }

        @Override public int hashCode() {
            return Objects.hash(in, out);
        }

        @Override public String toString() {
            return "ClassPair{" +
                "in=" + in +
                ", out=" + out +
                '}';
        }
    }
}
