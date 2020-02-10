package com.d136.smbsecuritycamera;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class MySettingsFragment extends PreferenceFragmentCompat {
    private static String TAG = "PreferenceFragment";
    private Preference portPref;

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



        Preference customPortPref = findPreference("portEnabled");
        assert customPortPref != null;
        if(!customPortPref.getSharedPreferences().getBoolean("portEnabled",false))
            portPref.setEnabled(false);
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

        Preference userPref = findPreference("user");
        assert userPref != null;
        userPref.setSummary(userPref.getSharedPreferences().getString("user",null));
        userPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(newValue+"");
                return true;
            }
        });

        Preference passwordPref = findPreference("password");
        assert passwordPref != null;
        passwordPref.setSummary(passwordPref.getSharedPreferences().getString("password",null));
        passwordPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(newValue+"");
                return true;
            }
        });

        final Preference timePref = findPreference("time");
        assert timePref != null;
        timePref.setSummary(timePref.getSharedPreferences().getString("time","5"));
        timePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                try{
                    int temp = Integer.valueOf((String) newValue);
                }catch(Exception e){
                    preference.setSummary(newValue+" is not a valid input. Set to 5 seconds.");
                    timePref.getSharedPreferences().edit().putString("time","5").apply();
                    return true;
                }
                preference.setSummary(newValue+"");
                return true;
            }
        });


        final Preference freqPref = findPreference("frequency");
        assert freqPref != null;
        freqPref.setSummary(freqPref.getSharedPreferences().getString("frequency","500"));
        freqPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                try{
                    int temp = Integer.valueOf((String) newValue);
                }catch(Exception e){
                    preference.setSummary(newValue+" is not a valid input. Set to 500 milli seconds.");
                    timePref.getSharedPreferences().edit().putString("frequency","500").apply();
                    return true;
                }
                preference.setSummary(newValue+"");
                return true;
            }
        });



        final Preference tolerPref = findPreference("tolerance");
        assert tolerPref != null;
        tolerPref.setSummary(tolerPref.getSharedPreferences().getString("tolerance","20"));
        tolerPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                try{
                    int temp = Integer.valueOf((String) newValue);
                }catch(Exception e){
                    preference.setSummary(newValue+" is not a valid input. Set to 20 units.");
                    tolerPref.getSharedPreferences().edit().putString("tolerance","20").apply();
                    return true;
                }
                preference.setSummary(newValue+"");
                return true;
            }
        });



        final Preference lumaPref = findPreference("luma");
        assert lumaPref != null;
        lumaPref.setSummary(lumaPref.getSharedPreferences().getString("luma","1000"));
        lumaPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                try{
                    int temp = Integer.valueOf((String) newValue);
                }catch(Exception e){
                    preference.setSummary(newValue+" is not a valid input. Set to 20 units.");
                    lumaPref.getSharedPreferences().edit().putString("luma","1000").apply();
                    return true;
                }
                preference.setSummary(newValue+"");
                return true;
            }
        });

    }

}