package com.mogujie.transformer.camera;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.example.retryu.myapplication.R;

/**
 * Created by retryu on 15/10/14.
 */
public class FocusView extends RelativeLayout {

    private View mFocus;
    public FocusView(Context context) {
        super(context);
        initView(context);
    }


    private  void initView(Context contenxt){
        mFocus = inflate(contenxt, R.layout.layout_focus_view,null);
        mFocus.setVisibility(View.GONE);
        addView(mFocus);
    }

    /**
     * 更新fouccusview的中心
     * @param x
     * @param y
     */
    public void  updateFouccusCenter(float x, float y ,Rect rect){
//        mFocus.setBackgroundColor(Color.WHITE);
        mFocus.setVisibility(View.VISIBLE);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        lp.width = rect.width();
        lp.height = rect.height();
        lp.leftMargin = (int) (x - rect.width()/2);
        lp.topMargin = (int) (y - rect.height()/2);
        setLayoutParams(lp);
    }

    /**
     * 对焦成功
     */
    public void focusSucessed(){
        mFocus.setVisibility(View.VISIBLE);
//        mFocus.setBackgroundColor(Color.GREEN);
    }

    /**
     * 完成对焦
     */
    public void focusFinish(){
        mFocus.setVisibility(View.GONE);
    }

}
