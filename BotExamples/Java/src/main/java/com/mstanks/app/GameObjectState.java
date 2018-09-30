package com.mstanks.app;

public class GameObjectState
{
    public String name;
    public String type;
    public float x;
    public float y;
    public float forwardX;
    public float forwardY;
    public float heading;
    public float turretHeading;
    public float turretForwardX;
    public float turretForwardY;

    public int health;
    public int ammo;

    @Override
    public String toString(){
        return String.format("%s[H:%d, A:%d] - %f,%f : %f : %f",name, health, ammo,x, y, heading, turretHeading);
    }
}