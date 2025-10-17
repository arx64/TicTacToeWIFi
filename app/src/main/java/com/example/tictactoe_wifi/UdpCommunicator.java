// com.example.tictactoe_wifi/UdpCommunicator.java (Koreksi Total)
package com.example.tictactoe_wifi;

import android.os.Handler;
import android.os.HandlerThread; // Import baru
import android.util.Log;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class UdpCommunicator extends Thread {
    private static final String TAG = "UdpCommunicator";
    private DatagramSocket socket;
    public Handler handler;
    private InetAddress remoteAddress;
    private final int remotePort = MainActivity.PORT;
    private volatile boolean running = true;
    private boolean isHost;

    // Logika Retransmission
    private String lastSentMove = null;
    private int retransmissionCount = 0;
    private static final int MAX_RETRANSMISSION = 3;
    private static final long RETRANSMISSION_DELAY_MS = 500;

    // HandlerThread dan Handler untuk operasi timing/sending
    private HandlerThread networkHandlerThread;
    private Handler networkHandler;

    private static final long HEARTBEAT_INTERVAL_MS = 5000;
    private long lastReceivedTime = System.currentTimeMillis();
    private static final long TIMEOUT_MS = 15000;

    // Konstruktor 1: Untuk Host (Server) - 2 Argumen
    public UdpCommunicator(Handler handler, boolean isHost) {
        this.handler = handler;
        this.isHost = isHost;

        // 1. Inisialisasi HandlerThread
        networkHandlerThread = new HandlerThread("UdpSenderThread");
        networkHandlerThread.start();
        networkHandler = new Handler(networkHandlerThread.getLooper());

        try {
            socket = new DatagramSocket(remotePort);
            if (isHost) {
                handler.obtainMessage(MainActivity.MESSAGE_STATUS, "Menunggu Client terhubung...").sendToTarget();
            }
            networkHandler.post(heartbeatChecker);
        } catch (SocketException e) {
            Log.e(TAG, "Error creating DatagramSocket", e);
            handler.obtainMessage(MainActivity.MESSAGE_CONNECTION_FAILED, "Gagal membuka socket UDP").sendToTarget();
            running = false;
        }
    }

    // Konstruktor 2: Untuk Client (Joiner) - 3 Argumen
    public UdpCommunicator(Handler handler, String hostIp, boolean isHost) {
        // Memanggil Konstruktor 1 untuk inisialisasi socket dan thread
        this(handler, isHost);
        try {
            // Mengatur alamat tujuan (remoteAddress)
            this.remoteAddress = InetAddress.getByName(hostIp);
        } catch (UnknownHostException e) {
            Log.e(TAG, "Host IP not found", e);
            running = false;
        }
    }

    @Override
    public void run() {
        // Thread ini hanya fokus pada RECEIVE (menerima data)
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (running) {
            try {
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8).trim();
                lastReceivedTime = System.currentTimeMillis();

                if (isHost && remoteAddress == null && message.startsWith("CONNECT:")) {
                    remoteAddress = packet.getAddress();
                    handler.obtainMessage(MainActivity.MESSAGE_CONNECTION_SUCCESS, "Terkoneksi sebagai Server (X)").sendToTarget();
                }

                if (message.startsWith("MOVE_ACK:")) {
                    // Panggil penanganan ACK di thread pengirim (networkHandler)
                    networkHandler.post(() -> handleMoveAck(message));
                }

                handler.obtainMessage(MainActivity.MESSAGE_READ, 0, 0, message).sendToTarget();

            } catch (IOException e) {
                if (running) {
                    Log.e(TAG, "Error receiving packet", e);
                }
                break;
            }
        }
    }

    // --- Logika Heartbeat dan Timeout ---
    private Runnable heartbeatChecker = new Runnable() {
        @Override
        public void run() {
            if (!running) return;

            long elapsed = System.currentTimeMillis() - lastReceivedTime;

            if (remoteAddress != null) {
                if (elapsed > TIMEOUT_MS) {
                    handler.obtainMessage(MainActivity.MESSAGE_STATUS, "TIMEOUT: Lawan terputus!").sendToTarget();
                    cancel();
                    return;
                }
                // Panggil send() di thread ini (networkHandlerThread)
                send("HEARTBEAT");
            }

            networkHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS);
        }
    };

    // --- Logika Retransmission ---
    private void handleMoveAck(String ackMessage) {
        if (lastSentMove != null && ackMessage.contains(lastSentMove.split(",")[2])) {
            networkHandler.removeCallbacks(retransmissionRunnable);
            lastSentMove = null;
            retransmissionCount = 0;
        }
    }

    private Runnable retransmissionRunnable = new Runnable() {
        @Override
        public void run() {
            if (lastSentMove != null) {
                if (retransmissionCount < MAX_RETRANSMISSION) {
                    // Panggil send() di thread ini (networkHandlerThread)
                    send(lastSentMove);
                    retransmissionCount++;
                    networkHandler.postDelayed(this, RETRANSMISSION_DELAY_MS);
                } else {
                    handler.obtainMessage(MainActivity.MESSAGE_STATUS, "Gagal mengirim gerakan. Koneksi buruk.").sendToTarget();
                    lastSentMove = null;
                    retransmissionCount = 0;
                }
            }
        }
    };

    // Metode send() sekarang dipanggil dari background thread (HandlerThread atau Thread eksternal)
    public void send(String message) {
        if (remoteAddress == null || socket == null) {
            Log.w(TAG, "Remote address or socket not set.");
            return;
        }

        if (message.startsWith("MOVE:")) {
            lastSentMove = message;
            retransmissionCount = 0;
            // Mulai retransmission di HandlerThread
            networkHandler.postDelayed(retransmissionRunnable, RETRANSMISSION_DELAY_MS);
        }

        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, remotePort);

        try {
            // Operasi I/O ini sekarang aman karena dipanggil dari HandlerThread atau Thread eksternal
            socket.send(packet);
        } catch (IOException e) {
            Log.e(TAG, "Error sending packet", e);
        }
    }

    public void cancel() {
        running = false;
        if (networkHandlerThread != null) {
            networkHandlerThread.quitSafely();
        }
        if (socket != null) {
            socket.close();
        }
    }

    public InetAddress getRemoteAddress() {
        return remoteAddress;
    }
}