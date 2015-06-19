package com.nuvyyo.tabloplayer.util;

/**
 * Created by mike on 05/06/15.
 */
public class MutableBoolean  {
    private boolean mValue;

    public MutableBoolean() {
    }

    public MutableBoolean(boolean b) {
        mValue = b;
    }

    public boolean getValue() {
        return mValue;
    }

    public void setValue(boolean b) {
        mValue = b;
    }
}
