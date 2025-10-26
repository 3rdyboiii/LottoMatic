package com.example.lottomatic.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottomatic.R;
import com.example.lottomatic.items.EntryItem;

import java.util.List;

public class EntryAdapter extends RecyclerView.Adapter<EntryAdapter.ViewHolder> {

    private List<EntryItem> itemList;
    private double totalSum = 0.0;
    private OnTotalUpdateListener totalUpdateListener;

    public interface OnTotalUpdateListener {
        void onTotalUpdate(double total);
    }

    public OnTotalUpdateListener getTotalUpdateListener() {
        return totalUpdateListener;
    }

    public EntryAdapter(List<EntryItem> itemList, OnTotalUpdateListener listener) {
        this.itemList = itemList;
        this.totalUpdateListener = listener;
        updateTotal();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.entry_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EntryItem entryItem = itemList.get(position);
        holder.comboTxt.setText(entryItem.getCombo());
        holder.amountTxt.setText("₱" + entryItem.getAmount());
        holder.gameTxt.setText(entryItem.getGame());
        holder.prizeTxt.setText("₱" + entryItem.getPrize());
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public void addItem(EntryItem item) {
        itemList.add(item);
        totalSum += item.getAmount();
        notifyItemInserted(itemList.size() - 1);
        if (totalUpdateListener != null) {
            totalUpdateListener.onTotalUpdate(totalSum);
        }
    }
    public void clearItemList() {
        itemList.clear(); // Clear the list
        totalSum = 0.0;   // Reset the total sum
        notifyDataSetChanged(); // Notify the adapter to refresh the RecyclerView
        if (totalUpdateListener != null) {
            totalUpdateListener.onTotalUpdate(totalSum); // Update the total
        }
    }
    public void removeItem(int position) {
        if (position >= 0 && position < itemList.size()) {
            EntryItem removedItem = itemList.get(position);
            itemList.remove(position);
            totalSum -= removedItem.getAmount();
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, itemList.size());
        }
    }
    public List<EntryItem> getItemList() {
        return itemList;
    }
    public double getTotal() {
        return totalSum;
    }

    private void updateTotal() {
        totalSum = 0.0;
        for (EntryItem item : itemList) {
            totalSum += item.getAmount();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView comboTxt;
        public TextView amountTxt;
        public TextView gameTxt;
        public TextView prizeTxt;

        public ViewHolder(View itemView) {
            super(itemView);
            comboTxt = itemView.findViewById(R.id.comboTxt);
            amountTxt = itemView.findViewById(R.id.amountTxt);
            gameTxt = itemView.findViewById(R.id.gameTxt);
            prizeTxt = itemView.findViewById(R.id.prizeTxt);
        }
    }
}
