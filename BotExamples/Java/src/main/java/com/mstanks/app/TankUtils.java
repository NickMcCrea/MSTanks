package com.mstanks.app;

import java.util.Random;

public class TankUtils {

    private static Random r = new Random();

    public static float getHeading(float x1, float y1, float x2, float y2)
    {
        float heading = (float)Math.atan2(y2 - y1, x2 - x1);
        heading = (float)radianToDegree(heading);
        heading = (heading - 360) % 360;
        return Math.abs(heading);

    }

    public static double radianToDegree(double angle)
    {
        return angle * (180.0 / Math.PI);
    }

    public static boolean isTurnLeft(float currentHeading, float desiredHeading)
    {
        float diff = desiredHeading - currentHeading;
        return diff > 0 ? diff > 180 : diff >= -180;
    }

    public static float calculateDistance(float ownX, float ownY, float otherX, float otherY)
    {
        float headingX = otherX - ownX;
        float headingY = otherY - ownY;
        return (float)Math.sqrt((headingX * headingX) + (headingY * headingY));
    }

    public static int getNext(int lower, int upper){
        return r.nextInt(upper-lower) + lower;
    }


}
