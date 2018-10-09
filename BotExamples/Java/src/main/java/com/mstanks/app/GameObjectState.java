package com.mstanks.app;

public class GameObjectState
{
    public int id;
    public String name;
    public String type;
    public float x;
    public float y;
    public float heading;
    public float turretHeading;
    public int health;
    public int ammo;


    @Override
    public String toString(){
        return String.format("%s(%d)[H:%d, A:%d] - %f,%f : %f : %f",name, id, health, ammo,x, y, heading, turretHeading);
    }
}