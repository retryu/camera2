package com.mogujie.transformer.camera;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.example.retryu.myapplication.R;


public class CameraActivity extends FragmentActivity {

    public static final String TAG = CameraActivity.class.getSimpleName();
    private FrameLayout frameLayout;

    CameraFragment cameraFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.squarecamera__CameraFullScreenTheme);
        super.onCreate(savedInstanceState);

//        if (getActionBar() != null) {
//            getSupportActionBar().hide();
//        }
        setContentView(R.layout.squarecamera__activity_camera);

        Log.d(TAG,"start CameraFragment:");
        if (savedInstanceState == null) {
            cameraFragment = (CameraFragment) CameraFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, CameraFragment.newInstance(), CameraFragment.TAG)
                    .commit();
        }
        Log.d(TAG,"end CameraFragment");

        final ImageView imageview = (ImageView) findViewById(R.id.img_content);
        findViewById(R.id.btn_showImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (imageview.getVisibility() != View.VISIBLE) {
//                    imageview.setImageResource(R.drawable.ic_camera_bg);
//                    imageview.setVisibility(View.VISIBLE);
//                } else {
//                    imageview.setVisibility(View.GONE);
//                }

//                Intent intent = new Intent(CameraActivity.this, MainActivity.class);
//                startActivity(intent);
                cameraFragment.startCamera();
            }
        });

        final  FrameLayout frameLayout = (FrameLayout) findViewById(R.id.fragment_container);
        findViewById(R.id.btn_hide_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(frameLayout.getVisibility() == View.VISIBLE){
                    frameLayout.setVisibility(View.GONE);
                } else {
                    frameLayout.setVisibility(View.VISIBLE);
                }
            }
        });

    }

    public void returnPhotoUri(Uri uri) {
        Intent data = new Intent();
        data.setData(uri);

        if (getParent() == null) {
            setResult(RESULT_OK, data);
        } else {
            getParent().setResult(RESULT_OK, data);
        }

        finish();
    }

    public void onCancel(View view) {
        getSupportFragmentManager().popBackStack();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
