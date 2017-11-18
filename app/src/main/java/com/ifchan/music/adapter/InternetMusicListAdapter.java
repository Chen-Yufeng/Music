package com.ifchan.music.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ifchan.music.R;
import com.ifchan.music.entity.InternetMusic;
import com.ifchan.music.entity.Music;

import java.util.List;

/**
 * Created by daily on 11/14/17.
 */

public class InternetMusicListAdapter extends RecyclerView.Adapter<InternetMusicListAdapter.ViewHolder> {

    public static final String INTENT_CHOOSE_INTERNET_MUSIC = "INTENT_CHOOSE_INTERNET_MUSIC";
    public static final String INTENT_CHOOSE_INTERNET_MUSIC_POSITION =
            "INTENT_CHOOSE_INTERNET_MUSIC_POSITION";
    private List<InternetMusic> mInternetMusicList;
    private Context mContext;

    public InternetMusicListAdapter(List<InternetMusic> internetMusicList, Context context) {
        this.mInternetMusicList = internetMusicList;
        mContext = context;
    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.internet_music_item, parent
                , false);
        final ViewHolder holder = new ViewHolder(view);
        holder.musicView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(INTENT_CHOOSE_INTERNET_MUSIC);
                intent.putExtra(INTENT_CHOOSE_INTERNET_MUSIC_POSITION, holder.getAdapterPosition());
                mContext.sendBroadcast(intent);
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        InternetMusic music = mInternetMusicList.get(position);
        holder.name.setText(music.getSongname());
        holder.singer.setText(music.getArtistname());
    }

    @Override
    public int getItemCount() {
        return mInternetMusicList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView singer;
        View musicView;

        public ViewHolder(View itemView) {
            super(itemView);
            musicView = itemView;
            name = itemView.findViewById(R.id.internet_music_info_name);
            singer = itemView.findViewById(R.id.internet_music_info_artist);
        }
    }
}
