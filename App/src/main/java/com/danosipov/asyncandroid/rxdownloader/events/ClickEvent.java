package com.danosipov.asyncandroid.rxdownloader.events;

import android.view.View;

public class ClickEvent {
    private View view;

    public ClickEvent(View v) {
        view = v;
    }

    public View getView() {
        return view;
    }
}
