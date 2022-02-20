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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import cubicchunks.converter.lib.conf.command.EditTaskContext;
import cubicchunks.converter.lib.conf.command.arguments.Matrix4dArgument;
import cubicchunks.converter.lib.conf.command.arguments.Vector3iArgument;
import cubicchunks.converter.lib.util.Matrix4d;
import cubicchunks.converter.lib.util.Schematic;
import cubicchunks.converter.lib.util.Vector3i;
import cubicchunks.converter.lib.util.edittask.SchematicTask;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SchematicCommand {
    public static void register(CommandDispatcher<EditTaskContext> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<EditTaskContext>literal("schematic")
            .then(RequiredArgumentBuilder.<EditTaskContext, String>argument("path", StringArgumentType.string())
                .then(LiteralArgumentBuilder.<EditTaskContext>literal("inDimension")
                    .then(RequiredArgumentBuilder.<EditTaskContext, String>argument("dimension", StringArgumentType.word())
                        .then(LiteralArgumentBuilder.<EditTaskContext>literal("at")
                            .then(RequiredArgumentBuilder.<EditTaskContext, Vector3i>argument("position", new Vector3iArgument())
                                .executes(SchematicCommand::executeWithPosition)
                                .then(LiteralArgumentBuilder.<EditTaskContext>literal("skipAir")
                                    .executes(SchematicCommand::executeWithPositionSkipAir))
                            )
                        )
                        .then(LiteralArgumentBuilder.<EditTaskContext>literal("transform")
                            .then(RequiredArgumentBuilder.<EditTaskContext, Matrix4d>argument("matrix", new Matrix4dArgument())
                                .executes(SchematicCommand::executeWithTransform)
                                .then(LiteralArgumentBuilder.<EditTaskContext>literal("skipAir")
                                    .executes(SchematicCommand::executeWithTransformSkipAir))
                            )
                        )
                    )
                )
            )
        );
    }

    private static int executeWithPositionSkipAir(CommandContext<EditTaskContext> ctx) {
        return executeWithPosition(ctx, true);
    }

    private static int executeWithTransformSkipAir(CommandContext<EditTaskContext> ctx) {
        return executeWithTransform(ctx, true);
    }

    private static int executeWithTransform(CommandContext<EditTaskContext> info) {
        return executeWithTransform(info, false);
    }

    private static int executeWithPosition(CommandContext<EditTaskContext> info) {
        return executeWithPosition(info, false);
    }

    private static int executeWithTransform(CommandContext<EditTaskContext> info, boolean skipAir) {
        Path path = Paths.get(StringArgumentType.getString(info, "path"));
        Matrix4d transform = info.getArgument("matrix", Matrix4d.class);
        String dimension = StringArgumentType.getString(info, "dimension");
        if (dimension.equals("0")) {
            dimension = "";
        }
        try {
            info.getSource().addEditTask(new SchematicTask(
                Schematic.load(path), transform, skipAir, dimension
            ));
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        return 1;
    }

    // TODO: add dimension specification in a generalized way?
    private static int executeWithPosition(CommandContext<EditTaskContext> info, boolean skipAir) {
        Path path = Paths.get(StringArgumentType.getString(info, "path"));
        Vector3i position = info.getArgument("position", Vector3i.class);
        String dimension = StringArgumentType.getString(info, "dimension");
        if (dimension.equals("0")) {
            dimension = "";
        }
        Matrix4d transform = new Matrix4d().setIdentity().translate(position);
        try {
            info.getSource().addEditTask(new SchematicTask(
                Schematic.load(path), transform, skipAir, dimension
            ));
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        return 1;
    }
}
