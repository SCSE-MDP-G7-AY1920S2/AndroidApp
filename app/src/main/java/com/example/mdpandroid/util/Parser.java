package com.example.mdpandroid.util;

import com.example.mdpandroid.entity.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;

public class Parser {
    /**
     * denotes the respective data received
     */
    //Example String:
            //{"robot":[{"x":10,"y":13,"direction":"Right"}],"status":[{"status":"test"}],"map":[{"explored":"0","obstacle":"0","length":1}]}
    private JSONObject payload;
    private int Robot_X;
    private int Robot_Y;
    private String Robot_Dir;
    private String Robot_Status;

    public static String hexMDF = "0x0000000000000000";
    public static String hexExplored = "0x0000000000000000";

    private String exploredMap[][] = new String[Map.COLUMN][Map.ROW];
    private int MDFLength;

    private boolean validPayload = true;


    public Parser(String payload){
        JSONObject tmpPayload = null;

        try {
            tmpPayload = new JSONObject(payload);
            this.payload = tmpPayload;

            setRobot();
            setStatus();
            setMDF();
        } catch(JSONException jsonEx){
            System.out.println("JSON EXCEPTION1");
            this.validPayload = false;
        }
    }

    private void setRobot(){
        if (this.validPayload == false || this.payload == null) return;

        try{
            JSONArray robot = this.payload.getJSONArray("robot");
            JSONObject objRobot = (JSONObject) robot.get(0);

            System.out.println(objRobot.toString());

            this.Robot_X = objRobot.getInt("x");
            this.Robot_Y = objRobot.getInt("y");
            this.Robot_Dir = objRobot.getString("direction").trim().toUpperCase();

            //Debug
            System.out.println("testtest X: " + this.Robot_X);
        } catch(JSONException jsonEx){
            System.out.println("JSON EXCEPTION");
            this.validPayload = false;
        } catch(IndexOutOfBoundsException indexEx){
            System.out.println("INDEX OUT OF BOUNDS EXCEPTION");
            this.validPayload = false;
        } catch(ClassCastException castEx){
            System.out.println("CLASS CAST EXCEPTION");
            this.validPayload = false;
        }
    }

    private void setStatus(){
        if (this.validPayload == false || this.payload == null) return;

        try{
            JSONArray status = this.payload.getJSONArray("status");
            JSONObject objStatus = (JSONObject) status.get(0);

            System.out.println(objStatus.toString());

            this.Robot_Status = objStatus.getString("status");
        } catch(JSONException jsonEx){
            System.out.println("JSON EXCEPTION");
            this.validPayload = false;
        } catch(IndexOutOfBoundsException indexEx){
            System.out.println("INDEX OUT OF BOUNDS EXCEPTION");
            this.validPayload = false;
        } catch(ClassCastException castEx){
            System.out.println("CLASS CAST EXCEPTION");
            this.validPayload = false;
        }
    }

    public void setImage(){
        if (this.validPayload == false || this.payload == null) return;

        try{
            JSONArray image = this.payload.getJSONArray("image");

            for (int i = 0 ; i < image.length(); i++) {
                JSONObject objImage = image.getJSONObject(i);
                String imgID = objImage.getString("imgID");
                int img_x = objImage.getInt("x");
                int img_y = objImage.getInt("y");
                this.exploredMap[img_x][img_y] = imgID;
            }

        }catch(JSONException jsonEx){
            System.out.println("JSON EXCEPTION");
            this.validPayload = false;
        } catch(IndexOutOfBoundsException indexEx){
            System.out.println("INDEX OUT OF BOUNDS EXCEPTION");
            this.validPayload = false;
        } catch(ClassCastException castEx){
            System.out.println("CLASS CAST EXCEPTION");
            this.validPayload = false;
        }
    }

    private void setMDF(){
        if (this.validPayload == false || this.payload == null) return;

        try{
            JSONArray map = this.payload.getJSONArray("map");
            JSONObject objMap = (JSONObject) map.get(0);

            /**
             * explored portion
             */
            String exploredMDF = objMap.getString("explored");
            exploredMDF = new BigInteger(exploredMDF, 16).toString(2);
            exploredMDF = exploredMDF.substring(2, 302);
            hexMDF = new BigInteger(exploredMDF, 2).toString(16);

            String obstacleMDF = objMap.getString("obstacle");
            hexExplored = obstacleMDF;
            int length =  exploredMDF.length() - exploredMDF.replaceAll("1", "").length();
            obstacleMDF = new BigInteger(obstacleMDF, 16).toString(2);


            int padLength = length - obstacleMDF.length();
            while (padLength != 0){
                obstacleMDF = "0" + obstacleMDF;
                padLength--;
            }

            for (int i = 0; i < Map.ROW; i++){
                for (int j = 0; j < Map.COLUMN; j++){
                    int characterIndex = (i * Map.COLUMN) + j;
                    exploredMap[j][i] = String.valueOf(exploredMDF.charAt(characterIndex));
                }
            }

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

        } catch(JSONException jsonEx){
            System.out.println("JSON EXCEPTION");
            this.validPayload = false;
        } catch(IndexOutOfBoundsException indexEx){
            System.out.println("INDEX OUT OF BOUNDS EXCEPTION");
            this.validPayload = false;
        } catch(ClassCastException castEx){
            System.out.println("CLASS CAST EXCEPTION");
            this.validPayload = false;
        }
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
}
