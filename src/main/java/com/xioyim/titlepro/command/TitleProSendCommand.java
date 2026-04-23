package com.xioyim.titlepro.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.xioyim.titlepro.network.NetworkHandler;
import com.xioyim.titlepro.network.TitleProPacket;
import com.xioyim.titlepro.scheme.TitleProScheme;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/**
 * Registers the /titleprosend command.
 * Usage: /titleprosend <targets> <schemeName>
 * Loads a saved scheme file and sends it to the given players.
 */
public class TitleProSendCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("titleprosend")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.players())
                    .then(Commands.argument("schemeName", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            String prefix = builder.getRemaining().toLowerCase();
                            TitleProScheme.listNames().stream()
                                    .filter(n -> n.toLowerCase().startsWith(prefix))
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "schemeName");

                            TitleProScheme.SchemeData scheme;
                            try {
                                scheme = TitleProScheme.load(name);
                            } catch (Exception e) {
                                ctx.getSource().sendFailure(Component.literal(
                                        "§cTitleProSend: Cannot load scheme '" + name + "': " + e.getMessage()));
                                return 0;
                            }

                            Collection<ServerPlayer> targets;
                            try {
                                targets = EntityArgument.getPlayers(ctx, "targets");
                            } catch (Exception e) {
                                ctx.getSource().sendFailure(
                                        Component.literal("§cTitleProSend: Invalid target selector"));
                                return 0;
                            }

                            for (ServerPlayer p : targets) {
                                NetworkHandler.sendToPlayer(p, new TitleProPacket(scheme.data));
                            }

                            // Execute optional extra command with the command source
                            if (scheme.extraCmdEnabled
                                    && scheme.extraCmd != null
                                    && !scheme.extraCmd.isBlank()) {
                                String cmd = scheme.extraCmd.trim();
                                if (!cmd.startsWith("/")) cmd = "/" + cmd;
                                try {
                                    ctx.getSource().getServer().getCommands()
                                            .performPrefixedCommand(ctx.getSource(), cmd);
                                } catch (Exception e) {
                                    ctx.getSource().sendFailure(Component.literal(
                                            "§cTitleProSend: Extra command failed: " + e.getMessage()));
                                }
                            }

                            return targets.size();
                        })
                    )
                )
        );
    }
}
