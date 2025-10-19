// com.example.tictactoe_wifi/MainActivity.java
package com.example.tictactoe_wifi;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import androidx.activity.result.ActivityResultLauncher;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class MainActivity extends BaseActivity {

    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_CONNECTION_SUCCESS = 3;
    public static final int MESSAGE_CONNECTION_FAILED = 4;
    public static final int MESSAGE_STATUS = 5;
    public static final int PORT = 8888;

    // --- 1. DEKLARASIKAN VARIABEL DI SINI (TANPA INISIALISASI findViewById) ---
    private EditText etIpAddress, etPlayerName;
    private TextView tvStatus, tvLocalIp;
    private RadioGroup rgSymbolChoice;
    private Button btnHost, btnJoin, btnScanQr;
    private ImageView imgQrCode;
    private LinearLayout layoutJoinArea;

    private UdpCommunicator udpCommunicator;
    private String playerName;
    private String chosenSymbol;

    private final ActivityResultLauncher<ScanOptions> qrCodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    // Pengguna membatalkan scan
                    Toast.makeText(this, "Scan dibatalkan", Toast.LENGTH_LONG).show();
                } else {
                    // Hasil scan didapatkan (ini adalah alamat IP dari Host)
                    String scannedIp = result.getContents();
                    Toast.makeText(this, "Hasil Scan: " + scannedIp, Toast.LENGTH_LONG).show();

                    // Masukkan IP hasil scan ke EditText dan panggil joinGame()
                    etIpAddress.setText(scannedIp);
                    joinGame();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        displayLocalIp();

        // --- INI ADALAH KODE BARU UNTUK btnHost ---
        btnHost.setOnClickListener(v -> {
            // Validasi input nama dan simbol sebelum melanjutkan
            if (!validateInput()) {
                return; // Berhenti jika input tidak valid
            }

            try {
                String localIp = getLocalIpAddress();
                if (localIp != null) {
                    // Sembunyikan tombol-tombol dan area input manual
                    layoutJoinArea.setVisibility(View.GONE);
                    btnJoin.setVisibility(View.GONE);
                    btnHost.setVisibility(View.GONE); // Sembunyikan juga tombol host setelah ditekan

                    // Tampilkan QR Code
                    Bitmap qrBitmap = generateQRCode(localIp);
                    imgQrCode.setImageBitmap(qrBitmap);
                    imgQrCode.setVisibility(View.VISIBLE);

                    tvStatus.setText("Pindai QR Code untuk bergabung...");
                    Toast.makeText(this, "Pindai QR Code ini agar lawan bisa bergabung.", Toast.LENGTH_LONG).show();

                    // Setelah menampilkan QR, perangkat ini otomatis menjadi Host
                    // hostGame() sudah termasuk validateInput, tapi kita panggil lagi di sini
                    // untuk memastikan proses hosting dimulai setelah QR ditampilkan.
                    // validateInput() yang dipanggil di awal memastikan nama pemain sudah terisi.
                    hostGame();

                } else {
                    Toast.makeText(this, "Gagal mendapatkan IP lokal. Pastikan terhubung ke Wi-Fi.", Toast.LENGTH_LONG).show();
                }
            } catch (WriterException e) {
                e.printStackTrace();
                Toast.makeText(this, "Gagal membuat QR Code.", Toast.LENGTH_SHORT).show();
            }
        });

        btnJoin.setOnClickListener(v -> joinGame());
        // Hapus pemanggilan joinGame() yang salah di sini
//        findViewById(R.id.btn_manual_join).setOnClickListener(v -> joinGame()); // Gunakan tombol terpisah untuk join manual

        // --- 3. TAMBAHKAN LISTENER UNTUK TOMBOL SCAN BARU ---
        btnScanQr.setOnClickListener(v -> {
            // Validasi nama pemain sebelum scan
            if (!validateInput()) {
                return;
            }
            // Mulai proses scan
            launchScanner();
        });
    }

    // --- 4. BUAT FUNGSI UNTUK MELUNCURKAN SCANNER ---
    private void launchScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Arahkan kamera ke QR Code lawan");
        options.setBeepEnabled(true); // Bunyi saat berhasil scan
        options.setOrientationLocked(false); // Izinkan rotasi
//        options.setCaptureActivity(CaptureActivityPortrait.class); // Gunakan activity portrait (opsional, perlu dependensi tambahan jika ingin custom)

        qrCodeLauncher.launch(options);
    }

    // Fungsi untuk membuat QR Code
    private Bitmap generateQRCode(String text) throws WriterException {
        // ... (kode Anda sudah benar)
        QRCodeWriter writer = new QRCodeWriter();
        int size = 400;
        BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bitmap;
    }

    // Fungsi untuk ambil IP lokal (Wi-Fi)
    private String getLocalIpAddress() {
        // ... (kode Anda sudah benar)
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    // Pastikan itu adalah IPv4 dan bukan loopback
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof java.net.Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private void initViews() {
        // --- 4. INISIALISASIKAN SEMUA VIEW DI SINI ---
        etIpAddress = findViewById(R.id.et_ip_address);
        etPlayerName = findViewById(R.id.et_player_name);
        tvStatus = findViewById(R.id.status_text);
        rgSymbolChoice = findViewById(R.id.rg_symbol_choice);

        // Inisialisasi variabel yang sebelumnya menyebabkan crash
        btnHost = findViewById(R.id.btn_host);
        imgQrCode = findViewById(R.id.img_qr_code);
        layoutJoinArea = findViewById(R.id.layout_join_area);
        btnJoin = findViewById(R.id.btn_join);
        btnScanQr = findViewById(R.id.btn_scan_qr);
        tvLocalIp = findViewById(R.id.tv_local_ip);
    }

    private void displayLocalIp() {
        String ipAddress = getLocalIpAddress();
        if (ipAddress != null) {
            tvLocalIp.setText("IP Lokal Anda: " + ipAddress + " (Port: " + PORT + ")");
        } else {
            tvLocalIp.setText("IP Lokal tidak ditemukan. Pastikan terhubung ke Wi-Fi.");
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