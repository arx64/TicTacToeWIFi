// com.example.tictactoe_wifi/HistoryActivity.java
package com.example.tictactoe_wifi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.util.List;

public class HistoryActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        ListView lvHistory = findViewById(R.id.lv_history);
        List<GameRecord> records = HistoryManager.loadRecords(this);

        if (records.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("Belum ada riwayat permainan.");
            lvHistory.setEmptyView(emptyView);
        }

        HistoryAdapter adapter = new HistoryAdapter(this, records);
        lvHistory.setAdapter(adapter);
    }

    // Custom Adapter untuk menampilkan GameRecord
    private class HistoryAdapter extends ArrayAdapter<GameRecord> {
        public HistoryAdapter(Context context, List<GameRecord> records) {
            super(context, 0, records);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            GameRecord record = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_history_record, parent, false);
            }

            TextView tvMatchup = convertView.findViewById(R.id.tv_matchup);
            TextView tvScore = convertView.findViewById(R.id.tv_score);
            TextView tvTime = convertView.findViewById(R.id.tv_time);

            // Tentukan pemenang untuk highlight
            String winnerStatus;
            if (record.scoreX > record.scoreO) {
                winnerStatus = record.playerXName + " MENANG!";
                tvMatchup.setTextColor(Color.parseColor("#006400")); // Hijau gelap
            } else if (record.scoreO > record.scoreX) {
                winnerStatus = record.playerOName + " MENANG!";
                tvMatchup.setTextColor(Color.parseColor("#006400"));
            } else {
                winnerStatus = "SERI";
                tvMatchup.setTextColor(Color.BLACK);
            }

            tvMatchup.setText(winnerStatus + " (" + record.playerXName + " vs " + record.playerOName + ")");
            tvScore.setText("Skor Akhir: " + record.scoreX + " - " + record.scoreO);
            tvTime.setText("Mulai: " + record.startTime + " | Selesai: " + record.endTime);

            return convertView;
        }
    }
}