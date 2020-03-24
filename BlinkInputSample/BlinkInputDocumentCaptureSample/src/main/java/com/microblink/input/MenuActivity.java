package com.microblink.input;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Toast;

import com.microblink.blinkinput.BaseMenuActivity;
import com.microblink.blinkinput.MenuListItem;
import com.microblink.entities.recognizers.blinkinput.documentcapture.DocumentCaptureRecognizer;
import com.microblink.entities.recognizers.blinkinput.documentcapture.DocumentCaptureRecognizerTransferable;
import com.microblink.image.Image;
import com.microblink.uisettings.ActivityRunner;
import com.microblink.uisettings.DocumentCaptureUISettings;
import com.microblink.util.RecognizerCompatibility;
import com.microblink.util.RecognizerCompatibilityStatus;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MenuActivity extends BaseMenuActivity {

    public static final int MY_DOCUMENT_CAPTURE_REQUEST_CODE = 123;

    @Override
    protected String getTitleText() {
        return getString(R.string.app_name);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        RecognizerCompatibilityStatus supportStatus = RecognizerCompatibility.getRecognizerCompatibilityStatus(this);
        if (supportStatus != RecognizerCompatibilityStatus.RECOGNIZER_SUPPORTED) {
            finish();
            Toast.makeText(this, "BlinkInput is not supported! Reason: " + supportStatus.name(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected List<MenuListItem> createMenuListItems() {
        List<MenuListItem> items = new ArrayList<>();

        items.add(new MenuListItem(getString(R.string.item_document_capture), new Runnable() {
            @Override
            public void run() {
                DocumentCaptureRecognizer documentCaptureRecognizer = new DocumentCaptureRecognizer();
                documentCaptureRecognizer.setReturnFullDocumentImage(true);
                DocumentCaptureUISettings uiSettings = new DocumentCaptureUISettings(
                        new DocumentCaptureRecognizerTransferable(
                                documentCaptureRecognizer
                        )
                );
                ActivityRunner.startActivityForResult(MenuActivity.this, MY_DOCUMENT_CAPTURE_REQUEST_CODE, uiSettings);
            }
        }));

        items.add(new MenuListItem(getString(R.string.item_document_capture_custom), new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MenuActivity.this, CustomDocumentCaptureActivity.class);
                startActivityForResult(intent, MY_DOCUMENT_CAPTURE_REQUEST_CODE);
            }
        }));

        return items;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MY_DOCUMENT_CAPTURE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            DocumentCaptureRecognizerTransferable documentCaptureRecognizerTransferable =
                    DocumentCaptureRecognizerTransferable.createFromIntent(data);

            DocumentCaptureRecognizer.Result recognizerResult =
                    documentCaptureRecognizerTransferable.getDocumentCaptureRecognizer().getResult();
            String documentImagePath = storeImageToFile(
                    recognizerResult.getFullDocumentImage(),
                    "documentImage.jpg"
            );

            String fullImagePath = storeImageToFile(documentCaptureRecognizerTransferable.getCapturedFullImage().getImage(),
                    "fullImage.jpg");

            String resultText = "Result state: " + recognizerResult.getResultState().name();

            startActivity(ResultsActivity.buildIntent(this, resultText, documentImagePath, fullImagePath));
        }
    }

    /**
     * Stores image to file.
     * @param image image to store.
     * @param filename file name.
     * @return full image path
     */
    private String storeImageToFile(Image image, String filename) {
        String filePath;

        try {
            Bitmap bitmap = image.convertToBitmap();
            File imageFile = new File(getFilesDir(), filename);
            OutputStream os = new BufferedOutputStream(new FileOutputStream(imageFile));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            filePath = imageFile.getAbsolutePath();
            os.close();
        } catch (Exception e) {
            filePath = null;
        }

        return filePath;
    }

}