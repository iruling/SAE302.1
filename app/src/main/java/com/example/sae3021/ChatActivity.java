package com.example.sae3021;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class ChatActivity extends AppCompatActivity {
    private String contactName;
    private String username;
    private LinearLayout messageList;
    private EditText messageInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        username = prefs.getString("username", "");

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
            String msg = messageInput.getText().toString().trim();
            if (!msg.isEmpty()) {
                // Pour l'instant on affiche juste un Toast (comportement initial)
                Toast.makeText(this, "Envoi à " + contactName + " : " + msg, Toast.LENGTH_SHORT).show();
                messageInput.setText("");
            }
        });
    }

    private void loadLocalHistory() {
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String history = prefs.getString("chat_history", "");
        if (history.isEmpty()) return;

        // Format: src:dst:content,src:dst:content
        String[] messages = history.split(",");
        for (String m : messages) {
            String[] parts = m.split(":", 3);
            if (parts.length >= 3) {
                String src = parts[0].trim();
                String dst = parts[1].trim();
                String content = parts[2];

                if (src.equals(contactName) && dst.equals(username)) {
                    addMessageToUI(contactName + ": " + content, Gravity.START);
                } else if (src.equals(username) && dst.equals(contactName)) {
                    addMessageToUI("Moi: " + content, Gravity.END);
                }
            }
        }
    }

    private void saveSentMessage(String msg) {
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String history = prefs.getString("chat_history", "");
        String newMsg = username + ":" + contactName + ":" + msg;
        
        if (history.isEmpty()) history = newMsg;
        else history += "," + newMsg;
        
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
