package cubicchunks.converter.lib.conf.command.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import cubicchunks.converter.lib.conf.command.EditTaskContext;
import cubicchunks.converter.lib.conf.command.arguments.BoundingBoxArgument;
import cubicchunks.converter.lib.conf.command.arguments.Vector3iArgument;
import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.EditTask;
import cubicchunks.converter.lib.util.Vector3i;

public class CopyCommand {
    public static void register(CommandDispatcher<EditTaskContext> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<EditTaskContext>literal("copy")
            .then(RequiredArgumentBuilder.<EditTaskContext, BoundingBox>argument("box", new BoundingBoxArgument())
                .then(LiteralArgumentBuilder.<EditTaskContext>literal("to")
                    .then(RequiredArgumentBuilder.<EditTaskContext, Vector3i>argument("dst", new Vector3iArgument())
                        .executes((info) -> {
                            BoundingBox box = info.getArgument("box", BoundingBox.class);
                            info.getSource().addEditTask(new EditTask(
                                box,
                                info.getArgument("dst", Vector3i.class).sub(box.getMinPos()),
                                EditTask.Type.COPY)
                            );
                            return 1;
                        })
                    )
                ).then(LiteralArgumentBuilder.<EditTaskContext>literal("by")
                    .then(RequiredArgumentBuilder.<EditTaskContext, Vector3i>argument("dst", new Vector3iArgument())
                        .executes((info) -> {
                            info.getSource().addEditTask(new EditTask(
                                info.getArgument("box", BoundingBox.class),
                                info.getArgument("dst", Vector3i.class),
                                EditTask.Type.COPY)
                            );
                            return 1;
                        })
                    )
                )
            )
        );
    }
}
