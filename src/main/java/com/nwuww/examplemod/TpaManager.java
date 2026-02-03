package com.nwuww.examplemod;

import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class TpaManager
{
    // 储存待处理的传送请求，target UUID -> requester UUID
    private static final Map<UUID, UUID> pendingRequests = new HashMap<>();
    private static final Map<UUID, Long> cooldowns = new HashMap<>();

    // 定时器，用来记录超时
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static void addRequest(UUID requester, UUID target, int timeoutSeconds)
    {
        pendingRequests.put(target, requester);

        scheduler.schedule(() -> {
            if(pendingRequests.remove(target, requester))
            {
                var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                if (server != null)
                {
                    var requesterPlayer = server.getPlayerList().getPlayer(requester);
                    var targetPlayer = server.getPlayerList().getPlayer(target);
                    if (requesterPlayer != null && targetPlayer != null)
                    {
                        var msg = Component.literal("传送请求已过期").withColor(0xFF8C00);
                        requesterPlayer.sendSystemMessage(msg);
                        targetPlayer.sendSystemMessage(msg);
                    }
                }
            }
        }, timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
    }

    public static UUID getRequestTarget(UUID requester)
    {
        return pendingRequests.get(requester);
    }

    public static void removeRequest(UUID requester)
    {
        pendingRequests.remove(requester);
    }

    public static void cooldown(UUID requester)
    {
        cooldowns.put(requester, System.currentTimeMillis());
    }

    public static Long getRemainingCooldown(UUID requester)
    {
        if(!cooldowns.containsKey(requester)) return 0L;

        long timeLeft = (cooldowns.get(requester) + 30000) - System.currentTimeMillis();
        if (timeLeft <= 0)
        {
            cooldowns.remove(requester);
            return 0L;
        }

        return timeLeft / 1000;
    }
}
