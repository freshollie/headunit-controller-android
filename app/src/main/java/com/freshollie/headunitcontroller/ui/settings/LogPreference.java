package com.freshollie.headunitcontroller.ui.settings;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.freshollie.headunitcontroller.R;
import com.freshollie.headunitcontroller.util.Logger;


/**
 * Created by freshollie on 07/05/17.
 */

public class LogPreference extends Preference {
    private final Logger logger;
    private TextView logView;

    private LayoutInflater inflater;

    public LogPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflater = LayoutInflater.from(context);
        logger = Logger.getInstance();
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        super.onCreateView(parent);
        View view = inflater.inflate(R.layout.layout_log, parent, false);
        logView = (TextView) view.findViewById(R.id.log_view);
        logView.setText(logger.getJoinedLog());

        return view;
    }

    public void updateLog(String log) {
        if (logView != null) {
            logView.setText(log);
        }
    }
}
