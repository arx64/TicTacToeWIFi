// com.example.tictactoe_wifi/HowToPlayActivity.java
package com.example.tictactoe_wifi;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

public class HowToPlayActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_how_to_play);

        // Opsional: Tampilkan tombol kembali di ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Cara Main");
        }
    }

    // Menangani tombol kembali di ActionBar
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}