package com.d136.smbsecuritycamera.preferences;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.d136.smbsecuritycamera.R;

public class MyPreferencesActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preferences_container);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.preference_container, new MyPreferencesFragment())
                .commit();
    }
}