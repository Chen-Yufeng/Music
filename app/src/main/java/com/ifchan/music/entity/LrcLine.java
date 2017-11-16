package com.ifchan.music.entity;

import java.io.Serializable;

/**
 * Created by daily on 11/8/17.
 */

public class LrcLine implements Serializable{
    private long millisecond;
    private String lrcText;

    public LrcLine(long millisecond, String lrcText) {
        this.millisecond = millisecond;
        this.lrcText = lrcText;
    }

    public long getMillisecond() {
        return millisecond;
    }

    public String getLrcText() {
        return lrcText;
    }
}
