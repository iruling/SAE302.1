package com.example.sae3021;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class ChatActivity extends AppCompatActivity {
    private String contactName;
    private String username;
    private LinearLayout messageList;
    private EditText messageInput;
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Set<String> displayedMessages = new HashSet<>();

    private final Runnable refreshChatTask = new Runnable() {
        @Override
        public void run() {
            checkForUpdates();
            loadLocalHistory();
            refreshHandler.postDelayed(this, 3000); // Rafraîchir toutes les 3 secondes
        }
    };

    private void checkForUpdates() {
        new Thread(() -> {
            try {
                DataHandler handler = new DataHandler();
                String response = handler.sendAndReceive("Update," + username);
                handler.close();
                
                if (response != null && response.contains("DATA;")) {
                    parseUpdateResponse(response);
                }
            } catch (IOException ignored) {}
        }).start();
    }

    private void parseUpdateResponse(String response) {
        try {
            int dataIndex = response.indexOf("DATA;");
            String dataPart = response.substring(dataIndex + 5);
            String[] segments = dataPart.split(";");
            for (String segment : segments) {
                String s = segment.trim();
                String upper = s.toUpperCase();
                if (upper.startsWith("MSG=")) {
                    String msgPart = s.substring("MSG=".length());
                    saveReceivedMessages(msgPart);
                }
            }
        } catch (Exception ignored) {}
    }

    private void saveReceivedMessages(String messagesPart) {
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String history = prefs.getString("chat_history", "");
        
        if (history.isEmpty()) history = messagesPart;
        else history += "," + messagesPart;
        
        prefs.edit().putString("chat_history", history).apply();
        
        runOnUiThread(this::loadLocalHistory);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        username = prefs.getString("username", "");
        String password = prefs.getString("password", ""); // Récupération du mot de passe

        contactName = getIntent().getStringExtra("contactName");
        
        TextView chatTitle = findViewById(R.id.chatTitle);
        if (contactName != null) {
            chatTitle.setText("Chat avec " + contactName);
        }

        messageList = findViewById(R.id.messageList);
        messageInput = findViewById(R.id.messageInput);
        Button sendBtn = findViewById(R.id.sendBtn);

        loadLocalHistory();

        sendBtn.setOnClickListener(v -> {
            String rawMsg = messageInput.getText().toString().trim();
            if (rawMsg.isEmpty()) return;

            // ✅ Encoder pour éviter les virgules / caractères spéciaux dans le transport CSV
            String encodedMsg;
            try {
                encodedMsg = URLEncoder.encode(rawMsg, "UTF-8");
            } catch (Exception e) {
                encodedMsg = rawMsg;
            }

            String finalMsg = encodedMsg;
            new Thread(() -> {
                try {
                    DataHandler handler = new DataHandler();
                    String Message = "Send_Msg," + username + "," + contactName + "," + finalMsg;

                    String response = handler.sendAndReceive(Message);
                    handler.close();

                    runOnUiThread(() -> {
                        if (isFinishing()) return;
                        if (response.startsWith("200")) {
                            // Afficher immédiatement et sauvegarder
                            addMessageToUI("Moi: " + rawMsg, Gravity.END);
                            saveSentMessage(rawMsg);
                            messageInput.setText("");
                        } else {
                            Toast.makeText(this, "Erreur envoi: " + response, Toast.LENGTH_SHORT).show();
                        }
                    });

                } catch (IOException e) {
                    runOnUiThread(() -> Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show());
                }
            }).start();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshHandler.post(refreshChatTask);
    }

    @Override
    protected void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshChatTask);
    }

    private void loadLocalHistory() {
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String history = prefs.getString("chat_history", "");
        if (history.isEmpty()) return;

        // Format: src:dst:content,src:dst:content
        String[] messages = history.split(",");
        for (String m : messages) {
            if (displayedMessages.contains(m)) continue;

            String[] parts = m.split(":", 3);
            if (parts.length >= 3) {
                String src = parts[0].trim();
                String dst = parts[1].trim();
                String content = parts[2];
                
                // Décoder le contenu pour l'affichage
                try {
                    content = URLDecoder.decode(content, "UTF-8");
                } catch (Exception ignored) {}

                if (src.equals(contactName) && dst.equals(username)) {
                    addMessageToUI(contactName + ": " + content, Gravity.START);
                    displayedMessages.add(m);
                } else if (src.equals(username) && dst.equals(contactName)) {
                    addMessageToUI("Moi: " + content, Gravity.END);
                    displayedMessages.add(m);
                }
            }
        }
    }

    private void saveSentMessage(String msg) {
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String history = prefs.getString("chat_history", "");
        // On sauvegarde le message encodé pour rester cohérent avec le format de réception
        String encoded;
        try {
            encoded = URLEncoder.encode(msg, "UTF-8");
        } catch (Exception e) {
            encoded = msg;
        }
        
        String newMsgEntry = username + ":" + contactName + ":" + encoded;
        displayedMessages.add(newMsgEntry); // Éviter de le re-charger via refreshChatTask
        
        if (history.isEmpty()) history = newMsgEntry;
        else history += "," + newMsgEntry;
        
        prefs.edit().putString("chat_history", history).apply();
    }

    private void addMessageToUI(String text, int gravity) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setPadding(16, 8, 16, 8);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = gravity;
        params.setMargins(0, 4, 0, 4);
        textView.setLayoutParams(params);
        textView.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        messageList.addView(textView);
    }
}
