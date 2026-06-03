package com.example.sae3021;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class CreateActivity extends AppCompatActivity {
    private TextView debugText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        EditText username = findViewById(R.id.username);
        EditText password = findViewById(R.id.password);
        EditText confirmPassword = findViewById(R.id.confirmPassword);
        Button registerBtn = findViewById(R.id.registerBtn);
        debugText = findViewById(R.id.debugText);

        registerBtn.setOnClickListener(v -> {
            String user = username.getText().toString().trim();
            String pass = password.getText().toString().trim();
            String confirmPass = confirmPassword.getText().toString().trim();

            if (user.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pass.equals(confirmPass)) {
                Toast.makeText(CreateActivity.this, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show();
                return;
            }

            if (user.contains(",") || user.contains(";") || pass.contains(",") || pass.contains(";")) {
                Toast.makeText(CreateActivity.this, "Caractères , ou ; interdits", Toast.LENGTH_SHORT).show();
                return;
            }

            new Thread(() -> {
                try {
                    DataHandler handler = new DataHandler();
                    String message = "Create," + user + "," + pass;

                    runOnUiThread(() -> {
                        debugText.append("📤 Envoyé: " + message + "\n");
                    });

                    handler.sendMessage(message);
                    String response = handler.receiveMessage();
                    handler.close();

                    runOnUiThread(() -> {
                        if (isFinishing()) return;
                        debugText.append("📥 Reçu: " + response + "\n");
                        Toast.makeText(CreateActivity.this, response, Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        debugText.append("❌ Erreur: " + e.getMessage() + "\n");
                    });
                }
            }).start();
        });
    }
}