package com.nwuww.TPManager;

import java.util.*;

public class Home
{
    public double x;
    public double y;
    public double z;
    public String dimension;
    public boolean isSet = false; // 是否已设置家位置

    private enum PermissionLevel
    {
        PUBLIC,
        PRIVATE,
        DEFAULT
    }
    private final Set<UUID> permissions = new HashSet<>(); // 权限列表
    private final Map<UUID, String> visitRequests = new HashMap<>(); // 访问请求列表: UUID -> 请求消息
    private PermissionLevel permissionLevel = PermissionLevel.DEFAULT; // 权限等级


    public Set<UUID> getPermissions()
    {
        return permissions;
    }
    public void addPermission(UUID uuid)
    {
        permissions.add(uuid);
    }
    public void removePermission(UUID uuid)
    {
        permissions.remove(uuid);
    }
    public void clearPermissions()
    {
        permissions.clear();
    }

    public Map<UUID, String> getVisitRequests()
    {
        return visitRequests;
    }
    public void addVisitRequest(UUID uuid, String message)
    {
        visitRequests.put(uuid, message);
    }
    public void removeVisitRequest(UUID uuid)
    {
        visitRequests.remove(uuid);
    }
    public void clearVisitRequests()
    {
        visitRequests.clear();
    }

    public String getPermissionLevel()
    {
        return permissionLevel.toString();
    }
    /// @param level 0: PUBLIC, 1: PRIVATE, others: DEFAULT
    public void setPermissionLevel(int level)
    {
        permissionLevel = switch (level)
        {
            case 0 -> PermissionLevel.PUBLIC;
            case 1 -> PermissionLevel.PRIVATE;
            default -> PermissionLevel.DEFAULT;
        };
    }

    @Override
    public String toString()
    {
        return isSet ?
                "家位置 (" +
                (int)x +
                ", " + (int)y +
                ", " + (int)z +
                ": " + dimension + ")" +
                "\n权限等级: " + permissionLevel +
                "\n权限列表: " + permissions.size() + " players" +
                "\n访问请求列表: " + visitRequests.size() + " requests" :
                "尚未设置家";
    }
}