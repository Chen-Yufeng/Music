package com.ifchan.music;

import android.content.Intent;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ViewPager
        .OnPageChangeListener {
    private Toolbar mToolbar;
    ViewPager pager;
    private List<View> viewContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initToolbar();
        initViewPager();
    }

    private void initToolbar() {
        mToolbar = findViewById(R.id.toolbar);
        mToolbar.inflateMenu(R.menu.menu_main);
//        Toolbar.OnMenuItemClickListener onMenuItemClickListener = new Toolbar
//                .OnMenuItemClickListener() {
//
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//                switch (item.getItemId()) {
//                    case R.id.menu_music_list:
//                        Intent intentToMusicList = new Intent(MainActivity.this, MusicList.class);
//                        startActivity(intentToMusicList);
//                        break;
//                }
//                return true;
//            }
//        };
//        mToolbar.setOnMenuItemClickListener(onMenuItemClickListener);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_music_list:
                Intent intentToMusicList = new Intent(MainActivity.this, MusicList.class);
                startActivity(intentToMusicList);
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    private void initViewPager() {
        LayoutInflater layoutInflater = getLayoutInflater().from(MainActivity.this);
        View page2 = layoutInflater.inflate(R.layout.page2, null);
        viewContainer = new ArrayList<>();
        viewContainer.add(page2);
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

    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
