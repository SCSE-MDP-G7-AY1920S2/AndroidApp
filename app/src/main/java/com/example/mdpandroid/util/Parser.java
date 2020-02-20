package com.example.mdpandroid.util;

import android.util.Log;

import com.example.mdpandroid.entity.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.math.BigInteger;

public class Parser {
    
    private static final String TAG = "Parser";
    private static final boolean DEBUG = false;
    /**
     * denotes the respective data received
     */

    private JSONObject payload;
    private int Robot_X;
    private int Robot_Y;
    private String Robot_Dir;
    private String Robot_Status;
    private String lastImgID;

    public static String hexMDF = "0x0000000000000000";
    public static String hexExplored = "0x0000000000000000";
    public static String hexImage = "";

    private String exploredMap[][] = new String[Map.COLUMN][Map.ROW];
    private int MDFLength;

    private boolean validPayload = true;


    public Parser(String payload){
        JSONObject tmpPayload = null;

        try {
            tmpPayload = new JSONObject(payload);
            this.payload = tmpPayload;

            setRobot();
            setMDF();
        } catch(JSONException jsonEx){
            Log.d(TAG, "JSON EXCEPTION1");
            this.validPayload = false;
        }
    }

    private void setRobot(){
        if (this.validPayload == false || this.payload == null) return;

        try{
            JSONArray robot = this.payload.getJSONArray("pos");

            this.Robot_X = robot.getInt(0);
            this.Robot_Y = robot.getInt(1);
            int angle = robot.getInt(2);

            if (angle==0)
                this.Robot_Dir = "UP";
            else if (angle==90)
                this.Robot_Dir = "RIGHT";
            else if (angle==180)
                this.Robot_Dir = "DOWN";
            else if (angle==270)
                this.Robot_Dir = "LEFT";


        } catch(JSONException jsonEx){
            Log.d(TAG, "JSON EXCEPTION");
            this.validPayload = false;
        } catch(IndexOutOfBoundsException indexEx){
            Log.d(TAG, "INDEX OUT OF BOUNDS EXCEPTION");
            this.validPayload = false;
        } catch(ClassCastException castEx){
            Log.d(TAG, "CLASS CAST EXCEPTION");
            this.validPayload = false;
        }
    }

    public boolean setStatus(){
        //if (this.validPayload == false || this.payload == null) return;

        try{
            this.Robot_Status = this.payload.getString("status");
            return true;

//            JSONArray status = this.payload.getJSONArray("status");
//            JSONObject objStatus = (JSONObject) status.get(0);
//
//            Log.d(TAG, objStatus.toString());
//
//            this.Robot_Status = objStatus.getString("status");
        } catch(Exception e){
            Log.d(TAG, "EXCEPTION");
            return false;
        }
    }

    public void processImage(){
        if (this.validPayload == false || this.payload == null) return;

        try{
            JSONArray images = this.payload.getJSONArray("imgs");
            String imgID = "0";
            JSONArray image = null;
            int img_x = 0;
            int img_y = 0;

            for (int i = 0 ; i < images.length(); i++) {
                image = images.getJSONArray(i);
                img_x = Integer.parseInt(image.getString(0));
                img_y = Integer.parseInt(image.getString(1));
                imgID = image.getString(2);
                hexImage += " (" + imgID + "," + img_x + "," + img_y + "),";
                this.exploredMap[img_x][img_y] = imgID;
            }

            if (!hexImage.isEmpty()) hexImage = hexImage.substring(0, hexImage.length() - 1);
            this.lastImgID = imgID;

        }catch(JSONException jsonEx){
            Log.d(TAG, "JSON EXCEPTION");
            this.validPayload = false;
        } catch(IndexOutOfBoundsException indexEx){
            Log.d(TAG, "INDEX OUT OF BOUNDS EXCEPTION");
            this.validPayload = false;
        } catch(ClassCastException castEx){
            Log.d(TAG, "CLASS CAST EXCEPTION");
            this.validPayload = false;
        }
    }

    private void setMDF(){
        if (this.validPayload == false || this.payload == null) {
            Log.d("MDF", "Invalid Payload");
            return;
        }

        try{
            String exploredMDF = this.payload.getString("expMDF");
            String obstacleMDF = this.payload.getString("objMDF");

            /**
             * explored portion
             */
            hexMDF = exploredMDF;
            exploredMDF = new BigInteger(exploredMDF, 16).toString(2);
            exploredMDF = exploredMDF.substring(2, 302);
            if (DEBUG) Log.d("MDF", "Explored MDF: " + exploredMDF);

            int exploredLength = exploredMDF.replaceAll("0", "").length();
            int obstaclePad = exploredLength % 4;
            if (DEBUG) Log.d("MDF", "Obstacle Padding: " + obstaclePad);

            hexExplored = obstacleMDF;
            obstacleMDF = new BigInteger(obstacleMDF, 16).toString(2);
            int obstacleMdfHexToBinLen = hexExplored.length() * 4;
            obstacleMDF = String.format("%" + obstacleMdfHexToBinLen + "s", obstacleMDF).replace(" ", "0");
            if (DEBUG) Log.d("MDF", "Obstacle MDF: " + obstacleMDF);

            Log.d("MDF", "Parsing Explored String on map");
            for (int i = 0; i < Map.ROW; i++){
                for (int j = 0; j < Map.COLUMN; j++){
                    int characterIndex = (i * Map.COLUMN) + j;
                    exploredMap[j][i] = String.valueOf(exploredMDF.charAt(characterIndex));
                }
            }
            if (DEBUG) printMapDbg();

            Log.d("MDF", "Parsing Obstacle String on map");
            int counter = 0;
            for (int i = 0; i < Map.ROW; i++){
                for (int j = 0; j < Map.COLUMN; j++){
                    if (exploredMap[j][i].equals("1")){
                        if (obstacleMDF.charAt(counter) == '1'){
                            exploredMap[j][i] = "O";
                        }
                        counter++;
                    }
                }
            }
            if (DEBUG) printMapDbg();

        } catch(JSONException jsonEx){
            Log.d(TAG, "JSON EXCEPTION");
            this.validPayload = false;
        } catch(IndexOutOfBoundsException indexEx){
            Log.d(TAG, "INDEX OUT OF BOUNDS EXCEPTION");
            this.validPayload = false;
        } catch(ClassCastException castEx){
            Log.d(TAG, "CLASS CAST EXCEPTION");
            this.validPayload = false;
        }
    }

    private void printMapDbg() {
        Log.d("MDF-Map", "=========================================");
        for (int i = 0; i < Map.ROW; i++){
            String s = "";
            for (int j = 0; j < Map.COLUMN; j++){
                s += exploredMap[j][i];
            }
            Log.d("MDF-Map", s);
        }
        Log.d("MDF-Map", "=========================================");
    }

    public boolean getValidPayload(){
        return this.validPayload;
    }

    public String getStatus(){
        return this.Robot_Status;
    }

    public String[][] getExploredMap(){
        return this.exploredMap;
    }

    public int getRobotX(){
        return this.Robot_X;
    }

    public int getRobotY(){
        return this.Robot_Y;
    }

    public String getRobotDir(){
        return this.Robot_Dir;
    }

    public String getlastImgID(){
        return this.lastImgID;
    }

}
