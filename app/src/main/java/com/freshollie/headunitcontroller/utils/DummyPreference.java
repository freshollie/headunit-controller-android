package com.freshollie.headunitcontroller.utils;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by freshollie on 26/04/17.
 * Fixes animations in preference fragments
 */
public class DummyPreference extends Preference {
    public DummyPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public View getView(View convertView, ViewGroup parent) {
        View v = new View(getContext());
        v.setVisibility(View.GONE);
        return v;
    }
}
