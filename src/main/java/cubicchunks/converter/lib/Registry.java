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
package cubicchunks.converter.lib;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import cubicchunks.converter.lib.conf.ConverterConfig;
import cubicchunks.converter.lib.convert.ChunkDataConverter;
import cubicchunks.converter.lib.convert.ChunkDataReader;
import cubicchunks.converter.lib.convert.ChunkDataWriter;
import cubicchunks.converter.lib.convert.LevelInfoConverter;
import cubicchunks.converter.lib.convert.anvil2cc.Anvil2CCDataConverter;
import cubicchunks.converter.lib.convert.anvil2cc.Anvil2CCLevelInfoConverter;
import cubicchunks.converter.lib.convert.cc2bigCubeCc.Cc2BigCubeCcDataConverter;
import cubicchunks.converter.lib.convert.cc2bigCubeCc.Cc2BigCubeCcLevelInfoConverter;
import cubicchunks.converter.lib.convert.cc2ccrelocating.CC2CCRelocatingDataConverter;
import cubicchunks.converter.lib.convert.cc2ccrelocating.CC2CCRelocatingLevelInfoConverter;
import cubicchunks.converter.lib.convert.cc2anvil.CC2AnvilDataConverter;
import cubicchunks.converter.lib.convert.cc2anvil.CC2AnvilLevelInfoConverter;
import cubicchunks.converter.lib.convert.data.*;
import cubicchunks.converter.lib.convert.io.*;
import cubicchunks.converter.lib.convert.robinton2cc.Robinton2CCConverter;
import cubicchunks.converter.lib.convert.robinton2cc.Robinton2CCLevelInfoConverter;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Registry {
    private static final String ID_PATTERN = "[a-z][a-z_0-9]*(:[a-z0-9_.-]+)?";

    private static final BiMap<String, BiFunction<Path, ConverterConfig, ? extends ChunkDataReader<?>>> readersByName = Maps.synchronizedBiMap(HashBiMap.create());
    private static final BiMap<String, BiFunction<Path, ConverterConfig, ? extends ChunkDataReader<?>>> readersById = Maps.synchronizedBiMap(HashBiMap.create());
    private static final BiMap<Class<?>, BiFunction<Path, ConverterConfig, ? extends ChunkDataReader<?>>> readersByClass = Maps.synchronizedBiMap(HashBiMap.create());

    private static final BiMap<String, Function<Path, ? extends ChunkDataWriter<?>>> writersByName = Maps.synchronizedBiMap(HashBiMap.create());
    private static final BiMap<String, Function<Path, ? extends ChunkDataWriter<?>>> writersById = Maps.synchronizedBiMap(HashBiMap.create());
    private static final BiMap<Class<?>, Function<Path, ? extends ChunkDataWriter<?>>> writersByClass = Maps.synchronizedBiMap(HashBiMap.create());

    private static final BiMap<StringTriple, Class<? extends ChunkDataConverter<?, ?>>> convertersByName = Maps.synchronizedBiMap(HashBiMap.create());
    private static final BiMap<StringTriple, Class<? extends ChunkDataConverter<?, ?>>> convertersById = Maps.synchronizedBiMap(HashBiMap.create());
    private static final BiMap<ClassTriple<?, ?, ?>, Function<ConverterConfig, ? extends ChunkDataConverter<?, ?>>> convertersByClass = Maps.synchronizedBiMap(HashBiMap.create());
    private static final BiMap<ClassTriple<?, ?, ?>, BiFunction<Path, Path, ? extends LevelInfoConverter<?, ?>>> levelConvertersByClass = Maps.synchronizedBiMap(HashBiMap.create());
    private static final BiMap<ClassTriple<?, ?, ?>, Function<Consumer<Throwable>, ConverterConfig>> configLoaders = Maps.synchronizedBiMap(HashBiMap.create());

    static {
        registerReader("Anvil", "anvil", AnvilChunkReader::new, AnvilChunkData.class);
        registerReader("CubicChunks 1.10 - 1.12", "cubicchunks:1.10-1.12", CubicChunkReader::new, CubicChunksColumnData.class);
        registerReader("CubicChunks 1.10 - 1.12 - Relocating", "cubicchunks:1.10-1.12-relocating", PriorityCubicChunkReader::new, PriorityCubicChunksColumnData.class);
        registerReader("RobintonCubicChunks", "robinton_cubicchunks", RobintonChunkReader::new, RobintonColumnData.class);
        registerReader("CubicChunks 1.10 - 1.12 (BigCube)", "cubicchunks_bigcube:1.10-1.12", CubicChunksBigCube112Reader::new, CubicChunksBigCube112Data.class);

        registerWriter("Anvil", "anvil", AnvilChunkWriter::new, MultilayerAnvilChunkData.class);
        registerWriter("CubicChunks 1.10 - 1.12", "cubicchunks:1.10-1.12", CubicChunkWriter::new, CubicChunksColumnData.class);
        registerWriter("CubicChunks 1.10 - 1.12 - Relocating", "cubicchunks:1.10-1.12-relocating", PriorityCubicChunkWriter::new, PriorityCubicChunksColumnData.class);
        registerWriter("CubicChunks 1.17+ (ProtoBigCube)", "cubicchunks:1.17", CubicChunksProtoBigCubeWriter::new, CubicChunksProtoBigCubeData.class);

        registerConverter("Default", "default", Anvil2CCDataConverter::new, Anvil2CCLevelInfoConverter::new, Anvil2CCDataConverter::loadConfig, AnvilChunkData.class, CubicChunksColumnData.class, Anvil2CCDataConverter.class);
        registerConverter("Default", "default", CC2AnvilDataConverter::new, CC2AnvilLevelInfoConverter::new, CubicChunksColumnData.class, MultilayerAnvilChunkData.class, CC2AnvilDataConverter.class);
        registerConverter("Relocating", "relocating", CC2CCRelocatingDataConverter::new, CC2CCRelocatingLevelInfoConverter::new, CC2CCRelocatingDataConverter::loadConfig, PriorityCubicChunksColumnData.class, PriorityCubicChunksColumnData.class, CC2CCRelocatingDataConverter.class);
        registerConverter("Default", "default", Robinton2CCConverter::new, Robinton2CCLevelInfoConverter::new, RobintonColumnData.class, CubicChunksColumnData.class, Robinton2CCConverter.class);
        registerConverter("Default", "default", Cc2BigCubeCcDataConverter::new, Cc2BigCubeCcLevelInfoConverter::new, CubicChunksBigCube112Data.class, CubicChunksProtoBigCubeData.class, Cc2BigCubeCcDataConverter.class);
    }

    // can't have all named register because of type erasure

    public static <T> void registerReader(String name, String id, Function<Path, ChunkDataReader<T>> reader, Class<T> clazz) {
        registerReader(name, id, (path, conf) -> reader.apply(path), clazz);
    }

    public static <T> void registerReader(String name, String id, BiFunction<Path, ConverterConfig, ChunkDataReader<T>> reader, Class<T> clazz) {
        Preconditions.checkArgument(id.matches(ID_PATTERN), "invalid id: %s", id);

        readersByName.put(name, reader);
        readersById.put(id, reader);
        readersByClass.put(clazz, reader);
    }

    public static <T> void registerWriter(String name, String id, Function<Path, ChunkDataWriter<T>> writer, Class<T> clazz) {
        Preconditions.checkArgument(id.matches(ID_PATTERN), "invalid id: %s", id);

        writersByName.put(name, writer);
        writersById.put(id, writer);
        writersByClass.put(clazz, writer);
    }

    public static <IN, OUT> void registerConverter(String name, String id, Supplier<ChunkDataConverter<IN, OUT>> converterFactory,
                                                   BiFunction<Path, Path, LevelInfoConverter<IN, OUT>> levelConv, Class<IN> in, Class<OUT> out,
                                                   Class<? extends ChunkDataConverter<IN, OUT>> converter) {
        Preconditions.checkArgument(id.matches(ID_PATTERN), "invalid id: %s", id);

        convertersByName.put(new StringTriple(getReader(in), getWriter(out), name), converter);
        convertersById.put(new StringTriple(getReaderId(in), getWriterId(out), id), converter);
        convertersByClass.put(new ClassTriple<>(in, out, converter), conf -> converterFactory.get());
        levelConvertersByClass.put(new ClassTriple<>(in, out, converter), levelConv);
    }

    public static <IN, OUT> void registerConverter(String name, String id, Function<ConverterConfig, ChunkDataConverter<IN, OUT>> converterFactory,
                                                   BiFunction<Path, Path, LevelInfoConverter<IN, OUT>> levelConv,
                                                   Function<Consumer<Throwable>, ConverterConfig> loadConfig,
                                                   Class<IN> in, Class<OUT> out,
                                                   Class<? extends ChunkDataConverter<IN, OUT>> converter) {
        Preconditions.checkArgument(id.matches(ID_PATTERN), "invalid id: %s", id);

        convertersByName.put(new StringTriple(getReader(in), getWriter(out), name), converter);
        convertersById.put(new StringTriple(getReaderId(in), getWriterId(out), id), converter);
        convertersByClass.put(new ClassTriple<>(in, out, converter), converterFactory);
        levelConvertersByClass.put(new ClassTriple<>(in, out, converter), levelConv);
        configLoaders.put(new ClassTriple<>(in, out, converter), loadConfig);
    }

    public static Iterable<String> getWriters() {
        return writersByName.keySet();
    }

    public static Iterable<String> getReaders() {
        return readersByName.keySet();
    }

    public static Iterable<ClassTriple<?, ?, ?>> getConverters() {
        return convertersByClass.keySet();
    }

    @SuppressWarnings("unchecked")
    public static <T> BiFunction<Path, ConverterConfig, ? extends ChunkDataReader<T>> getReader(String name) {
        return (BiFunction<Path, ConverterConfig, ? extends ChunkDataReader<T>>) readersByName.get(name);
    }

    @SuppressWarnings("unchecked")
    public static <T> BiFunction<Path, ConverterConfig, ? extends ChunkDataReader<T>> getReaderById(String id) {
        return (BiFunction<Path, ConverterConfig, ? extends ChunkDataReader<T>>) readersById.get(id);
    }

    @SuppressWarnings("unchecked")
    public static <T> Function<Path, ? extends ChunkDataWriter<T>> getWriter(String name) {
        return (Function<Path, ? extends ChunkDataWriter<T>>) writersByName.get(name);
    }

    @SuppressWarnings("unchecked")
    public static <T> Function<Path, ? extends ChunkDataWriter<T>> getWriterById(String id) {
        return (Function<Path, ? extends ChunkDataWriter<T>>) writersById.get(id);
    }

    @SuppressWarnings("unchecked")
    public static String getReader(Class<?> clazz) {
        return readersByName.inverse().get(readersByClass.get(clazz));
    }

    @SuppressWarnings("unchecked")
    public static String getReaderId(Class<?> clazz) {
        return readersById.inverse().get(readersByClass.get(clazz));
    }

    @SuppressWarnings("unchecked")
    public static String getWriter(Class<?> clazz) {
        return writersByName.inverse().get(writersByClass.get(clazz));
    }

    @SuppressWarnings("unchecked")
    public static String getWriterId(Class<?> clazz) {
        return writersById.inverse().get(writersByClass.get(clazz));
    }

    public static String getConverter(Class<?> clazz) {
        return convertersByName.inverse().get(clazz).converter;
    }

    public static String getConverterId(Class<?> clazz) {
        return convertersById.inverse().get(clazz).converter;
    }

    @SuppressWarnings("unchecked")
    public static <IN, OUT> Function<ConverterConfig, ChunkDataConverter<IN, OUT>> getConverter(String inputName, String outputName, String converterName) {
        ClassTriple<IN, OUT, ChunkDataConverter<IN, OUT>> pair = new ClassTriple<>(
                getReaderClass(inputName),
                getWriterClass(outputName),
                getConverterClass(new StringTriple(inputName, outputName, converterName))
        );
        return (Function<ConverterConfig, ChunkDataConverter<IN, OUT>>) convertersByClass.get(pair);
    }

    @SuppressWarnings("unchecked")
    public static <IN, OUT> Function<ConverterConfig, ChunkDataConverter<IN, OUT>> getConverterById(String inputId, String outputId, String converterId) {
        ClassTriple<IN, OUT, ChunkDataConverter<IN, OUT>> pair = new ClassTriple<>(
                getReaderClassById(inputId),
                getWriterClassById(outputId),
                getConverterClassById(new StringTriple(inputId, outputId, converterId))
        );
        return (Function<ConverterConfig, ChunkDataConverter<IN, OUT>>) convertersByClass.get(pair);
    }

    public static Function<Consumer<Throwable>, ConverterConfig> getConfigLoader(String inputName, String outputName, String converterName) {
        ClassTriple<?, ?, ChunkDataConverter<?, ?>> triple = new ClassTriple<>(
            getReaderClass(inputName),
            getWriterClass(outputName),
            getConverterClass(new StringTriple(inputName, outputName, converterName))
        );
        return configLoaders.get(triple);
    }

    public static Function<Consumer<Throwable>, ConverterConfig> getConfigLoaderById(String inId, String outId, String converterId) {
        ClassTriple<?, ?, ChunkDataConverter<?, ?>> triple = new ClassTriple<>(
            getReaderClassById(inId),
            getWriterClassById(outId),
            getConverterClassById(new StringTriple(inId, outId, converterId))
        );
        return configLoaders.get(triple);
    }

    public static Function<Consumer<Throwable>, ConverterConfig> getConfigLoader(ClassTriple<?, ?, ?> classes) {
        return configLoaders.get(classes);
    }

    @SuppressWarnings("unchecked")
    public static <IN, OUT, CONV> Supplier<ChunkDataConverter<IN, OUT>> getConverter(ClassTriple<IN, OUT, CONV> classes) {
        return (Supplier<ChunkDataConverter<IN, OUT>>) convertersByClass.get(classes);
    }

    @SuppressWarnings("unchecked")
    public static <IN, OUT> BiFunction<Path, Path, LevelInfoConverter<IN, OUT>> getLevelConverter(String inputName, String outputName, String converterName) {
        ClassTriple<IN, OUT, LevelInfoConverter<IN, OUT>> pair = new ClassTriple<>(
                getReaderClass(inputName),
                getWriterClass(outputName),
                getConverterClass(new StringTriple(inputName, outputName, converterName))
        );
        return (BiFunction<Path, Path, LevelInfoConverter<IN, OUT>>) levelConvertersByClass.get(pair);
    }

    @SuppressWarnings("unchecked")
    public static <IN, OUT> BiFunction<Path, Path, LevelInfoConverter<IN, OUT>> getLevelConverterById(String inputId, String outputId, String converterId) {
        ClassTriple<IN, OUT, ChunkDataConverter<IN, OUT>> pair = new ClassTriple<>(
                getReaderClassById(inputId),
                getWriterClassById(outputId),
                getConverterClassById(new StringTriple(inputId, outputId, converterId))
        );
        return (BiFunction<Path, Path, LevelInfoConverter<IN, OUT>>) levelConvertersByClass.get(pair);
    }

    @SuppressWarnings("unchecked")
    public static <IN, OUT, CONV extends LevelInfoConverter<IN, OUT>> BiFunction<Path, Path, CONV> getLevelConverter(ClassTriple<IN, OUT, ? extends ChunkDataConverter<IN, OUT>> classes) {
        return (BiFunction<Path, Path, CONV>) levelConvertersByClass.get(classes);
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> getReaderClass(String name) {
        return (Class<T>) readersByClass.inverse().get(getReader(name));
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> getReaderClassById(String id) {
        return (Class<T>) readersByClass.inverse().get(getReaderById(id));
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> getWriterClass(String name) {
        return (Class<T>) writersByClass.inverse().get(getWriter(name));
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> getWriterClassById(String id) {
        return (Class<T>) writersByClass.inverse().get(getWriterById(id));
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> getConverterClass(StringTriple registryKey) {
        return (Class<T>) convertersByName.get(registryKey);
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> getConverterClassById(StringTriple registryKey) {
        return (Class<T>) convertersById.get(registryKey);
    }

    public static class ClassTriple<X, Y, Z> {

        private final Class<X> in;
        private final Class<Y> out;
        private final Class<Z> converter;

        public ClassTriple(Class<X> in, Class<Y> out, Class<Z> converter) {
            this.in = in;
            this.out = out;
            this.converter = converter;
        }

        public Class<Y> getOut() {
            return out;
        }

        public Class<X> getIn() {
            return in;
        }

        public Class<Z> getConverter() {
            return converter;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClassTriple<?, ?, ?> that = (ClassTriple<?, ?, ?>) o;
            return in.equals(that.in) &&
                    out.equals(that.out) &&
                    converter.equals(that.converter);
        }

        @Override
        public int hashCode() {
            return Objects.hash(in, out, converter);
        }

        @Override
        public String toString() {
            return "ClassTriple{" +
                    "in=" + in +
                    ", out=" + out +
                    ", converter=" + converter +
                    '}';
        }
    }

    private static class StringTriple {
        public final String in;
        public final String out;
        public final String converter;

        public StringTriple(String in, String out, String converter) {
            this.in = in;
            this.out = out;
            this.converter = converter;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StringTriple that = (StringTriple) o;
            return in.equals(that.in) &&
                    out.equals(that.out) &&
                    converter.equals(that.converter);
        }

        @Override
        public int hashCode() {
            return Objects.hash(in, out, converter);
        }

        @Override
        public String toString() {
            return "StringTriple{" +
                    "in='" + in + '\'' +
                    ", out='" + out + '\'' +
                    ", converter='" + converter + '\'' +
                    '}';
        }
    }
}
