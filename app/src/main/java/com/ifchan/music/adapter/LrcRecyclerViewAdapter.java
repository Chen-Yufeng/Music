package com.ifchan.music.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ifchan.music.R;
import com.ifchan.music.entity.LrcLine;

import java.util.List;

/**
 * Created by daily on 11/16/17.
 */

public class LrcRecyclerViewAdapter extends RecyclerView.Adapter<LrcRecyclerViewAdapter.ViewHolder>{
    public static final String INTENT_CLICK_POSITION = "INTENT_CLICK_POSITION";
    public static final String INTENT_CLICK_POSITION_VALUE = "INTENT_CLICK_POSITION_VALUE";
    private List<LrcLine> mLrcLineList;
    private int currentMax = -1;
    private Context mContext;

    public LrcRecyclerViewAdapter(Context context, List<LrcLine> lrcLines) {
        mLrcLineList = lrcLines;
        mContext = context;
    }



    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.lrc_item, parent
                , false);
        final ViewHolder holder = new ViewHolder(view);
//        holder.lrcTextView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(INTENT_CLICK_POSITION);
//                intent.putExtra(INTENT_CLICK_POSITION_VALUE,holder.getAdapterPosition());
//                mContext.sendBroadcast(intent);
//            }
//        });
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (position == currentMax) {
            holder.lrcTextView.setTextSize(20);
        }
        holder.lrcTextView.setText(mLrcLineList.get(position).getLrcText());
    }

    @Override
    public int getItemCount() {
        return mLrcLineList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView lrcTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            lrcTextView = itemView.findViewById(R.id.lrc_line);
        }
    }

    public void setCurrentMax(int currentMax) {
        this.currentMax = currentMax;
    }

    public void setLrcLineList(List<LrcLine> lrcLineList) {
        mLrcLineList = lrcLineList;
    }
}
