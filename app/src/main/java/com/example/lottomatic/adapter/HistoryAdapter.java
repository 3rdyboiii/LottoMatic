package com.example.lottomatic.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.lottomatic.R;
import com.example.lottomatic.items.HistoryItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private Context context;
    private List<HistoryItem> itemList;

    public HistoryAdapter(Context context, List<HistoryItem> itemList) {
        this.context = context;
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = itemList.get(position);

        // Set game icon based on game type
        setGameIcon(holder.gameIcon, item.getGame());
        holder.drawText.setText(item.getDraw() + " DRAW");
        holder.typeValue.setText(item.getGame());
        holder.comboValue.setText(item.getCombo());
        holder.resultValue.setText(item.getResult() != null ? item.getResult() : "Pending");
        holder.betsValue.setText(item.getBets());
        holder.prizeValue.setText(item.getPrize());
        holder.transcodeValue.setText(item.getTranscode());
        holder.dateValue.setText(formatDate(item.getDate()));
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public void updateList(List<HistoryItem> newList) {
        itemList = newList;
        notifyDataSetChanged();
    }

    private void setGameIcon(ImageView imageView, String game) {
        int iconResId;
        switch (game.toLowerCase()) {
            case "4d":
                iconResId = R.drawable.icon_4d;
                break;
            case "rambolito 3":
                iconResId = R.drawable.icon_3d;
                break;
            case "standard":
                iconResId = R.drawable.icon_3d;
                break;
            case "2d":
                iconResId = R.drawable.icon_2d;
                break;
            default:
                iconResId = R.drawable.pcsologo;
                break;
        }
        imageView.setImageResource(iconResId);
    }

    private String formatDate(String dateString) {
        try {
            // Parse the SQL datetime format and format it nicely
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd, yyyy - h:mm a", java.util.Locale.getDefault());

            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateString; // Return original if parsing fails
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView gameIcon;
        TextView drawText;
        TextView typeValue;
        TextView comboValue;
        TextView resultValue;
        TextView betsValue;
        TextView prizeValue;
        TextView transcodeValue;
        TextView dateValue;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            gameIcon = itemView.findViewById(R.id.gameIcon);
            drawText = itemView.findViewById(R.id.drawText);
            typeValue = itemView.findViewById(R.id.typeValue);
            comboValue = itemView.findViewById(R.id.comboValue);
            resultValue = itemView.findViewById(R.id.resultValue);
            betsValue = itemView.findViewById(R.id.betsValue);
            prizeValue = itemView.findViewById(R.id.prizeValue);
            transcodeValue = itemView.findViewById(R.id.transcodeValue);
            dateValue = itemView.findViewById(R.id.dateValue);
        }
    }
}