package com.example.mdpandroid.entity;

import java.sql.Timestamp;
import java.util.Date;

public class Message {
    public static final String MESSAGE_RECEIVER = "You Received";
    public static final String MESSAGE_SENDER = "You Sent";

    private String role;
    private String message;
    private Timestamp time;

    public Message(String role, String message){
        long time = (new Date()).getTime();
        Timestamp ts = new Timestamp(time);

        this.time = ts;
        this.role = role;
        this.message = message;
    }

    public Timestamp getTime(){
        return this.time;
    }

    public String getRole(){
        return this.role;
    }

    public String getMessage(){
        return this.message;
    }
}
