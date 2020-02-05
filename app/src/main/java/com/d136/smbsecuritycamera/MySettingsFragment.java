package com.d136.smbsecuritycamera;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class MySettingsFragment extends PreferenceFragmentCompat {
    private static String TAG = "PreferenceFragment";
    Preference portPref, customPortPref, userPref, passwordPref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        portPref = findPreference("port");
        assert portPref != null;
        portPref.setSummary(portPref.getSharedPreferences().getString("port","445"));
        portPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                try{
                    int check = Integer.valueOf(newValue.toString());
                }catch(Exception e){return false;}
                preference.setSummary(newValue+"");
                return true;
            }
        });

        customPortPref = findPreference("portEnabled");
        assert customPortPref != null;
        customPortPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue.equals(false)){
                    portPref.getSharedPreferences().edit().putString("port","445").apply();
                    portPref.callChangeListener("445");
                    portPref.setEnabled(false);
                }
                else portPref.setEnabled(true);
                    return true;
        }});

        userPref = findPreference("user");
        assert userPref != null;
        userPref.setSummary(userPref.getSharedPreferences().getString("user",null));
        userPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(newValue+"");
                return true;
            }
        });

        passwordPref = findPreference("password");
        assert passwordPref != null;
        passwordPref.setSummary(passwordPref.getSharedPreferences().getString("password",null));
        passwordPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(newValue+"");
                return true;
            }
        });

    }
}