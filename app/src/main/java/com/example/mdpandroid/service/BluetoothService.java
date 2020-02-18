package com.example.mdpandroid.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import com.example.mdpandroid.entity.Protocol;


public class BluetoothService{
    
    private static final String TAG = "BluetoothSvc";
    /**
     * variables for bluetooth connection
     */
    private static final UUID device_uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String device_name = "MDPGroup4Android";
    private Handler BTHandler;
    private BluetoothConnection connection;
    private BluetoothServer server;
    private BluetoothStream stream;

    public BluetoothService(Handler handler){
        this.BTHandler = handler;
    }

    public synchronized void connectDevice(BluetoothDevice device){
        connection = new BluetoothConnection(device);
        connection.start();
    }

    public synchronized void startServer(BluetoothAdapter bluetoothAdapter){
        server = new BluetoothServer(bluetoothAdapter);
        server.start();
    }

    public synchronized void startStream(BluetoothSocket socket){
        stream = new BluetoothStream(socket);
        stream.start();
    }

    public synchronized void cancel(){
        if (connection != null)
            connection.cancel();
        if (stream != null)
            stream.cancel();
        if (server != null)
            server.cancel();
    }

    public synchronized void write(String data){
        Log.d(TAG, "Sends : " + data);
        if (stream != null)
            stream.write(data.getBytes());
    }

    /**
     * class used for establishing connection between two devices
     * device will act as the Bluetooth client
     */
    private class BluetoothConnection extends Thread{
        /**
         * variables
         */
        private final BluetoothSocket objSocket;
        private final BluetoothDevice objDevice;

        public BluetoothConnection(BluetoothDevice device){
            this.objDevice = device;
            BluetoothSocket tmpSocket = null;

            try {
                tmpSocket = device.createInsecureRfcommSocketToServiceRecord(device_uuid);
            } catch(IOException e){
                Log.d(TAG, "Failed to create socket");
                BTHandler.obtainMessage(Protocol.CONNECTION_ERROR);
            }
            objSocket = tmpSocket;
        }

        public void run(){
            try{
                objSocket.connect();
            } catch (IOException connectEx){
                Log.d(TAG, "Failed to establish connection");
                try{
                    objSocket.close();
                } catch(IOException closeEx){
                    Log.d(TAG, "Failed to close socket");
                    BTHandler.obtainMessage(Protocol.CONNECTION_ERROR);
                }
                return;
            }
            startStream(this.objSocket);
        }

        public void cancel(){
            try{
                objSocket.close();
            } catch(IOException closeEx){
                Log.d(TAG, "Failed to close socket");
            }
        }
    }

    /**
     * class used for establishing connection between two devices
     * device will act as the Bluetooth server
     */
    private class BluetoothServer extends Thread{
        /**
         * variables for bluetooth server
         */
        private final BluetoothServerSocket objServerSocket;
        private BluetoothSocket objSocket;

        public BluetoothServer(BluetoothAdapter bluetoothAdapter){
            BluetoothServerSocket tmpSocket = null;
            try{
                tmpSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(device_name,device_uuid);
            } catch(IOException socketEx){
                Log.d(TAG, "Failed to listen to socket");
            }
            objServerSocket = tmpSocket;
        }

        @Override
        public void run() {
            BluetoothSocket tmpSocket = null;

            while (true){
                try{
                    this.objSocket = objServerSocket.accept();

                    if (objSocket != null){
                        startStream(objSocket);
                        objServerSocket.close();
                        break;
                    }
                } catch(IOException acceptEx){
                    Log.d(TAG, "Failed to accept socket");
                }
            }
        }

        public void cancel(){
            try{
                this.objSocket.close();
            } catch(IOException closeEx){
                Log.d(TAG, "Failed to close socket");
            }
        }
    }

    /**
     * class used to handle bi-directional data transfer
     * used regardless whether it is Bluetooth client or server
     */
    private class BluetoothStream extends Thread{
        /**
         * variables for bluetooth streaming
         */
        private final BluetoothSocket objSocket;
        private final InputStream socketInput;
        private final OutputStream socketOutput;
        private byte[] bufferStream;


        public BluetoothStream(BluetoothSocket socket) {
            this.objSocket = socket;
            InputStream tmpInput = null;
            OutputStream tmpOutput = null;

            try {
                tmpInput = socket.getInputStream();
            } catch (IOException streamEx) {
                Log.d(TAG, "Failed to get input stream");
                BTHandler.obtainMessage(Protocol.CONNECTION_ERROR);
            }

            try {
                tmpOutput = socket.getOutputStream();
            } catch (IOException streamEx) {
                Log.d(TAG, "Failed to get output stream");
                BTHandler.obtainMessage(Protocol.CONNECTION_ERROR);
            }

            this.socketInput = tmpInput;
            this.socketOutput = tmpOutput;
        }

        public void run() {
            //bufferStream = new byte[1024];
            bufferStream = new byte[10248576];
            int byteSize;

            while (true) {
                try {
                    Log.d(TAG, "Reading buffer of size " + bufferStream.length);
                    byteSize = this.socketInput.read(bufferStream);
                    Log.d(TAG, "Total bytes read: " + byteSize);
                    Message message = BTHandler.obtainMessage(Protocol.MESSAGE_RECEIVE, byteSize, -1, bufferStream);
                    message.sendToTarget();
                } catch (IOException readEx) {
                    Log.d(TAG, "Failed to read from stream");
                    BTHandler.obtainMessage(Protocol.CONNECTION_ERROR);

                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                Log.d(TAG, "Writing here");
                this.socketOutput.write(bytes);
                this.socketOutput.flush();
            } catch (IOException writeEx) {
                Log.d(TAG, "Failed to write to receiver");

                Message reportError = BTHandler.obtainMessage(Protocol.MESSAGE_ERROR);

                Bundle bundle = new Bundle();
                bundle.putString("toast", "Failed to send data to other device");
                reportError.setData(bundle);
                BTHandler.sendMessage(reportError);
            }
        }

        public void cancel() {
            try {
                this.objSocket.close();
            } catch (IOException closeEx) {
                Log.d(TAG, "Failed to close stream");
            }
        }
    }

}
