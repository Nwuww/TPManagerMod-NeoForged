package com.nwuww.examplemod;

import java.util.*;

public class PlayerSettings
{
    private boolean autoAccept = false;
    private int timeCancel = 60;
    private Home home;

    public PlayerSettings()
    {
        this.home = new Home();
    }

    public boolean isAutoAccept()
    {
        return autoAccept;
    }
    public void setAutoAccept(boolean autoAccept)
    {
        this.autoAccept = autoAccept;
    }
    public int getTimeCancel()
    {
        return timeCancel;
    }
    public void setTimeCancel(int timeCancel)
    {
        this.timeCancel = timeCancel;
    }

    public void setHome(double x, double y, double z, String dimension)
    {
        home.x = x;
        home.y = y;
        home.z = z;
        home.dimension = dimension;
        home.isSet = true;
    }
    public Home getHome()
    {
        return home;
    }

    @Override
    public String toString()
    {
        String homeInfo = home.isSet ?
                String.format("(%d, %d, %d : %s)", (int)home.x, (int)home.y, (int)home.z, home.dimension) :
                "尚未设置家";
        return "====================" +
                "\n你的当前设置: " +
                "\n自动接受请求 = " + autoAccept +
                "\n超时时间 = " + timeCancel + " seconds" +
                homeInfo +
                "输入 /home view 来查看详情" +
                "\n====================";
    }


}
