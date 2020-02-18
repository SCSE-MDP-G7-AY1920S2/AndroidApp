package com.example.mdpandroid.util;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Created by Kenneth on 18/2/2020.
 * for com.example.mdpandroid.util in MDP-Android
 */
public class RawImageParser {

    private JSONObject payload;
    private String imgB64;

    private static final String TAG = "ImgParse";
    private Context context;

    private void saveDbgFile(String s, String filename) {
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

    public RawImageParser(String payload, Context context) {
        JSONObject tmpPayload = null;
        this.context = context;
        Log.d(TAG, "JSON String Length: " + payload.length());
        saveDbgFile(payload, "dbg");

        try {
            tmpPayload = new JSONObject(payload);
            this.payload = tmpPayload;

            saveImage();
        } catch (JSONException json) {
            Log.d(TAG, "JSON Exception (" + json.getLocalizedMessage() + ")");
            this.payload = null;
        }
    }

    private void saveImage() {
        if (this.payload == null || context == null) return;

        Log.d(TAG, "Saving images");

        try {
            JSONArray arr = this.payload.getJSONArray("imgRaw");
            for (int i = 0; i < arr.length(); i++) {
                Log.d(TAG, "Processing image #" + i);
                String img = arr.getString(i);
                saveDbgFile(img, "predecocde_" + i);
                byte[] imgBytes = Base64.decode(img, Base64.DEFAULT);
                saveDbgFile(imgBytes.toString(), "postDecode" + i);
                File f = new File(context.getFilesDir().getAbsoluteFile(), "img_" + i + ".jpg");
                f.getParentFile().mkdirs();
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f));
                bos.write(imgBytes);
                bos.flush();
                bos.close();
                Log.d(TAG, "Image saved to " + f.getAbsolutePath());
            }
        } catch (JSONException ex) {
            Log.d(TAG, "JSON Exception saveImage(): (" + ex.getLocalizedMessage() + ")");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
