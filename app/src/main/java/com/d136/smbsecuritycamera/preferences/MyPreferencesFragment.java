package com.d136.smbsecuritycamera.preferences;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;

import com.d136.smbsecuritycamera.R;

public class MyPreferencesFragment extends PreferenceFragmentCompat {
    private final static String TAG = "PreferenceFragment";
    private Preference portPref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
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
                    int check = Integer.parseInt(newValue.toString());
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
        timePref.setSummary(timePref.getSharedPreferences().getString("time","5")+" seconds");
        timePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                try{
                    int temp = Integer.parseInt((String) newValue);
                }catch(Exception e){
                    preference.setSummary(newValue+" is not a valid input. Set to 5 seconds.");
                    timePref.getSharedPreferences().edit().putString("time","5").apply();
                    return true;
                }
                preference.setSummary(newValue+" seconds");
                return true;
            }
        });


        final Preference freqPref = findPreference("frequency");
        assert freqPref != null;
        freqPref.setSummary(freqPref.getSharedPreferences().getString("frequency","500")+" milliseconds");
        freqPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                try{
                    int temp = Integer.parseInt((String) newValue);
                }catch(Exception e){
                    preference.setSummary(newValue+" is not a valid input. Set to 500 milli seconds.");
                    timePref.getSharedPreferences().edit().putString("frequency","500").apply();
                    return true;
                }
                preference.setSummary(newValue+" milliseconds");
                return true;
            }
        });








        final SeekBarPreference tolerance_seek_bar = findPreference("tolerance");
        assert tolerance_seek_bar != null;
        int tolerance_value = tolerance_seek_bar.getSharedPreferences().getInt("tolerance", 20);
        tolerance_seek_bar.getSharedPreferences().edit().putInt("tolerance",tolerance_value).apply();
        tolerance_seek_bar.setValue(tolerance_value);
        tolerance_seek_bar.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return true;
            }
        });


        final SeekBarPreference luminosity_sensibility_seek_bar = findPreference("luma");
        assert luminosity_sensibility_seek_bar != null;
        int luminosity_value = luminosity_sensibility_seek_bar.getSharedPreferences().getInt("luma", 1000);
        luminosity_sensibility_seek_bar.getSharedPreferences().edit().putInt("luma",luminosity_value).apply();
        luminosity_sensibility_seek_bar.setValue(luminosity_value);
        luminosity_sensibility_seek_bar.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return true;
            }
        });



        final Preference cache_size_pref = findPreference("cache_size");
        assert cache_size_pref != null;
        cache_size_pref.setSummary(cache_size_pref.getSharedPreferences().getString("cache_size","100")+" MB");
        cache_size_pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                try{
                    int temp = Integer.parseInt((String) newValue);
                }catch(Exception e){
                    preference.setSummary(newValue+" is not a valid input. Default to 100 MB.");
                    cache_size_pref.getSharedPreferences().edit().putString("cache_size","100").apply();
                    return true;
                }
                preference.setSummary(newValue+" MB");
                return true;
            }
        });




        final Preference quality_pref = findPreference("quality");
        String quality_value = cache_size_pref.getSharedPreferences().getString("quality","640x480");
        assert quality_pref != null;
        quality_pref.setSummary(quality_value);
        quality_pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary(newValue+"");
                return true;
            }
        });


    }

}