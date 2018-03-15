package com.microblink.input;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

public class ResultsActivity extends Activity {

    private static final String KEY_RESULT_TEXT = "resultText";
    private static final String KEY_RESULT_IMAGE_PATH = "resultImage";

    public static Intent buildIntent(Context context, String resultText, String resultImagePath) {
        Intent intent = new Intent(context, ResultsActivity.class);
        intent.putExtra(KEY_RESULT_TEXT, resultText);
        intent.putExtra(KEY_RESULT_IMAGE_PATH, resultImagePath);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);
        TextView resultsTv = findViewById(R.id.results_tv);
        resultsTv.setText(getResultsFromExtras());

        ImageView resultsImg = findViewById(R.id.results_img);
        resultsImg.setImageBitmap(getResultBitmapFromExtras());
    }

    private String getResultsFromExtras() {
        return getIntent().getStringExtra(KEY_RESULT_TEXT);
    }

    private Bitmap getResultBitmapFromExtras() {
        String imagePath = getIntent().getStringExtra(KEY_RESULT_IMAGE_PATH);
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
