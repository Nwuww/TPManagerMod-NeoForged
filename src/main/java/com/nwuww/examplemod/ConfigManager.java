package com.nwuww.examplemod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConfigManager
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, PlayerSettings> playerConfigs = new HashMap<>();

    private static File getConfigFile()
    {
        Path path = ServerLifecycleHooks.getCurrentServer().getWorldPath(LevelResource.ROOT).resolve("simpletpa_configs.json");
        return path.toFile();
    }

    public static PlayerSettings getPlayerSettings(UUID uuid)
    {
         return playerConfigs.computeIfAbsent(uuid, k -> new PlayerSettings());
    }

    // 保存配置
    public static void save()
    {
        try(Writer writer = new FileWriter(getConfigFile()))
        {
            GSON.toJson(playerConfigs, writer);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    // 加载配置
    public static void load()
    {
        File configFile = getConfigFile();
        if(!configFile.exists()) return;

        try(Reader reader = new FileReader(configFile))
        {
            Map<UUID, PlayerSettings> loadedConfigs = GSON.fromJson(reader, new TypeToken<Map<UUID, PlayerSettings>>(){}.getType());
            if(loadedConfigs != null)
            {
                playerConfigs.clear();
                playerConfigs.putAll(loadedConfigs);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
