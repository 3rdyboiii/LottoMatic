package com.example.lottomatic.items;

public class EntryItem {
    private String combo;
    private double amount;
    private String game;
    private String type;
    private String prize;

    public EntryItem(String combo, double amount, String game, String type, String prize) {
        this.combo = combo;
        this.amount = amount;
        this.game = game;
        this.type = type;
        this.prize = prize;
    }

    public String getCombo() {
        return combo;
    }

    public double getAmount() {
        return amount;
    }

    public String getGame() {
        return game;
    }

    public String getType() {
        return type;
    }

    public String getPrize() {
        return prize;
    }
}

