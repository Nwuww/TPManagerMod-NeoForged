package com.nwuww.examplemod;
class Home
{
    public double x;
    public double y;
    public double z;
    public String dimension;
}
public class PlayerSettings
{
    private boolean autoAccept = false;
    private int timeCancel = 60;
    private final Home home = new Home();
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
    }
    public Home getHome()
    {
        return home;
    }

    @Override
    public String toString()
    {
        return "====================" +
                "\n你的当前设置: " +
                "\n自动接受请求 = " + autoAccept +
                "\n超时时间 = " + timeCancel + " seconds" +
                "\n家位置 (" +
                (int)home.x +
                ", " + (int)home.y +
                ", " + (int)home.z +
                ": " + home.dimension + ")" +
                "\n====================";
    }

}
