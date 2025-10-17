// com.example.tictactoe_wifi/UdpCommunicator.java
package com.example.tictactoe_wifi;

import android.os.Handler;
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
    private boolean running = true;
    private boolean isHost;

    // Konstruktor untuk Host (Server)
    public UdpCommunicator(Handler handler, boolean isHost) {
        this.handler = handler;
        this.isHost = isHost;
        try {
            // Host akan bind ke port 8888
            socket = new DatagramSocket(remotePort);
            if (isHost) {
                handler.obtainMessage(MainActivity.MESSAGE_STATUS, "Menunggu Client terhubung...").sendToTarget();
            }
        } catch (SocketException e) {
            Log.e(TAG, "Error creating DatagramSocket", e);
            handler.obtainMessage(MainActivity.MESSAGE_CONNECTION_FAILED, "Gagal membuka socket UDP").sendToTarget();
            running = false;
        }
    }

    // Konstruktor untuk Client (Joiner)
    public UdpCommunicator(Handler handler, String hostIp, boolean isHost) {
        this(handler, isHost); // Panggil konstruktor Host untuk inisialisasi socket
        try {
            this.remoteAddress = InetAddress.getByName(hostIp);
        } catch (UnknownHostException e) {
            Log.e(TAG, "Host IP not found", e);
            running = false;
        }
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (running) {
            try {
                // Blokir hingga paket diterima
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

                // Jika ini Host, simpan alamat Client yang baru terhubung
                if (isHost && remoteAddress == null) {
                    remoteAddress = packet.getAddress();
                    // Kirim pesan sukses koneksi ke UI
                    handler.obtainMessage(MainActivity.MESSAGE_CONNECTION_SUCCESS, "Terkoneksi sebagai Server (X)").sendToTarget();
                }

                // Kirim data yang dibaca ke UI thread melalui Handler
                handler.obtainMessage(MainActivity.MESSAGE_READ, 0, 0, message).sendToTarget();

            } catch (IOException e) {
                if (running) {
                    Log.e(TAG, "Error receiving packet", e);
                }
                break;
            }
        }
    }

    // Metode untuk mengirim data (harus dijalankan di background thread)
    public void send(String message) {
        if (remoteAddress == null || socket == null) {
            Log.w(TAG, "Remote address or socket not set.");
            return;
        }

        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress, remotePort);

        try {
            socket.send(packet);
        } catch (IOException e) {
            Log.e(TAG, "Error sending packet", e);
        }
    }

    public void cancel() {
        running = false;
        if (socket != null) {
            socket.close();
        }
    }

    public InetAddress getRemoteAddress() {
        return remoteAddress;
    }
}