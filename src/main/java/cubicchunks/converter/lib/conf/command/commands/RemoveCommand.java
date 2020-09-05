package cubicchunks.converter.lib.conf.command.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import cubicchunks.converter.lib.conf.command.EditTaskContext;
import cubicchunks.converter.lib.conf.command.arguments.BoundingBoxArgument;
import cubicchunks.converter.lib.util.BoundingBox;
import cubicchunks.converter.lib.util.EditTask;

public class RemoveCommand {
    public static void register(CommandDispatcher<EditTaskContext> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<EditTaskContext>literal("remove")
            .then(RequiredArgumentBuilder.<EditTaskContext, BoundingBox>argument("box", new BoundingBoxArgument())
                .executes((info) -> {
                    info.getSource().addEditTask(new EditTask(
                        info.getArgument("box", BoundingBox.class),
                        null,
                        EditTask.Type.REMOVE
                    ));
                    return 1;
                })
            )
        );
    }
}
