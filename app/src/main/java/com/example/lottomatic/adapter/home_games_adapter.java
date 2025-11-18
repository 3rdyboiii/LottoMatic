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

        // Set basic item data
        holder.image.setImageResource(item.getImageResId());
        holder.drawTxt.setText(item.getDraw());
        holder.prizeTxt.setText("₱" + item.getPrize());

        // Stop previous countdown for this position
        if (position < countdownManagers.size() && countdownManagers.get(position) != null) {
            countdownManagers.get(position).stopCountdown();
        }

        // Create and start new countdown
        DrawCountdownManager countdownManager = new DrawCountdownManager(
                holder.timeTxt, item.getDraw()
        );

        countdownManager.setOnCountdownListener(() -> {
            // Disable item when draw time is reached
            holder.itemView.setEnabled(false);
            holder.itemView.setAlpha(0.5f);
        });

        // Store manager and start countdown
        if (position < countdownManagers.size()) {
            countdownManagers.set(position, countdownManager);
        } else {
            countdownManagers.add(countdownManager);
        }

        countdownManager.startCountdown();

        // Set initial enabled state
        boolean enabled = !countdownManager.isDrawTime();
        holder.itemView.setEnabled(enabled);
        holder.itemView.setAlpha(enabled ? 1.0f : 0.5f);

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (holder.itemView.isEnabled() && onItemClickListener != null) {
                onItemClickListener.onItemClick(position, item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
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