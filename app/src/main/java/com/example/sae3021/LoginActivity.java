package com.example.sae3021;

import android.os.Bundle;
import android.view.View;
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
            Toast.makeText(this, "Username: " + username.getText(), Toast.LENGTH_SHORT).show();
        });
    }
}