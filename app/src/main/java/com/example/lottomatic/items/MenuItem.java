package com.example.lottomatic.items;

public class MenuItem {
    private int imageResId;
    private String game;
    private String drawTime; // e.g., "9:00 PM"
    private String countdown; // e.g., "12H : 30M : 06S"
    private String prize;
    private long drawTimestamp; // timestamp in millis

    public MenuItem(int imageResId, String game, String drawTime, String countdown, String prize, long drawTimestamp) {
        this.imageResId = imageResId;
        this.game = game;
        this.drawTime = drawTime;
        this.countdown = countdown;
        this.prize = prize;
        this.drawTimestamp = drawTimestamp;
    }

    public int getImageResId() { return imageResId; }
    public String getGame() { return game; }
    public String getDraw() { return drawTime; }
    public String getCountdown() { return countdown; }
    public String getPrize() { return prize; }
    public long getDrawTimestamp() { return drawTimestamp; }
}
