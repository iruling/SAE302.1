package com.example.sae3021;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class LoginActivity extends AppCompatActivity {
    private TextView debugText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        EditText username = findViewById(R.id.username);
        EditText password = findViewById(R.id.password);
        Button loginBtn = findViewById(R.id.loginBtn);
        Button signupBtn = findViewById(R.id.signupBtn);
        debugText = findViewById(R.id.debugText);
        Button debugBtn = findViewById(R.id.debugBtn);
        debugBtn.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
        });
        loginBtn.setOnClickListener(v -> {
            String user = username.getText().toString();
            String pass = password.getText().toString();
            
            new Thread(() -> {
                try {
                    DataHandler handler = new DataHandler();
                    String message = "connect," + user + "," + pass;
                    
                    runOnUiThread(() -> {
                        debugText.append("📤 Envoyé: " + message + "\n");
                    });
                    
                    handler.sendMessage(message);
                    String response = handler.receiveMessage();
                    handler.close();
                    
                    runOnUiThread(() -> {
                        debugText.append("📥 Reçu: " + response + "\n");
                        Toast.makeText(LoginActivity.this, response, Toast.LENGTH_SHORT).show();
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        debugText.append("❌ Erreur: " + e.getMessage() + "\n");
                    });
                }
            }).start();
        });
        
        signupBtn.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }
}