package com.nwuww.examplemod;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.arguments.EntityArgument; // 用于解析玩家/实体

import java.util.UUID;

public class TPCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        // 命令处理
        dispatcher.register(
                Commands.literal("tpa")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer source = context.getSource().getPlayerOrException();
                                    ServerPlayer target = EntityArgument.getPlayer(context, "target");

                                    if(ConfigManager.getPlayerSettings(target.getUUID()).isAutoAccept())
                                    {
                                        source.teleportTo(
                                                target.serverLevel(),
                                                target.getX(),
                                                target.getY(),
                                                target.getZ(),
                                                target.getYRot(),
                                                target.getXRot()
                                        );

                                        source.sendSystemMessage(Component.literal("传送请求已被自动接受！"));
                                        target.sendSystemMessage(Component.literal(source.getScoreboardName() + " 已传送到你的位置！"));
                                        return Command.SINGLE_SUCCESS;
                                    }

                                    long remainingCooldown = TpaManager.getRemainingCooldown(source.getUUID());
                                    if (remainingCooldown > 0)
                                    {
                                        source.sendSystemMessage(Component.literal("还需要等待 " + remainingCooldown + " 秒后才能发送另一个传送请求").withColor(0xFF8C00));        ;
                                        return 0;
                                    }

                                    TpaManager.addRequest(source.getUUID(), target.getUUID(), 60);
                                    TpaManager.cooldown(source.getUUID());

                                    target.sendSystemMessage(buildRequestMessage(source.getScoreboardName()));
                                    source.sendSystemMessage(Component.literal("传送请求已发送到" + target.getScoreboardName() + "."));

                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                        .then(Commands.literal("config")
                                .then(Commands.literal("autoAccept")
                                        .then(Commands.argument("value", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    boolean value = com.mojang.brigadier.arguments.BoolArgumentType.getBool(context, "value");

                                                    ConfigManager.getPlayerSettings(player.getUUID()).setAutoAccept(value);
                                                    ConfigManager.save();

                                                    player.sendSystemMessage(Component.literal("自动接受传送请求已设置为: " + value));
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                        )
        );

        dispatcher.register(
                Commands.literal("tpdeny")
                        .executes(context -> {

                            ServerPlayer target = context.getSource().getPlayerOrException();
                            UUID requesterUUID = TpaManager.getRequestTarget(target.getUUID());

                            if (requesterUUID != null)
                            {
                                ServerPlayer requester = context.getSource().getServer().getPlayerList().getPlayer(requesterUUID);
                                if (requester != null)
                                {
                                    requester.sendSystemMessage(Component.literal("传送请求被拒绝！").withColor(0xCD5C5C));
                                    target.sendSystemMessage(Component.literal("你已拒绝传送请求"));
                                }
                                else
                                {
                                    target.sendSystemMessage(Component.literal("请求者已下线").withColor(0xFF8C00));
                                }
                                TpaManager.removeRequest(target.getUUID());
                            }
                            else
                            {
                                target.sendSystemMessage(Component.literal("你没有待处理的传送请求"));
                            }
                            return Command.SINGLE_SUCCESS;
                        })
        );

        dispatcher.register(
                Commands.literal("tpaccept")
                        .executes(context -> {

                            ServerPlayer target = context.getSource().getPlayerOrException();
                            UUID requesterUUID = TpaManager.getRequestTarget(target.getUUID());

                            if (requesterUUID != null)
                            {
                                ServerPlayer requester = context.getSource().getServer().getPlayerList().getPlayer(requesterUUID);
                                if (requester != null)
                                {
                                    requester.teleportTo(
                                            target.serverLevel(),
                                            target.getX(),
                                            target.getY(),
                                            target.getZ(),
                                            target.getYRot(),
                                            target.getXRot()
                                    );

                                    requester.sendSystemMessage(Component.literal("传送请求已被接受！"));
                                    target.sendSystemMessage(Component.literal("你已接受传送请求"));
                                }
                                else
                                {
                                    target.sendSystemMessage(Component.literal("请求者不在线"));
                                }
                                TpaManager.removeRequest(target.getUUID());
                            }
                            else
                            {
                                target.sendSystemMessage(Component.literal("你没有待处理的传送请求"));
                            }
                            return Command.SINGLE_SUCCESS;
                        })
        );
    }

    private static Component buildRequestMessage(String requesterName)
    {

        return Component.literal(requesterName + " 请求传送到你的位置，请输入 ")
                .append(Component.literal(("/tpaccept")))
                .append(Component.literal(" [接受]")
                        .withStyle(style -> style
                                .withColor(0x3CB371)
                                .withBold(true)
                                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                                        net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("点击以接受传送请求").withColor(0x3CB371)))
                                .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                        net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/tpaccept"))))
                .append(Component.literal(" 或 "))
                .append(Component.literal(("/tpdeny")))
                .append(Component.literal(" [拒绝]")
                        .withStyle(style -> style
                                .withColor(0xCD5C5C)
                                .withBold(true)
                                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                                        net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("点击以拒绝传送请求").withColor(0xCD5C5C)))
                                .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                        net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/tpdeny"))));
    }
}
