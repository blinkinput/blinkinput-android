package com.microblink.ocr;

import android.content.Context;

import com.microblink.recognizers.ocr.blinkocr.parser.OcrParserSettings;

/**
 * Created by dodo on 03/03/15.
 *
 * This class will hold a combination of parser settings, parser name, parser title (shown in UI),
 * and parser message (shown in UI).
 */
public class ScanConfiguration {

    private int mTitleResource;
    private int mTextResource;
    private String mParserName;
    private OcrParserSettings mParserSettings;

    private String mTitle;

    public ScanConfiguration(int titleResource, int textResource, String parserName, OcrParserSettings parserSettings) {
        mTitleResource = titleResource;
        mTextResource = textResource;
        mParserName = parserName;
        mParserSettings = parserSettings;
    }

    public int getTitleResource() {
        return mTitleResource;
    }

    public void setTitleResource(int titleResource) {
        mTitleResource = titleResource;
    }

    public int getTextResource() {
        return mTextResource;
    }

    public void setTextResource(int textResource) {
        mTextResource = textResource;
    }

    public String getParserName() {
        return mParserName;
    }

    public void setParserName(String parserName) {
        mParserName = parserName;
    }

    public OcrParserSettings getParserSettings() {
        return mParserSettings;
    }

    public void setParserSettings(OcrParserSettings parserSettings) {
        mParserSettings = parserSettings;
    }

    public void loadTitle(Context ctx) {
        mTitle = ctx.getString(mTitleResource);
    }

    @Override
    public String toString() {
        return mTitle;
    }
}
