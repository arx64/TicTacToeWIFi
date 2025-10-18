// com.example.tictactoe_wifi/GameActivity.java
package com.example.tictactoe_wifi;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
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
    private EditText etChatInput;
    private Button btnReset;
    private ProgressBar pbTurnTimer;
    private ScrollView svChatLogContainer;

    private boolean isHost;
    private boolean myTurn;
    private String mySymbol;
    private String opponentSymbol;
    private String myName, opponentName;

    private int scoreX = 0;
    private int scoreO = 0;
    private int moveCount = 0;
    private boolean gameActive = true;

    // Timer Logic
    private Handler timerHandler = new Handler();
    private static final int TURN_TIME_SECONDS = 15;
    private AtomicInteger currentTimerValue = new AtomicInteger(TURN_TIME_SECONDS);
    private int opponentTimerProgress = TURN_TIME_SECONDS * 10;

    // Sequence Number untuk ACK
    private int currentSequenceNumber = 0;
    private String lastProcessedSeqNum = "";
    private long startTimeMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        isHost = getIntent().getBooleanExtra("IS_HOST", false);
        myName = getIntent().getStringExtra("MY_NAME");
        mySymbol = getIntent().getStringExtra("MY_SYMBOL");
        opponentName = getIntent().getStringExtra("OPPONENT_NAME");
        opponentSymbol = getIntent().getStringExtra("OPPONENT_SYMBOL");

        startTimeMillis = System.currentTimeMillis(); // Catat waktu mulai

        initViews();
        initializeBoard();
        setupGame();
    }

    private void saveGameRecord() {
        String playerXName = mySymbol.equals("X") ? myName : opponentName;
        String playerOName = mySymbol.equals("O") ? myName : opponentName;

        String startTimeStr = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(new java.util.Date(startTimeMillis));
        String endTimeStr = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(new java.util.Date());

        GameRecord record = new GameRecord(
                playerXName,
                playerOName,
                scoreX,
                scoreO,
                startTimeStr,
                endTimeStr
        );
        HistoryManager.saveRecord(this, record);
    }

    private void initViews() {
        tvGameStatus = findViewById(R.id.tv_game_status);
        tvScoreX = findViewById(R.id.tv_score_x);
        tvScoreO = findViewById(R.id.tv_score_o);
        btnReset = findViewById(R.id.btn_reset);
        pbTurnTimer = findViewById(R.id.pb_turn_timer);
        tvChatLog = findViewById(R.id.tv_chat_log);
        etChatInput = findViewById(R.id.et_chat_input);
        svChatLogContainer = findViewById(R.id.sv_chat_log_container);

        pbTurnTimer.setMax(TURN_TIME_SECONDS * 10);
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
        else if (msg.what == MainActivity.MESSAGE_STATUS && msg.obj.toString().startsWith("TIMEOUT:")) {
            Toast.makeText(this, msg.obj.toString(), Toast.LENGTH_LONG).show();
            returnToMain();
        }
        return true;
    });

    private void handleIncomingMessage(String message) {
        message = message.trim();

        if (message.startsWith("MOVE:")) {
            String data = message.substring(5);
            String[] parts = data.split(",");

            if (parts.length == 3) {
                try {
                    int row = Integer.parseInt(parts[0]);
                    int col = Integer.parseInt(parts[1]);
                    String seqNum = parts[2];

                    if (seqNum.equals(lastProcessedSeqNum)) {
                        sendData("MOVE_ACK:" + seqNum);
                        return;
                    }
                    lastProcessedSeqNum = seqNum;

                    sendData("MOVE_ACK:" + seqNum);
                    makeMove(row, col, opponentSymbol, false);

                } catch (NumberFormatException e) {
                    // Error parsing
                }
            }
        } else if (message.startsWith("RESET_REQ")) {
            Toast.makeText(this, opponentName + " meminta main lagi!", Toast.LENGTH_SHORT).show();
            resetBoard(false);
        } else if (message.startsWith("CHAT:")) {
            String chatMsg = message.substring(5);
            updateChatLog(opponentName, chatMsg);
        } else if (message.startsWith("TIMER_SYNC:")) {
            try {
                int remainingSeconds = Integer.parseInt(message.substring(11));
                opponentTimerProgress = remainingSeconds * 10;
                if (!myTurn && gameActive) {
                    pbTurnTimer.setProgress(opponentTimerProgress);
                }
            } catch (NumberFormatException ignored) {}
        } else if (message.startsWith("TIMEOUT_LOSS")) {
            timerHandler.removeCallbacks(timerRunnable);

            if (mySymbol.equals("X")) {
                scoreX++;
            } else {
                scoreO++;
            }

            // --- SIMPAN RECORD GAME (LAWANN TIMEOUT) ---
            saveGameRecord();
            gameActive = false;
            updateScoreDisplay();
            updateStatusText(opponentName + " kehabisan waktu! Anda Menang.");
            btnReset.setVisibility(View.VISIBLE);
        } else if (message.startsWith("DISCONNECT")) {
            handleOpponentDisconnect();
        }
    }

    private void handleOpponentDisconnect() {
        gameActive = false;
        timerHandler.removeCallbacks(timerRunnable);

        new AlertDialog.Builder(this)
                .setTitle("Lawan Keluar")
                .setMessage(opponentName + " telah keluar dari permainan. Anda akan kembali ke menu utama.")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    returnToMain();
                })
                .show();
    }

    // --- Logika Timer ---
    private void startTurnTimer() {
        timerHandler.removeCallbacks(timerRunnable);
        if (!gameActive) {
            pbTurnTimer.setProgress(0);
            return;
        }

        currentTimerValue.set(TURN_TIME_SECONDS);

        if (myTurn) {
            pbTurnTimer.setProgress(TURN_TIME_SECONDS * 10);
            timerHandler.postDelayed(timerRunnable, 100);
        } else {
            pbTurnTimer.setProgress(opponentTimerProgress);
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

                if (currentProgress % 10 == 0) {
                    int remainingSeconds = currentProgress / 10;
                    sendData("TIMER_SYNC:" + remainingSeconds);
                }

                timerHandler.postDelayed(this, 100);
            } else {
                timerHandler.removeCallbacks(this);
                sendData("TIMEOUT_LOSS");

                Toast.makeText(GameActivity.this, "Waktu habis! Anda kalah di babak ini.", Toast.LENGTH_LONG).show();


                if (opponentSymbol.equals("X")) {
                    scoreX++;
                } else {
                    scoreO++;
                }
                saveGameRecord();
                gameActive = false;
                updateScoreDisplay();
                updateStatusText(myName + " kehabisan waktu!");
                btnReset.setVisibility(View.VISIBLE);
            }
        }
    };

    // --- Logika Keluar ---
    private void handleCleanDisconnect() {
        sendData("DISCONNECT");
        timerHandler.removeCallbacks(timerRunnable);
        returnToMain();
    }

    @Override
    public void onBackPressed() {
        // Peringatan: Jika Anda menggunakan API 33+, Anda mungkin perlu menggunakan OnBackPressedCallback.
        // Untuk kompatibilitas luas, kita menggunakan struktur ini.

        new AlertDialog.Builder(this)
                .setTitle("Keluar dari Game")
                .setMessage("Apakah Anda yakin ingin keluar? Lawan akan menang secara otomatis.")
                .setPositiveButton("Ya, Keluar", (dialog, which) -> {
                    handleCleanDisconnect();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    // --- Logika Game Lainnya ---

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

        currentSequenceNumber = (currentSequenceNumber + 1) % 1000;

        makeMove(row, col, mySymbol, true);

        String moveMsg = "MOVE:" + row + "," + col + "," + currentSequenceNumber;
        sendData(moveMsg);
    }

    public void onSendChatClicked(View v) {
        String message = etChatInput.getText().toString().trim();
        if (message.isEmpty()) return;

        String sanitizedMessage = message.replaceAll("[\r\n]", " ");
        sanitizedMessage = sanitizedMessage.replace(":", ";");

        if (sanitizedMessage.length() > 100) {
            sanitizedMessage = sanitizedMessage.substring(0, 100);
        }

        updateChatLog(myName, sanitizedMessage);
        sendData("CHAT:" + sanitizedMessage);
        etChatInput.setText("");
    }

    private void updateChatLog(String sender, String message) {
        String currentLog = tvChatLog.getText().toString();
        tvChatLog.setText(currentLog + "\n" + sender + ": " + message);

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

        if (buttons[row][col] != null) {
            buttons[row][col].setText(symbol);
        }

        moveCount++;
        timerHandler.removeCallbacks(timerRunnable);

        if (checkForWin() || moveCount == 9) {
            gameActive = false;

            if (symbol.equals("X")) {
                scoreX++;
            } else {
                scoreO++;
            }
            updateScoreDisplay();

            // --- SIMPAN RECORD GAME ---
            saveGameRecord();

            String winnerName = (symbol.equals(mySymbol)) ? myName : opponentName;
            updateStatusText(winnerName + " (" + symbol + ") MENANG!");
            highlightWinningLine(symbol);

            btnReset.setVisibility(View.VISIBLE);

        } else {
            String nextPlayerSymbol = symbol.equals("X") ? "O" : "X";
            myTurn = nextPlayerSymbol.equals(mySymbol);

            updateStatusText(null);
            startTurnTimer();
        }
    }

    private void resetBoard(boolean localInitiated) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setText("");
                buttons[i][j].setBackgroundColor(Color.parseColor("#E0E0E0"));
            }
        }
        moveCount = 0;
        gameActive = true;
        btnReset.setVisibility(View.GONE);

        myTurn = mySymbol.equals("X");

        lastProcessedSeqNum = "";
        currentSequenceNumber = 0;

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

    private boolean checkForWin() {
        String[][] board = new String[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = buttons[i][j].getText().toString();
            }
        }

        // Cek baris
        for (int i = 0; i < 3; i++) {
            // Kunci: Pastikan board[i][0] TIDAK KOSONG, lalu cek apakah semua sel di baris itu sama.
            if (!board[i][0].isEmpty() &&
                    board[i][0].equals(board[i][1]) &&
                    board[i][0].equals(board[i][2])) {
                return true;
            }
        }

        // Cek kolom
        for (int i = 0; i < 3; i++) {
            // Kunci: Pastikan board[0][i] TIDAK KOSONG, lalu cek apakah semua sel di kolom itu sama.
            if (!board[0][i].isEmpty() &&
                    board[0][i].equals(board[1][i]) &&
                    board[0][i].equals(board[2][i])) {
                return true;
            }
        }

        // Cek Diagonal Utama (0,0 -> 2,2)
        if (!board[0][0].isEmpty() &&
                board[0][0].equals(board[1][1]) &&
                board[0][0].equals(board[2][2])) {
            return true;
        }

        // Cek Diagonal Sekunder (0,2 -> 2,0)
        if (!board[0][2].isEmpty() &&
                board[0][2].equals(board[1][1]) &&
                board[0][2].equals(board[2][0])) {
            return true;
        }

        return false;
    }

    private void highlightWinningLine(String symbol) {
        String[][] board = new String[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = buttons[i][j].getText().toString();
            }
        }

        int highlightColor = Color.parseColor("#8BC34A");

        for (int i = 0; i < 3; i++) {
            if (board[i][0].equals(symbol) && board[i][1].equals(symbol) && board[i][2].equals(symbol)) {
                buttons[i][0].setBackgroundColor(highlightColor);
                buttons[i][1].setBackgroundColor(highlightColor);
                buttons[i][2].setBackgroundColor(highlightColor);
                return;
            }
        }
        for (int i = 0; i < 3; i++) {
            if (board[0][i].equals(symbol) && board[1][i].equals(symbol) && board[2][i].equals(symbol)) {
                buttons[0][i].setBackgroundColor(highlightColor);
                buttons[1][i].setBackgroundColor(highlightColor);
                buttons[2][i].setBackgroundColor(highlightColor);
                return;
            }
        }
        if (board[0][0].equals(symbol) && board[1][1].equals(symbol) && board[2][2].equals(symbol)) {
            buttons[0][0].setBackgroundColor(highlightColor);
            buttons[1][1].setBackgroundColor(highlightColor);
            buttons[2][2].setBackgroundColor(highlightColor);
            return;
        }
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

    private void returnToMain() {
        timerHandler.removeCallbacks(timerRunnable);
        if (udpCommunicator != null) {
            udpCommunicator.cancel();
            udpCommunicator = null;
        }

        Intent intent = new Intent(GameActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
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