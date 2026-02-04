package com.nwuww.examplemod;

public class PlayerSettings
{
    private boolean autoAccept = false;
    private int timeCancel = 60;
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
}
