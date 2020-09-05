package cubicchunks.converter.lib.conf.command.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import cubicchunks.converter.lib.conf.command.EditTaskContext;
import cubicchunks.converter.lib.conf.command.arguments.Vector3iArgument;
import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.EditTask;
import cubicchunks.converter.lib.util.Vector3i;

public class CutCommand {
    public static void register(CommandDispatcher<EditTaskContext> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<EditTaskContext>literal("cut")
            .then(RequiredArgumentBuilder.<EditTaskContext, Vector3i>argument("from", new Vector3iArgument())
                .then(RequiredArgumentBuilder.<EditTaskContext, Vector3i>argument("to", new Vector3iArgument())
                    .then(LiteralArgumentBuilder.<EditTaskContext>literal("to")
                        .then(RequiredArgumentBuilder.<EditTaskContext, Vector3i>argument("dst", new Vector3iArgument())
                            .executes((info) -> {
                                Vector3i from = info.getArgument("from", Vector3i.class);
                                info.getSource().addEditTask(
                                    new EditTask(
                                        new BoundingBox(
                                            from,
                                            info.getArgument("to", Vector3i.class)
                                        ),
                                        info.getArgument("dst", Vector3i.class).sub(from),
                                        EditTask.Type.CUT
                                    )
                                );
                                return 1;
                            })
                        )
                    ).then(LiteralArgumentBuilder.<EditTaskContext>literal("by")
                        .then(RequiredArgumentBuilder.<EditTaskContext, Vector3i>argument("dst", new Vector3iArgument())
                            .executes((info) -> {
                                info.getSource().addEditTask(new EditTask(
                                    new BoundingBox(
                                        info.getArgument("from", Vector3i.class),
                                        info.getArgument("to", Vector3i.class)
                                    ),
                                    info.getArgument("dst", Vector3i.class),
                                    EditTask.Type.CUT)
                                );
                                return 1;
                            })
                        )
                    ).executes((info) -> {
                        info.getSource().addEditTask(new EditTask(
                            new BoundingBox(
                                info.getArgument("from", Vector3i.class),
                                info.getArgument("to", Vector3i.class)
                            ),
                            null,
                            EditTask.Type.CUT
                        ));
                        return 1;
                    })
                )
            ).then(LiteralArgumentBuilder.<EditTaskContext>literal("all").executes((info) -> {
                    info.getSource().addEditTask(new EditTask(
                        new BoundingBox(Vector3i.MIN_VECTOR, Vector3i.MAX_VECTOR),
                        null,
                        EditTask.Type.CUT
                    ));
                    return 1;
                }).then(LiteralArgumentBuilder.<EditTaskContext>literal("to")
                    .then(RequiredArgumentBuilder.<EditTaskContext, Vector3i>argument("dst", new Vector3iArgument())
                        .executes((info) -> {
                            Vector3i from = info.getArgument("from", Vector3i.class);
                            info.getSource().addEditTask(new EditTask(
                                new BoundingBox(
                                    from,
                                    info.getArgument("to", Vector3i.class)
                                ),
                                info.getArgument("dst", Vector3i.class).sub(from),
                                EditTask.Type.CUT)
                            );
                            return 1;
                        })
                    )
                ).then(LiteralArgumentBuilder.<EditTaskContext>literal("by")
                    .then(RequiredArgumentBuilder.<EditTaskContext, Vector3i>argument("dst", new Vector3iArgument())
                        .executes((info) -> {
                            info.getSource().addEditTask(new EditTask(
                                new BoundingBox(
                                    info.getArgument("from", Vector3i.class),
                                    info.getArgument("to", Vector3i.class)
                                ),
                                info.getArgument("dst", Vector3i.class),
                                EditTask.Type.CUT)
                            );
                            return 1;
                        })
                    )
                )
            )
        );
    }
}
