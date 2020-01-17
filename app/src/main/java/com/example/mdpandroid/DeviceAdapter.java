package com.example.mdpandroid;

import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.content.Context;
import android.widget.TextView;
import android.view.View;
import android.view.LayoutInflater;

import java.util.ArrayList;
import com.example.mdpandroid.entity.Device;

public class DeviceAdapter extends BaseAdapter {
    /**
     * class variables
     */
    Context context;
    ArrayList<Device> deviceList;
    LayoutInflater inflater;

    public DeviceAdapter(Context context, ArrayList<Device> deviceList){
        this.context = context;
        this.deviceList = deviceList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return deviceList.size();
    }

    @Override
    public Object getItem(int i){
        return null;
    }

    @Override
    public long getItemId(int i){
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        view = inflater.inflate(R.layout.listview_device, null);
        TextView device = (TextView) view.findViewById(R.id.textView);
        TextView macAddr = (TextView) view.findViewById(R.id.textView2);

        device.setText(deviceList.get(i).getDeviceName());
        macAddr.setText(deviceList.get(i).getMacAddr());
        return view;
    }
}
