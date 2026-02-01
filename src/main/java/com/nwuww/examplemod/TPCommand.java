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

public class TPCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        // 命令处理
        dispatcher.register(
                Commands.literal("tpa")
                        .requires(source -> source.hasPermission(1))
                        .then(
                                Commands.argument("target", EntityArgument.player()) // 定义一个玩家参数，名称为target
                                        .executes(TPCommand::executeTpa)
                        )
        );
    }

    private static int executeTpa(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        // 获取命令发送者
        ServerPlayer sourcePlayer = context.getSource().getPlayerOrException();

        // 获取目标玩家参数
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "target");

        // 传送源玩家到目标玩家位置
        sourcePlayer.teleportTo(
                targetPlayer.serverLevel(),
                targetPlayer.getX(),
                targetPlayer.getY(),
                targetPlayer.getZ(),
                targetPlayer.getYRot(),
                targetPlayer.getXRot()
        );

        // 发送反馈消息
        sourcePlayer.sendSystemMessage(Component.literal("You've been teleported to " + targetPlayer.getName().getString() + " successfully!"));
        targetPlayer.sendSystemMessage(Component.literal(sourcePlayer.getName().getString() + " has been teleported to you!"));

        return Command.SINGLE_SUCCESS;
    }
    private static int executeTeleport(CommandContext<CommandSourceStack> context)
    {
        // CommandSourceStack 命令发送源
        try
        {
            // 获取发送命令的实体（可能报错）
            // 尝试将发送源转为玩家对象
            ServerPlayer player = context.getSource().getPlayerOrException();

            // 传送玩家
            player.teleportTo(0.0, 100.0, 0.0);

            player.sendSystemMessage(Component.literal("You're now at (0, 0)!"));

            return Command.SINGLE_SUCCESS;
        }
        catch(CommandSyntaxException ex)
        {
            context.getSource().sendFailure(Component.literal("Only player could be tp."));
            return 0;
        }
    }
}
