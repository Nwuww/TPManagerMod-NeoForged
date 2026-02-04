package com.nwuww.examplemod;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
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
                        .executes(context -> {
                            var source = context.getSource().getPlayerOrException();
                            source.sendSystemMessage(Component.literal("用法: /tpa <玩家> 或 /tpa config <设置项>"));
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(TPCommand::TpaToPlayer)
                        )
                        .then(Commands.literal("config")
                                .executes(context ->{
                                    var source = context.getSource().getPlayerOrException();
                                    String configs = ConfigManager.getPlayerSettings(source.getUUID()).toString();
                                    source.sendSystemMessage(Component.literal(configs));
                                    return Command.SINGLE_SUCCESS;
                                })
                                .then(Commands.literal("autoAccept")
                                        .executes(context ->{
                                            var source = context.getSource().getPlayerOrException();
                                            boolean configs = ConfigManager.getPlayerSettings(source.getUUID()).isAutoAccept();
                                            source.sendSystemMessage(Component.literal("当前自动接受传送请求设置为: " + configs));
                                            return Command.SINGLE_SUCCESS;
                                        })
                                        .then(Commands.argument("value", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                                                .executes(TPCommand::configAutoAccept)
                                        )
                                )
                                .then(Commands.literal("timeCancel")
                                        .executes(context ->{
                                            var source = context.getSource().getPlayerOrException();
                                            int configs = ConfigManager.getPlayerSettings(source.getUUID()).getTimeCancel();
                                            source.sendSystemMessage(Component.literal("当前请求超时时间设置为: " + configs + " 秒"));
                                            return Command.SINGLE_SUCCESS;
                                        })
                                        .then(Commands.argument("value", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                                                .executes(TPCommand::configTimeCancel)
                                        )
                                )
                        )
        );
        dispatcher.register(
                Commands.literal("tpdeny")
                        .executes(TPCommand::TpDeny)
        );
        dispatcher.register(
                Commands.literal("tpaccept")
                        .executes(TPCommand::TpAccept)
        );
        dispatcher.register(
                Commands.literal("sethome")
                        .executes(TPCommand::SetHome)
        );
        dispatcher.register(
                Commands.literal("tphome")
                        .executes(TPCommand::TpHome)
        );
    }

    // /tpa <target>
    private static int TpaToPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
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

        TpaManager.addRequest(source.getUUID(), target.getUUID(), ConfigManager.getPlayerSettings(target.getUUID()).getTimeCancel());
        TpaManager.cooldown(source.getUUID());

        target.sendSystemMessage(buildRequestMessage(source.getScoreboardName()));
        source.sendSystemMessage(Component.literal("传送请求已发送到" + target.getScoreboardName() + "."));

        return Command.SINGLE_SUCCESS;
    }

    // /tpa config autoAccept <value>
    private static int configAutoAccept(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean value = com.mojang.brigadier.arguments.BoolArgumentType.getBool(context, "value");

        ConfigManager.getPlayerSettings(player.getUUID()).setAutoAccept(value);
        ConfigManager.save();

        player.sendSystemMessage(Component.literal("自动接受传送请求已设置为: " + value));
        return Command.SINGLE_SUCCESS;
    }

    // /tpa config timeCancel <value>
    private static int configTimeCancel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int value = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "value");

        ConfigManager.getPlayerSettings(player.getUUID()).setTimeCancel(value);
        ConfigManager.save();

        player.sendSystemMessage(Component.literal("请求超时时间已设置为: " + value));
        return Command.SINGLE_SUCCESS;
    }

    // /tpdeny
    private static int TpDeny(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
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
    }

    // /tpaccept
    private static int TpAccept(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
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
    }

    // /sethome
    private static int SetHome(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        ServerPlayer source = context.getSource().getPlayerOrException();
        ConfigManager.getPlayerSettings(source.getUUID()).setHome(
                source.getX(),
                source.getY(),
                source.getZ(),
                source.level().dimension().location().toString()
        );
        ConfigManager.save();
        source.sendSystemMessage(Component.literal("家已设置到" +
                String.format("X: %.1f Y: %.1f Z: %.1f in %s",
                        source.getX(), source.getY(), source.getZ(), source.level().dimension().location().toString()
                )
        ));
        return Command.SINGLE_SUCCESS;
    }

    // /tphome
    private static int TpHome(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        ServerPlayer source = context.getSource().getPlayerOrException();
        PlayerSettings settings = ConfigManager.getPlayerSettings(source.getUUID());
        Home home = settings.getHome();

        if (home.dimension == null || home.dimension.isEmpty())
        {
            source.sendSystemMessage(Component.literal("你还没有设置家！").withColor(0xFF8C00));
            return 0;
        }

        var dimensionKey =
                net.minecraft.resources.ResourceKey.create(
                        Registries.DIMENSION,
                        net.minecraft.resources.ResourceLocation.parse(home.dimension)
                );

        source.teleportTo(
                source.getServer().getLevel(dimensionKey),
                home.x,
                home.y,
                home.z,
                source.getYRot(),
                source.getXRot()
        );

        source.sendSystemMessage(Component.literal("已传送到家！"));
        return Command.SINGLE_SUCCESS;
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
