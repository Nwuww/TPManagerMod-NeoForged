package com.nwuww.examplemod;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TPCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        // 命令处理
        dispatcher.register(
                Commands.literal("tp00")
                        .requires(source -> source.hasPermission(2))
                        .executes(TPCommand::executeTeleport)
        );
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
