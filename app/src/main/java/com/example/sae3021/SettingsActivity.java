package com.example.sae3021;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
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

        EditText passwordConfirm = findViewById(R.id.passwordConfirm);
        Button deleteAccountBtn = findViewById(R.id.deleteAccountBtn);

        deleteAccountBtn.setOnClickListener(v -> {
            String password = passwordConfirm.getText().toString();
            if (password.isEmpty()) {
                Toast.makeText(this, "Veuillez entrer votre mot de passe", Toast.LENGTH_SHORT).show();
                return;
            }

            new Thread(() -> {
                try {
                    DataHandler handler = new DataHandler();
                    // Commande du README: Delete,User,password
                    String message = "Delete," + username + "," + password;
                    String response = handler.sendAndReceive(message);
                    handler.close();

                    runOnUiThread(() -> {
                        if (response.startsWith("200")) {
                            Toast.makeText(this, "Compte supprimé", Toast.LENGTH_SHORT).show();
                            // Nettoyer la session et quitter
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
                }
            }).start();
        });
    }
}
