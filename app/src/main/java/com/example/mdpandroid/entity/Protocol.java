package com.example.mdpandroid.entity;


public interface Protocol {
    /**
     * denotes the type of messages
     * used when communicating between two devices
     */
    public static final int MESSAGE_RECEIVE = 0;
    public static final int MESSAGE_ERROR = 1;
    public static final int CONNECTION_ERROR = 2;
}
