package com.example.sae3021;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private LinearLayout friendsList;
    private TextView requestsBadge;
    private String username;
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRequestsTask = new Runnable() {
        @Override
        public void run() {
            loadFriendRequestsCount();
            refreshHandler.postDelayed(this, 10000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Récupérer l'username de SharedPreferences
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        username = prefs.getString("username", "");

        friendsList = findViewById(R.id.friendsList);
        requestsBadge = findViewById(R.id.requestsBadge);
        ImageButton addFriendBtn = findViewById(R.id.addFriendBtn);
        ImageButton friendRequestsBtn = findViewById(R.id.friendRequestsBtn);
        ImageButton createGroupBtn = findViewById(R.id.createGroupBtn);

        addFriendBtn.setOnClickListener(v -> addFriend());
        friendRequestsBtn.setOnClickListener(v -> openFriendRequests());
        createGroupBtn.setOnClickListener(v -> openCreateGroup());

        loadFriends();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshHandler.post(refreshRequestsTask);
    }

    @Override
    protected void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshRequestsTask);
    }

    private void loadFriends() {
        new Thread(() -> {
            try {
                DataHandler handler = new DataHandler();
                // README suggère que Connect renvoie la liste,
                // mais si on a besoin de rafraîchir, on peut supposer une commande de type getFriends
                String message = "getFriends," + username;
                String response = handler.sendAndReceive(message);
                handler.close();

                parseAndDisplayFriends(response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void parseAndDisplayFriends(String response) {
        List<String> friends = parseUserList(response);
        runOnUiThread(() -> {
            friendsList.removeAllViews();
            for (String friend : friends) {
                addFriendItem(friend);
            }
        });
    }

    private void loadFriendRequestsCount() {
        if (username == null || username.isEmpty()) {
            return;
        }
        new Thread(() -> {
            try {
                DataHandler handler = new DataHandler();
                // Utiliser la commande Update du README
                String response = handler.sendAndReceive("Update," + username);
                handler.close();

                // Parser la réponse Code,Type,Message
                // Exemple: 200,UPDATE,DATA;MSG=...;FRIEND_REQUEST=...
                int count = parseUpdateResponse(response);
                runOnUiThread(() -> updateRequestsBadge(count));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private int parseUpdateResponse(String response) {
        if (response == null || !response.startsWith("200,UPDATE,DATA")) {
            return 0;
        }

        int count = 0;
        try {
            // Extraire la partie DATA
            String dataPart = response.substring(response.indexOf("DATA;") + 5);
            String[] segments = dataPart.split(";");
            for (String segment : segments) {
                if (segment.startsWith("FRIEND_REQUEST=")) {
                    String list = segment.substring("FRIEND_REQUEST=".length());
                    if (!list.isEmpty()) {
                        count = list.split(",").length;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    private List<String> parseUserList(String response) {
        List<String> users = new ArrayList<>();
        if (response == null || response.trim().isEmpty()) {
            return users;
        }
        String[] values = response.split(",");
        for (String value : values) {
            String user = value.trim();
            if (!user.isEmpty() && !user.matches("\\d+") && !"OK".equalsIgnoreCase(user) && !"200".equals(user)) {
                users.add(user);
            }
        }
        return users;
    }

    private void updateRequestsBadge(int count) {
        if (count > 0) {
            requestsBadge.setText(String.valueOf(count));
            requestsBadge.setVisibility(TextView.VISIBLE);
        } else {
            requestsBadge.setVisibility(TextView.GONE);
        }
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
        startActivity(new Intent(this, AddFriendActivity.class));
    }

    private void openFriendRequests() {
        startActivity(new Intent(this, FriendRequestsActivity.class));
    }

    private void openCreateGroup() {
        startActivity(new Intent(this, CreateGroupActivity.class));
    }

    private void openChat(String friendName) {
        Toast.makeText(this, "Chat avec " + friendName, Toast.LENGTH_SHORT).show();
    }

    private void showMenu(String friendName) {
        Toast.makeText(this, "Menu de " + friendName, Toast.LENGTH_SHORT).show();
    }
}
