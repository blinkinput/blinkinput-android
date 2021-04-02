package com.microblink.input.customcamera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.microblink.blinkinput.directApi.DirectApiErrorListener;
import com.microblink.blinkinput.directApi.RecognizerRunner;
import com.microblink.blinkinput.entities.recognizers.RecognizerBundle;
import com.microblink.blinkinput.hardware.orientation.Orientation;
import com.microblink.blinkinput.image.Image;
import com.microblink.blinkinput.image.ImageBuilder;
import com.microblink.input.R;
import com.microblink.input.util.ResultFormater;
import com.microblink.blinkinput.recognition.FeatureNotSupportedException;
import com.microblink.blinkinput.recognition.RecognitionSuccessType;
import com.microblink.blinkinput.view.recognition.ScanResultListener;

import java.io.IOException;
import java.util.List;

import androidx.annotation.NonNull;

@SuppressWarnings("deprecation")
public class Camera1Activity extends Activity implements ScanResultListener, SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = "Camera1Activity";

    private SurfaceView mSurfaceView;
    private Camera mCamera;
    private boolean mHaveSurfaceView = false;
    private byte[] mPixelBuffer;
    private int mFrameWidth;
    private int mFrameHeight;

    private RecognizerRunner mRecognizerRunner;
    private RecognizerBundle mRecognizerBundle = new RecognizerBundle();

    private long mTimestamp;
    private TextView mTvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera1);

        Intent intent = getIntent();

        mRecognizerBundle.loadFromIntent(intent);

        // setup camera view
        mSurfaceView = findViewById(R.id.camera1SurfaceView);

        mTvResult = findViewById(R.id.tv_result);
        mTvResult.setVisibility(View.VISIBLE);
        mTvResult.setMovementMethod(new ScrollingMovementMethod());
    }

    @Override
    protected void onStart() {
        super.onStart();
        mRecognizerRunner = RecognizerRunner.getSingletonInstance();
        mRecognizerRunner.initialize(this, mRecognizerBundle, new DirectApiErrorListener() {
            @Override
            public void onRecognizerError(Throwable t) {
                Toast.makeText(Camera1Activity.this, "There was an error in RecognizerRunner: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        mSurfaceView.getHolder().addCallback(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mHaveSurfaceView) {
            startCamera();
        }

    }

    private void startCamera() {
        try {
            mCamera = Camera.open();
            Camera.Parameters params = mCamera.getParameters();
            List<String> supportedFocusModes = params.getSupportedFocusModes();
            if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }

            List<Camera.Size> sizes = params.getSupportedPreviewSizes();
            Camera.Size bestSize = null;
            int bestDiff = Integer.MAX_VALUE;
            for (Camera.Size size : sizes) {
                int diff = size.width * size.height - 1920*1080;
                if (Math.abs(diff) < Math.abs(bestDiff)) {
                    bestDiff = diff;
                    bestSize = size;
                }
            }
            params.setPreviewFormat(ImageFormat.NV21);
            params.setPreviewSize(bestSize.width, bestSize.height);
            mFrameWidth = bestSize.width;
            mFrameHeight = bestSize.height;

            mCamera.setParameters(params);

            mPixelBuffer = new byte[bestSize.width * bestSize.height * ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8];
            mCamera.addCallbackBuffer(mPixelBuffer);
            mCamera.setPreviewCallbackWithBuffer(this);

            mCamera.setPreviewDisplay(mSurfaceView.getHolder());

            mCamera.startPreview();

        } catch (RuntimeException exc) {
            Log.e(TAG, "Failed to open camera", exc);
            finish();
        } catch (IOException e) {
            Log.e(TAG, "Failed to set preview display!", e);
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mRecognizerRunner != null) {
            mRecognizerRunner.terminate();
        }
        mSurfaceView.getHolder().removeCallback(this);
    }

    @Override
    public void onScanningDone(@NonNull RecognitionSuccessType successType) {
        long timePassed = System.currentTimeMillis() - mTimestamp;
        Log.w(TAG, "Frame processing took " + timePassed + " ms");

        // check if results contain valid data
        if (successType != RecognitionSuccessType.UNSUCCESSFUL) {
            final String s = ResultFormater.stringifyRecognitionResults(mRecognizerBundle.getRecognizers());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTvResult.setText(s);
                }
            });
            // ask for another frame
            if (mCamera != null) {
                mCamera.addCallbackBuffer(mPixelBuffer);
            }
        } else {
            if (mCamera != null) {
                mCamera.addCallbackBuffer(mPixelBuffer);
            }
        }
    }

    @Override
    public void onUnrecoverableError(@NonNull Throwable throwable) {
        Toast.makeText(this, throwable.toString(), Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mHaveSurfaceView = true;
        startCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mHaveSurfaceView = false;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (mRecognizerRunner.getCurrentState() == RecognizerRunner.State.READY) {
            // create image
            Image img = ImageBuilder.buildImageFromCamera1NV21Frame(data, mFrameWidth, mFrameHeight, Orientation.ORIENTATION_LANDSCAPE_RIGHT, null);
            mTimestamp = System.currentTimeMillis();
            mRecognizerRunner.recognizeImage(img, this);
        } else {
            // just ask for another frame
            camera.addCallbackBuffer(data);
        }
    }
}
