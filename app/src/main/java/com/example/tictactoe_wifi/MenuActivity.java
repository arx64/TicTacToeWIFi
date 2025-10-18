// com.example.tictactoe_wifi/MenuActivity.java
package com.example.tictactoe_wifi;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        Button btnStartGame = findViewById(R.id.btn_start_game);
        Button btnHistory = findViewById(R.id.btn_history);
        Button btnHowToPlay = findViewById(R.id.btn_how_to_play);

        // 1. Tombol "Main Game" -> Membuka MainActivity (Layar Koneksi)
        btnStartGame.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, MainActivity.class);
            startActivity(intent);
        });

        // 2. Tombol "Riwayat" (Placeholder)
        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        // 3. Tombol "Cara Main" (Placeholder)
        btnHowToPlay.setOnClickListener(v -> {
            // Di sini Anda bisa meluncurkan Activity Cara Main atau menampilkan dialog
            Toast.makeText(MenuActivity.this, "Cara Main: Hubungkan dua perangkat ke Wi-Fi yang sama. Satu menjadi HOST, yang lain JOIN dengan memasukkan IP HOST.", Toast.LENGTH_LONG).show();
        });
    }
}