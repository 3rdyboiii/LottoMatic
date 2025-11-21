package com.example.lottomatic.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottomatic.R;
import com.example.lottomatic.items.MenuItem;
import com.example.lottomatic.utility.DrawCountdownManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class home_games_adapter extends RecyclerView.Adapter<home_games_adapter.ViewHolder> {

    private final List<MenuItem> items;
    private final List<DrawCountdownManager> countdownManagers;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(int position, MenuItem item);
    }

    public home_games_adapter(List<MenuItem> items) {
        this.items = items;
        this.countdownManagers = new ArrayList<>();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.home_games_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuItem item = items.get(position);

        holder.image.setImageResource(item.getImageResId());
        holder.drawTxt.setText(item.getDraw());
        holder.prizeTxt.setText("₱" + item.getPrize());

        SharedPreferences prefs = holder.itemView.getContext().getSharedPreferences("draw_prefs", Context.MODE_PRIVATE);
        String key = item.getGame() + "_" + item.getDraw() + "_disabled_date";
        String disabledDate = prefs.getString(key, null);
        String today = getToday();

        boolean isEnabled = !(disabledDate != null && disabledDate.equals(today));
        holder.itemView.setEnabled(isEnabled);
        holder.itemView.setAlpha(isEnabled ? 1f : 0.5f);

        // Stop old countdown if exists
        if (position < countdownManagers.size() && countdownManagers.get(position) != null) {
            countdownManagers.get(position).stopCountdown();
        }

        // In onBindViewHolder
        DrawCountdownManager countdownManager = new DrawCountdownManager(holder.timeTxt, item.getDrawTimestamp());
        countdownManager.setOnCountdownListener(() -> disableItemForToday(holder, item));

        if (position < countdownManagers.size()) {
            countdownManagers.set(position, countdownManager);
        } else {
            countdownManagers.add(countdownManager);
        }
        countdownManager.startCountdown();

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            if (holder.itemView.isEnabled() && onItemClickListener != null) {
                onItemClickListener.onItemClick(position, item);
            }
        });
    }

    private String getToday() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private void disableItemForToday(ViewHolder holder, MenuItem item) {
        int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
            holder.itemView.post(() -> {
                holder.itemView.setEnabled(false);
                holder.itemView.setAlpha(0.5f);
            });
        }

        // Save disabled date
        SharedPreferences prefs = holder.itemView.getContext().getSharedPreferences("draw_prefs", Context.MODE_PRIVATE);
        String key = item.getGame() + "_" + item.getDraw() + "_disabled_date";
        prefs.edit().putString(key, getToday()).apply();
    }

    public void stopAllCountdowns() {
        for (DrawCountdownManager manager : countdownManagers) {
            if (manager != null) manager.stopCountdown();
        }
        countdownManagers.clear();
    }

    public void startAllCountdowns() {
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView drawTxt, timeTxt, prizeTxt;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image);
            drawTxt = itemView.findViewById(R.id.drawTxt);
            timeTxt = itemView.findViewById(R.id.timeTxt);
            prizeTxt = itemView.findViewById(R.id.prizeTxt);
        }
    }
}
