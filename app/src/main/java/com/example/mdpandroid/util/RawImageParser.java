package com.example.mdpandroid.util;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by Kenneth on 18/2/2020.
 * for com.example.mdpandroid.util in MDP-Android
 */
public class RawImageParser {

    private String imgB64;

    private static boolean DEBUG = true;

    private static final String TAG = "ImgParse";
    private Context context;

    private int imageCount = 1;
    private int imagePacketCount = -1;
    private HashMap<Integer, String> base64MapData;
    private ArrayList<File> imageFiles;
    private CallbackHandler mHandler;
    private static final String MSG_ACK = "ACK";

    public RawImageParser(Context context, CallbackHandler handler) {
        this.context = context;
        this.mHandler = handler;
        this.imageFiles = new ArrayList<>();
        this.base64MapData = new HashMap<>();
    }

    public void parseString(String s) {
        try {
            JSONObject payload = new JSONObject(s);
            saveDbgFile(s, "payload-" + (new Date().toString()));

            Log.d(TAG, "Processing " + s);
            if (!(payload.has("state") || payload.has("c"))) {
                Log.e(TAG, "Payload no state or packet number, discarding");
                return; // Nothing to do
            }

            switch (payload.getString("state").toUpperCase()) {
                case "S":
                    Log.d(TAG, "Detected Start String, saving image packet count");
                    this.imagePacketCount = payload.getInt("data");
                    this.base64MapData.clear(); // We clear in case we are receiving another image
                    this.mHandler.sendCommand(MSG_ACK);
                    break;
                case "E":
                    Log.d(TAG, "Detected Ennd String, checking integrity");
                    if (base64MapData.size() != imagePacketCount) resetAndResend();
                    else processString();
                    break;
                case "D":
                    this.base64MapData.put(payload.getInt("c"), payload.getString("data"));
                    this.mHandler.sendCommand(MSG_ACK);
                    break;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void processString() {
        Log.d(TAG, "Data integrity confirmed. Concatanating string");
        this.imgB64 = "";
        for (int i = 1; i <= this.imagePacketCount; i++) {
            this.imgB64 += this.base64MapData.get(i);
        }
        this.mHandler.sendCommand(MSG_ACK);
        saveImage();
    }

    private void resetAndResend() {
        Log.d(TAG, "Data Integrity failed. discarding results and re-request for new image data");
        this.imagePacketCount = -1;
        this.imageCount = 1;
        this.base64MapData.clear();
        this.imageFiles.clear();
        this.mHandler.sendCommand(Cmd.REQIMG);
    }

    private void saveDbgFile(String s, String filename) {
        if (!DEBUG) return;
        try {
            File f = new File(context.getFilesDir().getAbsoluteFile(), filename + ".txt");
            if (f.exists()) f.delete();
            f.getParentFile().mkdirs();
            OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(f));
            osw.write(s);
            osw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveImage() {
        if (context == null) return;

        Log.d(TAG, "Saving image #" + this.imageCount + " to file");
        try {
            saveDbgFile(imgB64, "predecode_" + this.imageCount);
            byte[] imgBytes = Base64.decode(this.imgB64, Base64.DEFAULT);
            saveDbgFile(imgBytes.toString(), "postDecode_" + this.imageCount);

            File f = new File(context.getFilesDir().getAbsoluteFile(), "img_" + this.imageCount + ".jpg");
            f.getParentFile().mkdirs();
            if (f.exists()) f.delete();
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f));
            bos.write(imgBytes);
            bos.flush();
            bos.close();
            Log.d(TAG, "Image saved to " + f.getAbsolutePath());
            this.imageCount++;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stitchImages() {
        // TODO: Get all images and stitch them together
    }

    @Nullable
    public File getImageFile(int image) {
        if (this.imageFiles.size() <= image) return null;
        return this.imageFiles.get(image);
    }

    public interface CallbackHandler {
        void sendCommand(String msg);
    }
}
