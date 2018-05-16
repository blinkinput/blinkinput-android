package com.microblink.help.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.microblink.help.fragments.HelpFragment;

public class BlinkOCRAdapter extends FragmentPagerAdapter {

    private int[] mHelpMessages = null;
    private int[] mHelpImages = null;
    private int mSwipeMsgId;
    private int mSize = 2;

    public BlinkOCRAdapter(FragmentManager fm, Context context) {
        super(fm);
        // dynamically load resources, as they might not exist
        mHelpMessages = new int[mSize];
        mHelpImages = new int[mSize];
        String pkg = context.getPackageName();
        Resources res = context.getResources();
        mHelpMessages[0] = res.getIdentifier("help_msg_01", "string", pkg);
        mHelpMessages[1] = res.getIdentifier("help_msg_02", "string", pkg);

        mSwipeMsgId = res.getIdentifier("PhotoPaySwipeMessage", "string", pkg);

        mHelpImages[0] = res.getIdentifier("help01", "drawable", pkg);
        mHelpImages[1] = res.getIdentifier("help02", "drawable", pkg);
    }

    @Override
    public Fragment getItem(int arg0) {
        int index = arg0 % getCount();
        return HelpFragment.newInstance(mHelpMessages[index], mHelpImages[index], index == 0 ? mSwipeMsgId : 0);
    }

    @Override
    public int getCount() {
        return mSize;
    }

}
