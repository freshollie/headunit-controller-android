package com.freshollie.headunitcontroller.ui;

import android.content.Context;
import android.graphics.Path;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.freshollie.headunitcontroller.R;
import com.freshollie.headunitcontroller.utils.StatusUtil;

import java.util.zip.Inflater;


/**
 * Created by freshollie on 07/05/17.
 */

public class LogPreference extends Preference {
    private TextView logView;

    private LayoutInflater inflater;

    public LogPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflater = LayoutInflater.from(context);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        super.onCreateView(parent);
        View view = inflater.inflate(R.layout.layout_log, parent, false);
        logView = (TextView) view.findViewById(R.id.log_view);
        logView.setText(StatusUtil.getInstance().getHistory());

        return view;
    }

    public void updateLog(String log) {
        if (logView != null) {
            logView.setText(log);
        }
    }
}
