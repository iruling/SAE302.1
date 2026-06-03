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

public class LoginActivity extends AppCompatActivity {
    private TextView debugText;

    private String normalizeConnectGroupMessages(String rawGMsg) {
        if (rawGMsg == null || rawGMsg.isEmpty()) return "";
        
        // Format serveur (Connect): Sender:Group:Content
        // Format Interne: Group:Sender:Content
        String[] messages = rawGMsg.split(",");
        StringBuilder sb = new StringBuilder();
        
        for (String m : messages) {
            String[] parts = m.split(":", 3);
            if (parts.length >= 3) {
                String sender = parts[0];
                String group = parts[1];
                String content = parts[2];
                
                if (sb.length() > 0) sb.append(",");
                sb.append(group).append(":").append(sender).append(":").append(content);
            }
        }
        return sb.toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_login);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        EditText username = findViewById(R.id.username);
        EditText password = findViewById(R.id.password);
        Button loginBtn = findViewById(R.id.loginBtn);
        Button signupBtn = findViewById(R.id.signupBtn);
        debugText = findViewById(R.id.debugText);

        if (loginBtn == null || signupBtn == null) return;



        loginBtn.setOnClickListener(v -> {
            String user = username.getText().toString().trim();
            String pass = password.getText().toString().trim();

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                return;
            }

            new Thread(() -> {
                try {
                    DataHandler handler = new DataHandler();
                    String message = "Connect," + user + "," + pass;

                    runOnUiThread(() -> {
                        debugText.append("📤 Envoyé: " + message + "\n");
                    });

                    handler.sendMessage(message);
                    String response = handler.receiveMessage();
                    handler.close();

                    runOnUiThread(() -> {
                        if (isFinishing()) return;
                        debugText.append("📥 Reçu: " + response + "\n");

                        // Vérifier si la réponse est 200,CONNECT,OK
                        if (response.startsWith("200,CONNECT,OK")) {
                            String friendsList = "";
                            String groupsList = "";
                            String initialMessages = "";
                            String initialGroupMessages = "";

                            try {
                                // Format attendu: 200,CONNECT,OK,Token;FRIENDS=Pierre,Paul;GROUPS=Sae,Dev;MSG=...;MSG_G=...
                                String[] mainParts = response.split(";");

                                for (int i = 1; i < mainParts.length; i++) {
                                    String block = mainParts[i].trim();
                                    String upperBlock = block.toUpperCase();
                                    
                                    if (upperBlock.startsWith("FRIENDS=")) {
                                        friendsList = block.substring("FRIENDS=".length());
                                    } else if (upperBlock.startsWith("GROUPS=")) {
                                        groupsList = block.substring("GROUPS=".length());
                                    } else if (upperBlock.startsWith("MSG=")) {
                                        initialMessages = block.substring("MSG=".length());
                                    } else if (upperBlock.startsWith("MSG_G=")) {
                                        String rawGMsg = block.substring("MSG_G=".length());
                                        initialGroupMessages = normalizeConnectGroupMessages(rawGMsg);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            // Sauvegarde des informations (on vide tout avant pour repartir sur une base saine)
                            SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.clear(); 
                            
                            editor.putString("username", user);
                            editor.putString("password", pass);
                            editor.putString("initial_friends", friendsList);
                            editor.putString("initial_groups", groupsList);
                            
                            // Sauvegarder les messages de connexion s'il y en a
                            if (!initialMessages.isEmpty()) {
                                editor.putString("chat_history", initialMessages);
                            }
                            if (!initialGroupMessages.isEmpty()) {
                                editor.putString("group_chat_history", initialGroupMessages);
                            }

                            editor.apply();

                            Toast.makeText(LoginActivity.this, "Connexion réussie!", Toast.LENGTH_SHORT).show();

                            // Aller à MainActivity
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, response, Toast.LENGTH_SHORT).show();
                        }
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
            Intent intent = new Intent(LoginActivity.this, CreateActivity.class);
            startActivity(intent);
        });
    }
}