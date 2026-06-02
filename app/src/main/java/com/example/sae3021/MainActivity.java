package com.example.sae3021;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private EditText messageEditText;
    private TextView logTextView;
    private UdpClient udpClient;
    private StringBuilder logMessages = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        messageEditText = findViewById(R.id.messageEditText);
        logTextView = findViewById(R.id.logTextView);
        Button sendButton = findViewById(R.id.sendButton);

        // Initialiser le client UDP
        udpClient = new UdpClient(message -> {
            runOnUiThread(() -> {
                logMessages.append("Reçu: ").append(message).append("\n");
                logTextView.setText(logMessages.toString());
            });
        });

        udpClient.start();

        // Bouton Envoyer
        sendButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString();
            if (!message.isEmpty()) {
                logMessages.append("Envoyé: ").append(message).append("\n");
                logTextView.setText(logMessages.toString());
                udpClient.sendMessage(message);
                messageEditText.setText("");
            }
        });
    }

    @Override
    protected void onDestroy() {
        udpClient.stop();
        super.onDestroy();
    }

    // Classe interne UDP Client
    private static class UdpClient {
        private static final String TAG = "UdpClient";
        private static final int UDP_PORT = 6010;

        private InetAddress serverAddress;
        private DatagramSocket socket;
        private volatile boolean running = false;
        private ExecutorService sendExecutor = Executors.newSingleThreadExecutor();
        private ExecutorService receiveExecutor = Executors.newSingleThreadExecutor();
        private OnMessageReceivedListener onMessageReceived;

        public UdpClient(OnMessageReceivedListener onMessageReceived) {
            this.onMessageReceived = onMessageReceived;
            try {
                this.serverAddress = InetAddress.getByName("172.20.10.2");
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de la création de l'adresse", e);
            }
        }

        public void start() {
            if (running) return;
            receiveExecutor.execute(() -> {
                try {
                    socket = new DatagramSocket();
                    socket.setSoTimeout(1000);
                    running = true;
                } catch (Exception e) {
                    Log.e(TAG, "Impossible d'initialiser le socket UDP", e);
                    onMessageReceived.onMessageReceived("Erreur réception UDP");
                    return;
                }

                while (running && socket != null) {
                    try {
                        byte[] buffer = new byte[1024];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        String message = new String(packet.getData(), 0, packet.getLength());
                        onMessageReceived.onMessageReceived(message);
                    } catch (java.net.SocketTimeoutException e) {
                        // Continue to check running flag
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur lors de la réception UDP", e);
                        if (running) {
                            onMessageReceived.onMessageReceived("Erreur réception UDP");
                        }
                    }
                }
            });
        }

        public void sendMessage(String message) {
            sendExecutor.execute(() -> {
                try {
                    if (!running || socket == null) return;
                    byte[] bytes = message.getBytes();
                    DatagramPacket packet = new DatagramPacket(bytes, bytes.length, serverAddress, UDP_PORT);
                    socket.send(packet);
                } catch (Exception e) {
                    Log.e(TAG, "Erreur lors de l'envoi UDP", e);
                    onMessageReceived.onMessageReceived("Erreur envoi UDP");
                }
            });
        }

        public void stop() {
            running = false;
            sendExecutor.shutdownNow();
            receiveExecutor.shutdownNow();
            if (socket != null) {
                socket.close();
                socket = null;
            }
        }
    }

    // Interface de callback
    interface OnMessageReceivedListener {
        void onMessageReceived(String message);
    }
}