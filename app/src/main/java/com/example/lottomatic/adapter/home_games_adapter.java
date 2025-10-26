package com.example.lottomatic.adapter;

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
import java.util.ArrayList;
import java.util.List;

public class home_games_adapter extends RecyclerView.Adapter<home_games_adapter.ViewHolder> {
    private List<MenuItem> items;
    private List<DrawCountdownManager> countdownManagers;
    private OnItemClickListener onItemClickListener;

    // Interface for click listener
    public interface OnItemClickListener {
        void onItemClick(int position, MenuItem item);
    }

    public home_games_adapter(List<MenuItem> items) {
        this.items = items;
        this.countdownManagers = new ArrayList<>();
    }

    // Set the click listener
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
        MenuItem entryItem = items.get(position);
        holder.image.setImageResource(entryItem.getImageResId());
        holder.drawTxt.setText(entryItem.getDraw());
        holder.prizeTxt.setText("₱" + entryItem.getPrize());

        // Initialize or update countdown manager
        if (position < countdownManagers.size() && countdownManagers.get(position) != null) {
            // Stop existing countdown
            countdownManagers.get(position).stopCountdown();
        }

        // Create new countdown manager
        DrawCountdownManager countdownManager = new DrawCountdownManager(
                holder.timeTxt, holder.drawTxt, entryItem.getDraw()
        );
        countdownManager.startCountdown();

        // Store reference
        if (position < countdownManagers.size()) {
            countdownManagers.set(position, countdownManager);
        } else {
            countdownManagers.add(countdownManager);
        }

        // Set click listener for the entire item view
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(position, entryItem);
                }
            }
        });

        // Optional: Add visual feedback for clickable items
        holder.itemView.setClickable(true);
        holder.itemView.setFocusable(true);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public MenuItem getItem(int position) {
        return items.get(position);
    }

    public void setItems(List<MenuItem> items) {
        // Stop all existing countdowns
        stopAllCountdowns();

        this.items = items;
        this.countdownManagers = new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addItem(MenuItem item) {
        items.add(item);
        notifyItemInserted(items.size() - 1);
    }

    public void stopAllCountdowns() {
        for (DrawCountdownManager manager : countdownManagers) {
            if (manager != null) {
                manager.stopCountdown();
            }
        }
        countdownManagers.clear();
    }

    public void startAllCountdowns() {
        // This will be handled automatically in onBindViewHolder
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView image;
        public TextView drawTxt;
        public TextView timeTxt;
        public TextView prizeTxt;

        public ViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image);
            drawTxt = itemView.findViewById(R.id.drawTxt);
            timeTxt = itemView.findViewById(R.id.timeTxt);
            prizeTxt = itemView.findViewById(R.id.prizeTxt);
        }
    }
}