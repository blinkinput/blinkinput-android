package com.microblink.ocr;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.microblink.activity.SegmentScanActivity;
import com.microblink.help.HelpActivity;
import com.microblink.recognizers.blinkocr.engine.BlinkOCREngineOptions;
import com.microblink.recognizers.blinkocr.parser.generic.AmountParserSettings;
import com.microblink.recognizers.blinkocr.parser.generic.IbanParserSettings;
import com.microblink.recognizers.blinkocr.parser.regex.RegexParserSettings;
import com.microblink.results.ocr.OcrFont;


public class MainActivity extends Activity {

    private static final int BLINK_OCR_REQUEST_CODE = 100;
    // obtain your licence key at http://microblink.com/login or
    // contact us at http://help.microblink.com
    private static final String LICENSE_KEY = "CNDHGUQS-3REAUYG3-OJYH4FCG-QNW7QSOK-DEO5SIWW-MKYTEYZT-UGBW36CJ-YIELTPLQ";
    private static final String NAME_TOTAL_AMOUNT = "TotalAmount";
    private static final String NAME_TAX = "Tax";
    private static final String NAME_IBAN = "IBAN";

    private static final int BLINK_OCR_VIN_REQUEST_CODE = 101;
    private static final String NAME_VIN = "VIN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * Called as handler for "custom scan ui integration" button.
     */
    public void advancedIntegration(View v) {
        // advanced integration example is given in ScanActivity source code
        Intent intent = new Intent(this, ScanActivity.class);
        startActivity(intent);
    }

    /**
     * Called as handler for "simple integration" button.
     */
    public void simpleIntegration(View v) {
        /*
         * In this simple example we will use BlinkOCR SDK to create a simple app
         * that scans an amount from invoice, tax amount from invoice and IBAN
         * to which amount has to be paid.
         */

        Intent intent = new Intent(this, SegmentScanActivity.class);
        // license key is required for recognizer to work.
        intent.putExtra(SegmentScanActivity.EXTRAS_LICENSE_KEY, LICENSE_KEY);

        // we need to scan 3 items, so we will add 3 scan configurations to scan configuration array
        ScanConfiguration conf[] = new ScanConfiguration[] {
                // each scan configuration contains two string resource IDs: string shown in title bar and string shown
                // in text field above scan box. Besides that, it contains name of the result and settings object
                // which defines what will be scanned.
                new ScanConfiguration(R.string.amount_title, R.string.amount_msg, NAME_TOTAL_AMOUNT, new AmountParserSettings()),
                new ScanConfiguration(R.string.tax_title, R.string.tax_msg, NAME_TAX, new AmountParserSettings()),
                new ScanConfiguration(R.string.iban_title, R.string.iban_msg, NAME_IBAN, new IbanParserSettings())
        };

        intent.putExtra(SegmentScanActivity.EXTRAS_SCAN_CONFIGURATION, conf);

        // optionally, if we want the help screen to be available to user on camera screen,
        // we can simply prepare an intent for help activity and pass it to BlinkOCRActivity
        Intent helpIntent = new Intent(this, HelpActivity.class);
        intent.putExtra(SegmentScanActivity.EXTRAS_HELP_INTENT, helpIntent);

        // once intent is prepared, we start the BlinkOCRActivity which will preform scan and return results
        // by calling onActivityResult
        startActivityForResult(intent, BLINK_OCR_REQUEST_CODE);
    }

    /**
     * Called as handler for "regex example" button
     */
    public void regexExample(View v) {
        /*
         * In this example we will use default BlinkOCRActivity to drive the recognition,
         * but here we will show how to setup a Regex parser. Regex parser allows configuring
         * custom regular expression which should be extracted from OCR result.
         *
         * In this example we will show how to setup Regex parser to scan Vehicle Identification Numbers
         * (VINs) also known as Chassis numbers of a car. The VIN is 17-character string constisting
         * of digits and uppercase letters.
         */

        // same as in simple integration example, we will invoke scanning on BlinkOCRActivity,
        // so we need to setup an Intent for it.
        Intent intent = new Intent(this, SegmentScanActivity.class);
        // license key is required for recognizer to work.
        intent.putExtra(SegmentScanActivity.EXTRAS_LICENSE_KEY, LICENSE_KEY);

        // now let's setup OCR engine parameters for scanning VIN:
        BlinkOCREngineOptions engineOptions = new BlinkOCREngineOptions();
        // only uppercase chars and digits are allowed. Don't waste time on classifying other characters as we
        // do not need them.
        engineOptions.addAllDigitsToWhitelist(OcrFont.OCR_FONT_ANY).addUppercaseCharsToWhitelist(OcrFont.OCR_FONT_ANY);
        // do not bother with text lines that are smaller than 40 pixels
        engineOptions.setMinimumLineHeight(40);
        // we expect the VIN to be black text, so we can drop all colors from image - this will give better accuracy
        // because coloured text will be automatically discarded.
        engineOptions.setColorDropoutEnabled(true);

        // now let's create a RegexParser
        RegexParserSettings regexParserSettings = new RegexParserSettings("[A-Z0-9]{17}", engineOptions);

        // same as in simple integration, create a scan configuration array
        ScanConfiguration conf[] = new ScanConfiguration[] {
                new ScanConfiguration(R.string.vin_title, R.string.vin_msg, NAME_VIN, regexParserSettings)
        };

        intent.putExtra(SegmentScanActivity.EXTRAS_SCAN_CONFIGURATION, conf);

        // optionally, if we want the help screen to be available to user on camera screen,
        // we can simply prepare an intent for help activity and pass it to BlinkOCRActivity
        Intent helpIntent = new Intent(this, HelpActivity.class);
        intent.putExtra(SegmentScanActivity.EXTRAS_HELP_INTENT, helpIntent);

        // once intent is prepared, we start the BlinkOCRActivity which will preform scan and return results
        // by calling onActivityResult
        startActivityForResult(intent, BLINK_OCR_VIN_REQUEST_CODE);
    }

    /**
     * This method is called whenever control is returned from activity started with
     * startActivityForResult.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // first we need to check that we have indeed returned from BlinkOCRActivity with
        // success
        if(requestCode == BLINK_OCR_REQUEST_CODE && resultCode == SegmentScanActivity.RESULT_OK) {
            // now we can obtain bundle with scan results
            Bundle result = data.getBundleExtra(SegmentScanActivity.EXTRAS_SCAN_RESULTS);

            // each result is stored under key equal to the name of the scan configuration that generated it
            String totalAmount = result.getString(NAME_TOTAL_AMOUNT);
            String taxAmount = result.getString(NAME_TAX);
            String iban = result.getString(NAME_IBAN);

            Toast.makeText(this, "To IBAN: " + iban + " we will pay total " + totalAmount + ", tax: " + taxAmount, Toast.LENGTH_LONG).show();
        } else if (requestCode == BLINK_OCR_VIN_REQUEST_CODE && resultCode == SegmentScanActivity.RESULT_OK) {
            // now we can obtain bundle with scan results
            Bundle result = data.getBundleExtra(SegmentScanActivity.EXTRAS_SCAN_RESULTS);

            // each result is stored under key equal to the name of the scan configuration that generated it
            String vin = result.getString(NAME_VIN);

            Toast.makeText(this, "Vehicle identification number is: " + vin, Toast.LENGTH_LONG).show();
        }
    }
}
