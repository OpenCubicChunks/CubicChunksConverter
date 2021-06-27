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
package cubicchunks.converter.headless.command.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import cubicchunks.converter.headless.command.HeadlessCommandContext;
import cubicchunks.converter.lib.Registry;

import java.util.stream.StreamSupport;

public class OutFormatCommand {
    public static void register(CommandDispatcher<HeadlessCommandContext> dispatcher) {
        dispatcher.register(StreamSupport.stream(Registry.getWriters().spliterator(), false)
                .map(Registry::getWriterClass)
                .map(Registry::getWriterId)
                .distinct()
                .reduce(LiteralArgumentBuilder.literal("outFormat"),
                        (builder, name) -> builder.then(LiteralArgumentBuilder.<HeadlessCommandContext>literal(name)
                                .executes(context -> {
                                    context.getSource().setOutFormat(name);
                                    return 1;
                                })),
                        (a, b) -> {
                            throw new UnsupportedOperationException();
                        }));
    }
}
