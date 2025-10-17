// com.example.tictactoe_wifi/GameActivity.java
package com.example.tictactoe_wifi;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.concurrent.atomic.AtomicInteger;

public class GameActivity extends AppCompatActivity {

    public static UdpCommunicator udpCommunicator;

    private Button[][] buttons = new Button[3][3];
    private TextView tvGameStatus, tvScoreX, tvScoreO, tvChatLog;
    private ScrollView svChatLogContainer;
    private EditText etChatInput;
    private Button btnReset;
    private ProgressBar pbTurnTimer;

    private boolean isHost;
    private boolean myTurn;
    private String mySymbol;
    private String opponentSymbol;
    private String myName, opponentName;

    private int scoreX = 0;
    private int scoreO = 0;
    private int moveCount = 0;
    private boolean gameActive = true;

    // Turn Timer Logic
    private Handler timerHandler = new Handler();
    private static final int TURN_TIME_SECONDS = 15;
    private AtomicInteger currentTimerValue = new AtomicInteger(TURN_TIME_SECONDS);

    // Sequence Number untuk ACK
    private int currentSequenceNumber = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        isHost = getIntent().getBooleanExtra("IS_HOST", false);
        myName = getIntent().getStringExtra("MY_NAME");
        mySymbol = getIntent().getStringExtra("MY_SYMBOL");
        opponentName = getIntent().getStringExtra("OPPONENT_NAME");
        opponentSymbol = getIntent().getStringExtra("OPPONENT_SYMBOL");

        initViews();
        initializeBoard();
        setupGame();
    }

    private void initViews() {
        tvGameStatus = findViewById(R.id.tv_game_status);
        tvScoreX = findViewById(R.id.tv_score_x);
        tvScoreO = findViewById(R.id.tv_score_o);
        btnReset = findViewById(R.id.btn_reset);
        pbTurnTimer = findViewById(R.id.pb_turn_timer);
        tvChatLog = findViewById(R.id.tv_chat_log);
        etChatInput = findViewById(R.id.et_chat_input);

        // Inisialisasi ScrollView
        svChatLogContainer = findViewById(R.id.sv_chat_log_container);

        pbTurnTimer.setMax(TURN_TIME_SECONDS * 10); // Max 150
    }

    private void initializeBoard() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                String buttonID = "btn_" + i + j;
                int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
                buttons[i][j] = findViewById(resID);
            }
        }
    }

    private void setupGame() {
        // Tentukan giliran awal
        myTurn = mySymbol.equals("X");

        updateScoreDisplay();

        if (udpCommunicator != null) {
            udpCommunicator.handler = gameHandler;
        }
        updateStatusText(null);
        startTurnTimer();
    }

    private void updateScoreDisplay() {
        String nameX = (mySymbol.equals("X")) ? myName : opponentName;
        String nameO = (mySymbol.equals("O")) ? myName : opponentName;

        tvScoreX.setText(nameX + " (X): " + scoreX);
        tvScoreO.setText(nameO + " (O): " + scoreO);
    }

    // --- Handler Jaringan ---
    Handler gameHandler = new Handler(msg -> {
        if (msg.what == MainActivity.MESSAGE_READ) {
            String message = (String) msg.obj;
            handleIncomingMessage(message);
        }
        return true;
    });

    private void handleIncomingMessage(String message) {
        if (message.startsWith("MOVE:")) {
            // Format: MOVE:<R>,<C>,<SeqNum>
            String[] parts = message.substring(5).split(",");
            if (parts.length == 3) {
                int row = Integer.parseInt(parts[0]);
                int col = Integer.parseInt(parts[1]);
                String seqNum = parts[2];

                // 1. Kirim ACK kembali (Keandalan UDP)
                sendData("MOVE_ACK:" + seqNum);

                // 2. Lakukan gerakan lawan
                makeMove(row, col, opponentSymbol, false);
            }
        } else if (message.startsWith("RESET_REQ")) {
            Toast.makeText(this, opponentName + " meminta main lagi!", Toast.LENGTH_SHORT).show();
            resetBoard(false);
        } else if (message.startsWith("CHAT:")) {
            String chatMsg = message.substring(5);
            updateChatLog(opponentName, chatMsg);
        } else if (message.startsWith("HEARTBEAT")) {
            // Abaikan Heartbeat, hanya untuk reset timeout di UdpCommunicator
        }
    }

    // --- Logika Timer ---
    private void startTurnTimer() {
        timerHandler.removeCallbacks(timerRunnable);
        if (!gameActive) {
            pbTurnTimer.setProgress(0);
            return;
        }

        currentTimerValue.set(TURN_TIME_SECONDS);
        pbTurnTimer.setProgress(TURN_TIME_SECONDS * 10);

        if (myTurn) {
            timerHandler.postDelayed(timerRunnable, 100);
        } else {
            // Timer lawan tidak perlu dihitung mundur secara visual, tapi kita tetap reset
            pbTurnTimer.setProgress(TURN_TIME_SECONDS * 10);
        }
    }

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!gameActive || !myTurn) {
                timerHandler.removeCallbacks(this);
                return;
            }

            int currentProgress = pbTurnTimer.getProgress();
            if (currentProgress > 0) {
                pbTurnTimer.setProgress(currentProgress - 1);
                timerHandler.postDelayed(this, 100); // Update setiap 100ms
            } else {
                // Waktu Habis!
                Toast.makeText(GameActivity.this, "Waktu habis! Anda kalah di babak ini.", Toast.LENGTH_LONG).show();

                // Lawan menang
                scoreO++;
                gameActive = false;
                updateScoreDisplay();
                updateStatusText(myName + " kehabisan waktu!");
                btnReset.setVisibility(View.VISIBLE);
            }
        }
    };

    // --- Logika Klik ---
    public void onCellClicked(View v) {
        if (!myTurn || !gameActive) {
            Toast.makeText(this, "Bukan giliran Anda.", Toast.LENGTH_SHORT).show();
            return;
        }

        Button b = (Button) v;
        if (!b.getText().toString().isEmpty()) {
            Toast.makeText(this, "Kotak sudah terisi.", Toast.LENGTH_SHORT).show();
            return;
        }

        int row = -1, col = -1;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (buttons[i][j] == b) {
                    row = i;
                    col = j;
                    break;
                }
            }
        }

        // Increment Sequence Number
        currentSequenceNumber = (currentSequenceNumber + 1) % 1000;

        // Lakukan gerakan lokal (isLocalMove = true)
        makeMove(row, col, mySymbol, true);

        // Kirim gerakan ke lawan: MOVE:<R>,<C>,<SeqNum>
        String moveMsg = "MOVE:" + row + "," + col + "," + currentSequenceNumber;
        sendData(moveMsg);
    }

    public void onSendChatClicked(View v) {
        String message = etChatInput.getText().toString().trim();
        if (message.isEmpty()) return;

        updateChatLog(myName, message);
        sendData("CHAT:" + message);
        etChatInput.setText("");
    }

    // com.example.tictactoe_wifi/GameActivity.java

    private void updateChatLog(String sender, String message) {
        // Ambil teks lama, tambahkan pesan baru
        String currentLog = tvChatLog.getText().toString();

        // Batasi log agar tidak terlalu panjang (opsional, tapi disarankan)
        if (currentLog.length() > 500) {
            currentLog = "Chat Log:\n";
        }

        tvChatLog.setText(currentLog + "\n" + sender + ": " + message);

        // Logika Auto-Scroll: Memastikan ScrollView menggulir ke bawah
        // Kita menggunakan post() karena pembaruan layout mungkin belum selesai
        svChatLogContainer.post(() -> {
            svChatLogContainer.fullScroll(View.FOCUS_DOWN);
        });
    }

    public void onResetClicked(View v) {
        sendData("RESET_REQ");
        resetBoard(true);
    }

    private void makeMove(int row, int col, String symbol, boolean isLocalMove) {
        if (!gameActive) return;

        buttons[row][col].setText(symbol);
        moveCount++;

        // Hentikan timer saat gerakan dilakukan
        timerHandler.removeCallbacks(timerRunnable);

        if (checkForWin(symbol)) {
            gameActive = false;

            if (symbol.equals("X")) {
                scoreX++;
            } else {
                scoreO++;
            }
            updateScoreDisplay();

            String winnerName = (symbol.equals(mySymbol)) ? myName : opponentName;
            updateStatusText(winnerName + " (" + symbol + ") MENANG!");
            highlightWinningLine(symbol);

            btnReset.setVisibility(View.VISIBLE);

        } else if (moveCount == 9) {
            gameActive = false;
            updateStatusText("SERI!");
            btnReset.setVisibility(View.VISIBLE);
        } else {
            // Ganti giliran
            myTurn = !isLocalMove;
            updateStatusText(null);
            startTurnTimer(); // Mulai timer untuk giliran baru
        }
    }

    private void resetBoard(boolean localInitiated) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setText("");
                buttons[i][j].setBackgroundColor(Color.parseColor("#E0E0E0")); // Reset warna
            }
        }
        moveCount = 0;
        gameActive = true;
        btnReset.setVisibility(View.GONE);

        // X selalu mulai duluan
        myTurn = mySymbol.equals("X");

        updateStatusText(null);
        startTurnTimer();
        Toast.makeText(this, "Permainan baru dimulai!", Toast.LENGTH_SHORT).show();
    }

    private void updateStatusText(String customMessage) {
        if (customMessage != null) {
            tvGameStatus.setText(customMessage);
            return;
        }

        if (gameActive) {
            if (myTurn) {
                tvGameStatus.setText("Giliran Anda (" + mySymbol + ")");
            } else {
                tvGameStatus.setText("Giliran Lawan (" + opponentSymbol + ")");
            }
        }
    }

    private boolean checkForWin(String symbol) {
        // ... (Logika pengecekan kemenangan sama)
        String[][] board = new String[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = buttons[i][j].getText().toString();
            }
        }

        // Cek baris, kolom, dan diagonal
        for (int i = 0; i < 3; i++) {
            if (board[i][0].equals(symbol) && board[i][1].equals(symbol) && board[i][2].equals(symbol)) return true;
            if (board[0][i].equals(symbol) && board[1][i].equals(symbol) && board[2][i].equals(symbol)) return true;
        }
        if (board[0][0].equals(symbol) && board[1][1].equals(symbol) && board[2][2].equals(symbol)) return true;
        if (board[0][2].equals(symbol) && board[1][1].equals(symbol) && board[2][0].equals(symbol)) return true;

        return false;
    }

    private void highlightWinningLine(String symbol) {
        // Logika ini harus diulang karena checkForWin tidak menyimpan koordinat
        String[][] board = new String[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = buttons[i][j].getText().toString();
            }
        }

        int highlightColor = Color.parseColor("#8BC34A"); // Hijau

        // Cek baris
        for (int i = 0; i < 3; i++) {
            if (board[i][0].equals(symbol) && board[i][1].equals(symbol) && board[i][2].equals(symbol)) {
                buttons[i][0].setBackgroundColor(highlightColor);
                buttons[i][1].setBackgroundColor(highlightColor);
                buttons[i][2].setBackgroundColor(highlightColor);
                return;
            }
        }
        // Cek kolom
        for (int i = 0; i < 3; i++) {
            if (board[0][i].equals(symbol) && board[1][i].equals(symbol) && board[2][i].equals(symbol)) {
                buttons[0][i].setBackgroundColor(highlightColor);
                buttons[1][i].setBackgroundColor(highlightColor);
                buttons[2][i].setBackgroundColor(highlightColor);
                return;
            }
        }
        // Diagonal utama
        if (board[0][0].equals(symbol) && board[1][1].equals(symbol) && board[2][2].equals(symbol)) {
            buttons[0][0].setBackgroundColor(highlightColor);
            buttons[1][1].setBackgroundColor(highlightColor);
            buttons[2][2].setBackgroundColor(highlightColor);
            return;
        }
        // Diagonal sekunder
        if (board[0][2].equals(symbol) && board[1][1].equals(symbol) && board[2][0].equals(symbol)) {
            buttons[0][2].setBackgroundColor(highlightColor);
            buttons[1][1].setBackgroundColor(highlightColor);
            buttons[2][0].setBackgroundColor(highlightColor);
        }
    }

    private void sendData(String message) {
        if (udpCommunicator != null) {
            new Thread(() -> {
                udpCommunicator.send(message);
            }).start();
        } else {
            Toast.makeText(this, "Koneksi terputus! Tidak dapat mengirim data.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
        if (udpCommunicator != null) {
            udpCommunicator.cancel();
            udpCommunicator = null;
        }
    }
}