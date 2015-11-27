package com.microblink.photopay;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.microblink.directApi.DirectApiErrorListener;
import com.microblink.directApi.Recognizer;
import com.microblink.hardware.orientation.Orientation;
import com.microblink.recognition.FeatureNotSupportedException;
import com.microblink.recognition.InvalidLicenceKeyException;
import com.microblink.recognizers.BaseRecognitionResult;
import com.microblink.recognizers.RecognitionResults;
import com.microblink.recognizers.blinkocr.BlinkOCRRecognitionResult;
import com.microblink.recognizers.blinkocr.BlinkOCRRecognizerSettings;
import com.microblink.recognizers.blinkocr.engine.BlinkOCREngineOptions;
import com.microblink.recognizers.blinkocr.parser.generic.RawParserSettings;
import com.microblink.recognizers.settings.RecognitionSettings;
import com.microblink.recognizers.settings.RecognizerSettings;
import com.microblink.view.recognition.ScanResultListener;

import java.io.IOException;
import java.io.InputStream;


public class MainActivity extends Activity {

    private static final String TAG = "DirectApiDemo";

    private Recognizer mRecognizer = null;
    private Button mScanAssetBtn = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mScanAssetBtn = (Button)findViewById(R.id.button);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // get the recognizer instance
        try {
            mRecognizer = Recognizer.getSingletonInstance();
        } catch (FeatureNotSupportedException e) {
            Toast.makeText(this, "Feature not supported! Reason: " + e.getReason().getDescription(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // set the license key
        try {
            mRecognizer.setLicenseKey(this, "CNDHGUQS-3REAUYG3-OJYH4FCG-QNW7QSOK-DEO5SIWW-MKYTEYZT-UGBW36CJ-YIELTPLQ");
        } catch (InvalidLicenceKeyException e) {
            Log.e(TAG, "Failed to set licence key!");
            Toast.makeText(this, "Failed to set licence key!", Toast.LENGTH_LONG).show();
            finish();
            mRecognizer = null;
            return;
        }

        // prepare recognition settings
        RecognitionSettings settings = new RecognitionSettings();

        // prepare settings for raw OCR
        BlinkOCRRecognizerSettings sett = new BlinkOCRRecognizerSettings();
        RawParserSettings rawSett = new RawParserSettings();

        // set OCR engine options
        BlinkOCREngineOptions engineOptions = new BlinkOCREngineOptions();
        // set to false to scan colored text (set to true only for black text on color background)
        engineOptions.setColorDropoutEnabled(false);
        rawSett.setOcrEngineOptions(engineOptions);

        // add raw parser to default parser group
        sett.addParser("Raw", rawSett);

        settings.setRecognizerSettingsArray(new RecognizerSettings[] { sett });

        // initialize recognizer singleton with defined settings
        mRecognizer.initialize(this, settings, new DirectApiErrorListener() {
            @Override
            public void onRecognizerError(Throwable throwable) {
                Log.e(TAG, "Failed to initialize recognizer.", throwable);
                Toast.makeText(MainActivity.this, "Failed to initialize recognizer. Reason: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    public void onScanAssetClick(View v) {
        // check whether the recognizer is ready
        if(mRecognizer.getCurrentState() != Recognizer.State.READY) {
            Log.e(TAG, "Recognizer not ready!");
            return;
        }
        // load Bitmap from assets
        AssetManager assets = getAssets();
        InputStream istr;
        Bitmap bitmap = null;
        try {
            istr = assets.open("lipsum.png");
            bitmap = BitmapFactory.decodeStream(istr);
        } catch (IOException e) {
            // handle exception
            Log.e(TAG, "Failed to load image from assets!");
            Toast.makeText(this, "Failed to load image from assets!", Toast.LENGTH_LONG).show();
            return;
        }

        if(bitmap != null) {
            // disable button
            mScanAssetBtn.setEnabled(false);
            // show progress dialog
            final ProgressDialog pd = new ProgressDialog(this);
            pd.setIndeterminate(true);
            pd.setMessage("Performing recognition");
            pd.setCancelable(false);
            pd.show();
            // recognize image
            mRecognizer.recognizeBitmap(bitmap, Orientation.ORIENTATION_LANDSCAPE_RIGHT, new ScanResultListener() {
                @Override
                public void onScanningDone(RecognitionResults results) {
                    // get results array
                    BaseRecognitionResult[] dataArray = results.getRecognitionResults();
                    if (dataArray != null && dataArray.length > 0) {
                        // only single result from BlinkOCRRecognizer is expected
                        if (dataArray[0] instanceof BlinkOCRRecognitionResult) {
                            BlinkOCRRecognitionResult result = (BlinkOCRRecognitionResult) dataArray[0];
                            // get string result from configured parser with parser name "Raw"
                            final String parsed = result.getParsedResult("Raw");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // enable scan button
                                    mScanAssetBtn.setEnabled(true);
                                    pd.dismiss();

                                    AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
                                    b.setTitle("OCR").setMessage(parsed).setCancelable(false).setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    }).show();
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // enable scan button
                                    mScanAssetBtn.setEnabled(true);
                                    pd.dismiss();
                                }
                            });
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Nothing scanned!", Toast.LENGTH_SHORT).show();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // enable scan button
                                mScanAssetBtn.setEnabled(true);
                                pd.dismiss();
                            }
                        });
                    }
                }
            });
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // terminate the native library and free unnecessary resources
        mRecognizer.terminate();
        // for further use, recognizer must be initialized again
        mRecognizer = null;
    }

}
