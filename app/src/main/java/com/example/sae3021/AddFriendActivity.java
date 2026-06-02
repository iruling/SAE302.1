package com.example.sae3021;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class AddFriendActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String username = prefs.getString("username", "");
        String token = prefs.getString("token", "");

        EditText targetUsernameInput = findViewById(R.id.targetUsernameInput);
        Button sendRequestBtn = findViewById(R.id.sendRequestBtn);

        sendRequestBtn.setOnClickListener(v -> {
            String targetUsername = targetUsernameInput.getText().toString().trim();
            if (targetUsername.isEmpty()) {
                Toast.makeText(this, "Entrez un utilisateur", Toast.LENGTH_SHORT).show();
                return;
            }
            if (username.isEmpty() || token.isEmpty()) {
                Toast.makeText(this, "Session invalide", Toast.LENGTH_SHORT).show();
                return;
            }
            new Thread(() -> {
                try {
                    DataHandler handler = new DataHandler();
                    String response = handler.sendAndReceive("addFriend," + username + "," + targetUsername + "," + token);
                    handler.close();
                    runOnUiThread(() -> Toast.makeText(this, response, Toast.LENGTH_SHORT).show());
                } catch (IOException e) {
                    runOnUiThread(() -> Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show());
                }
            }).start();
        });
    }
}
