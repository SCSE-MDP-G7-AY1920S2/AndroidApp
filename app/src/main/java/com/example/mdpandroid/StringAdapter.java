package com.example.mdpandroid;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class StringAdapter extends BaseAdapter {
    /**
     * class variables
     */
    Context context;
    ArrayList<String> stringList;
    LayoutInflater inflater;

    public StringAdapter(Context context, ArrayList<String> stringList){
        this.context = context;
        this.stringList = stringList;
        Collections.reverse(this.stringList);
        this.inflater = LayoutInflater.from(context);
    }

    public StringAdapter(Context context, String[] stringList){
        this.context = context;
        this.stringList = new ArrayList<>(Arrays.asList(stringList));
        Collections.reverse(this.stringList);
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return stringList.size();
    }

    @Override
    public String getItem(int i){
        return stringList.get(i);
    }

    @Override
    public long getItemId(int i){
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        view = inflater.inflate(android.R.layout.simple_list_item_1, null);
        TextView main = view.findViewById(android.R.id.text1);

        main.setText(stringList.get(i));
        return view;
    }
}
