package com.microblink.ocr;

import com.microblink.recognizers.blinkocr.parser.generic.AmountParserSettings;
import com.microblink.recognizers.blinkocr.parser.generic.DateParserSettings;
import com.microblink.recognizers.blinkocr.parser.generic.EMailParserSettings;
import com.microblink.recognizers.blinkocr.parser.generic.RawParserSettings;

/**
 * Created by dodo on 03/03/15.
 */
public class Configurator {
    public static ScanConfiguration[] createScanConfigurations() {
        // here we will create scan configuration for Date, E-Mail and Raw text
        // in Raw text parser we will enable Sieve algorithm which will
        // reuse OCR results from multiple video frames to improve quality

        RawParserSettings rawSett = new RawParserSettings();
        rawSett.setUseSieve(false);

        // create amount parser settings from preset for large amounts
        // If parser is initialized with settings from preset LARGE_AMOUNT, amounts without decimal point
        // will be valid and spaces that separate digit groups (thousands) will be allowed.
        AmountParserSettings amountSettings =
                AmountParserSettings.createFromPreset(AmountParserSettings.Preset.LARGE_AMOUNT);
        // in most cases the best option is to use preset GENERIC
        // AmountParserSettings amountSettings =
        //        AmountParserSettings.createFromPreset(AmountParserSettings.Preset.GENERIC);

        return new ScanConfiguration[] {
                new ScanConfiguration(R.string.date_title, R.string.date_msg, "Date", new DateParserSettings()),
                new ScanConfiguration(R.string.large_amount_title, R.string.amount_msg, "Amount", amountSettings),
                new ScanConfiguration(R.string.email_title, R.string.email_msg, "EMail", new EMailParserSettings()),
                new ScanConfiguration(R.string.raw_title, R.string.raw_msg, "Raw", rawSett)
        };
    }
}
