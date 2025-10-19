// com.example.tictactoe_wifi/BaseActivity.java
package com.example.tictactoe_wifi;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    protected SharedPreferences prefs;
    private static final String PREFS_KEY = "GameSettings";
    private static final String KEY_IS_MUTED = "isMuted";
    protected boolean isMuted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
        isMuted = prefs.getBoolean(KEY_IS_MUTED, false);
    }

    // --- LOGIKA MENU ACTION BAR (MUTE/UNMUTE) ---

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

            Toast.makeText(this, isMuted ? "Musik Dimatikan" : "Musik Dinyalakan", Toast.LENGTH_SHORT).show();

            // Perbarui ikon
            invalidateOptionsMenu();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}