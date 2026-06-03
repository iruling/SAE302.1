package com.example.sae3021;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.net.URLEncoder;

public class ChatActivity extends AppCompatActivity {
    private String contactName;

    private TextView debugText;
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

            // ✅ Vérifier message vide
            if (msg.isEmpty()) {
                debugText.append("⚠️ Message vide non envoyé\n");
                return;
            }

            // ✅ Encoder pour éviter les virgules / caractères spéciaux
            try {
                msg = URLEncoder.encode(msg, "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
            }

            String finalMsg = msg; // important pour le thread

            new Thread(() -> {
                try {
                    DataHandler handler = new DataHandler();

                    String Message = "Send_Msg," + contactName + "," + finalMsg;

                    runOnUiThread(() -> {
                        debugText.append("📤 Envoyé: " + Message + "\n");
                    });

                    handler.sendMessage(Message);

                    String response = handler.receiveMessage();

                    runOnUiThread(() -> {
                        debugText.append("📥 Réponse: " + response + "\n");
                    });

                    handler.close();

                } catch (IOException e) {
                    e.printStackTrace();

                    runOnUiThread(() -> {
                        debugText.append("❌ Erreur: " + e.getMessage() + "\n");
                    });
                }
            }).start();

            // ✅ Vider le champ après envoi
            messageInput.setText("");
        });
    }
}
