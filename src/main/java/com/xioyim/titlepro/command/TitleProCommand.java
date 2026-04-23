package com.xioyim.titlepro.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.xioyim.titlepro.network.NetworkHandler;
import com.xioyim.titlepro.network.TitleProPacket;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;

@Mod.EventBusSubscriber
public class TitleProCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("titlepro")
                .requires(src -> src.hasPermission(2))

                // /titlepro <targets> <args...>
                // Supports: @s @a @p @r player_name — also works inside /execute
                .then(Commands.argument("targets", EntityArgument.players())
                    .then(Commands.argument("args", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String raw = StringArgumentType.getString(ctx, "args");
                            TitleProParser.ParseResult result = TitleProParser.parse(raw);
                            if (!result.valid) {
                                ctx.getSource().sendFailure(
                                    Component.literal("§cTitlePro: " + result.error));
                                return 0;
                            }
                            Collection<ServerPlayer> targets;
                            try {
                                targets = EntityArgument.getPlayers(ctx, "targets");
                            } catch (Exception e) {
                                ctx.getSource().sendFailure(
                                    Component.literal("§cTitlePro: Invalid target selector"));
                                return 0;
                            }
                            for (ServerPlayer p : targets) {
                                NetworkHandler.sendToPlayer(p, new TitleProPacket(result.data));
                            }
                            // Execute optional extra command with the command source
                            if (result.data.extraCmdEnabled
                                    && result.data.extraCmd != null
                                    && !result.data.extraCmd.isBlank()) {
                                String cmd = result.data.extraCmd.trim();
                                if (!cmd.startsWith("/")) cmd = "/" + cmd;
                                try {
                                    ctx.getSource().getServer().getCommands()
                                            .performPrefixedCommand(ctx.getSource(), cmd);
                                } catch (Exception ignored) {}
                            }
                            return targets.size();
                        })
                    )
                )

        );

        // Register /titleprosend <targets> <schemeName>
        TitleProSendCommand.register(event.getDispatcher());
    }
}
