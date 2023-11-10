package com.nexgo.apiv3demo;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nexgo.mdbDemo.R;

public class Slider_Adapter extends PagerAdapter {
    Context context;
    LayoutInflater inflater;

    public int[] list_img = {
        R.drawable.oferta1,
    R.drawable.oferta2,
    R.drawable.oferta3,
    R.drawable.oferta5,
    R.drawable.oferta6
    };

    public String[] list_title = {
      "HEADING 1",
            "HEADING 2",
            "HEADING 3",
            "HEADING 4"

    };

    public String[] list_description = {
            "Description 1",
            "Description 2",
            "Description 3",
            "Description 4"
    };

    public int[] list_bg_color = {
            Color.rgb(110,49,89),
            Color.rgb(239,85,85),
            Color.rgb(55,55,55),
            Color.rgb(1,188,212)
    };

    public Slider_Adapter(Context context){
        this.context = context;
    }

    @Override
    public int getCount() {
        return list_title.length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return (view == (LinearLayout) object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        inflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.slide, container, false);
        LinearLayout linearLayout = view.findViewById(R.id.slider_layout);
        ImageView imageView = view.findViewById(R.id.slide_img);
        TextView heading_text = view.findViewById(R.id.slide_heading);
        TextView des_text = view.findViewById(R.id.slider_des);
        linearLayout.setBackgroundColor(list_bg_color[position]);
        imageView.setImageResource(list_img[position]);
        heading_text.setText(list_title[position]);
        des_text.setText(list_description[position]);
        container.addView(view);

        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((LinearLayout) object);
    }
}
