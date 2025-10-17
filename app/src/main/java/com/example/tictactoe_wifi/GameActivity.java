// com.example.tictactoe_wifi/GameActivity.java
package com.example.tictactoe_wifi;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.nio.charset.StandardCharsets;

public class GameActivity extends AppCompatActivity {

    // Referensi statis ke Communicator dari MainActivity
    public static UdpCommunicator udpCommunicator;

    private Button[][] buttons = new Button[3][3];
    private TextView tvGameStatus, tvScoreX, tvScoreO;
    private Button btnReset;

    private boolean isHost;
    private boolean myTurn;
    private String mySymbol;
    private String opponentSymbol;
    private String myName, opponentName;

    private int scoreX = 0;
    private int scoreO = 0;
    private int moveCount = 0;
    private boolean gameActive = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        isHost = getIntent().getBooleanExtra("IS_HOST", false);
        myName = getIntent().getStringExtra("MY_NAME");
        opponentName = getIntent().getStringExtra("OPPONENT_NAME");

        tvGameStatus = findViewById(R.id.tv_game_status);
        tvScoreX = findViewById(R.id.tv_score_x);
        tvScoreO = findViewById(R.id.tv_score_o);
        btnReset = findViewById(R.id.btn_reset);

        initializeBoard();
        setupGame();
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
        if (isHost) {
            mySymbol = "X";
            opponentSymbol = "O";
            myTurn = true;
        } else {
            mySymbol = "O";
            opponentSymbol = "X";
            myTurn = false;
        }

        // Update tampilan skor awal
        updateScoreDisplay();

        // Ganti handler di Communicator agar pesan masuk ke GameActivity
        if (udpCommunicator != null) {
            udpCommunicator.handler = gameHandler;
        }
        updateStatusText(null);
    }

    private void updateScoreDisplay() {
        String nameX = (mySymbol.equals("X")) ? myName : opponentName;
        String nameO = (mySymbol.equals("O")) ? myName : opponentName;

        tvScoreX.setText(nameX + " (X): " + scoreX);
        tvScoreO.setText(nameO + " (O): " + scoreO);
    }

    // Handler khusus untuk GameActivity (menerima gerakan lawan dan perintah reset)
    Handler gameHandler = new Handler(msg -> {
        if (msg.what == MainActivity.MESSAGE_READ) {
            String message = (String) msg.obj;
            handleIncomingMessage(message);
        }
        return true;
    });

    private void handleIncomingMessage(String message) {
        if (message.startsWith("MOVE:")) {
            String[] parts = message.substring(5).split(",");
            if (parts.length == 2) {
                int row = Integer.parseInt(parts[0]);
                int col = Integer.parseInt(parts[1]);

                // Lakukan gerakan lawan (isLocalMove = false)
                makeMove(row, col, opponentSymbol, false);
            }
        } else if (message.startsWith("RESET_REQ")) {
            // Lawan meminta reset, kita konfirmasi dan reset
            Toast.makeText(this, opponentName + " meminta main lagi!", Toast.LENGTH_SHORT).show();
            resetBoard(false); // Reset tanpa mengirim pesan lagi
        }
    }

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

        // Dapatkan koordinat tombol yang diklik
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

        // Lakukan gerakan lokal (isLocalMove = true)
        makeMove(row, col, mySymbol, true);

        // Kirim gerakan ke lawan (di background thread)
        String moveMsg = "MOVE:" + row + "," + col;
        sendData(moveMsg);
    }

    public void onResetClicked(View v) {
        // Kirim permintaan reset ke lawan
        sendData("RESET_REQ");
        resetBoard(true); // Reset lokal
    }

    private void makeMove(int row, int col, String symbol, boolean isLocalMove) {
        if (!gameActive) return;

        buttons[row][col].setText(symbol);
        moveCount++;

        if (checkForWin(symbol)) {
            gameActive = false;

            // Update skor
            if (symbol.equals("X")) {
                scoreX++;
            } else {
                scoreO++;
            }
            updateScoreDisplay();

            String winnerName = (symbol.equals(mySymbol)) ? myName : opponentName;
            updateStatusText(winnerName + " (" + symbol + ") MENANG!");

            btnReset.setVisibility(View.VISIBLE);

        } else if (moveCount == 9) {
            gameActive = false;
            updateStatusText("SERI!");
            btnReset.setVisibility(View.VISIBLE);
        } else {
            // Ganti giliran
            myTurn = !isLocalMove;
            updateStatusText(null);
        }
    }

    private void resetBoard(boolean localInitiated) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setText("");
            }
        }
        moveCount = 0;
        gameActive = true;
        btnReset.setVisibility(View.GONE);

        // Tentukan siapa yang mulai duluan di babak baru
        // Kita bisa mempertahankan Host (X) selalu mulai, atau bergantian.
        // Kita pertahankan Host (X) selalu mulai untuk menyederhanakan logika.
        myTurn = isHost;

        updateStatusText(null);
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
        String[][] board = new String[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = buttons[i][j].getText().toString();
            }
        }

        // Cek baris, kolom, dan diagonal
        for (int i = 0; i < 3; i++) {
            // Baris
            if (board[i][0].equals(symbol) && board[i][1].equals(symbol) && board[i][2].equals(symbol)) return true;
            // Kolom
            if (board[0][i].equals(symbol) && board[1][i].equals(symbol) && board[2][i].equals(symbol)) return true;
        }

        // Diagonal utama
        if (board[0][0].equals(symbol) && board[1][1].equals(symbol) && board[2][2].equals(symbol)) return true;

        // Diagonal sekunder
        if (board[0][2].equals(symbol) && board[1][1].equals(symbol) && board[2][0].equals(symbol)) return true;

        return false;
    }

    private void sendData(String message) {
        if (udpCommunicator != null) {
            // Operasi I/O harus di background thread
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
        // Pastikan communicator ditutup saat game selesai
        if (udpCommunicator != null) {
            udpCommunicator.cancel();
            udpCommunicator = null;
        }
    }
}