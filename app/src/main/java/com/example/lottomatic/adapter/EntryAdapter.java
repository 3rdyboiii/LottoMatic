package com.example.lottomatic.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottomatic.R;
import com.example.lottomatic.helper.Account;
import com.example.lottomatic.items.EntryItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntryAdapter extends RecyclerView.Adapter<EntryAdapter.ViewHolder> {

    private List<EntryItem> itemList;
    private double totalSum = 0.0;
    private OnTotalUpdateListener totalUpdateListener;
    private Map<String, Double> comboTotalMap = new HashMap<>();
    private Account account;

    public interface OnTotalUpdateListener {
        void onTotalUpdate(double total);
        void onComboLimitReached(String combo, double remaining, double limit);
    }

    public OnTotalUpdateListener getTotalUpdateListener() {
        return totalUpdateListener;
    }

    public EntryAdapter(List<EntryItem> itemList, OnTotalUpdateListener listener, Account account) {
        this.itemList = itemList != null ? itemList : new ArrayList<>();
        this.totalUpdateListener = listener;
        this.account = account;
        updateTotals();
    }

    public EntryItem getItemAt(int position) {
        if (position >= 0 && position < itemList.size()) {
            return itemList.get(position);
        }
        return null;
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
        holder.gameTxt.setText(entryItem.getType());
        holder.prizeTxt.setText("₱" + entryItem.getPrize());
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }


    public boolean addItem(EntryItem item) {
        // Get the limit for this game type
        double comboLimit = account.getBetLimit(item.getGame(), 1000.0);

        // Check if adding this would exceed the combo limit
        double currentComboTotal = comboTotalMap.getOrDefault(item.getCombo(), 0.0);
        double newComboTotal = currentComboTotal + item.getAmount();

        if (newComboTotal > comboLimit) {
            if (totalUpdateListener != null) {
                double remaining = comboLimit - currentComboTotal;
                totalUpdateListener.onComboLimitReached(item.getCombo(), remaining, comboLimit);
            }
            return false;
        }

        // Add the item if within limit
        itemList.add(item);
        comboTotalMap.put(item.getCombo(), newComboTotal);
        totalSum += item.getAmount();

        notifyItemInserted(itemList.size() - 1);
        if (totalUpdateListener != null) {
            totalUpdateListener.onTotalUpdate(totalSum);
        }
        return true;
    }

    public void clearItemList() {
        itemList.clear(); // Clear the list
        comboTotalMap.clear();
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

            // Update combo total map
            String combo = removedItem.getCombo();
            double newComboTotal = comboTotalMap.getOrDefault(combo, 0.0) - removedItem.getAmount();
            if (newComboTotal <= 0) {
                comboTotalMap.remove(combo);
            } else {
                comboTotalMap.put(combo, newComboTotal);
            }

            totalSum -= removedItem.getAmount();
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, itemList.size());

            if (totalUpdateListener != null) {
                totalUpdateListener.onTotalUpdate(totalSum);
            }
        }
    }
    public List<EntryItem> getItemList() {
        return itemList;
    }

    public double getTotal() {
        return totalSum;
    }
    public double getComboTotal(String combo) {
        return comboTotalMap.getOrDefault(combo, 0.0);
    }

    private void updateTotals() {
        totalSum = 0.0;
        comboTotalMap.clear();

        for (EntryItem item : itemList) {
            totalSum += item.getAmount();
            comboTotalMap.put(item.getCombo(),
                    comboTotalMap.getOrDefault(item.getCombo(), 0.0) + item.getAmount());
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
