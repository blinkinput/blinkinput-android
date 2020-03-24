package com.microblink.blinkinput.help;

import android.annotation.SuppressLint;
import android.os.Bundle;

import com.microblink.blinkinput.R;
import com.microblink.locale.LanguageUtils;

import androidx.fragment.app.FragmentActivity;

public class HelpActivity extends FragmentActivity {

    @SuppressLint("InlinedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LanguageUtils.setLanguageConfiguration(getResources());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help_view);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LanguageUtils.setLanguageConfiguration(getResources());
    }

}
