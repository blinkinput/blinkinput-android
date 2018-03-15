package com.microblink.input;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import com.microblink.blinkinput.BaseMenuActivity;
import com.microblink.blinkinput.MenuListItem;
import com.microblink.entities.ocrengine.legacy.BlinkOCREngineOptions;
import com.microblink.entities.parsers.raw.RawParser;
import com.microblink.entities.processors.parserGroup.ParserGroupProcessor;
import com.microblink.entities.recognizers.RecognizerBundle;
import com.microblink.entities.recognizers.blinkinput.BlinkInputRecognizer;
import com.microblink.input.customcamera.Camera1Activity;
import com.microblink.input.customcamera.camera2.Camera2Activity;
import com.microblink.input.imagescan.ScanImageActivity;

import java.util.ArrayList;
import java.util.List;


public class MenuActivity extends BaseMenuActivity {

    private static final int MY_REQUEST_CODE = 1337;

    private static final int PERMISSION_REQUEST_CODE = 0x123;

    /**
     * Parser which is used for data extraction
     */
    private RawParser mRawParser;

    /**
     * Recognizer bundle that will wrap the recognizer in order for recognition to be performed
     */
    private RecognizerBundle mRecognizerBundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initRecognizer();

        // Request permissions if not granted, we need CAMERA permission and
        // WRITE_EXTERNAL_STORAGE permission because images that are taken by camera
        // will be stored on external storage and used in recognition process
        List<String> requiredPermissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.CAMERA);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (requiredPermissions.size() > 0) {
            String[] permArray = new String[requiredPermissions.size()];
            permArray = requiredPermissions.toArray(permArray);
            ActivityCompat.requestPermissions(this, permArray, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    protected List<MenuListItem> createMenuListItems() {
        List<MenuListItem> items = new ArrayList<>();

        items.add(new MenuListItem("Scan Image", new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MenuActivity.this, ScanImageActivity.class);
                mRecognizerBundle.saveToIntent(intent);
                startActivityForResult(intent, MY_REQUEST_CODE);
            }
        }));

        items.add(new MenuListItem("Camera 1 Activity", new Runnable() {
            @Override
            public void run() {
                startCameraActivity(Camera1Activity.class);
            }
        }));

        items.add(new MenuListItem("Camera 2 Activity", new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= 21) {
                    startCameraActivity(Camera2Activity.class);
                } else {
                    Toast.makeText(MenuActivity.this, "Camera2 API requires Android 5.0 or newer. Camera1 direct API will be used", Toast.LENGTH_SHORT).show();
                    startCameraActivity(Camera1Activity.class);
                }
            }
        }));

        return items;
    }

    @Override
    protected String getTitleText() {
        return getString(R.string.app_name);
    }

    private void initRecognizer() {

        // prepare parser for raw OCR
        mRawParser = new RawParser();

        // set OCR engine options
        BlinkOCREngineOptions engineOptions = new BlinkOCREngineOptions();
        // set to false to scan colored text (set to true only for black text on color background)
        engineOptions.setColorDropoutEnabled(false);
        mRawParser.setOcrEngineOptions(engineOptions);

        // Recognizer that will perform recognition of images
        BlinkInputRecognizer blinkInputRecognizer = new BlinkInputRecognizer(
                // parser group processor is used on the input image and only raw parser is used
                new ParserGroupProcessor(mRawParser)
        );

        mRecognizerBundle = new RecognizerBundle(blinkInputRecognizer);
    }

    private void startCameraActivity(Class targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        mRecognizerBundle.saveToIntent(intent);
        startActivityForResult(intent, MY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MY_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            // updates bundled recognizers with results that have arrived
            mRecognizerBundle.loadFromIntent(data);
            // after calling mRecognizerBundle.loadFromIntent, results are stored within mRawParser
            String parsedResult = mRawParser.getResult().toString();
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Scan result")
                    .setMessage(parsedResult)
                    .setCancelable(false)
                    .setPositiveButton("OK", null)
                    .create();
            dialog.show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                new AlertDialog.Builder(this)
                        .setTitle("Exiting")
                        .setMessage("Exiting app, permission(s) not granted.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .setCancelable(false)
                        .create()
                        .show();
                return;
            }
        }
    }

}
