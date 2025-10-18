// com.example.tictactoe_wifi/MenuActivity.java (Kode Lengkap dan Final)
package com.example.tictactoe_wifi;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

public class MenuActivity extends AppCompatActivity {

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

        prefs = getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
        isMuted = prefs.getBoolean(KEY_IS_MUTED, false);

        // --- KONTROL SERVICE: Mulai Service Musik ---
        Intent musicIntent = new Intent(this, BackgroundMusicService.class);
        musicIntent.putExtra(BackgroundMusicService.EXTRA_IS_MUTED, isMuted);
        startService(musicIntent);
        // ---------------------------------------------

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

    // --- MANAJEMEN ACTION BAR MENU ---

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem muteItem = menu.findItem(R.id.action_toggle_mute);
        if (muteItem != null) {
            if (isMuted) {
                muteItem.setIcon(R.drawable.ic_volume_off);
            } else {
                muteItem.setIcon(R.drawable.ic_volume_up);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_toggle_mute) {
            isMuted = !isMuted;

            // Kirim perintah mute/unmute ke Service
            Intent muteIntent = new Intent(this, BackgroundMusicService.class);
            muteIntent.setAction(BackgroundMusicService.ACTION_TOGGLE_MUTE);
            muteIntent.putExtra(BackgroundMusicService.EXTRA_IS_MUTED, isMuted);
            startService(muteIntent);

            // Simpan status ke SharedPreferences
            prefs.edit().putBoolean(KEY_IS_MUTED, isMuted).apply();

            // Perbarui ikon
            invalidateOptionsMenu();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Hapus onResume, onPause, dan onDestroy yang lama karena Service yang mengurus musik
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Opsional: Jika Anda ingin musik berhenti total saat user keluar dari MenuActivity
         stopService(new Intent(this, BackgroundMusicService.class));
    }
}