package com.example.sae3021;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class GroupChatActivity extends AppCompatActivity {
    private String groupName;
    private LinearLayout groupMessageList;
    private EditText groupMessageInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        groupName = getIntent().getStringExtra("groupName");
        
        TextView groupChatTitle = findViewById(R.id.groupChatTitle);
        if (groupName != null) {
            groupChatTitle.setText("Groupe : " + groupName);
        }

        groupMessageList = findViewById(R.id.groupMessageList);
        groupMessageInput = findViewById(R.id.groupMessageInput);
        Button groupSendBtn = findViewById(R.id.groupSendBtn);

        groupSendBtn.setOnClickListener(v -> {
            String msg = groupMessageInput.getText().toString().trim();
            if (!msg.isEmpty()) {
                // Pour l'instant on affiche juste un Toast
                Toast.makeText(this, "Envoi au groupe " + groupName + " : " + msg, Toast.LENGTH_SHORT).show();
                groupMessageInput.setText("");
            }
        });
    }
}
