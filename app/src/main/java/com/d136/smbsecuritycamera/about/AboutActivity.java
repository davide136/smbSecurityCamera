package com.d136.smbsecuritycamera.about;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.d136.smbsecuritycamera.R;

public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_container);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.about_container, new AboutFragment())
                .commit();
    }
}
