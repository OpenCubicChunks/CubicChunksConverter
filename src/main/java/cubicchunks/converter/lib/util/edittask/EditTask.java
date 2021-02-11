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
package cubicchunks.converter.lib.util.edittask;

import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.Vector3i;

public class EditTask {
    private final BoundingBox source;
    private final Vector3i offset;

    public enum Type {
        NONE(false, false, false, false),
        CUT(true, true, true, false),
        COPY(false, true, true, false),
        MOVE(true, true, true, false),
        REMOVE(true, false, false, false),
        KEEP(false, false, true, false),
        SET(true, false, true, false),
        REPLACE(true, false, true, false);

        final boolean REQUIRES_SRC_RELOAD;
        final boolean REQUIRED_DST_RELOAD;

        final boolean REQUIRES_SRC_SAVE;
        final boolean REQUIRES_DST_SAVE;

        Type(boolean requires_src_reload, boolean requires_dst_reload, boolean requires_src_save, boolean requires_dst_save) {
            REQUIRES_SRC_RELOAD = requires_src_reload;
            REQUIRED_DST_RELOAD = requires_dst_reload;

            REQUIRES_SRC_SAVE = requires_src_save;
            REQUIRES_DST_SAVE = requires_dst_save;
        }
    }

    private final Type type;

    public EditTask(BoundingBox src, Vector3i offset, Type type) {
        this.source = src;
        this.offset = offset;
        this.type = type;
    }

    public BoundingBox getSourceBox() {
        return this.source;
    }

    public Vector3i getOffset() {
        return this.offset;
    }

    public Type getType() {
        return this.type;
    }
}
