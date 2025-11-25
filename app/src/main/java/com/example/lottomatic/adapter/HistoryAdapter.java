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

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final Context context;
    private List<HistoryItem> itemList;

    private final DecimalFormat currencyFormat = new DecimalFormat("#,##0.00");

    public HistoryAdapter(Context context, List<HistoryItem> itemList) {
        this.context = context;
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.history_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = itemList.get(position);

        // Game icon
        setGameIcon(holder.gameIcon, item.getGame());

        // Text fields
        holder.drawText.setText(item.getDraw() + " DRAW");
        holder.typeValue.setText(item.getType());
        holder.comboValue.setText(item.getCombo());
        holder.resultValue.setText(item.getResult() != null && !item.getResult().isEmpty()
                ? item.getResult()
                : "Pending");

        holder.betsValue.setText(item.getBets());
        holder.prizeValue.setText(item.getPrize());
        holder.transcodeValue.setText(item.getTranscode());
        holder.dateValue.setText(formatDate(item.getDate()));

        // Winner text visibility
        holder.winTxt.setVisibility(item.isWinner() ? View.VISIBLE : View.GONE);
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
        if (game == null) {
            imageView.setImageResource(R.drawable.pcsologo);
            return;
        }

        String g = game.trim().toLowerCase();
        int iconResId;
        if (g.contains("4d")) {
            iconResId = R.drawable.icon_4d;
        } else if (g.contains("3d") || g.contains("rambolito") || g.equals("standard")) {
            iconResId = R.drawable.icon_3d;
        } else if (g.contains("2d")) {
            iconResId = R.drawable.icon_2d;
        } else {
            iconResId = R.drawable.pcsologo;
        }
        imageView.setImageResource(iconResId);
    }

    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) return "";

        try {
            // Adjust input format depending on your SQL DB
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd, yyyy - h:mm a", Locale.getDefault());

            Date date = inputFormat.parse(dateString);
            return date != null ? outputFormat.format(date) : dateString;
        } catch (ParseException e) {
            return dateString; // fallback
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
        TextView winTxt;

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
            winTxt = itemView.findViewById(R.id.winTxt);
        }
    }
}
