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
import com.microblink.recognition.InvalidLicenceKeyException;
import com.microblink.recognizers.ocr.blinkocr.BlinkOCRRecognitionResult;
import com.microblink.recognizers.ocr.blinkocr.BlinkOCRRecognizerSettings;
import com.microblink.recognizers.ocr.blinkocr.engine.BlinkOCREngineOptions;
import com.microblink.recognizers.ocr.blinkocr.parser.generic.RawParserSettings;
import com.microblink.recognizers.settings.RecognizerSettings;
import com.microblink.results.ocr.OcrFont;
import com.microblink.view.recognition.ScanResultListener;
import com.microblink.recognizers.BaseRecognitionResult;
import com.microblink.view.recognition.RecognitionType;

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

        mRecognizer = Recognizer.getSingletonInstance();

        // set license key
        try {
            mRecognizer.setLicenseKey(this, "CNDHGUQS-3REAUYG3-OJYH4FCG-QNW7QSOK-DEO5SIWW-MKYTEYZT-UGBW36CJ-YIELTPLQ");
        } catch (InvalidLicenceKeyException e) {
            Log.e(TAG, "Failed to set licence key!");
            Toast.makeText(this, "Failed to set licence key!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // prepare settings for raw OCR
        BlinkOCRRecognizerSettings sett = new BlinkOCRRecognizerSettings();
        RawParserSettings rawSett = new RawParserSettings();

        BlinkOCREngineOptions engineOptions = new BlinkOCREngineOptions();
        engineOptions.setColorDropoutEnabled(false);

        rawSett.setOcrEngineOptions(engineOptions);

        sett.addParser("Raw", rawSett);

        // initialize recognizer singleton
        mRecognizer.initialize(this, null, new RecognizerSettings[] {sett}, new DirectApiErrorListener() {
            @Override
            public void onRecognizerError(Throwable throwable) {
                Log.e(TAG, "Failed to initialize recognizer.", throwable);
                Toast.makeText(MainActivity.this, "Failed to initialize recognizer. Reason: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    public void onScanAssetClick(View v) {
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
            mRecognizer.setOrientation(Orientation.ORIENTATION_LANDSCAPE_RIGHT);
            // disable button
            mScanAssetBtn.setEnabled(false);
            // show progress dialog
            final ProgressDialog pd = new ProgressDialog(this);
            pd.setIndeterminate(true);
            pd.setMessage("Performing recognition");
            pd.setCancelable(false);
            pd.show();
            // recognize image
            mRecognizer.recognize(bitmap, new ScanResultListener() {
                @Override
                public void onScanningDone(BaseRecognitionResult[] dataArray, RecognitionType recognitionType) {

                    if (dataArray != null && dataArray.length > 0) {
                        if (dataArray[0] instanceof BlinkOCRRecognitionResult) {
                            BlinkOCRRecognitionResult result = (BlinkOCRRecognitionResult) dataArray[0];
                            final String parsed = result.getParsedResult("Raw");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
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
                                    mScanAssetBtn.setEnabled(true);
                                    pd.dismiss();
                                }
                            });
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Nothing scanned!", Toast.LENGTH_SHORT).show();
                        // enable button again
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
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
        mRecognizer.terminate();
        mRecognizer = null;
    }
}
