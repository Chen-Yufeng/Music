package com.ifchan.music;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.ifchan.music.adapter.InternetMusicListAdapter;
import com.ifchan.music.entity.InternetMusic;
import com.ifchan.music.entity.Music;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SearchInternetActivity extends AppCompatActivity {
    private final String TAG = "@vir";
    private EditText mSearchEditText;
    private Button mSendButton;
    private RecyclerView mResultRecyclerView;
    private InternetMusicListAdapter mInternetMusicListAdapter;
    private String mName;
    private String mSongsJson;
    private String mSongJson;
    private InternetMusic chosenSong;
    private Music mMusic;
    private List<InternetMusic> mInternetMusicList = new ArrayList<>();
    private ChooseInternetMusicReceiver mReceiver;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    prepareSongs();
                    break;
                case 2:
                    prepareSong();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_internet);

        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private void init() {
        Intent intentReceived = getIntent();
        mMusic = (Music) intentReceived.getSerializableExtra(MainActivity.INTENT_INTERNET_MUSIC);
        mSearchEditText = findViewById(R.id.search_name);
        mSendButton = findViewById(R.id.send_name_to_internet);
        mResultRecyclerView = findViewById(R.id.search_result_recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        mInternetMusicListAdapter = new InternetMusicListAdapter
                (mInternetMusicList, this);
        mResultRecyclerView.setLayoutManager(linearLayoutManager);
        mResultRecyclerView.setAdapter(mInternetMusicListAdapter);

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mSearchEditText.getText().toString().isEmpty()) {
                    mName = mSearchEditText.getText().toString().trim();
                    getSongsJson(mName);
                }
            }
        });

        mReceiver = new ChooseInternetMusicReceiver();
        IntentFilter intentFilter = new IntentFilter(InternetMusicListAdapter
                .INTENT_CHOOSE_INTERNET_MUSIC);
        registerReceiver(mReceiver, intentFilter);
    }

    private void prepareSongs() {
        try {
            JSONObject jsonObject = new JSONObject(mSongsJson);
            JSONArray jsonArray = jsonObject.getJSONArray("song");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject song = jsonArray.getJSONObject(i);
                InternetMusic internetMusic = new InternetMusic();
                internetMusic.setSongname(song.getString("songname"));
                internetMusic.setArtistname(song.getString("artistname"));
                internetMusic.setSongid(song.getString("songid"));
                mInternetMusicList.add(internetMusic);
            }
            if (mInternetMusicListAdapter.getItemCount() != 0) {
                mInternetMusicListAdapter.notifyItemRangeRemoved(0,
                        mInternetMusicListAdapter.getItemCount());
            }
            if (mInternetMusicList.size() >= 2) {
                mInternetMusicListAdapter.notifyItemRangeInserted(0, mInternetMusicList.size
                        () - 1);
            } else if (mInternetMusicList.size() == 1) {
                mInternetMusicListAdapter.notifyItemInserted(0);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {

        }
    }

    private void prepareSong() {
        try {
            JSONObject singleSong = new JSONObject(mSongJson);
            JSONObject songData = singleSong.getJSONObject("data");
            JSONArray songInfoList = songData.getJSONArray("songList");
            JSONObject songInfoListContain = songInfoList.getJSONObject(0);
            chosenSong.setAlbumName(songInfoListContain.getString
                    ("albumName"));
            chosenSong.setSongPicBig(songInfoListContain.getString("songPicBig"));
            chosenSong.setLrcLink(songInfoListContain.getString("lrcLink"));
            chosenSong.setSongLink(songInfoListContain.getString("songLink"));
            chosenSong.setSongLink(chosenSong.getSongLink().substring(0, chosenSong.getSongLink()
                    .lastIndexOf('?')));
            Log.d(TAG, chosenSong.toString());

            getLrcFile();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void getLrcFile() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                URL url;
                InputStream inputStream = null;
                FileOutputStream fileOutputStream = null;
                try {
                    url = new URL(chosenSong.getLrcLink());
                    inputStream = url.openStream();
                    fileOutputStream = new FileOutputStream(mMusic.getPath().substring
                            (0, mMusic.getPath().lastIndexOf('.')) + ".lrc");
                    byte[] buffer = new byte[512];
                    int read;
                    while ((read = inputStream.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, read);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }
//http://music.baidu.com/data/music/links?songIds={544169425}

    private void getSongJson(final String id) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                BufferedReader reader = null;
                try {
                    URL url = new URL("http://music.baidu" +
                            ".com/data/music/links?songIds={" + id + "}");
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);
                    InputStream in = connection.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder response = new StringBuilder();
                    String line;
//                    int ch;
//                    while ((ch = reader.read()) != -1) {
//                        if (ch != '\\' && ch != '(' && ch != ')') {
//                            if (ch == '"') {
//                                response.append('\'');
//                            } else {
//                                char change = (char) ch;
//                                response.append(change);
//                            }
//                        }
//                    }
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    mSongJson = response.toString();
                    mSongJson = mSongJson.replace("\\", "");
//                    mSongJson.replace("\\", "");
//                    mSongJson.replace("\"", "'");
                    Message messageComplete = new Message();
                    messageComplete.what = 2;
                    mHandler.sendMessage(messageComplete);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }

    private void getSongsJson(final String name) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                BufferedReader reader = null;
                try {
                    URL url = new URL("http://tingapi.ting.baidu" +
                            ".com/v1/restserver/ting?from=webapp_music&method=baidu.ting.search" +
                            ".catalogSug&format=json&callback=&query=" + name);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);
                    InputStream in = connection.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
//                    int ch;
//                    while ((ch = reader.read()) != -1) {
//                        if (ch != '\\' && ch != '(' && ch != ')') {
//                            if (ch == '"') {
//                                response.append('\'');
//                            } else {
//                                char change = (char) ch;
//                                response.append(change);
//                            }
//                        }
//                    }
                    mSongsJson = response.toString();
                    mSongsJson = mSongsJson.replace("(", "");
                    mSongsJson = mSongsJson.replace(")", "");
//                    mSongsJson.replace("\\", "");
//                    mSongsJson.replace("\"", "'");
                    Message messageComplete = new Message();
                    messageComplete.what = 1;
                    mHandler.sendMessage(messageComplete);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }

    class ChooseInternetMusicReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            chosenSong = mInternetMusicList.get(intent.getIntExtra(InternetMusicListAdapter
                    .INTENT_CHOOSE_INTERNET_MUSIC_POSITION, -1));
            getSongJson(chosenSong.getSongid());
        }
    }

}
