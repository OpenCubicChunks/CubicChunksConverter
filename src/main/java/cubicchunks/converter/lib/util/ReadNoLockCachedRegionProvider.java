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
package cubicchunks.converter.lib.util;

import cubicchunks.regionlib.api.region.IRegion;
import cubicchunks.regionlib.api.region.IRegionProvider;
import cubicchunks.regionlib.api.region.key.IKey;
import cubicchunks.regionlib.api.region.key.RegionKey;
import cubicchunks.regionlib.util.CheckedConsumer;
import cubicchunks.regionlib.util.CheckedFunction;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A region caching provider that uses a shared underlying cache for all instances
 */
public class ReadNoLockCachedRegionProvider<K extends IKey<K>> implements IRegionProvider<K> {

    private final IRegionProvider<K> sourceProvider;

    private final Map<RegionKey, IRegion<?>> regionLocationToRegion = new ConcurrentHashMap<>(512);
    private final int maxCacheSize = 256;

    private boolean closed;

    /**
     * Creates a RegionProvider using the given {@code regionFactory} and {@code maxCacheSize}
     *
     * @param sourceProvider provider used as source of regions
     */
    public ReadNoLockCachedRegionProvider(IRegionProvider<K> sourceProvider) {
        this.sourceProvider = sourceProvider;
    }

    @Override
    public <R> Optional<R> fromExistingRegion(K key, CheckedFunction<? super IRegion<K>, R, IOException> func) throws IOException {
        if (closed) {
            throw new IllegalStateException("Already closed");
        }
        return fromRegion(key, func, false);
    }

    @Override
    public <R> R fromRegion(K key, CheckedFunction<? super IRegion<K>, R, IOException> func) throws IOException {
        if (closed) {
            throw new IllegalStateException("Already closed");
        }
        return fromRegion(key, func, true).get();
    }

    @Override
    public void forRegion(K key, CheckedConsumer<? super IRegion<K>, IOException> cons) throws IOException {
        if (closed) {
            throw new IllegalStateException("Already closed");
        }
        forRegion(key, cons, true);
    }

    @Override
    public void forExistingRegion(K key, CheckedConsumer<? super IRegion<K>, IOException> cons) throws IOException {
        if (closed) {
            throw new IllegalStateException("Already closed");
        }
        forRegion(key, cons, false);
    }

    @SuppressWarnings("unchecked") @Override public IRegion<K> getRegion(K key) throws IOException {
        throw new UnsupportedEncodingException("Not supported for reading");
    }

    @SuppressWarnings("unchecked") @Override public Optional<IRegion<K>> getExistingRegion(K key) throws IOException {
        throw new UnsupportedEncodingException("Not supported for reading");
    }

    @Override public void forAllRegions(CheckedConsumer<? super IRegion<K>, IOException> consumer) throws IOException {
        if (closed) {
            throw new IllegalStateException("Already closed");
        }
        sourceProvider.forAllRegions(consumer);
    }

    @Override public void close() throws IOException {
        synchronized (regionLocationToRegion) {
            if (closed) {
                throw new IllegalStateException("Already closed");
            }
            clearRegions();
            this.sourceProvider.close();
            this.closed = true;
        }
    }

    @SuppressWarnings("unchecked")
    private void forRegion(K location, CheckedConsumer<? super IRegion<K>, IOException> cons, boolean canCreate) throws IOException {
        IRegion<K> region;

        RegionKey regionKey = location.getRegionKey();

        region = (IRegion<K>) regionLocationToRegion.get(regionKey);
        if (region == null) {
            region = sourceProvider.getExistingRegion(location).orElse(null);
            if (region != null) {
                regionLocationToRegion.put(regionKey, region);
            }
            if (region == null && canCreate) {
                throw new UnsupportedEncodingException("Creating regions not supported for reading");
            }
        }

        if (region != null) {
            cons.accept(region);
        }
    }

    @SuppressWarnings("unchecked")
    public <R> Optional<R> fromRegion(K location, CheckedFunction<? super IRegion<K>, R, IOException> func, boolean canCreate) throws IOException {
        IRegion<K> region;
        RegionKey regionKey = location.getRegionKey();

        region = (IRegion<K>) regionLocationToRegion.get(regionKey);
        if (region == null) {
            region = sourceProvider.getExistingRegion(location).orElse(null);
            if (region != null) {
                regionLocationToRegion.put(regionKey, region);
            }
            if (region == null && canCreate) {
                throw new UnsupportedEncodingException("Creating regions not supported for reading");
            }
        }

        if (region != null) {
            return Optional.of(func.apply(region));
        }
        return Optional.empty();
    }

    public synchronized void clearRegions() throws IOException {
        Iterator<IRegion<?>> it = regionLocationToRegion.values().iterator();
        while (it.hasNext()) {
            it.remove();
            it.next().close();
        }
    }
}
