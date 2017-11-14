package com.ifchan.music;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.ifchan.music.adapter.MusicListAdapter;
import com.ifchan.music.entity.Music;

import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.FACTORY_TEST;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static com.ifchan.music.adapter.MusicListAdapter.INTENT_PLAY_NEW;
import static com.ifchan.music.adapter.MusicListAdapter.INTENT_POSITION;
import static com.ifchan.music.adapter.MusicListAdapter.INTENT_TO_REMOVE_MUSIC;

public class MainActivity extends AppCompatActivity implements ViewPager
        .OnPageChangeListener {
    public static final String INTENT_TO_MUSICLIST_ACTIVITY_MUSICLIST =
            "INTENT_TO_MUSICLIST_ACTIVITY_MUSICLIST";
    public final static String INTENT_MEDIA = "MEDIA";
    private final String TAG = "@vir MainActivity";
    private ArrayList<Music> mMusicList = new ArrayList<>();
    private Toolbar mToolbar;
    ViewPager pager;
    private ImageView mImageView;
    private List<View> viewContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getPermission();
        initToolbar();
        initViewPager();
        initBroadcastReceiver();
        setPlayerListener();
    }

    private void setPlayerListener() {

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
        pager.setCurrentItem(2,false);

    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        if (positionOffset == 0.0) {
            if (position == 0) {
                //动画会跳动
                pager.setCurrentItem(1,false);
            } else if (position == 2) {
                pager.setCurrentItem(1,false);
            }
        }
    }

    @Override
    public void onPageSelected(int position) {

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
                // TODO: 11/14/17 stop play this music 
            }
        }, new IntentFilter(INTENT_TO_REMOVE_MUSIC));
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int playNewPosition = intent.getIntExtra(INTENT_POSITION, 0);
            }
        },new IntentFilter(INTENT_PLAY_NEW));
    }
}
