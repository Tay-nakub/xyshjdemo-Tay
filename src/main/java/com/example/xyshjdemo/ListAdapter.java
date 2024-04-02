package com.example.xyshjdemo;


import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

class ListAdapter extends BaseAdapter {
    SimpleDateFormat f = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss:SSS]");
    ArrayList<String> texts = new ArrayList<String>();

    Context context;

    public void setContext(Context context){
        this.context = context;
    }
    public void addText(String text){
        texts.add(0,f.format(new Date())+text);
        notifyDataSetChanged();
        if(texts.size()>100)
        {
            for(int i= texts.size() - 1; i>= 100; i--)
            {
                texts.remove(i);
            }
        }
    }
    @Override
    public int getCount() {
        if (texts == null)
            return 0;

        return texts.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return texts.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            TextView textView = new TextView(context);
            textView.setTextColor(Color.BLACK);
            view = textView;
        }

        TextView text = (TextView) view;
        text.setText(getItem(position).toString());
        return text;
    }

}
