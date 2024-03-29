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
package cubicchunks.converter.lib.conf.command.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import cubicchunks.converter.lib.conf.command.EditTaskContext;
import cubicchunks.converter.lib.util.edittask.ConfigEditTask;

public class ConfigCommand {
    public static void register(CommandDispatcher<EditTaskContext> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<EditTaskContext>literal("config")
            .then(LiteralArgumentBuilder.<EditTaskContext>literal("relight")
                .then(LiteralArgumentBuilder.<EditTaskContext>literal("src")
                    .then(LiteralArgumentBuilder.<EditTaskContext>literal("off") //src off
                        .executes((info) -> {
                            info.getSource().addEditTask(new ConfigEditTask((config) -> {
                                config.relightSrc(false);
                            }));
                            return 1;
                        })
                    ).then(LiteralArgumentBuilder.<EditTaskContext>literal("on") //src on
                        .executes((info) -> {
                            info.getSource().addEditTask(new ConfigEditTask((config) -> {
                                config.relightSrc(true);
                            }));
                            return 1;
                        })
                    )
                ).then(LiteralArgumentBuilder.<EditTaskContext>literal("dst")
                    .then(LiteralArgumentBuilder.<EditTaskContext>literal("off") //dst off
                        .executes((info) -> {
                            info.getSource().addEditTask(new ConfigEditTask((config) -> {
                                config.relightDst(false);
                            }));
                            return 1;
                        })
                    ).then(LiteralArgumentBuilder.<EditTaskContext>literal("on") //dst on
                        .executes((info) -> {
                            info.getSource().addEditTask(new ConfigEditTask((config) -> {
                                config.relightDst(true);
                            }));
                            return 1;
                        })
                    )
                ).then(LiteralArgumentBuilder.<EditTaskContext>literal("off") //both off
                    .executes((info) -> {
                        info.getSource().addEditTask(new ConfigEditTask((config) -> {
                            config.relightSrc(false);
                            config.relightDst(false);
                        }));
                        return 1;
                    })
                ).then(LiteralArgumentBuilder.<EditTaskContext>literal("on") //both on
                    .executes((info) -> {
                        info.getSource().addEditTask(new ConfigEditTask((config) -> {
                            config.relightSrc(true);
                            config.relightDst(true);
                        }));
                        return 1;
                    })
                )
            )
        );
    }
}
