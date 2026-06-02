package com.example.sae3021;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private LinearLayout friendsList;
    private String username;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Récupérer le token et l'username de SharedPreferences
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        token = prefs.getString("token", "");
        username = prefs.getString("username", "");

        friendsList = findViewById(R.id.friendsList);
        ImageButton addFriendBtn = findViewById(R.id.addFriendBtn);

        addFriendBtn.setOnClickListener(v -> addFriend());

        loadFriends();
    }

    private void loadFriends() {
        new Thread(() -> {
            try {
                DataHandler handler = new DataHandler();
                String message = "getFriends," + username + "," + token;
                handler.sendMessage(message);
                String response = handler.receiveMessage();
                handler.close();

                parseAndDisplayFriends(response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void parseAndDisplayFriends(String response) {
        String[] friends = response.split(",");
        runOnUiThread(() -> {
            for (String friend : friends) {
                addFriendItem(friend);
            }
        });
    }

    private void addFriendItem(String friendName) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(16, 16, 16, 16);
        item.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                120
        ));
        item.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));

        Button avatar = new Button(this);
        avatar.setText("👤");
        avatar.setLayoutParams(new LinearLayout.LayoutParams(60, 60));
        avatar.setEnabled(false);

        Button nameBtn = new Button(this);
        nameBtn.setText(friendName);
        nameBtn.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));
        nameBtn.setOnClickListener(v -> openChat(friendName));

        ImageButton menuBtn = new ImageButton(this);
        menuBtn.setImageResource(android.R.drawable.ic_menu_more);
        menuBtn.setLayoutParams(new LinearLayout.LayoutParams(60, 60));
        menuBtn.setOnClickListener(v -> showMenu(friendName));

        item.addView(avatar);
        item.addView(nameBtn);
        item.addView(menuBtn);

        friendsList.addView(item);
    }

    private void addFriend() {
        // Ouvrir dialog ou activity pour ajouter ami
    }

    private void openChat(String friendName) {
        // Ouvrir le chat avec cet ami
    }

    private void showMenu(String friendName) {
        // Afficher menu contextuel
    }
}
