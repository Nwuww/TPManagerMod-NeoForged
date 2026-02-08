package com.nwuww.TPManager;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class TPCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        // 命令处理
        // =========================================TPA COMMANDS=========================================
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
                                        .then(Commands.argument("value", BoolArgumentType.bool())
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
                                        .then(Commands.argument("value", IntegerArgumentType.integer())
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


        // =========================================HOME COMMANDS=========================================
        dispatcher.register(
                Commands.literal("sethome")
                        .executes(TPCommand::SetHome)
        );
        dispatcher.register(
                Commands.literal("tphome")
                        .executes(TPCommand::TpHome)
                        .then(Commands.argument("target", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    var player = context.getSource().getPlayerOrException();
                                    var onlinePlayers = context.getSource().getServer().getPlayerList().getPlayers();
                                    for (var onlinePlayer : onlinePlayers)
                                    {
                                        if (onlinePlayer.getUUID().equals(player.getUUID()))
                                            continue;
                                        var targetSettings = ConfigManager.getPlayerSettings(onlinePlayer.getUUID());
                                        var home = targetSettings.getHome();
                                        if (home.dimension != null && !home.dimension.isEmpty() &&
                                                home.getPermissions().contains(player.getUUID()) &&
                                                !home.getPermissionLevel().equals("PRIVATE"))
                                        {
                                            builder.suggest(onlinePlayer.getScoreboardName());
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(TPCommand::TpTargetHome)
                        )
        );
        dispatcher.register(
                Commands.literal("home")
                        .executes(TPCommand::HomeInfo)
                        .then(Commands.literal("visit")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                                .executes(context -> {
                                                    return HomeVisit(context, StringArgumentType.getString(context, "message"));
                                                })
                                        )
                                        .executes(context -> {
                                            return HomeVisit(context, "请求获取访问权限");
                                        })
                                )
                                .then(Commands.literal("view")
                                        .executes(TPCommand::HomeVisitView)
                                )
                        )
                        .then(Commands.literal("accept")
                                .then(Commands.argument("target", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            var player = context.getSource().getPlayerOrException();
                                            var requests = ConfigManager.getPlayerSettings(player.getUUID()).getHome().getVisitRequests();
                                            for (var uuid : requests.keySet())
                                            {
                                                var name = getNameByUUID(uuid);
                                                if (name != null)
                                                {
                                                    builder.suggest(name);
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(TPCommand::HomeVisitAccept)
                                )
                        )
                        .then(Commands.literal("deny")
                                .then(Commands.argument("target", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            var player = context.getSource().getPlayerOrException();
                                            var requests = ConfigManager.getPlayerSettings(player.getUUID()).getHome().getVisitRequests();
                                            for (var uuid : requests.keySet())
                                            {
                                                var name = getNameByUUID(uuid);
                                                if (name != null)
                                                {
                                                    builder.suggest(name);
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(TPCommand::HomeVisitDeny)
                                )
                        )
                        .then(Commands.literal("remove")
                                .then(Commands.argument("target", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            var player = context.getSource().getPlayerOrException();
                                            var permissions = ConfigManager.getPlayerSettings(player.getUUID()).getHome().getPermissions();
                                            for (var uuid : permissions)
                                            {
                                                var name = getNameByUUID(uuid);
                                                if (name != null)
                                                {
                                                    builder.suggest(name);
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(TPCommand::HomeVisitRemove)
                                )
                        )
                        .then(Commands.literal("clear")
                            .executes(context -> {
                                var player = context.getSource().getPlayerOrException();
                                var home = ConfigManager.getPlayerSettings(player.getUUID()).getHome();
                                home.clearPermissions();
                                home.clearVisitRequests();
                                ConfigManager.save();
                                player.sendSystemMessage(Component.literal("已清除所有访问权限和访问请求").withColor(0xFF8C00));
                                return Command.SINGLE_SUCCESS;
                            })
                        )
                        .then(Commands.literal("config")
                                .then(Commands.literal("permissionLevel")
                                        .then(Commands.argument("value", StringArgumentType.string())
                                                .suggests((context, builder) -> {
                                                    builder.suggest("PUBLIC");
                                                    builder.suggest("PRIVATE");
                                                    builder.suggest("DEFAULT");
                                                    return builder.buildFuture();
                                                })
                                                .executes(TPCommand::HomeConfigPermissionLevel)
                                        )
                                )
                        )
        );
    }

    // ========================TPA COMMANDS IMPLEMENTATION=========================================
    /// /tpa <target>
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

        // target.sendSystemMessage(buildRequestMessage(source.getScoreboardName()));
        target.sendSystemMessage(buildHyperRequestMessage(
                "传送到你的位置",
                source.getScoreboardName(),
                "/tpaccept",
                "/tpdeny",
                "传送"
        ));
        source.sendSystemMessage(Component.literal("传送请求已发送到" + target.getScoreboardName() + "."));

        return Command.SINGLE_SUCCESS;
    }

    /// /tpa config autoAccept <value>
    private static int configAutoAccept(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean value = BoolArgumentType.getBool(context, "value");

        ConfigManager.getPlayerSettings(player.getUUID()).setAutoAccept(value);
        ConfigManager.save();

        player.sendSystemMessage(Component.literal("自动接受传送请求已设置为: " + value));
        return Command.SINGLE_SUCCESS;
    }

    /// /tpa config timeCancel <value>
    private static int configTimeCancel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int value = IntegerArgumentType.getInteger(context, "value");

        ConfigManager.getPlayerSettings(player.getUUID()).setTimeCancel(value);
        ConfigManager.save();

        player.sendSystemMessage(Component.literal("请求超时时间已设置为: " + value));
        return Command.SINGLE_SUCCESS;
    }

    /// /tpdeny
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

    /// /tpaccept
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


    // ========================HOME COMMANDS IMPLEMENTATION=========================================
    /// /sethome
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

    /// /tphome
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
                ResourceKey.create(
                        Registries.DIMENSION,
                        ResourceLocation.parse(home.dimension)
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

    /// /home visit <target> \[message]
    private static int HomeVisit(CommandContext<CommandSourceStack> context, String message) throws CommandSyntaxException
    {
        var source = context.getSource().getPlayerOrException();
        var target = EntityArgument.getPlayer(context, "target");

        if (ConfigManager.getPlayerSettings(target.getUUID()).getHome().getVisitRequests().containsKey(source.getUUID()))
        {
            source.sendSystemMessage(Component.literal("你已经向 " + target.getScoreboardName() + " 的家发送过访问请求了！").withColor(0xFF8C00));
            return 0;
        }

        if (ConfigManager.getPlayerSettings(target.getUUID()).getHome().getPermissions().contains(source.getUUID()) ||
                ConfigManager.getPlayerSettings(target.getUUID()).getHome().getPermissionLevel().equals("PUBLIC"))
        {
            source.sendSystemMessage(Component.literal("你已经有权限访问 " + target.getScoreboardName() + " 的家了！").withColor(0xFF8C00));
            return 0;
        }

        // 向target的家发送来自source的访问请求
        ConfigManager.getPlayerSettings(target.getUUID()).getHome().addVisitRequest(source.getUUID(), message);
        ConfigManager.save();
        target.sendSystemMessage(buildHyperRequestMessage(
                "访问你的家",
                source.getScoreboardName(),
                "/home accept " + source.getScoreboardName(),
                "/home deny " + source.getScoreboardName(),
                "来访"
        ));
        source.sendSystemMessage(Component.literal("已向 " + target.getScoreboardName() + " 的家发送访问请求"));
        return Command.SINGLE_SUCCESS;
    }

    /// /home visit view
    private static int HomeVisitView(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        var source = context.getSource().getPlayerOrException();
        var home = ConfigManager.getPlayerSettings(source.getUUID()).getHome();
        if(home.dimension == null || home.dimension.isEmpty())
        {
            source.sendSystemMessage(Component.literal("你还没有设置家！").withColor(0xFF8C00));
            return 1;
        }
        if(home.getVisitRequests().isEmpty())
        {
            source.sendSystemMessage(Component.literal("你没有访问请求！").withColor(0xFF8C00));
            return 1;
        }
        source.sendSystemMessage(Component.literal("收到的访问请求: "));
        for (var entry : home.getVisitRequests().entrySet())
        {
            var requesterUUID = entry.getKey();
            var requesterName = getNameByUUID(entry.getKey());
            var message = entry.getValue();
            source.sendSystemMessage(Component.literal("- " + requesterName + ": \"" + message + "\"")
                    .withStyle(style -> style
                            .withHoverEvent(new HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    Component.literal("点击接受访问请求").withColor(0x3CB371)))
                            .withClickEvent(new ClickEvent(
                                    ClickEvent.Action.RUN_COMMAND, "/home accept " + requesterName)))
                    .append(Component.literal(" [接受]")
                            .withStyle(style -> style
                                    .withColor(0x3CB371)
                                    .withBold(true)
                                    .withHoverEvent(new HoverEvent(
                                            HoverEvent.Action.SHOW_TEXT,
                                            Component.literal("点击接受访问请求").withColor(0x3CB371)))
                                    .withClickEvent(new ClickEvent(
                                            ClickEvent.Action.RUN_COMMAND, "/home accept " + requesterName))))
                    .append(Component.literal(" 或 "))
                    .append(Component.literal(" [拒绝]")
                            .withStyle(style -> style
                                    .withColor(0xCD5C5C)
                                    .withBold(true)
                                    .withHoverEvent(new HoverEvent(
                                            HoverEvent.Action.SHOW_TEXT,
                                            Component.literal("点击拒绝访问请求").withColor(0xCD5C5C)))
                                    .withClickEvent(new ClickEvent(
                                            ClickEvent.Action.RUN_COMMAND, "/home deny " + requesterName))))
            );
        }
        source.sendSystemMessage(Component.literal(home.toString()));
        return Command.SINGLE_SUCCESS;
    }

    /// /home accept <target>
    private static int HomeVisitAccept(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        String targetName = StringArgumentType.getString(context, "target");
        var source = context.getSource().getPlayerOrException();
        var home = ConfigManager.getPlayerSettings(source.getUUID()).getHome();

        UUID targetUUID = getUUIDByName(targetName, home.getVisitRequests());
        if(targetUUID == null)
        {
            source.sendSystemMessage(Component.literal("没有找到名为 " + targetName + "的玩家，或该玩家没有发送请求").withColor(0xFF8C00));
            return 0;
        }
        if (home.getPermissionLevel().equals("PRIVATE"))
        {
            source.sendSystemMessage(Component.literal("你的家目前是 PRIVATE状态，无法接受访问请求").withColor(0xFF8C00));
            return 0;
        }
        if (home.getPermissions().contains(targetUUID))
        {
            source.sendSystemMessage(Component.literal(targetName + " 已经有访问权限了！").withColor(0xFF8C00));
            return 0;
        }

        // 遍历请求列表，找到对应的请求并接受
        if (home.getVisitRequests().containsKey(targetUUID))
        {
            home.removeVisitRequest(targetUUID);
            home.addPermission(targetUUID);
            ConfigManager.save();
            source.sendSystemMessage(Component.literal("你已接受 " + targetName + " 的访问请求").withColor(0x3CB371));
            var server = context.getSource().getServer();
            var targetPlayer = server.getPlayerList().getPlayer(targetUUID);
            if (targetPlayer != null)
                targetPlayer.sendSystemMessage(Component.literal(source.getScoreboardName() + " 已接受了你的访问请求！").withColor(0x3CB371));
        }
        else
            source.sendSystemMessage(Component.literal("没有找到来自 " + targetName + " 的访问请求").withColor(0xFF8C00));
        return Command.SINGLE_SUCCESS;
    }

    /// /home deny <target>
    private static int HomeVisitDeny(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        String targetName = StringArgumentType.getString(context, "target");
        var source = context.getSource().getPlayerOrException();
        var home = ConfigManager.getPlayerSettings(source.getUUID()).getHome();

        UUID targetUUID = getUUIDByName(targetName, home.getVisitRequests());
        if(targetUUID == null)
        {
            source.sendSystemMessage(Component.literal("没有找到名为 " + targetName + "的玩家，或该玩家没有发送请求").withColor(0xFF8C00));
            return 0;
        }
        if(home.getPermissions().contains(targetUUID))
        {
            source.sendSystemMessage(
                    Component.literal(targetName + " 已经有访问权限了! ").withColor(0xFF8C00)
                            .append(Component.literal("可以通过 /home remove " + targetName))
                            .append(Component.literal(" [移除访问权限] ").withColor(0xCD5C5C))
                            .withStyle(style -> style
                                    .withHoverEvent(new HoverEvent(
                                            HoverEvent.Action.SHOW_TEXT,
                                            Component.literal("点击移除访问权限").withColor(0xCD5C5C))
                                    )
                                    .withClickEvent(new ClickEvent(
                                            ClickEvent.Action.RUN_COMMAND, "/home remove " + targetName)
                                    )
                            )
            );
            return 0;
        }

        home.removeVisitRequest(targetUUID);
        ConfigManager.save();
        source.sendSystemMessage(Component.literal("你已拒绝 " + targetName + " 的访问请求").withColor(0xCD5C5C));
        var server = context.getSource().getServer();
        var targetPlayer = server.getPlayerList().getPlayer(targetUUID);
        if (targetPlayer != null)
            targetPlayer.sendSystemMessage(Component.literal(source.getScoreboardName() + " 拒绝了你的访问请求！").withColor(0xCD5C5C));

        return Command.SINGLE_SUCCESS;
    }

    /// /home remove <target>
    private static int HomeVisitRemove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        String targetName = StringArgumentType.getString(context, "target");
        var source = context.getSource().getPlayerOrException();
        var home = ConfigManager.getPlayerSettings(source.getUUID()).getHome();

        UUID targetUUID = getUUIDByName(targetName, home.getPermissions());
        if(targetUUID == null)
        {
            source.sendSystemMessage(Component.literal("没有找到名为 " + targetName + "的玩家").withColor(0xFF8C00));
            return 0;
        }

        // 遍历权限列表，找到对应的玩家并移除权限
        if (home.getPermissions().contains(targetUUID))
        {
            home.removePermission(targetUUID);
            ConfigManager.save();
            source.sendSystemMessage(Component.literal("已移除 " + targetName + " 的访问权限").withColor(0xFF8C00));
            var server = context.getSource().getServer();
            var targetPlayer = server.getPlayerList().getPlayer(targetUUID);
            if (targetPlayer != null)
                targetPlayer.sendSystemMessage(Component.literal(source.getScoreboardName() + " 移除了你的访问权限！").withColor(0xFF8C00));
        }
        else
        {
            source.sendSystemMessage(Component.literal(targetName + " 本来就没有访问权限").withColor(0xFF8C00));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    /// /home config permissionLevel <value>
    private static int HomeConfigPermissionLevel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        var player = context.getSource().getPlayerOrException();
        var home = ConfigManager.getPlayerSettings(player.getUUID()).getHome();
        String value = StringArgumentType.getString(context, "value");
        int level = switch (value.toUpperCase())
        {
            case "PUBLIC" -> 0;
            case "PRIVATE" -> 1;
            default -> 2;
        };
        home.setPermissionLevel(level);
        ConfigManager.save();
        player.sendSystemMessage(Component.literal("权限等级已设置为: " + home.getPermissionLevel()));
        return Command.SINGLE_SUCCESS;
    }

    /// /home 显示home信息与permissions列表
    private static int HomeInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        var source = context.getSource().getPlayerOrException();
        var home = ConfigManager.getPlayerSettings(source.getUUID()).getHome();

        if (home.dimension == null || home.dimension.isEmpty())
        {
            source.sendSystemMessage(Component.literal("你还没有设置家！").withColor(0xFF8C00));
            return 1;
        }

        source.sendSystemMessage(Component.literal("家的位置: " +
                String.format("X: %.1f Y: %.1f Z: %.1f in %s",
                        home.x, home.y, home.z, home.dimension
                )
        ));
        source.sendSystemMessage(Component.literal("有访问权限的玩家:"));
        for (var uuid : home.getPermissions())
        {
            var name = getNameByUUID(uuid);
            source.sendSystemMessage(Component.literal("- " + name + " (" + uuid + ")"));
        }
        return Command.SINGLE_SUCCESS;
    }

    /// /tphome <target>
    /// target是玩家名字，前提是target的家允许source访问，且target的家权限等级不为PRIVATE
    /// 权限等级为PUBLIC: 任何人都可以访问；PRIVATE: 仅自己可访问；DEFAULT: 只有被允许的玩家可以访问
    /// target可以是在线玩家，也可以是离线玩家（只要之前设置过家并且source有权限访问）
    /// 若target离线则通过缓存记录的玩家名字和UUID进行匹配
    private static int TpTargetHome(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        ServerPlayer visitor = context.getSource().getPlayerOrException();
        String targetName = StringArgumentType.getString(context, "target");
        MinecraftServer server = visitor.server;

        UUID targetUUID = getUUIDInCacheFromName(server, targetName);
        if (targetUUID == null)
        {
            visitor.sendSystemMessage(Component.literal("未找到玩家: " + targetName).withColor(0xFF5555));
            return 0;
        }

        PlayerSettings targetSettings = ConfigManager.getPlayerSettings(targetUUID);
        Home targetHome = targetSettings.getHome();

        if (!targetHome.isSet)
        {
            visitor.sendSystemMessage(Component.literal(targetName + " 还没有设置家"));
            return 0;
        }

        String level = targetHome.getPermissionLevel();
        boolean allowed = level.equalsIgnoreCase("PUBLIC") ||
                (level.equalsIgnoreCase("DEFAULT") && targetHome.getPermissions().contains(visitor.getUUID()));
        if (allowed)
        {
            ResourceLocation dimLoc = ResourceLocation.parse(targetHome.dimension);
            ServerLevel targetLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimLoc));

            if (targetLevel != null)
            {
                visitor.teleportTo(targetLevel, targetHome.x, targetHome.y, targetHome.z, visitor.getYRot(), visitor.getXRot());
                visitor.sendSystemMessage(Component.literal("已传送到 " + targetName + " 的家~"));
                return 1;
            }
        }
        else
            visitor.sendSystemMessage(Component.literal("你没有权限拜访 " + targetName + " 的家!").withColor(0xFF5555));
        return 0;
    }

    /// @param requestContent 请求内容描述
    /// @param requesterName 请求者名称
    /// @param acceptCommand 接受命令
    /// @param denyCommand 拒绝命令
    /// @param HoverText 悬停文本描述
    /// @return 构建好的请求消息组件
    private static Component buildHyperRequestMessage(String requestContent, String requesterName, String acceptCommand, String denyCommand, String HoverText)
    {
        return Component.literal(requesterName + " 请求" + requestContent + ", 请输入 ")
                .append(Component.literal((acceptCommand)))
                .append(Component.literal(" [接受]")
                        .withStyle(style -> style
                                .withColor(0x3CB371)
                                .withBold(true)
                                .withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("点击以接受" + HoverText + "请求").withColor(0x3CB371)))
                                .withClickEvent(new ClickEvent(
                                        ClickEvent.Action.RUN_COMMAND, acceptCommand))))
                .append(Component.literal(" 或 "))
                .append(Component.literal((denyCommand)))
                .append(Component.literal(" [拒绝]")
                        .withStyle(style -> style
                                .withColor(0xCD5C5C)
                                .withBold(true)
                                .withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("点击以拒绝" + HoverText + "请求").withColor(0xCD5C5C)))
                                .withClickEvent(new ClickEvent(
                                        ClickEvent.Action.RUN_COMMAND, denyCommand))));
    }

    private static String getNameByUUID(UUID uuid)
    {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null)
        {
            var player = server.getPlayerList().getPlayer(uuid);
            if (player != null) return player.getScoreboardName();
            return server.getProfileCache().get(uuid).map(GameProfile::getName).orElse("未知玩家");
        }
        return "未知玩家";
    }
    private static UUID getUUIDByName(String name, Map<UUID, String> requests)
    {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null)
        {
            for (var uuid : requests.keySet())
            {
                if(getNameByUUID(uuid).equalsIgnoreCase(name))
                {
                    return uuid;
                }
            }
        }
        return null;
    }
    private static UUID getUUIDByName(String name, Set<UUID> permissions)
    {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null)
        {
            for (var uuid : permissions)
            {
                if(getNameByUUID(uuid).equalsIgnoreCase(name))
                {
                    return uuid;
                }
            }
        }
        return null;
    }
    public static UUID getUUIDInCacheFromName(MinecraftServer server, String username)
    {
        ServerPlayer onlinePlayer = server.getPlayerList().getPlayerByName(username);
        if (onlinePlayer != null) return onlinePlayer.getUUID();

        return server.getProfileCache()
                .get(username)
                .map(GameProfile::getId)
                .orElse(null);
    }

    private static Component buildRequestMessage(String requesterName)
    {
        return Component.literal(requesterName + " 请求传送到你的位置，请输入 ")
                .append(Component.literal(("/tpaccept")))
                .append(Component.literal(" [接受]")
                        .withStyle(style -> style
                                .withColor(0x3CB371)
                                .withBold(true)
                                .withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("点击以接受传送请求").withColor(0x3CB371)))
                                .withClickEvent(new ClickEvent(
                                        ClickEvent.Action.RUN_COMMAND, "/tpaccept"))))
                .append(Component.literal(" 或 "))
                .append(Component.literal(("/tpdeny")))
                .append(Component.literal(" [拒绝]")
                        .withStyle(style -> style
                                .withColor(0xCD5C5C)
                                .withBold(true)
                                .withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("点击以拒绝传送请求").withColor(0xCD5C5C)))
                                .withClickEvent(new ClickEvent(
                                        ClickEvent.Action.RUN_COMMAND, "/tpdeny"))));
    }
}
