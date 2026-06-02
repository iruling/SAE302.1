package com.example.sae3021;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ChatActivity extends AppCompatActivity {
    private String contactName;
    private LinearLayout messageList;
    private EditText messageInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        contactName = getIntent().getStringExtra("contactName");
        
        TextView chatTitle = findViewById(R.id.chatTitle);
        if (contactName != null) {
            chatTitle.setText("Chat avec " + contactName);
        }

        messageList = findViewById(R.id.messageList);
        messageInput = findViewById(R.id.messageInput);
        Button sendBtn = findViewById(R.id.sendBtn);

        sendBtn.setOnClickListener(v -> {
            String msg = messageInput.getText().toString().trim();
            if (!msg.isEmpty()) {
                // Pour l'instant on affiche juste un Toast
                Toast.makeText(this, "Envoi à " + contactName + " : " + msg, Toast.LENGTH_SHORT).show();
                messageInput.setText("");
            }
        });
    }
}
