package com.d136.smbsecuritycamera;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MySettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_container);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, new MySettingsFragment())
                .commit();
    }
}