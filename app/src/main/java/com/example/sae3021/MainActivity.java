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
    private LinearLayout groupsList;
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

        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        username = prefs.getString("username", "");

        if (username.isEmpty()) {
            goToLogin();
            return;
        }

        friendsList = findViewById(R.id.friendsList);
        groupsList = findViewById(R.id.groupsList);
        requestsBadge = findViewById(R.id.requestsBadge);
        
        ImageButton logoutBtn = findViewById(R.id.logoutBtn);
        ImageButton addFriendBtn = findViewById(R.id.addFriendBtn);
        ImageButton friendRequestsBtn = findViewById(R.id.friendRequestsBtn);
        ImageButton settingsBtn = findViewById(R.id.settingsBtn);
        ImageButton createGroupBtn = findViewById(R.id.createGroupBtn);

        logoutBtn.setOnClickListener(v -> logout());
        addFriendBtn.setOnClickListener(v -> addFriend());
        friendRequestsBtn.setOnClickListener(v -> openFriendRequests());
        settingsBtn.setOnClickListener(v -> openSettings());
        createGroupBtn.setOnClickListener(v -> openCreateGroup());

        // Charger d'abord les données du cache de connexion si elles existent
        String initialFriends = prefs.getString("initial_friends", "");
        String initialGroups = prefs.getString("initial_groups", "");

        if (!initialFriends.isEmpty()) {
            parseAndDisplayItems(initialFriends, friendsList, true);
        } else {
            loadFriends();
        }

        if (!initialGroups.isEmpty()) {
            parseAndDisplayItems(initialGroups, groupsList, false);
        } else {
            loadGroups();
        }
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

    private void logout() {
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        prefs.edit().clear().apply();
        Toast.makeText(this, "Déconnexion réussie", Toast.LENGTH_SHORT).show();
        goToLogin();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void loadFriends() {
        new Thread(() -> {
            try {
                DataHandler handler = new DataHandler();
                String message = "getFriends," + username;
                String response = handler.sendAndReceive(message);
                handler.close();

                parseAndDisplayItems(response, friendsList, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadGroups() {
        new Thread(() -> {
            try {
                DataHandler handler = new DataHandler();
                String message = "getGroups," + username;
                String response = handler.sendAndReceive(message);
                handler.close();

                parseAndDisplayItems(response, groupsList, false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void parseAndDisplayItems(String response, LinearLayout listLayout, boolean isFriend) {
        List<String> items = parseUserList(response);
        runOnUiThread(() -> {
            listLayout.removeAllViews();
            for (String item : items) {
                addItemView(item, listLayout, isFriend);
            }
        });
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

    private void addItemView(String name, LinearLayout listLayout, boolean isFriend) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(16, 16, 16, 16);
        item.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                120
        ));
        item.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));

        TextView icon = new TextView(this);
        icon.setText(isFriend ? "👤" : "👥");
        icon.setLayoutParams(new LinearLayout.LayoutParams(60, 60));

        Button nameBtn = new Button(this);
        nameBtn.setText(name);
        nameBtn.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));
        nameBtn.setOnClickListener(v -> {
            if (isFriend) openChat(name);
            else openManageGroup(name);
        });

        item.addView(icon);
        item.addView(nameBtn);
        listLayout.addView(item);
    }

    private void loadFriendRequestsCount() {
        if (username == null || username.isEmpty()) {
            return;
        }
        new Thread(() -> {
            try {
                DataHandler handler = new DataHandler();
                String response = handler.sendAndReceive("Update," + username);
                handler.close();
                int count = parseUpdateResponse(response);
                runOnUiThread(() -> updateRequestsBadge(count));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private int parseUpdateResponse(String response) {
        if (response == null || !response.contains("DATA;")) {
            return 0;
        }
        int count = 0;
        try {
            // Extraire tout ce qui est après DATA;
            int dataIndex = response.indexOf("DATA;");
            String dataPart = response.substring(dataIndex + 5);
            
            String[] segments = dataPart.split(";");
            for (String segment : segments) {
                if (segment.startsWith("FRIEND_REQUEST=")) {
                    String list = segment.substring("FRIEND_REQUEST=".length());
                    if (!list.isEmpty()) {
                        String[] items = list.split(",");
                        for (String item : items) {
                            if (!item.trim().isEmpty()) {
                                count++;
                            }
                        }
                    }
                }
                // Ici on pourra ajouter d'autres traitements pour MSG=, GROUP_MSG=, etc.
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    private void updateRequestsBadge(int count) {
        if (count > 0) {
            requestsBadge.setText(String.valueOf(count));
            requestsBadge.setVisibility(TextView.VISIBLE);
        } else {
            requestsBadge.setVisibility(TextView.GONE);
        }
    }

    private void addFriend() {
        startActivity(new Intent(this, AddFriendActivity.class));
    }

    private void openFriendRequests() {
        startActivity(new Intent(this, FriendRequestsActivity.class));
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void openCreateGroup() {
        startActivity(new Intent(this, CreateGroupActivity.class));
    }

    private void openChat(String name) {
        Toast.makeText(this, "Chat avec " + name, Toast.LENGTH_SHORT).show();
    }

    private void openManageGroup(String name) {
        Intent intent = new Intent(this, ManageGroupActivity.class);
        intent.putExtra("groupName", name);
        startActivity(intent);
    }

    private void openGroupChat(String name) {
        Toast.makeText(this, "Groupe: " + name, Toast.LENGTH_SHORT).show();
    }
}
