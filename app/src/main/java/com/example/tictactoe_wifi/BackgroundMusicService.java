// com.example.tictactoe_wifi/BackgroundMusicService.java
package com.example.tictactoe_wifi;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;

public class BackgroundMusicService extends Service {
    private static final String TAG = "MusicService";
    private MediaPlayer mediaPlayer;
    public static final String ACTION_TOGGLE_MUTE = "com.example.tictactoe_wifi.TOGGLE_MUTE";
    public static final String EXTRA_IS_MUTED = "isMuted";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service Created");

        // Inisialisasi MediaPlayer di Service
        mediaPlayer = MediaPlayer.create(this, R.raw.backsound);
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null && intent.getAction().equals(ACTION_TOGGLE_MUTE)) {
            // Menerima perintah mute/unmute dari Activity
            boolean isMuted = intent.getBooleanExtra(EXTRA_IS_MUTED, false);
            setVolume(isMuted ? 0.0f : 1.0f);
        }

        // Memastikan musik tetap berjalan
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }

        return START_STICKY; // Service akan dihidupkan ulang jika dihentikan oleh sistem
    }

    private void setVolume(float volume) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume, volume);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service Destroyed");
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}