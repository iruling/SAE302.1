package com.example.sae3021;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class AddFriendActivity extends AppCompatActivity {
    private TextView debugText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String username = prefs.getString("username", "");

        EditText targetUsernameInput = findViewById(R.id.targetUsernameInput);
        Button sendRequestBtn = findViewById(R.id.sendRequestBtn);
        debugText = findViewById(R.id.debugText);

        sendRequestBtn.setOnClickListener(v -> {
            String targetUsername = targetUsernameInput.getText().toString().trim();
            if (targetUsername.isEmpty()) {
                Toast.makeText(this, "Entrez un utilisateur", Toast.LENGTH_SHORT).show();
                return;
            }
            if (username.isEmpty()) {
                Toast.makeText(this, "Session invalide", Toast.LENGTH_SHORT).show();
                return;
            }
            new Thread(() -> {
                try {
                    DataHandler handler = new DataHandler();
                    // F_add,Src_User,Dst_User
                    String message = "F_add," + username + "," + targetUsername;
                    
                    runOnUiThread(() -> debugText.append("📤 Envoyé: " + message + "\n"));
                    
                    String response = handler.sendAndReceive(message);
                    handler.close();
                    
                    runOnUiThread(() -> {
                        debugText.append("📥 Reçu: " + response + "\n");
                        Toast.makeText(this, response, Toast.LENGTH_SHORT).show();
                    });
                } catch (IOException e) {
                    runOnUiThread(() -> {
                        debugText.append("❌ Erreur: " + e.getMessage() + "\n");
                        Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        });
    }
}
