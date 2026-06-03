package com.example.sae3021;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String username = prefs.getString("username", "");

        // --- Section Suppression de compte ---
        EditText passwordConfirm = findViewById(R.id.passwordConfirm);
        Button deleteAccountBtn = findViewById(R.id.deleteAccountBtn);

        deleteAccountBtn.setOnClickListener(v -> {
            String password = passwordConfirm.getText().toString();
            if (password.isEmpty()) {
                Toast.makeText(this, "Veuillez entrer votre mot de passe", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.contains(",") || password.contains(";")) {
                Toast.makeText(this, "Caractères , ou ; interdits", Toast.LENGTH_SHORT).show();
                return;
            }

            new Thread(() -> {
                DataHandler handler = null;
                try {
                    handler = new DataHandler();
                    String message = "Delete," + username + "," + password;
                    String response = handler.sendAndReceive(message);
                    
                    runOnUiThread(() -> {
                        if (isFinishing()) return;
                        if (response.startsWith("200")) {
                            Toast.makeText(this, response, Toast.LENGTH_SHORT).show();
                            prefs.edit().clear().apply();
                            Intent intent = new Intent(this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(this, "Erreur: " + response, Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (IOException e) {
                    runOnUiThread(() -> Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show());
                } finally {
                    if (handler != null) handler.close();
                }
            }).start();
        });

        // --- Section Console Debug (Manuel) ---
        EditText customCommandInput = findViewById(R.id.customCommandInput);
        Button sendCustomCmdBtn = findViewById(R.id.sendCustomCmdBtn);
        TextView debugConsole = findViewById(R.id.debugConsole);

        sendCustomCmdBtn.setOnClickListener(v -> {
            String cmd = customCommandInput.getText().toString().trim();
            if (cmd.isEmpty()) return;

            new Thread(() -> {
                DataHandler handler = null;
                try {
                    handler = new DataHandler();
                    
                    runOnUiThread(() -> debugConsole.append("📤 CMD: " + cmd + "\n"));
                    
                    String response = handler.sendAndReceive(cmd);
                    
                    runOnUiThread(() -> {
                        if (isFinishing()) return;
                        debugConsole.append("📥 REP: " + response + "\n");
                    });
                } catch (IOException e) {
                    runOnUiThread(() -> debugConsole.append("❌ ERR: " + e.getMessage() + "\n"));
                } finally {
                    if (handler != null) handler.close();
                }
            }).start();
        });
    }
}
