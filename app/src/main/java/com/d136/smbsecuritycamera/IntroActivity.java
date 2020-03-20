package com.d136.smbsecuritycamera;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class IntroActivity extends Activity {

    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 2;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 3;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 4;

    Switch switch_camera, switch_record_audio, switch_read_ext_storage, switch_write_ext_storage;
    private boolean camera_ok = false,
            record_audio_ok = false,
            read_ext_storage_ok = false,
            write_ext_storage_ok = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.intro_layout);
        switch_camera = findViewById(R.id.switch_camera);
        switch_record_audio = findViewById(R.id.switch_record_audio);
        switch_read_ext_storage = findViewById(R.id.switch_read_ext_storage);
        switch_write_ext_storage = findViewById(R.id.switch_write_ext_storage);

        final Activity context = this;

        updateUI();

        //CAMERA
        switch_camera.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateUI();
                Toast.makeText(context,"Impossible, go to permission manager to do that.",Toast.LENGTH_LONG).show();
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
                Toast.makeText(context,"Impossible, go to permission manager to do that.",Toast.LENGTH_LONG).show();
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

        //READ EXT STORAGE
        switch_read_ext_storage.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateUI();
                Toast.makeText(context,"Impossible, go to permission manager to do that.",Toast.LENGTH_LONG).show();
            }
        });
        if(!read_ext_storage_ok)
            switch_read_ext_storage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityCompat.requestPermissions( context,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                }
            });

        //WRITE EXT STORAGE
        switch_write_ext_storage.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateUI();
                Toast.makeText(context,"Impossible, go to permission manager to do that.",Toast.LENGTH_LONG).show();
            }
        });
        if(!write_ext_storage_ok)
            switch_write_ext_storage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityCompat.requestPermissions( context,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                }
            });




        /*if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED){
            switch_record_audio.toggle();

        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED ){
            switch_read_ext_storage.toggle();

        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED ){
            switch_write_ext_storage.toggle();

        }*/
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
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:{
                if(grantResults[0] > 0) read_ext_storage_ok = true;
            }
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:{
                if(grantResults[0] > 0) write_ext_storage_ok = true;
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED ){
            read_ext_storage_ok = true;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED ){
            write_ext_storage_ok = true;
        }
        switch_camera.setChecked(camera_ok);
        switch_record_audio.setChecked(record_audio_ok);
        switch_read_ext_storage.setChecked(read_ext_storage_ok);
        switch_write_ext_storage.setChecked(write_ext_storage_ok);
        if(camera_ok&&record_audio_ok&&read_ext_storage_ok&&write_ext_storage_ok)
        {
            new Intent(IntroActivity.this,MainActivity.class);
            this.finish();
        }
    }
}
