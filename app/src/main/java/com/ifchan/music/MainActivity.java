package com.ifchan.music;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mToolbar = findViewById(R.id.toolbar);

        initToolbar();
    }

    private void initToolbar() {
        mToolbar.inflateMenu(R.menu.menu_main);
        Toolbar.OnMenuItemClickListener onMenuItemClickListener = new Toolbar.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_music_list:
                        Toast.makeText(MainActivity.this,"Me?",Toast.LENGTH_SHORT);
                        break;
                }
                return true;
            }
        };
        mToolbar.setOnMenuItemClickListener(onMenuItemClickListener);
    }
}
