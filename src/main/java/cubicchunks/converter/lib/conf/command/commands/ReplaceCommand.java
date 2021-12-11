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
package cubicchunks.converter.lib.conf.command.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import cubicchunks.converter.lib.conf.command.EditTaskContext;
import cubicchunks.converter.lib.conf.command.arguments.BoundingBoxArgument;
import cubicchunks.converter.lib.conf.command.arguments.WildcardIntegerArgument;
import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.edittask.ReplaceEditTask;

public class ReplaceCommand {
    public static void register(CommandDispatcher<EditTaskContext> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<EditTaskContext>literal("replace")
            .then(RequiredArgumentBuilder.<EditTaskContext, BoundingBox>argument("box", new BoundingBoxArgument())
                .then(LiteralArgumentBuilder.<EditTaskContext>literal("like")
                    .then(RequiredArgumentBuilder.<EditTaskContext, Integer>argument("inId", IntegerArgumentType.integer(0, 255))
                        .then(RequiredArgumentBuilder.<EditTaskContext, Integer>argument("inMeta", WildcardIntegerArgument.integer(0, 127))
                            .then(LiteralArgumentBuilder.<EditTaskContext>literal("with")
                                .then(RequiredArgumentBuilder.<EditTaskContext, Integer>argument("outId", IntegerArgumentType.integer(0, 255))
                                    .then(RequiredArgumentBuilder.<EditTaskContext, Integer>argument("outMeta", IntegerArgumentType.integer(0, 127))
                                        .executes((info) -> {
                                            Integer inMeta = WildcardIntegerArgument.getInteger(info, "inMeta");
                                            info.getSource().addEditTask(new ReplaceEditTask(
                                                    info.getArgument("box", BoundingBox.class),
                                                    (byte) IntegerArgumentType.getInteger(info, "inId"),
                                                    (byte) (inMeta == null ? -1 : inMeta), // -1 is a sentinel value representing a wildcard
                                                    (byte) IntegerArgumentType.getInteger(info, "outId"),
                                                    (byte) IntegerArgumentType.getInteger(info, "outMeta")
                                            ));
                                            return 1;
                                        })
                                    )
                                )
                            )
                        )
                    )
                )
            )
        );
    }
}
