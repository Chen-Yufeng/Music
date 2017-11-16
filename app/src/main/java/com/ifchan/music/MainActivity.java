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
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.ifchan.music.adapter.LrcRecyclerViewAdapter;
import com.ifchan.music.adapter.MusicListAdapter;
import com.ifchan.music.entity.LrcLine;
import com.ifchan.music.entity.Music;
import com.ifchan.music.helper.DataBaseHelper;
import com.ifchan.music.helper.LrcHelper;
import com.ifchan.music.manager.CenterLayoutManager;
import com.ifchan.music.service.MediaPlayerService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static com.ifchan.music.adapter.LrcRecyclerViewAdapter.INTENT_CLICK_POSITION;
import static com.ifchan.music.adapter.MusicListAdapter.INTENT_PLAY_NEW;
import static com.ifchan.music.adapter.MusicListAdapter.INTENT_POSITION;
import static com.ifchan.music.adapter.MusicListAdapter.INTENT_TO_REMOVE_MUSIC;
import static com.ifchan.music.service.MediaPlayerService.ACTION_NEXT;
import static com.ifchan.music.service.MediaPlayerService.ACTION_PAUSE;
import static com.ifchan.music.service.MediaPlayerService.ACTION_PLAY;
import static com.ifchan.music.service.MediaPlayerService.ACTION_PREVIOUS;
import static com.ifchan.music.service.MediaPlayerService.INTENT_RENEW_MAIN_ACTIVITY_LRC;
import static com.ifchan.music.service.MediaPlayerService.MODE_INITIALIZE_OR_PLAY_NEW;

public class MainActivity extends AppCompatActivity implements ViewPager
        .OnPageChangeListener {
    public static final String INTENT_TO_MUSICLIST_ACTIVITY_MUSICLIST =
            "INTENT_TO_MUSICLIST_ACTIVITY_MUSICLIST";
    public final static String INTENT_MEDIA = "MEDIA";
    private final String TAG = "@vir MainActivity";
    private boolean isPlaying = false;
    private int nowPosition;
    private int nowLrcPosition = 0;
    private long lrcTime = 0;
    private DataBaseHelper mDataBaseHelper;
    private SQLiteDatabase db;
    private ArrayList<Music> mMusicList = new ArrayList<>();
    private List<LrcLine> mLrcLineList;
    private List<LrcLine> originalLrcList;
    private Toolbar mToolbar;
    private CardView mCardView;
    private ImageView mPointer;
    private ImageView mDisk;
    private RecyclerView lrcRecyclerView;
    private CenterLayoutManager mCenterLayoutManager;
    private LrcRecyclerViewAdapter lrcListAdapter;
    private boolean autoMove = false;
    ViewPager pager;
    private SeekBar mSeekBar;
    ImageButton imageButtonPlayOrStop;
    private List<View> viewContainer;

    private boolean serviceBound = false;
    private MediaPlayerService player;
    private int playingFlag = 0;
    private LrcRecyclerViewBroadCastReceiver mLrcRecyclerViewBroadCastReceiver;
    private RenewLrcBroadcastReceiver renewLrcBroadcastReceiver;

    private Animation animation_off_to_on;
    private Animation animation_on_to_off;


    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        private long time = System.currentTimeMillis();

        @Override
        public void handleMessage(Message msg) {
            if (System.currentTimeMillis() >= time + 1000) {
                time = System.currentTimeMillis();
                int progress = (int) (msg.what * 10000.0 / player.getDurationInMilliseconds());
                lrcTime = msg.what;
                mSeekBar.setProgress(progress);
                if (mLrcLineList.get(nowLrcPosition).getMillisecond() < lrcTime) {
                    if (nowLrcPosition < mLrcLineList.size() - 1) {
                        while (mLrcLineList.get(nowLrcPosition).getMillisecond() < lrcTime) {
                            nowLrcPosition++;
                        }
                        nowLrcPosition--;
                    }
                } else if (mLrcLineList.get(nowLrcPosition).getMillisecond() > lrcTime) {
                    if (nowLrcPosition > 0) {
                        while (mLrcLineList.get(nowLrcPosition).getMillisecond() > lrcTime) {
                            nowLrcPosition--;
                        }
                    }
                }
                lrcListAdapter.setCurrentMax(nowLrcPosition);
                lrcRecyclerView.smoothScrollToPosition(nowLrcPosition);
//                scrollToCenter(nowLrcPosition);
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
        initAnim();
        initBroadcastReceiver();
        setPlayerListener();
        setSeekBar();
        setRecyclerView();
    }

    private void initAnim() {
        animation_off_to_on = AnimationUtils.loadAnimation(MainActivity.this, R.anim
                .pointer_anim);
        animation_on_to_off = AnimationUtils.loadAnimation(MainActivity.this, R
                .anim.pointer_anim_reset);
        animation_off_to_on.setFillAfter(true);
        animation_on_to_off.setFillAfter(true);
    }

    private void setRecyclerView() {
        if (mLrcLineList == null) {
            originalLrcList = new ArrayList<>();
            originalLrcList.add(new LrcLine(0, "无歌词"));
            mLrcLineList = originalLrcList;
            lrcListAdapter.setLrcLineList(mLrcLineList);
        } else {
            lrcListAdapter.setLrcLineList(mLrcLineList);
            lrcListAdapter.notifyItemRangeInserted(0, mLrcLineList.size());
        }
    }

    private void setLrcLineList() {
        lrcListAdapter.notifyItemRangeRemoved(0, mLrcLineList.size());
        mLrcLineList = new ArrayList<>();
        LrcHelper lrcHelper = new LrcHelper();
        Music music = mMusicList.get(nowPosition);
        String path = music.getPath();
        String title = music.getName();
//        ///sdcard/Download/b.mp3
        String lrcPath = path.substring(0, path.lastIndexOf('/') + 1) + title
                .substring(0, title.lastIndexOf('.') + 1) + "lrc";
        File lrcFile = new File(lrcPath);
        if (lrcFile.exists()) {
            mLrcLineList = lrcHelper.getLrcList(new File(lrcPath));
        } else {
            mLrcLineList = null;
        }
    }

    private void setSeekBar() {
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (player.myIsPlaying()) {
                    int duration = player.getDurationInMilliseconds();
                    int seek = (int) ((seekBar.getProgress() / 10000.0) * duration);
                    player.mySeekTo(seek);
                }
            }
        });
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
        final ImageButton imageButtonPlayMode = findViewById(R.id.play_mode_pic);
        imageButtonPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                playPrevious();
                pager.setCurrentItem(0);
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
//                playNext();
                pager.setCurrentItem(2);
            }
        });
        imageButtonPlayMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (playingFlag) {
                    case 0:
                        playingFlag = 1;
                        player.setPlayMode(1);
                        imageButtonPlayMode.setImageResource(R.drawable.list_random);
                        break;
                    case 1:
                        playingFlag = 2;
                        player.setPlayMode(2);
                        imageButtonPlayMode.setImageResource(R.drawable.list_once);
                        break;
                    case 2:
                        playingFlag = 0;
                        player.setPlayMode(0);
                        imageButtonPlayMode.setImageResource(R.drawable.list_recycle);
                }
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
            setLrcLineList();
            setRecyclerView();
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
            setLrcLineList();
            setRecyclerView();
        }
    }

    private void playOrStop() {
        if (serviceBound) {
            player.pauseOrPlay();
            if (isPlaying) {
                isPlaying = !isPlaying;
                if (mPointer.getVisibility() == View.VISIBLE) {
                    mPointer.startAnimation(animation_on_to_off);
                }
                imageButtonPlayOrStop.setImageResource(R.drawable.play);
            } else {
                isPlaying = !isPlaying;
                if (mPointer.getVisibility() == View.VISIBLE) {
                    mPointer.startAnimation(animation_off_to_on);
                }
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
        mPointer = page2.findViewById(R.id.pointer);
        lrcRecyclerView = page2.findViewById(R.id.lrc_recycler_view);
        mDisk = page2.findViewById(R.id.disk);
        mCardView = page2.findViewById(R.id.disk_album_pic);
        lrcRecyclerView.setVisibility(View.INVISIBLE);
        mCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPointer.clearAnimation();
                if (lrcRecyclerView.getVisibility() == View.INVISIBLE) {
                    lrcRecyclerView.setVisibility(View.VISIBLE);
                    mCardView.setVisibility(View.GONE);
                    mPointer.clearAnimation();
                    mPointer.setVisibility(View.INVISIBLE);
                } else {
                    lrcRecyclerView.setVisibility(View.INVISIBLE);
                    mCardView.setVisibility(View.VISIBLE);
                    mPointer.setVisibility(View.VISIBLE);
                }
            }
        });

        mCenterLayoutManager = new CenterLayoutManager(this);
        lrcRecyclerView.setLayoutManager(mCenterLayoutManager);
        lrcListAdapter = new LrcRecyclerViewAdapter(MainActivity.this, originalLrcList);
        lrcRecyclerView.setAdapter(lrcListAdapter);
        // TODO: 11/16/17 handle conflict!
//        lrcRecyclerView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (lrcRecyclerView.getVisibility() == View.INVISIBLE) {
//                    lrcRecyclerView.setVisibility(View.VISIBLE);
//                    mCardView.setVisibility(View.GONE);
//                    mPointer.clearAnimation();
//                    mPointer.setVisibility(View.INVISIBLE);
//                } else {
//                    lrcRecyclerView.setVisibility(View.INVISIBLE);
//                    mCardView.setVisibility(View.VISIBLE);
//                    mPointer.setVisibility(View.VISIBLE);
//                }
//            }
//        });
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
        if (mPointer.getVisibility() == View.VISIBLE) {
            if (state == ViewPager.SCROLL_STATE_DRAGGING) {
                if (isPlaying) {
                    mPointer.startAnimation(animation_on_to_off);
                }
            } else if (state == ViewPager.SCROLL_STATE_IDLE) {
                if (isPlaying) {
                    mPointer.startAnimation(animation_off_to_on);
                }
            }
        }
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

        mLrcRecyclerViewBroadCastReceiver = new
                LrcRecyclerViewBroadCastReceiver();
        IntentFilter intentFilterForLrcItem = new IntentFilter(INTENT_CLICK_POSITION);
        registerReceiver(mLrcRecyclerViewBroadCastReceiver, intentFilterForLrcItem);

        IntentFilter intentFilterForRenewLrc = new IntentFilter(INTENT_RENEW_MAIN_ACTIVITY_LRC);
        renewLrcBroadcastReceiver = new RenewLrcBroadcastReceiver();
        registerReceiver(renewLrcBroadcastReceiver, intentFilterForRenewLrc);
    }

    class LrcRecyclerViewBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
//            int clickPosition = intent.getIntExtra(INTENT_CLICK_POSITION_VALUE,0);
            if (lrcRecyclerView.getVisibility() == View.INVISIBLE) {
                lrcRecyclerView.setVisibility(View.VISIBLE);
                mCardView.setVisibility(View.GONE);
                mPointer.clearAnimation();
                mPointer.setVisibility(View.INVISIBLE);
            } else {
                lrcRecyclerView.setVisibility(View.INVISIBLE);
                mCardView.setVisibility(View.VISIBLE);
                mPointer.setVisibility(View.VISIBLE);
            }
        }
    }

    class RenewLrcBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            setLrcLineList();
            setRecyclerView();
        }

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
        unregisterReceiver(mLrcRecyclerViewBroadCastReceiver);
        unregisterReceiver(renewLrcBroadcastReceiver);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;
            connectSeekBarAndHandle();
//            Toast.makeText(MainActivity.this, "Service Bound", Toast.LENGTH_SHORT)
//                    .show();
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
            playNewIntent.putExtra(MODE_INITIALIZE_OR_PLAY_NEW, 2);
            playNewIntent.putExtra("position", position);
            sendBroadcast(playNewIntent);
            // TODO: 11/15/17 may need call  connectSeekBarAndHandle()
        }
        imageButtonPlayOrStop.setImageResource(R.drawable.stop);
        if (!isPlaying) {
            mPointer.startAnimation(animation_off_to_on);
        }
        isPlaying = true;
        setLrcLineList();
        setRecyclerView();
    }

    private void syncMusicList() {
        Intent playNewIntent = new Intent(MediaPlayerService.INITIALIZE_OR_PLAY_NEW);
        playNewIntent.putExtra(INTENT_MEDIA, mMusicList);
        playNewIntent.putExtra(MODE_INITIALIZE_OR_PLAY_NEW, 1);
        sendBroadcast(playNewIntent);
    }

    private void scrollToCenter(int position) {

//        int firstItem = mCenterLayoutManager.findFirstVisibleItemPosition();
//        int lastItem = mCenterLayoutManager.findLastVisibleItemPosition();
//        int lengthAll = lrcRecyclerView.getChildAt(lastItem).getTop();
//        int lengthEach = (int) (lengthAll / (float)(lastItem - firstItem));
//        int medium = (firstItem + lastItem);
//        if (position <= firstItem) {
//            lrcRecyclerView.smoothScrollToPosition(position);
//            autoMove = true;
//        } else if (position <= lastItem) {
//            int top = lrcRecyclerView.getChildAt(position - firstItem).getTop();
//            lrcRecyclerView.smoothScrollBy(0, top);
//        } else {
//            lrcRecyclerView.smoothScrollToPosition(position);
//            autoMove = true;
//        }
//        lrcRecyclerView.smoothScrollBy(0,(position - medium) * lengthEach);
    }
}
