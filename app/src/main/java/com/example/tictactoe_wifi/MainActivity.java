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
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {

    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_CONNECTION_SUCCESS = 3;
    public static final int MESSAGE_CONNECTION_FAILED = 4;
    public static final int MESSAGE_STATUS = 5;
    public static final int PORT = 8888;

    private EditText etIpAddress, etPlayerName;
    private TextView tvStatus;
    private RadioGroup rgSymbolChoice;

    private UdpCommunicator udpCommunicator;
    private String playerName;
    private String chosenSymbol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        displayLocalIp();

        findViewById(R.id.btn_host).setOnClickListener(v -> hostGame());
        findViewById(R.id.btn_join).setOnClickListener(v -> joinGame());
    }

    private void initViews() {
        etIpAddress = findViewById(R.id.et_ip_address);
        etPlayerName = findViewById(R.id.et_player_name);
        tvStatus = findViewById(R.id.status_text);
        rgSymbolChoice = findViewById(R.id.rg_symbol_choice);
    }

    private void displayLocalIp() {
        // ... (Logika display IP sama)
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            @SuppressWarnings("deprecation")
            int ipAddressInt = wifiManager.getConnectionInfo().getIpAddress();
            String ipAddress = Formatter.formatIpAddress(ipAddressInt);
            ((TextView)findViewById(R.id.tv_local_ip)).setText("IP Lokal Anda: " + ipAddress + " (Port: " + PORT + ")");
        }
    }

    private boolean validateInput() {
        playerName = etPlayerName.getText().toString().trim();
        if (playerName.isEmpty()) {
            Toast.makeText(this, "Masukkan nama Anda!", Toast.LENGTH_SHORT).show();
            return false;
        }
        int selectedId = rgSymbolChoice.getCheckedRadioButtonId();
        chosenSymbol = (selectedId == R.id.rb_x) ? "X" : "O";

        if (udpCommunicator != null) udpCommunicator.cancel();
        return true;
    }

    private void setRoleActive(boolean active) {
        Button btnHost = findViewById(R.id.btn_host);
        Button btnJoin = findViewById(R.id.btn_join);

        // Jika sudah aktif, nonaktifkan tombol lawan
        btnHost.setEnabled(!active);
        btnJoin.setEnabled(!active);
        etIpAddress.setEnabled(!active);
        rgSymbolChoice.setEnabled(!active);
        etPlayerName.setEnabled(!active);
    }

    private void hostGame() {
        if (!validateInput()) return;
        tvStatus.setText("Mencoba menjadi Host...");

        setRoleActive(true); // Kunci tombol lain

        udpCommunicator = new UdpCommunicator(handler, true);
        udpCommunicator.start();
    }

    private void joinGame() {
        String ip = etIpAddress.getText().toString().trim();
        if (!validateInput() || ip.isEmpty()) return;

        // PENCEGAHAN BUG: Cek apakah IP yang dimasukkan adalah IP lokal sendiri
        TextView tvLocalIp = findViewById(R.id.tv_local_ip);
        String localIpText = tvLocalIp.getText().toString();
        if (localIpText.contains(ip)) {
            Toast.makeText(this, "Tidak bisa bergabung ke IP Anda sendiri!", Toast.LENGTH_LONG).show();
            return;
        }

        tvStatus.setText("Mencoba terhubung ke " + ip + "...");

        setRoleActive(true); // Kunci tombol lain

        try {
            // ... (Logika koneksi sama)
            InetAddress.getByName(ip);
            udpCommunicator = new UdpCommunicator(handler, ip, false);
            udpCommunicator.start();

            new Thread(() -> {
                udpCommunicator.send("CONNECT:" + playerName + "," + chosenSymbol);
            }).start();
        } catch (UnknownHostException e) {
            tvStatus.setText("IP tidak valid.");
            Toast.makeText(this, "IP tidak valid", Toast.LENGTH_SHORT).show();
            setRoleActive(false); // Buka kembali jika gagal
        }
    }

    Handler handler = new Handler(msg -> {
        switch (msg.what) {
            case MESSAGE_STATUS:
                tvStatus.setText(msg.obj.toString());
                break;
            case MESSAGE_CONNECTION_SUCCESS:
                // Host menerima koneksi
                String status = msg.obj.toString();
                tvStatus.setText(status);
                break;
            case MESSAGE_CONNECTION_FAILED:
                tvStatus.setText(msg.obj.toString());
                Toast.makeText(MainActivity.this, msg.obj.toString(), Toast.LENGTH_LONG).show();
                break;
            case MESSAGE_READ:
                String message = (String) msg.obj;

                if (message.startsWith("ACK:")) {
                    // Client menerima ACK: <NamaHost>,<SimbolHost>
                    String[] parts = message.substring(4).split(",");
                    String opponentName = parts[0];
                    String opponentSymbol = parts[1];

                    // Resolusi Simbol: Simbol kita mungkin berubah jika ada konflik
                    String finalMySymbol = chosenSymbol;
                    if (chosenSymbol.equals(opponentSymbol)) {
                        finalMySymbol = (chosenSymbol.equals("X")) ? "O" : "X";
                        Toast.makeText(this, "Simbol diubah menjadi " + finalMySymbol + " karena konflik.", Toast.LENGTH_LONG).show();
                    }
                    startGame(false, playerName, finalMySymbol, opponentName, opponentSymbol);
                }
                else if (message.startsWith("CONNECT:")) {
                    // Host menerima CONNECT: <NamaClient>,<SimbolClient>
                    String[] parts = message.substring(8).split(",");
                    String opponentName = parts[0];
                    String opponentSymbol = parts[1];

                    // Resolusi Simbol (Host punya prioritas)
                    String finalMySymbol = chosenSymbol;
                    String finalOpponentSymbol = opponentSymbol;

                    if (chosenSymbol.equals(opponentSymbol)) {
                        // Host mempertahankan simbolnya, Client harus berubah
                        finalOpponentSymbol = (chosenSymbol.equals("X")) ? "O" : "X";
                    }

                    // Kirim ACK ke Client dengan simbol yang sudah diresolusi
                    String finalOpponentSymbolForAck = finalOpponentSymbol;
                    new Thread(() -> {
                        udpCommunicator.send("ACK:" + playerName + "," + finalMySymbol);
                    }).start();

                    startGame(true, playerName, finalMySymbol, opponentName, finalOpponentSymbol);
                }
                break;
        }
        return true;
    });

    private void startGame(boolean isHost, String myName, String mySymbol, String opponentName, String opponentSymbol) {
        Intent intent = new Intent(MainActivity.this, GameActivity.class);
        intent.putExtra("IS_HOST", isHost);
        intent.putExtra("MY_NAME", myName);
        intent.putExtra("MY_SYMBOL", mySymbol);
        intent.putExtra("OPPONENT_NAME", opponentName);
        intent.putExtra("OPPONENT_SYMBOL", opponentSymbol);

        GameActivity.udpCommunicator = udpCommunicator;

        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (udpCommunicator != null) udpCommunicator.cancel();
    }
}