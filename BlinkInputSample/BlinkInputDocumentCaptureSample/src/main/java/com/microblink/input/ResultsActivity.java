package com.microblink.input;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

import androidx.annotation.Nullable;

public class ResultsActivity extends Activity {

    private static final String KEY_RESULT_TEXT = "resultText";
    private static final String KEY_DOCUMENT_IMAGE_PATH = "documentImagePath";
    private static final String KEY_FULL_IMAGE_PATH = "fullImagePath";

    public static Intent buildIntent(Context context,
                                     String resultText,
                                     String documentImagePath,
                                     String fullImagePath) {
        Intent intent = new Intent(context, ResultsActivity.class);
        intent.putExtra(KEY_RESULT_TEXT, resultText);
        intent.putExtra(KEY_DOCUMENT_IMAGE_PATH, documentImagePath);
        intent.putExtra(KEY_FULL_IMAGE_PATH, fullImagePath);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);
        TextView resultsTv = findViewById(R.id.results_tv);
        resultsTv.setText(getResultsFromExtras());

        ImageView fullDocumentIv = findViewById(R.id.document_img);
        Bitmap documentBmp = getResultBitmapFromExtras(KEY_DOCUMENT_IMAGE_PATH);
        if (documentBmp != null) {
            fullDocumentIv.setVisibility(View.VISIBLE);
            fullDocumentIv.setImageBitmap(documentBmp);
        } else {
            fullDocumentIv.setVisibility(View.GONE);
        }

        ImageView fullFrameIv = findViewById(R.id.full_frame);
        Bitmap fullFrameBmp = getResultBitmapFromExtras(KEY_FULL_IMAGE_PATH);
        if (fullFrameBmp != null) {
            fullFrameIv.setVisibility(View.VISIBLE);
            fullFrameIv.setImageBitmap(fullFrameBmp);
        } else {
            fullFrameIv.setVisibility(View.GONE);
        }
    }

    private String getResultsFromExtras() {
        return getIntent().getStringExtra(KEY_RESULT_TEXT);
    }

    private Bitmap getResultBitmapFromExtras(String pathKey) {
        String imagePath = getIntent().getStringExtra(pathKey);
        if(imagePath == null) {
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap resultBitmap = BitmapFactory.decodeFile(imagePath, options);
        //noinspection ResultOfMethodCallIgnored
        new File(imagePath).delete();
        return resultBitmap;
    }

}
