// com.example.tictactoe_wifi/GameRecord.java
package com.example.tictactoe_wifi;

public class GameRecord {
    public String playerXName;
    public String playerOName;
    public int scoreX;
    public int scoreO;
    public String startTime;
    public String endTime;

    public GameRecord(String playerXName, String playerOName, int scoreX, int scoreO, String startTime, String endTime) {
        this.playerXName = playerXName;
        this.playerOName = playerOName;
        this.scoreX = scoreX;
        this.scoreO = scoreO;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Metode untuk mengonversi objek menjadi format string yang dapat disimpan (CSV sederhana)
    public String toSavableString() {
        return playerXName + "|" + playerOName + "|" + scoreX + "|" + scoreO + "|" + startTime + "|" + endTime;
    }

    // Metode statis untuk membuat objek dari string yang disimpan
    public static GameRecord fromSavedString(String savedString) {
        String[] parts = savedString.split("\\|");
        if (parts.length != 6) return null;

        try {
            return new GameRecord(
                    parts[0],
                    parts[1],
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]),
                    parts[4],
                    parts[5]
            );
        } catch (NumberFormatException e) {
            return null;
        }
    }
}