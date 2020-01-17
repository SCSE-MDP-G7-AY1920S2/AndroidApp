package com.example.mdpandroid.entity;

import java.util.ArrayList;

public class MessageLog {
    private ArrayList<Message> messageLog;

    public MessageLog(){
        messageLog = new ArrayList<>();
    }

    public void addMessage(String type, String message){
        Message objMessage = new Message(type, message);
        messageLog.add(objMessage);
    }

    public String getLog(){
        String log = "";

        for (Message objMessage : messageLog){
            log += "(" + objMessage.getTime() + ") " + objMessage.getRole() + " : " + objMessage.getMessage() + "\n\n";
        }
        return log;
    }
}
