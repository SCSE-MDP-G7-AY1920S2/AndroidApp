package com.example.mdpandroid.entity;

import android.bluetooth.BluetoothDevice;

public class Device {
    /**
     * class variables
     */
    private BluetoothDevice device;
    private String deviceName;
    private String macAddr;

    public Device(BluetoothDevice device, String deviceName, String macAddr){
        this.device = device;
        this.deviceName = (deviceName == null) ? "Unknown Device" : deviceName;
        this.macAddr = macAddr;
    }

    public BluetoothDevice getDevice(){
        return this.device;
    }
    public void setDevice(BluetoothDevice device){
        this.device = device;
    }

    public String getDeviceName(){
        return this.deviceName;
    }
    public void setDeviceName(String deviceName){
        this.deviceName = (deviceName == null) ? "Unknown Device" : deviceName;
    }

    public String getMacAddr(){
        return this.macAddr;
    }
    public void setMacAddr(String macAddr){
        this.macAddr = macAddr;
    }
}
