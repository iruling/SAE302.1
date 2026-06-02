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

public class ManageGroupActivity extends AppCompatActivity {
    private String groupName;
    private String username;
    private TextView debugConsole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_group);

        groupName = getIntent().getStringExtra("groupName");
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        username = prefs.getString("username", "");

        TextView groupTitle = findViewById(R.id.groupTitle);
        if (groupName != null) {
            groupTitle.setText("Groupe : " + groupName);
        }

        EditText memberUsernameInput = findViewById(R.id.memberUsernameInput);
        Button addMemberBtn = findViewById(R.id.addMemberBtn);
        debugConsole = findViewById(R.id.debugConsole);

        addMemberBtn.setOnClickListener(v -> {
            String targetMember = memberUsernameInput.getText().toString().trim();
            if (targetMember.isEmpty()) {
                Toast.makeText(this, "Entrez un utilisateur", Toast.LENGTH_SHORT).show();
                return;
            }
            if (groupName == null || groupName.isEmpty()) {
                Toast.makeText(this, "Erreur : Nom de groupe inconnu", Toast.LENGTH_SHORT).show();
                return;
            }

            addMember(targetMember);
        });
    }

    private void addMember(String targetMember) {
        new Thread(() -> {
            DataHandler handler = null;
            try {
                handler = new DataHandler();
                // G_Add_M,Src_User,G_Name,User
                String cmd = "G_Add_M," + username + "," + groupName + "," + targetMember;
                
                runOnUiThread(() -> debugConsole.append("📤 CMD: " + cmd + "\n"));
                
                String response = handler.sendAndReceive(cmd);
                
                runOnUiThread(() -> {
                    debugConsole.append("📥 REP: " + response + "\n");
                    Toast.makeText(this, response, Toast.LENGTH_SHORT).show();
                    
                    if (response.startsWith("200")) {
                        findViewById(R.id.memberUsernameInput).setEnabled(true);
                        ((EditText)findViewById(R.id.memberUsernameInput)).setText("");
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    debugConsole.append("❌ ERR: " + e.getMessage() + "\n");
                    Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show();
                });
            } finally {
                if (handler != null) handler.close();
            }
        }).start();
    }
}
