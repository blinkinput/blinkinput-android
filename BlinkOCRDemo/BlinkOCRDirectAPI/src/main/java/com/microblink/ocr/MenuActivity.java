package com.microblink.ocr;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.microblink.directApi.DirectApiErrorListener;
import com.microblink.directApi.Recognizer;
import com.microblink.hardware.orientation.Orientation;
import com.microblink.ocr.customcamera.Camera1Activity;
import com.microblink.ocr.customcamera.camera2.Camera2Activity;
import com.microblink.ocr.imagescan.ScanImageActivity;
import com.microblink.recognition.FeatureNotSupportedException;
import com.microblink.recognition.InvalidLicenceKeyException;
import com.microblink.recognizers.BaseRecognitionResult;
import com.microblink.recognizers.RecognitionResults;
import com.microblink.recognizers.blinkbarcode.BarcodeType;
import com.microblink.recognizers.blinkbarcode.bardecoder.BarDecoderScanResult;
import com.microblink.recognizers.blinkbarcode.pdf417.Pdf417ScanResult;
import com.microblink.recognizers.blinkbarcode.zxing.ZXingScanResult;
import com.microblink.recognizers.blinkocr.BlinkOCRRecognitionResult;
import com.microblink.recognizers.blinkocr.BlinkOCRRecognizerSettings;
import com.microblink.recognizers.blinkocr.engine.BlinkOCREngineOptions;
import com.microblink.recognizers.blinkocr.parser.generic.RawParserSettings;
import com.microblink.recognizers.settings.RecognitionSettings;
import com.microblink.recognizers.settings.RecognizerSettings;
import com.microblink.results.barcode.BarcodeDetailedData;
import com.microblink.view.recognition.ScanResultListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class MenuActivity extends Activity {

    // obtain your licence key at http://microblink.com/login or
    // contact us at http://help.microblink.com
    private static final String LICENSE_KEY = "H5SIMEWT-YGSAO47Z-LEJJI2C6-ZOIHQI4S-HAQIOSRB-MKYTEYZT-UGBW36CI-C2LAREQJ";

    private static final int MY_REQUEST_CODE = 1337;
    private static final String TAG = "DirectApiDemo";

    private static final int PERMISSION_REQUEST_CODE = 0x123;

    /**
     * Recognition settings instance, same recognition settings are used for all examples.
     */
    private RecognitionSettings mRecognitionSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        buildRecognitionSettings();

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

    private void buildRecognitionSettings() {
        // prepare settings for raw OCR
        BlinkOCRRecognizerSettings ocrSett = new BlinkOCRRecognizerSettings();
        RawParserSettings rawSett = new RawParserSettings();

        // set OCR engine options
        BlinkOCREngineOptions engineOptions = new BlinkOCREngineOptions();
        // set to false to scan colored text (set to true only for black text on color background)
        engineOptions.setColorDropoutEnabled(false);
        rawSett.setOcrEngineOptions(engineOptions);

        // add raw parser with name "Raw" to default parser group
        // parser name is important for obtaining results later
        ocrSett.addParser("Raw", rawSett);

        // prepare recognition settings
        mRecognitionSettings = new RecognitionSettings();
        // set recognizer settings array that is used to configure recognition
        // BlinkOCRRecognizer will be used in the recognition process
        mRecognitionSettings.setRecognizerSettingsArray(new RecognizerSettings[]{ocrSett});
    }


    /**
     * Handler for "Scan Image" button
     */
    public void onScanImageClick(View v) {
        Intent intent = new Intent(this, ScanImageActivity.class);
        // send license key over intent to scan activity
        intent.putExtra(ExtrasKeys.EXTRAS_LICENSE_KEY, LICENSE_KEY);
        // send settings over intent to scan activity
        intent.putExtra(ExtrasKeys.EXTRAS_RECOGNITION_SETTINGS, mRecognitionSettings);
        startActivityForResult(intent, MY_REQUEST_CODE);
    }

    /**
     * Handler for "Camera 1 Activity" and "Camera 2 Activity" buttons
     */
    public void onCameraScanClick(View view) {
        Class<?> targetActivity = null;
        switch (view.getId()) {
            case R.id.btn_camera1:
                targetActivity = Camera1Activity.class;
                break;
            case R.id.btn_camera2:
                if (Build.VERSION.SDK_INT >= 21) {
                    targetActivity = Camera2Activity.class;
                } else {
                    Toast.makeText(this, "Camera2 API requires Android 5.0 or newer. Camera1 direct API will be used", Toast.LENGTH_SHORT).show();
                    targetActivity = Camera1Activity.class;
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown button clicked!");
        }

        Intent intent = new Intent(this, targetActivity);
        // send license key over intent to scan activity
        intent.putExtra(ExtrasKeys.EXTRAS_LICENSE_KEY, LICENSE_KEY);
        // send settings over intent to scan activity
        intent.putExtra(ExtrasKeys.EXTRAS_RECOGNITION_SETTINGS, mRecognitionSettings);
        startActivityForResult(intent, MY_REQUEST_CODE);
    }

    public void showResults(RecognitionResults results) {
        // get results array
        BaseRecognitionResult[] dataArray = results.getRecognitionResults();
        if (dataArray != null && dataArray.length > 0) {
            // only single result from BlinkOCRRecognizer is expected
            if (dataArray[0] instanceof BlinkOCRRecognitionResult) {
                BlinkOCRRecognitionResult result = (BlinkOCRRecognitionResult) dataArray[0];
                // get string result from configured parser with parser name "Raw"
                final String parsed = result.getParsedResult("Raw");
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle("Scan result")
                        .setMessage(parsed)
                        .setCancelable(false)
                        .setPositiveButton("OK", null)
                        .create();
                dialog.show();
            }
        } else {
            Toast.makeText(MenuActivity.this, "Nothing scanned!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // First, obtain recognition result
            RecognitionResults results = data.getParcelableExtra(ExtrasKeys.EXTRAS_RECOGNITION_RESULTS);
            showResults(results);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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
