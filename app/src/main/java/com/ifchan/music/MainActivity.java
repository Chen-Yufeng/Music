package com.ifchan.music;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.ifchan.music.adapter.MusicListAdapter;
import com.ifchan.music.entity.Music;
import com.ifchan.music.helper.DataBaseHelper;
import com.ifchan.music.service.MediaPlayerService;

import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static com.ifchan.music.adapter.MusicListAdapter.INTENT_PLAY_NEW;
import static com.ifchan.music.adapter.MusicListAdapter.INTENT_POSITION;
import static com.ifchan.music.adapter.MusicListAdapter.INTENT_TO_REMOVE_MUSIC;
import static com.ifchan.music.service.MediaPlayerService.ACTION_NEXT;
import static com.ifchan.music.service.MediaPlayerService.ACTION_PAUSE;
import static com.ifchan.music.service.MediaPlayerService.ACTION_PLAY;
import static com.ifchan.music.service.MediaPlayerService.ACTION_PREVIOUS;
import static com.ifchan.music.service.MediaPlayerService.MODE_INITIALIZE_OR_PLAY_NEW;

public class MainActivity extends AppCompatActivity implements ViewPager
        .OnPageChangeListener {
    public static final String INTENT_TO_MUSICLIST_ACTIVITY_MUSICLIST =
            "INTENT_TO_MUSICLIST_ACTIVITY_MUSICLIST";
    public final static String INTENT_MEDIA = "MEDIA";
    private final String TAG = "@vir MainActivity";
    private boolean isPlaying = false;
    private int nowPosition;
    private DataBaseHelper mDataBaseHelper;
    private SQLiteDatabase db;
    private ArrayList<Music> mMusicList = new ArrayList<>();
    private Toolbar mToolbar;
    ViewPager pager;
    private ImageView mImageView;
    private SeekBar mSeekBar;
    ImageButton imageButtonPlayOrStop;
    private List<View> viewContainer;

    private boolean serviceBound = false;
    private MediaPlayerService player;
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        private long time = System.currentTimeMillis();

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: msg.what=" + msg.what);
            if (System.currentTimeMillis() >= time + 1000) {
                time = System.currentTimeMillis();
                int progress = (int) (msg.what * 10000.0 / player.getDurationInMilliseconds());
                mSeekBar.setProgress(progress);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getPermission();
        initDataBase();
        loadFromDataBase();
        initToolbar();
        initBasicItems();
        initViewPager();
        initBroadcastReceiver();
        setPlayerListener();

//        test();
    }


    private void initDataBase() {
        mDataBaseHelper = DataBaseHelper.getInstance(MainActivity.this);
        db = mDataBaseHelper.getWritableDatabase();
    }

    private void loadFromDataBase() {
        Cursor cursor = db.query("Musics", null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndex("name"));
                String singer = cursor.getString(cursor.getColumnIndex("singer"));
                String path = cursor.getString(cursor.getColumnIndex("path"));
                Music music = new Music(name, singer, path);
                mMusicList.add(music);
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    private void initBasicItems() {
        mSeekBar = findViewById(R.id.seek_bar);
    }

    private void connectSeekBarAndHandle() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (player.myIsPlaying() && isPlaying) {
                        Message mMessage = new Message();
                        mMessage.what = player.getCurrentPosition();
                        mHandler.sendMessage(mMessage);
                    } else if (!isPlaying) {
                        break;
                    }
                }
            }
        }).start();
    }


    private void setPlayerListener() {
        ImageButton imageButtonPrevious = findViewById(R.id.image_button_previous);
        imageButtonPlayOrStop = findViewById(R.id.image_button_play_or_stop);
        ImageButton imageButtonNext = findViewById(R.id.image_button_next);
        imageButtonPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrevious();
            }
        });
        imageButtonPlayOrStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playOrStop();
            }
        });
        imageButtonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               playNext();
            }
        });
    }

    private void playPrevious() {
        if (serviceBound) {
            connectSeekBarAndHandle();
            player.skipToPrevious();
            isPlaying = true;
            if (nowPosition == 0) {
                nowPosition = mMusicList.size() - 1;
            } else {
                nowPosition--;
            }
            renewToolBar(nowPosition);
        }
    }

    private void playNext() {
        if (serviceBound) {
            connectSeekBarAndHandle();
            player.skipToNext();
            isPlaying = true;
            if (nowPosition == mMusicList.size() - 1) {
                nowPosition = 0;
            } else {
                nowPosition++;
            }
            renewToolBar(nowPosition);
        }
    }

    private void playOrStop() {
        if (serviceBound) {
            player.pauseOrPlay();
            if (isPlaying) {
                isPlaying = !isPlaying;
                imageButtonPlayOrStop.setImageResource(R.drawable.play);
            } else {
                isPlaying = !isPlaying;
                connectSeekBarAndHandle();
                imageButtonPlayOrStop.setImageResource(R.drawable.stop);
            }
        }
    }

    private void getPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new
                    String[]{READ_EXTERNAL_STORAGE}, 1);
        }
    }

    private void initToolbar() {
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
//        mToolbar.inflateMenu(R.menu.menu_main);
//        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
//
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//                Log.d(TAG, "onMenuItemClick: ");
//                switch (item.getItemId()) {
//                    case R.id.menu_music_list:
//                        Intent intentToMusicList = new Intent(MainActivity.this, MusicList.class);
//                        startActivity(intentToMusicList);
//                        break;
//                }
//                return true;
//            }
//        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.menu_music_list:
                Intent intentToMusicList = new Intent(MainActivity.this, MusicList.class);
                intentToMusicList.putExtra(INTENT_TO_MUSICLIST_ACTIVITY_MUSICLIST, mMusicList);
                startActivity(intentToMusicList);
                break;
            case R.id.menu_add_music_from_file:
                // TODO: 11/14/17 complete it
                performFileSearch();
                break;
            case R.id.clear_data_base:
                mDataBaseHelper.removeAllColumns(db);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.menu_music_list:
//                Intent intentToMusicList = new Intent(MainActivity.this, MusicList.class);
//                startActivity(intentToMusicList);
//                return true;
//            default:
//                return super.onOptionsItemSelected(item);
//
//        }
//    }

    private void initViewPager() {
        LayoutInflater layoutInflater = getLayoutInflater().from(MainActivity.this);
        View page1 = layoutInflater.inflate(R.layout.page1, null);
        View page2 = layoutInflater.inflate(R.layout.page2, null);
        View page3 = layoutInflater.inflate(R.layout.page3, null);
        mImageView = page2.findViewById(R.id.pointer);
        viewContainer = new ArrayList<>();
        viewContainer.add(page1);
        viewContainer.add(page2);
        viewContainer.add(page3);
        pager = findViewById(R.id.viewpager);
        pager.setOnPageChangeListener(this);
        pager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return viewContainer.size();
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                container.addView(viewContainer.get(position));
                return viewContainer.get(position);
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
//                super.destroyItem(container, position, object);  //may be wrong!!!
                container.removeView(viewContainer.get(position));
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }
        });
        pager.setCurrentItem(1, false);

    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        if (positionOffset == 0.0) {
            if (position == 0) {
                pager.setCurrentItem(1, false);
            } else if (position == 2) {
                pager.setCurrentItem(1, false);
            }
        }
    }

    @Override
    public void onPageSelected(int position) {
        if (position == 0) {
//            sendBroadcast(playbackAction(3));
            playPrevious();
        } else if (position == 2) {
//            sendBroadcast(playbackAction(2));
            playNext();
        }

    }


    private static Intent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent();
        switch (actionNumber) {
            case 0:
                // Play
                playbackAction.setAction(ACTION_PLAY);
                playbackAction.putExtra("todo", 0);
                return playbackAction;
            case 1:
                // Pause
                playbackAction.setAction(ACTION_PAUSE);
                playbackAction.putExtra("todo", 1);
                return playbackAction;
            case 2:
                // Next track
                playbackAction.setAction(ACTION_NEXT);
                playbackAction.putExtra("todo", 2);
                return playbackAction;
            case 3:
                // Previous track
                playbackAction.setAction(ACTION_PREVIOUS);
                playbackAction.putExtra("todo", 3);
                return playbackAction;
            default:
                break;
        }
        return null;

    }

    @Override
    public void onPageScrollStateChanged(int state) {
//        Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim
//                .pointer_anim);
//        Animation animationReset = AnimationUtils.loadAnimation(MainActivity.this, R.anim
//                .pointer_anim_reset);
//        animation.setFillAfter(true);
//        animationReset.setFillAfter(true);
//        if (state == ViewPager.SCROLL_STATE_DRAGGING) {
//            mImageView.startAnimation(animation);
//        } else if (state == ViewPager.SCROLL_STATE_IDLE) {
//            mImageView.startAnimation(animationReset);
//        }
    }

    private static final int READ_REQUEST_CODE = 42;

    /**
     * Fires an intent to spin up the "file chooser" UI and select an image.
     */
    public void performFileSearch() {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("file/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        //TODO handle your request here
        switch (requestCode) {
            case READ_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    Music music = new Music();
                    String path = resultData.getDataString();
                    path = path.substring(path.indexOf('/') + 2);
                    music.setName(path.substring(path.lastIndexOf('/') + 1));
                    music.setPath(path);
                    music.setSinger("singer");
                    mMusicList.add(music);
                    mDataBaseHelper.addMusic(mMusicList.size(), music, db);
                    syncMusicList();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, resultData);
    }

    private void initBroadcastReceiver() {
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int position = intent.getIntExtra(MusicListAdapter.REMOVE_POSITION, 0);
                mMusicList.remove(position);
                syncMusicList();
                if (position <= nowPosition && position != 0) {
                    nowPosition--;
                }
                mDataBaseHelper.delMusic(position + 1, db);
                // TODO: 11/14/17 stop play this music
            }
        }, new IntentFilter(INTENT_TO_REMOVE_MUSIC));
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                nowPosition = intent.getIntExtra(INTENT_POSITION, 0);
                playMusic(nowPosition);
                renewToolBar(nowPosition);
            }
        }, new IntentFilter(INTENT_PLAY_NEW));
    }

    private void renewToolBar(int position) {
        mToolbar.setTitle(mMusicList.get(position).getName());
        mToolbar.setSubtitle(mMusicList.get(position).getSinger());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("ServiceState", serviceBound);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
        if (serviceBound) {
            unbindService(serviceConnection);
            player.stopSelf();
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;
            connectSeekBarAndHandle();
            Toast.makeText(MainActivity.this, "Service Bound", Toast.LENGTH_SHORT)
                    .show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    private void playMusic(int position) {
        if (!serviceBound) {
            Intent playerIntent = new Intent(this, MediaPlayerService.class);
            playerIntent.putExtra(INTENT_MEDIA, mMusicList);
            playerIntent.putExtra("position", position);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            //Service is active
            //Send media with BroadcastReceiver
            Intent playNewIntent = new Intent(MediaPlayerService.INITIALIZE_OR_PLAY_NEW);
            playNewIntent.putExtra(INTENT_MEDIA, mMusicList);
            playNewIntent.putExtra(MODE_INITIALIZE_OR_PLAY_NEW,2);
            playNewIntent.putExtra("position", position);
            sendBroadcast(playNewIntent);
            // TODO: 11/15/17 may need call  connectSeekBarAndHandle()
        }
        imageButtonPlayOrStop.setImageResource(R.drawable.stop);
        isPlaying = true;
//        LrcHelper lrcHelper = new LrcHelper();
//        Music music = audioList.get(position);
//        String path = music.getPath();
//        String title = music.getTitle();
//        ///sdcard/Download/b.mp3
//        String lrcPath = path.substring(0, path.lastIndexOf('/') + 1) + title
//                .substring(0, title.lastIndexOf('.') + 1) + "lrc";
//        mLrcLineList = lrcHelper.getLrcList(new File(lrcPath));
//        mAudioInfo = new AudioInfo();
//        mAudioInfo.setAlbum(lrcHelper.getAlbum());
//        mAudioInfo.setName(lrcHelper.getArtist());
//        mAudioInfo.setTitle(lrcHelper.getTitle());
    }

    private void syncMusicList() {
        Intent playNewIntent = new Intent(MediaPlayerService.INITIALIZE_OR_PLAY_NEW);
        playNewIntent.putExtra(INTENT_MEDIA, mMusicList);
        playNewIntent.putExtra(MODE_INITIALIZE_OR_PLAY_NEW,1);
        sendBroadcast(playNewIntent);
    }
}
