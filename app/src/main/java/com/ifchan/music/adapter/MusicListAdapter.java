package com.ifchan.music.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.ifchan.music.R;
import com.ifchan.music.entity.Music;

import java.util.List;

/**
 * Created by daily on 11/14/17.
 */

public class MusicListAdapter extends RecyclerView.Adapter<MusicListAdapter.ViewHolder> {
    public static final String INTENT_PLAY_NEW = "INTENT_PLAY_NEW";
    public static final String INTENT_POSITION = "INTENT_POSITION";
    public static final String INTENT_TO_REMOVE_MUSIC = "INTENT_TO_REMOVE_MUSIC";
    public static final String REMOVE_POSITION = "REMOVE_POSITION";
    private List<Music> mMusicList;
    private Context mContext;

    public MusicListAdapter(List<Music> musicList, Context context) {
        this.mMusicList = musicList;
        mContext = context;
    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.music_item, parent
                , false);
        final ViewHolder holder = new ViewHolder(view);
        holder.musicView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentPlayNew = new Intent(INTENT_PLAY_NEW);
                intentPlayNew.putExtra(INTENT_POSITION, holder.getAdapterPosition());
                //may be wrong
                assert (mMusicList.get(holder.getAdapterPosition())!=null);
                mContext.sendBroadcast(intentPlayNew);
            }
        });
        holder.removeMusicItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentToMainActivityForRemove = new Intent(INTENT_TO_REMOVE_MUSIC);
                intentToMainActivityForRemove.putExtra(REMOVE_POSITION, holder.getAdapterPosition());
                mContext.sendBroadcast(intentToMainActivityForRemove);
                mMusicList.remove(holder.getAdapterPosition());
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Music music = mMusicList.get(position);
        holder.name.setText(music.getName());
        holder.singer.setText(music.getSinger());
    }

    @Override
    public int getItemCount() {
        return mMusicList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView singer;
        View musicView;
        ImageButton removeMusicItem;

        public ViewHolder(View itemView) {
            super(itemView);
            musicView = itemView;
            name = itemView.findViewById(R.id.music_item_name);
            singer = itemView.findViewById(R.id.music_item_singer);
            removeMusicItem = itemView.findViewById(R.id.remove_music_item);
        }
    }
}
