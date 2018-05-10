package com.microblink.input;

import android.os.Bundle;
import android.widget.Toast;

import com.microblink.blinkinput.BaseMenuActivity;
import com.microblink.blinkinput.MenuListItem;
import com.microblink.util.RecognizerCompatibility;
import com.microblink.util.RecognizerCompatibilityStatus;

import java.util.ArrayList;
import java.util.List;

public class MenuActivity extends BaseMenuActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RecognizerCompatibilityStatus supportStatus = RecognizerCompatibility.getRecognizerCompatibilityStatus(this);
        if (supportStatus != RecognizerCompatibilityStatus.RECOGNIZER_SUPPORTED) {
            Toast.makeText(this, "BlinkInput is not supported! Reason: " + supportStatus.name(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected List<MenuListItem> createMenuListItems() {
        ArrayList<MenuListItem> items = new ArrayList<>();

        items.add(new MenuListItem("Blink OCR", new Runnable() {
            @Override
            public void run() {
                startActivity(ScanActivity.buildBlinkOcrIntent(MenuActivity.this));
            }
        }));

        items.add(new MenuListItem("Deep OCR", new Runnable() {
            @Override
            public void run() {
                startActivity(ScanActivity.buildDeepOcrIntent(MenuActivity.this));
            }
        }));

        return items;
    }

    @Override
    protected String getTitleText() {
        return getString(R.string.app_name);
    }

}