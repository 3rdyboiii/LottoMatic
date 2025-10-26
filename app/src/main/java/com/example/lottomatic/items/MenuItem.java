package com.example.lottomatic.items;

public class MenuItem {
    private int imageResId;
    private String game;
    private String draw;
    private String time;
    private String prize;

    public MenuItem(int imageResId,String game, String draw, String time, String prize) {
        this.imageResId = imageResId;
        this.game = game;
        this.draw = draw;
        this.time = time;
        this.prize = prize;
    }

    public int getImageResId() {
        return imageResId;
    }
    public String getGame() {
        return game;
    }
    public String getDraw() {
        return draw;
    }
    public String getTime() {
        return time;
    }
    public String getPrize() {
        return prize;
    }

    public void setImageResId(int imageResId) { this.imageResId = imageResId; }
    public void setGame(String game) { this.game = game; }
    public void setDraw(String draw) { this.draw = draw; }
    public void setTime(String time) { this.time = time; }
    public void setPrize(String prize) { this.prize = prize; }
}
