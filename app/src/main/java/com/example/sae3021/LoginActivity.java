package com.example.sae3021;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        EditText username = findViewById(R.id.username);
        EditText password = findViewById(R.id.password);
        Button loginBtn = findViewById(R.id.loginBtn);
        
        loginBtn.setOnClickListener(v -> {
            String user = username.getText().toString();
            String pass = password.getText().toString();
            
            // Envoyer au serveur en arrière-plan
            new Thread(() -> {
                try {
                    DataHandler handler = new DataHandler();
                    String message = "connect," + user + "," + pass;
                    handler.sendMessage(message);
                    
                    String response = handler.receiveMessage();
                    handler.close();
                    
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, response, Toast.LENGTH_SHORT).show();
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, "Erreur connexion", Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        });
    }
}