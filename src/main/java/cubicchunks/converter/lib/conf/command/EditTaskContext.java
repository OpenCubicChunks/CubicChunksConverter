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
package cubicchunks.converter.lib.conf.command;

import cubicchunks.converter.lib.util.edittask.EditTask;

import java.util.ArrayList;
import java.util.List;

public class EditTaskContext {
    private final EditTaskConfig config = new EditTaskConfig();
    private final List<EditTask> tasks = new ArrayList<>();

    public void addEditTask(EditTask task) {
        this.tasks.add(task);
    }

    public List<EditTask> getTasks() {
        return tasks;
    }

    public EditTaskConfig config() {
        return config;
    }

    public static class EditTaskConfig {
        private boolean shouldRelightSrc = true;
        private boolean shouldRelightDst = true;

        public EditTaskConfig() {}

        public void relightSrc(boolean val) {
            shouldRelightSrc = val;
        }

        public boolean shouldRelightSrc() {
            return shouldRelightSrc;
        }

        public void relightDst(boolean val) {
            shouldRelightDst = val;
        }

        public boolean shouldRelightDst() {
            return shouldRelightDst;
        }
    }
}
