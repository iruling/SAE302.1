package com.example.sae3021;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class CreateGroupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String username = prefs.getString("username", "");

        EditText groupNameInput = findViewById(R.id.groupNameInput);
        Button createGroupBtn = findViewById(R.id.createGroupBtn);

        createGroupBtn.setOnClickListener(v -> {
            String groupName = groupNameInput.getText().toString().trim();
            if (groupName.isEmpty()) {
                Toast.makeText(this, "Entrez un nom de groupe", Toast.LENGTH_SHORT).show();
                return;
            }
            if (groupName.contains(",") || groupName.contains(";")) {
                Toast.makeText(this, "Caractères , ou ; interdits", Toast.LENGTH_SHORT).show();
                return;
            }
            if (username.isEmpty()) {
                Toast.makeText(this, "Session invalide", Toast.LENGTH_SHORT).show();
                return;
            }
            new Thread(() -> {
                DataHandler handler = null;
                try {
                    handler = new DataHandler();
                    // G_add,Src_User,G_Name
                    String response = handler.sendAndReceive("G_add," + username + "," + groupName);
                    
                    runOnUiThread(() -> {
                        Toast.makeText(this, response, Toast.LENGTH_SHORT).show();
                        if (response.startsWith("201")) {
                            // Redirection vers l'étape 2 : ajout de membres
                            Intent intent = new Intent(this, ManageGroupActivity.class);
                            intent.putExtra("groupName", groupName);
                            startActivity(intent);
                            finish(); // Fermer l'étape 1
                        }
                    });
                } catch (IOException e) {
                    runOnUiThread(() -> Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show());
                } finally {
                    if (handler != null) handler.close();
                }
            }).start();
        });
    }
}
