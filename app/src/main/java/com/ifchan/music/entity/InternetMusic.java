package com.ifchan.music.entity;

import java.io.Serializable;

/**
 * Created by daily on 11/17/17.
 */

public class InternetMusic implements Serializable {
    private String songname;
    private String songid;
    private String artistname;
    private String albumName;
    private String songPicBig;
    private String lrcLink;
    private String songLink;
    private String format;

    public InternetMusic() {
    }

    public String getSongname() {
        return songname;
    }

    public void setSongname(String songname) {
        this.songname = songname;
    }

    public String getSongid() {
        return songid;
    }

    public void setSongid(String songid) {
        this.songid = songid;
    }

    public String getArtistname() {
        return artistname;
    }

    public void setArtistname(String artistname) {
        this.artistname = artistname;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getSongPicBig() {
        return songPicBig;
    }

    public void setSongPicBig(String songPicBig) {
        this.songPicBig = songPicBig;
    }

    public String getLrcLink() {
        return lrcLink;
    }

    public void setLrcLink(String lrcLink) {
        this.lrcLink = lrcLink;
    }

    public String getSongLink() {
        return songLink;
    }

    public void setSongLink(String songLink) {
        this.songLink = songLink;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public String toString() {
        return "InternetMusic{" +
                "songname='" + songname + '\'' +
                ", songid='" + songid + '\'' +
                ", artistname='" + artistname + '\'' +
                ", albumName='" + albumName + '\'' +
                ", songPicBig='" + songPicBig + '\'' +
                ", lrcLink='" + lrcLink + '\'' +
                ", songLink='" + songLink + '\'' +
                ", format='" + format + '\'' +
                '}';
    }
}
