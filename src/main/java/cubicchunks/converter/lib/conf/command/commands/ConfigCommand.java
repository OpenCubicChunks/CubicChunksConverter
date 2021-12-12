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
                    .then(LiteralArgumentBuilder.<EditTaskContext>literal("off")
                        .executes((info) -> {
                            info.getSource().addEditTask(new ConfigEditTask((config) -> {
                                config.relightSrc(false);
                            }));
                            return 1;
                        })
                    ).then(LiteralArgumentBuilder.<EditTaskContext>literal("on")
                        .executes((info) -> {
                            info.getSource().addEditTask(new ConfigEditTask((config) -> {
                                config.relightSrc(true);
                            }));
                            return 1;
                        })
                    )
                ).then(LiteralArgumentBuilder.<EditTaskContext>literal("dst")
                    .then(LiteralArgumentBuilder.<EditTaskContext>literal("off")
                        .executes((info) -> {
                            info.getSource().addEditTask(new ConfigEditTask((config) -> {
                                config.relightDst(false);
                            }));
                            return 1;
                        })
                    ).then(LiteralArgumentBuilder.<EditTaskContext>literal("on")
                        .executes((info) -> {
                            info.getSource().addEditTask(new ConfigEditTask((config) -> {
                                config.relightDst(true);
                            }));
                            return 1;
                        })
                    )
                )
            )
        );
    }
}
