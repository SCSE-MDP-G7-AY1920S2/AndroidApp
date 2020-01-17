package com.example.mdpandroid.util;

import java.text.DecimalFormat;

public class Cmd {
    /**
     * Utility Variables
     */
    private static final DecimalFormat coordinatesFormatter = new DecimalFormat("00");

    /**
     * Exploration/Fastest Path
     */
    public static final String EXPLORATION_START = "XE";
    public static final String EXPLORATION_STOP = "XStopEx";
    public static final String FASTEST_PATH_START = "XF";   // settle this
    public static final String FASTEST_PATH_STOP = "XStopFast"; // settle this

    /**
     * Robot movements
     */
    public static final String DIRECTION_LEFT = "Aa";
    public static final String DIRECTION_RIGHT = "Ad";
    public static final String DIRECTION_UP = "Aw";

    public static String getWayPoint(int x, int y){
        return "XWP" + coordinatesFormatter.format(x) + coordinatesFormatter.format(y);
    }

    public static String getStartPoint(int x, int y){
        return "XWP" + coordinatesFormatter.format(x) + coordinatesFormatter.format(y);
    }
}
