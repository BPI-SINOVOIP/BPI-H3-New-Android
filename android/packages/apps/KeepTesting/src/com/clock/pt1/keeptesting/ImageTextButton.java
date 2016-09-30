package com.clock.pt1.keeptesting;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ImageTextButton extends LinearLayout {
    private  ImageView   imageView;
    private  TextView   textView;

    public ImageTextButton(Context context,AttributeSet attrs) {
        super(context,attrs);
        
        int[] attrsArray = new int[] {
                android.R.attr.id, // 0
                android.R.attr.src, // 1
                android.R.attr.text, // 2
                android.R.attr.textColor //3
            };
        TypedArray ta = context.obtainStyledAttributes(attrs, attrsArray);
        int src = ta.getResourceId(1, View.NO_ID);
        int text = ta.getResourceId(2, View.NO_ID);
        int color = ta.getResourceId(3, View.NO_ID);
        
        LayoutInflater inflater=(LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.my_image_view_layout, this);
        imageView = (ImageView) findViewById(R.id.button_image);
        textView = (TextView) findViewById(R.id.button_text);
        setClickable(true);
        setFocusable(true);
        setBackgroundResource(R.layout.my_btn);

        imageView.setImageResource(src);
        
        textView.setText(text);
        textView.setTextColor(color);
    }
}
