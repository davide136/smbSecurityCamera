package com.d136.smbsecuritycamera.preferences;



import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import androidx.preference.ListPreference;

import java.util.List;

public class DynamicPreference extends ListPreference {

    private CharSequence[] entries;

    public DynamicPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DynamicPreference(Context context) {
        super(context);
    }

    @Override
    protected void onClick() {
        super.onClick();
        ListView view = new ListView(getContext());
        view.setAdapter(adapter());
        setEntries(entries());
        setEntryValues(entryValues());
        setValueIndex(initializeIndex());

    }

    private int initializeIndex() {
        SharedPreferences sharedPreferences = getSharedPreferences();
        int index = 0;
        CharSequence currentValue = sharedPreferences.getString("quality","640x480");
        for(int i=0; i<entries.length; i++)
            if(currentValue.equals(entries[i]))
                index = i;
        return index;
    }

    private ListAdapter adapter() {
        return new ArrayAdapter(getContext(), android.R.layout.select_dialog_singlechoice);
    }

    private CharSequence[] entries() {
        //action to provide entry data in char sequence array for list
        Camera camera = Camera.open();
        Camera.Parameters param = camera.getParameters();
        List<Camera.Size> list = param.getSupportedPreviewSizes();
        int i=0;
        entries = new CharSequence[list.size()] ;
        while( i<list.size() ){
            entries[i] = list.get(i).width+"x"+list.get(i).height;
            i++;
        }
        orderArray();
        return entries;
    }

    private void orderArray() {
        int i=0;
        int[] temp = new int[entries.length];
        while(i<entries.length){
            String[] pair = entries[i].toString().split("x");
            temp[i]=Integer.valueOf(pair[0])*Integer.valueOf(pair[1]);
            i++;
        }
        int j=0;
        while(j<entries.length){
            for(int k=0; k<entries.length; k++){
                if( temp[j]<temp[k] ){
                    //switch temp
                    int temp_value = temp[j];
                    temp[j]=temp[k];
                    temp[k]=temp_value;
                    //switch real
                    CharSequence temp_char = entries[j];
                    entries[j]=entries[k];
                    entries[k]=temp_char;
                }
            }
            j++;
        }
    }

    private CharSequence[] entryValues() {
        //action to provide value data for list
        return entries.clone();
    }
}