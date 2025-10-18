// com.example.tictactoe_wifi/HistoryManager.java
package com.example.tictactoe_wifi;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HistoryManager {
    private static final String PREFS_NAME = "GameHistoryPrefs";
    private static final String KEY_RECORDS = "GameRecords";
    private static final String RECORD_SEPARATOR = ";";

    public static void saveRecord(Context context, GameRecord record) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String existingRecords = prefs.getString(KEY_RECORDS, "");

        String newRecordString = record.toSavableString();
        String updatedRecords;

        if (existingRecords.isEmpty()) {
            updatedRecords = newRecordString;
        } else {
            // Tambahkan record baru di awal (agar yang terbaru muncul di atas)
            updatedRecords = newRecordString + RECORD_SEPARATOR + existingRecords;
        }

        prefs.edit().putString(KEY_RECORDS, updatedRecords).apply();
    }

    public static List<GameRecord> loadRecords(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedString = prefs.getString(KEY_RECORDS, "");

        List<GameRecord> records = new ArrayList<>();
        if (savedString.isEmpty()) {
            return records;
        }

        String[] recordStrings = savedString.split(RECORD_SEPARATOR);
        for (String recordString : recordStrings) {
            GameRecord record = GameRecord.fromSavedString(recordString);
            if (record != null) {
                records.add(record);
            }
        }
        return records;
    }

    // Opsional: Untuk debugging atau reset
    public static void clearHistory(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().remove(KEY_RECORDS).apply();
    }
}