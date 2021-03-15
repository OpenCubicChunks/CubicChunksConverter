package cubicchunks.converter.lib.conf.command.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import cubicchunks.converter.lib.conf.command.EditTaskContext;
import cubicchunks.converter.lib.conf.command.arguments.BoundingBoxArgument;
import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.edittask.SetEditTask;

public class SetCommand {
    public static void register(CommandDispatcher<EditTaskContext> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<EditTaskContext>literal("set")
            .then(RequiredArgumentBuilder.<EditTaskContext, BoundingBox>argument("box", new BoundingBoxArgument())
                .then(RequiredArgumentBuilder.<EditTaskContext, Integer>argument("id", IntegerArgumentType.integer(0, 255))
                    .then(RequiredArgumentBuilder.<EditTaskContext, Integer>argument("meta", IntegerArgumentType.integer(0, 127))
                        .executes((info) -> {
                            info.getSource().addEditTask(new SetEditTask(
                                info.getArgument("box", BoundingBox.class),
                                (byte)IntegerArgumentType.getInteger(info, "id"),
                                (byte)IntegerArgumentType.getInteger(info, "meta")
                            ));
                            return 1;
                        })
                    )
                )
            )
        );
    }
}
