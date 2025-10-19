// com.example.tictactoe_wifi/MenuActivity.java (Kode Lengkap dan Final)
package com.example.tictactoe_wifi;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

public class MenuActivity extends BaseActivity {

    private SharedPreferences prefs;
    private static final String PREFS_KEY = "GameSettings";
    private static final String KEY_IS_MUTED = "isMuted";
    private boolean isMuted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("TicTacToe Wi-Fi");
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        Button btnStartGame = findViewById(R.id.btn_start_game);
        Button btnHistory = findViewById(R.id.btn_history);
        Button btnHowToPlay = findViewById(R.id.btn_how_to_play);

        btnStartGame.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, MainActivity.class);
            startActivity(intent);
        });

        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        btnHowToPlay.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, HowToPlayActivity.class);
            startActivity(intent);
        });
    }
}