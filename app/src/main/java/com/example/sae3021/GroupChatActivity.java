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
import java.util.HashSet;
import java.util.Set;

public class GroupChatActivity extends AppCompatActivity {
    private String groupName;
    private String username;
    private LinearLayout groupMessageList;
    private EditText groupMessageInput;
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
                if (upper.startsWith("GROUP_MSG=")) {
                    String msgPart = s.substring("GROUP_MSG=".length());
                    saveReceivedGroupMessages(msgPart);
                }
            }
        } catch (Exception ignored) {}
    }

    private void saveReceivedGroupMessages(String messagesPart) {
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String history = prefs.getString("group_chat_history", "");
        
        // Format attendu du serveur: Groupe:Emetteur:Contenu
        if (history.isEmpty()) history = messagesPart;
        else history += "," + messagesPart;
        
        prefs.edit().putString("group_chat_history", history).apply();
        
        runOnUiThread(this::loadLocalHistory);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        username = prefs.getString("username", "");
        String password = prefs.getString("password", "");

        groupName = getIntent().getStringExtra("groupName");
        
        TextView groupChatTitle = findViewById(R.id.groupChatTitle);
        if (groupName != null) {
            groupChatTitle.setText("Groupe : " + groupName);
        }

        groupMessageList = findViewById(R.id.groupMessageList);
        groupMessageInput = findViewById(R.id.groupMessageInput);
        Button groupSendBtn = findViewById(R.id.groupSendBtn);

        loadLocalHistory();

        groupSendBtn.setOnClickListener(v -> {
            String rawMsg = groupMessageInput.getText().toString().trim();
            if (rawMsg.isEmpty()) return;

            // ✅ Encoder pour éviter les virgules / caractères spéciaux
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
                    // Protocole: Send_G_Msg,Src_User,G_Name,Msg
                    String command = "Send_G_Msg," + username + "," + groupName + "," + finalMsg;

                    String response = handler.sendAndReceive(command);
                    handler.close();

                    runOnUiThread(() -> {
                        if (isFinishing()) return;
                        if (response.startsWith("200")) {
                            addMessageToUI("Moi: " + rawMsg, Gravity.END);
                            saveSentMessage(rawMsg);
                            groupMessageInput.setText("");
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
        String history = prefs.getString("group_chat_history", "");
        if (history.isEmpty()) return;

        // Format attendu: group:sender:content
        String[] messages = history.split(",");
        for (String m : messages) {
            if (displayedMessages.contains(m)) continue;

            String[] parts = m.split(":", 3);
            if (parts.length >= 3) {
                String targetGroup = parts[0].trim();
                String src = parts[1].trim();
                String content = parts[2];
                
                // Décoder le contenu pour l'affichage
                try {
                    content = URLDecoder.decode(content, "UTF-8");
                } catch (Exception ignored) {}

                if (targetGroup.equals(groupName)) {
                    if (src.equals(username)) {
                        addMessageToUI("Moi: " + content, Gravity.END);
                    } else {
                        addMessageToUI(src + ": " + content, Gravity.START);
                    }
                    displayedMessages.add(m);
                }
            }
        }
    }

    private void saveSentMessage(String msg) {
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String history = prefs.getString("group_chat_history", "");
        String encoded;
        try {
            encoded = URLEncoder.encode(msg, "UTF-8");
        } catch (Exception e) {
            encoded = msg;
        }
        
        // Format: group:sender:content
        String newMsgEntry = groupName + ":" + username + ":" + encoded;
        displayedMessages.add(newMsgEntry);
        
        if (history.isEmpty()) history = newMsgEntry;
        else history += "," + newMsgEntry;
        
        prefs.edit().putString("group_chat_history", history).apply();
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
        groupMessageList.addView(textView);
    }
}
