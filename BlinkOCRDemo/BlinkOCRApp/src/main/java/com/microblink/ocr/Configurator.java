package com.microblink.ocr;

import com.microblink.recognizers.ocr.blinkocr.parser.generic.AmountParserSettings;
import com.microblink.recognizers.ocr.blinkocr.parser.generic.IbanParserSettings;
import com.microblink.recognizers.ocr.blinkocr.parser.generic.RawParserSettings;

/**
 * Created by dodo on 03/03/15.
 */
public class Configurator {
    public static ScanConfiguration[] createScanConfigurations() {
        return new ScanConfiguration[] {
                new ScanConfiguration(R.string.amount_title, R.string.amount_msg, "Amount", new AmountParserSettings()),
                new ScanConfiguration(R.string.iban_title, R.string.iban_msg, "IBAN", new IbanParserSettings()),
                new ScanConfiguration(R.string.raw_title, R.string.raw_msg, "Raw", new RawParserSettings())
        };
    }
}
