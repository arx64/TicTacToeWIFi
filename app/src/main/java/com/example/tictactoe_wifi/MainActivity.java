// com.example.tictactoe_wifi/MainActivity.java
package com.example.tictactoe_wifi;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {

    // --- KONSTANTA JARINGAN ---
    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_CONNECTION_SUCCESS = 3;
    public static final int MESSAGE_CONNECTION_FAILED = 4;
    public static final int MESSAGE_STATUS = 5;
    public static final int PORT = 8888;
    // --------------------------

    private Button btnHost, btnJoin;
    private EditText etIpAddress, etPlayerName;
    private TextView tvStatus, tvLocalIp;

    private UdpCommunicator udpCommunicator;
    private String playerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        displayLocalIp();

        btnHost.setOnClickListener(v -> hostGame());
        btnJoin.setOnClickListener(v -> joinGame());
    }

    private void initViews() {
        btnHost = findViewById(R.id.btn_host);
        btnJoin = findViewById(R.id.btn_join);
        etIpAddress = findViewById(R.id.et_ip_address);
        etPlayerName = findViewById(R.id.et_player_name);
        tvStatus = findViewById(R.id.status_text);
        tvLocalIp = findViewById(R.id.tv_local_ip);
    }

    private void displayLocalIp() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            @SuppressWarnings("deprecation")
            int ipAddressInt = wifiManager.getConnectionInfo().getIpAddress();
            String ipAddress = Formatter.formatIpAddress(ipAddressInt);
            tvLocalIp.setText("IP Lokal Anda: " + ipAddress + " (Port: " + PORT + ")");
        } else {
            tvLocalIp.setText("IP Lokal Anda: WiFi tidak aktif.");
        }
    }

    private boolean validateInput() {
        playerName = etPlayerName.getText().toString().trim();
        if (playerName.isEmpty()) {
            Toast.makeText(this, "Masukkan nama Anda!", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (udpCommunicator != null) udpCommunicator.cancel();
        return true;
    }

    private void hostGame() {
        if (!validateInput()) return;
        tvStatus.setText("Mencoba menjadi Host...");

        // Inisialisasi Host Communicator
        udpCommunicator = new UdpCommunicator(handler, true);
        udpCommunicator.start();
    }

    private void joinGame() {
        if (!validateInput()) return;
        String ip = etIpAddress.getText().toString().trim();
        if (ip.isEmpty()) {
            Toast.makeText(this, "Masukkan IP Server", Toast.LENGTH_SHORT).show();
            return;
        }
        tvStatus.setText("Mencoba terhubung ke " + ip + "...");

        // Inisialisasi Client Communicator
        udpCommunicator = new UdpCommunicator(handler, ip, false);
        udpCommunicator.start();

        // Kirim paket koneksi pertama (harus di thread terpisah karena send() memanggil I/O)
        new Thread(() -> {
            // Format: CONNECT:<NamaPemain>
            udpCommunicator.send("CONNECT:" + playerName);
        }).start();
    }

    // Handler untuk menerima pesan dari thread jaringan
    Handler handler = new Handler(msg -> {
        switch (msg.what) {
            case MESSAGE_STATUS:
                tvStatus.setText(msg.obj.toString());
                break;
            case MESSAGE_CONNECTION_SUCCESS:
                // Hanya Host yang menerima MESSAGE_CONNECTION_SUCCESS dari UdpCommunicator
                String status = msg.obj.toString();
                tvStatus.setText(status);

                // Host mengirim ACK ke Client
                if (status.contains("Server")) {
                    new Thread(() -> {
                        // Format: ACK:<NamaHost>
                        udpCommunicator.send("ACK:" + playerName);
                    }).start();
                    startGame(true, playerName, "Lawan"); // Nama lawan sementara "Lawan"
                }
                break;
            case MESSAGE_CONNECTION_FAILED:
                tvStatus.setText(msg.obj.toString());
                Toast.makeText(MainActivity.this, msg.obj.toString(), Toast.LENGTH_LONG).show();
                break;
            case MESSAGE_READ:
                String message = (String) msg.obj;
                // Client menerima ACK dari Host
                if (message.startsWith("ACK:")) {
                    String opponentName = message.substring(4);
                    startGame(false, playerName, opponentName);
                }
                // Host menerima CONNECT dari Client
                else if (message.startsWith("CONNECT:")) {
                    String opponentName = message.substring(8);
                    // Karena Host sudah mengirim ACK di MESSAGE_CONNECTION_SUCCESS, kita hanya perlu memulai game
                    startGame(true, playerName, opponentName);
                }
                break;
        }
        return true;
    });

    private void startGame(boolean isHost, String myName, String opponentName) {
        Intent intent = new Intent(MainActivity.this, GameActivity.class);
        intent.putExtra("IS_HOST", isHost);
        intent.putExtra("MY_NAME", myName);
        intent.putExtra("OPPONENT_NAME", opponentName);

        // Kirim referensi Communicator (meskipun tidak ideal, ini cara tercepat)
        GameActivity.udpCommunicator = udpCommunicator;

        startActivity(intent);
        // finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (udpCommunicator != null) udpCommunicator.cancel();
    }
}