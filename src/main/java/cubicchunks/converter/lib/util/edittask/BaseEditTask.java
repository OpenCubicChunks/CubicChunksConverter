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
package cubicchunks.converter.lib.util.edittask;

import cubicchunks.converter.lib.convert.cc2ccrelocating.CC2CCRelocatingDataConverter;
import cubicchunks.converter.lib.util.BoundingBox;
import net.kyori.nbt.ByteTag;
import net.kyori.nbt.CompoundTag;
import net.kyori.nbt.TagType;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
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

    protected void markCubeForLightUpdates(CompoundTag cubeLevelMap) {
        cubeLevelMap.put("isSurfaceTracked", new ByteTag((byte) 0));
        cubeLevelMap.put("initLightDone", new ByteTag((byte) 1));

        if(!cubeLevelMap.contains("LightingInfo", TagType.COMPOUND))
            return;

        CompoundTag lightingInfo = cubeLevelMap.getCompound("LightingInfo");
        Arrays.fill((lightingInfo.getIntArray("LastHeightMap")), Integer.MAX_VALUE);
        lightingInfo.put("EdgeNeedSkyLightUpdate", new ByteTag((byte) 1));
    }

    public void markCubePopulated(CompoundTag cubeLevelMap) {
        cubeLevelMap.put("populated", new ByteTag((byte) 1));
        cubeLevelMap.put("fullyPopulated", new ByteTag((byte) 1));
    }
}
