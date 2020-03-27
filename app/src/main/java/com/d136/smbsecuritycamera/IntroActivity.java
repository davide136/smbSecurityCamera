package com.d136.smbsecuritycamera;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class IntroActivity extends Activity {

    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 2;

    Switch switch_camera, switch_record_audio;
    private boolean camera_ok = false,
            record_audio_ok = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.intro_layout);
        switch_camera = findViewById(R.id.switch_camera);
        switch_record_audio = findViewById(R.id.switch_record_audio);

        final Activity context = this;

        updateUI();

        //CAMERA
        switch_camera.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateUI();
            }
        });
        if(!camera_ok)
            switch_camera.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityCompat.requestPermissions( context,
                            new String[]{Manifest.permission.CAMERA},
                            MY_PERMISSIONS_REQUEST_CAMERA);
                }
            });

        //RECORD_AUDIO
        switch_record_audio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateUI();
            }
        });
        if(!record_audio_ok)
            switch_record_audio.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityCompat.requestPermissions( context,
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
                }
            });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case MY_PERMISSIONS_REQUEST_CAMERA:{
                if(grantResults[0] > 0) camera_ok = true;
            }
            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO:{
                if(grantResults[0] > 0) record_audio_ok = true;
            }
        }

        updateUI();
    }

    private void updateUI() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED ){
            camera_ok = true;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED ){
            record_audio_ok = true;
        }
        switch_camera.setChecked(camera_ok);
        switch_record_audio.setChecked(record_audio_ok);
        if( camera_ok&&record_audio_ok )
        {
            new Intent(IntroActivity.this,MainActivity.class);
            this.finish();
        }
    }
}
