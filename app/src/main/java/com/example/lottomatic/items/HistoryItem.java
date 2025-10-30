package com.example.lottomatic.items;

public class HistoryItem {
    private String combo;
    private String bets;
    private String draw;
    private String game;
    private String transcode;
    private String date;
    private String prize;
    private String result;

    public HistoryItem(String combo, String bets, String draw, String game, String transcode, String date, String prize, String result) {
        this.combo = combo;
        this.bets = bets;
        this.draw = draw;
        this.game = game;
        this.transcode = transcode;
        this.date = date;
        this.prize = prize;
        this.result = result;
    }

    public String getCombo() {
        return combo;
    }

    public String getBets() {
        return bets;
    }

    public String getDraw() {
        return draw;
    }

    public String getGame() {
        return game;
    }

    public String getTranscode() {
        return transcode;
    }

    public String getDate() {
        return date;
    }

    public String getPrize() {
        return prize;
    }

    public String getResult() {
        return result;
    }
}