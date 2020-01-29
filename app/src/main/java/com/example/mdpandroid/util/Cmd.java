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
    public static final String EXPLORATION_START = "ex";
    public static final String EXPLORATION_STOP = "XStopEx"; // settle this
    public static final String FASTEST_PATH_START = "fp";
    public static final String FASTEST_PATH_STOP = "XStopFast"; // settle this
    public static final String STOP = "T";

    /**
     * Robot movements
     */
    public static final String DIRECTION_LEFT = "a";
    public static final String DIRECTION_RIGHT = "d";
    public static final String DIRECTION_UP = "w";

    /*
    * MAP Status
     */
    public static final String CLEAR = "clr";

    public static String getWayPoint(int x, int y){
        return "XWP" + coordinatesFormatter.format(x) + coordinatesFormatter.format(y);
    }

    public static String getStartPoint(int x, int y){
        return "XWP" + coordinatesFormatter.format(x) + coordinatesFormatter.format(y);
    }
}
