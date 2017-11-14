package com.ifchan.music;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.ifchan.music.adapter.MusicListAdapter;
import com.ifchan.music.entity.Music;

import java.util.ArrayList;

public class MusicList extends AppCompatActivity {
    private Toolbar mToolbar;
    private ArrayList<Music> mMusicList;
    private MusicListAdapter musicListAdapter;
    RemoveBroadcastReceiver receiver;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_list);

        receiveMusicList();
        initToolBar();
        initRecyclerView();
        initBroadcastReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private void initBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter(MusicListAdapter.INTENT_TO_REMOVE_MUSIC);
        receiver = new RemoveBroadcastReceiver();
        registerReceiver(receiver,intentFilter);
    }

    private class RemoveBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int position = intent.getIntExtra(MusicListAdapter.REMOVE_POSITION, 0);
            musicListAdapter.notifyItemRemoved(position);
        }
    }

    private void receiveMusicList() {
        Intent intentReceive = getIntent();
        mMusicList = (ArrayList<Music>) intentReceive.getSerializableExtra(MainActivity
                .INTENT_TO_MUSICLIST_ACTIVITY_MUSICLIST);
    }

    private void initRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.music_list_recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        musicListAdapter = new MusicListAdapter(mMusicList, this);
        recyclerView.setAdapter(musicListAdapter);
    }

    private void initToolBar() {
        mToolbar = findViewById(R.id.toolbar_in_musicList);
        setSupportActionBar(mToolbar);  //maybe wrong
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
