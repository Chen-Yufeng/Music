package com.ifchan.music.entity;

import java.io.Serializable;

/**
 * Created by daily on 11/14/17.
 */

public class Music implements Serializable {
    private String name = "";
    private String singer = "";
    private String path = "";

    public Music() {
    }

    public Music(String name, String singer, String path) {
        this.name = name;
        this.singer = singer;
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSinger() {
        return singer;
    }

    public void setSinger(String singer) {
        this.singer = singer;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
