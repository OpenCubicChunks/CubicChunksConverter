package cubicchunks.converter.lib.conf.command.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import cubicchunks.converter.lib.conf.command.EditTaskContext;
import cubicchunks.converter.lib.conf.command.arguments.BoundingBoxArgument;
import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.edittask.ReplaceEditTask;

public class ReplaceCommand {
    public static void register(CommandDispatcher<EditTaskContext> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<EditTaskContext>literal("replace")
            .then(RequiredArgumentBuilder.<EditTaskContext, BoundingBox>argument("box", new BoundingBoxArgument())
                .then(RequiredArgumentBuilder.<EditTaskContext, Integer>argument("inId", IntegerArgumentType.integer(0, 255))
                    .then(RequiredArgumentBuilder.<EditTaskContext, Integer>argument("inMeta", IntegerArgumentType.integer(0, 127))
                        .then(RequiredArgumentBuilder.<EditTaskContext, Integer>argument("outId", IntegerArgumentType.integer(0, 255))
                            .then(RequiredArgumentBuilder.<EditTaskContext, Integer>argument("outMeta", IntegerArgumentType.integer(0, 127))
                                .executes((info) -> {
                                    info.getSource().addEditTask(new ReplaceEditTask(
                                        info.getArgument("box", BoundingBox.class),
                                        (byte) IntegerArgumentType.getInteger(info, "inId"),
                                        (byte) IntegerArgumentType.getInteger(info, "inMeta"),
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
        );
    }
}
