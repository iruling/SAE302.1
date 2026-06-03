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

    private final Runnable refreshDataTask = new Runnable() {
        @Override
        public void run() {
            // Envoi automatique de la commande Update (Upload) toutes les 30 secondes
            loadFriendRequestsCount();
            refreshHandler.postDelayed(this, 30000); 
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
        TextView currentUserDisplay = findViewById(R.id.currentUserDisplay);

        if (currentUserDisplay != null) {
            currentUserDisplay.setText(username);
        }
        
        ImageButton logoutBtn = findViewById(R.id.logoutBtn);
        ImageButton addFriendBtn = findViewById(R.id.addFriendBtn);
        ImageButton friendRequestsBtn = findViewById(R.id.friendRequestsBtn);
        ImageButton settingsBtn = findViewById(R.id.settingsBtn);
        ImageButton uploadBtn = findViewById(R.id.uploadBtn);
        ImageButton createGroupBtn = findViewById(R.id.createGroupBtn);

        logoutBtn.setOnClickListener(v -> logout());
        addFriendBtn.setOnClickListener(v -> addFriend());
        friendRequestsBtn.setOnClickListener(v -> openFriendRequests());
        settingsBtn.setOnClickListener(v -> openSettings());
        uploadBtn.setOnClickListener(v -> performUpload());
        createGroupBtn.setOnClickListener(v -> openCreateGroup());

        // Charger les données de session (remplies lors du Connect)
        loadDataFromSession();
    }

    private void loadDataFromSession() {
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String savedFriends = prefs.getString("initial_friends", "");
        String savedGroups = prefs.getString("initial_groups", "");

        parseAndDisplayItems(savedFriends, friendsList, true);
        parseAndDisplayItems(savedGroups, groupsList, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshHandler.post(refreshDataTask);
        loadDataFromSession();
    }

    @Override
    protected void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshDataTask);
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
            // On ne filtre plus les chiffres, car un pseudo peut en contenir (ex: tom1)
            // On filtre uniquement le code 200 et OK
            if (!user.isEmpty() && !"OK".equalsIgnoreCase(user) && !"200".equals(user)) {
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
            else openGroupChat(name);
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
                
                // parseUpdateResponse met à jour SharedPreferences
                parseUpdateResponse(response);
                
                // On récupère le nombre total de demandes en attente (stockées + nouvelles)
                int totalCount = getStoredRequestsCount();
                
                runOnUiThread(() -> {
                    if (!isFinishing()) {
                        updateRequestsBadge(totalCount);
                        // Rafraîchir l'affichage des amis/groupes au cas où un Update a apporté des modifs
                        loadDataFromSession();
                    }
                });
            } catch (java.net.SocketTimeoutException e) {
                // Timeout silencieux pour l'utilisateur, mais on rafraîchit l'UI depuis le cache
                runOnUiThread(() -> {
                    if (!isFinishing()) loadDataFromSession();
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private int getStoredRequestsCount() {
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String saved = prefs.getString("pending_requests", "");
        if (saved.isEmpty()) return 0;
        return saved.split(",").length;
    }

    private int parseUpdateResponse(String response) {
        if (response == null || !response.contains("DATA;")) {
            return 0;
        }
        int count = 0;
        try {
            int dataIndex = response.indexOf("DATA;");
            String dataPart = response.substring(dataIndex + 5);
            
            String[] segments = dataPart.split(";");
            for (String segment : segments) {
                String s = segment.trim();
                String upper = s.toUpperCase();
                
                if (upper.startsWith("FRIEND_REQUEST=")) {
                    String list = s.substring("FRIEND_REQUEST=".length());
                    if (!list.isEmpty()) {
                        savePendingRequests(list);
                        String[] items = list.split(",");
                        for (String item : items) {
                            if (!item.trim().isEmpty()) {
                                count++;
                            }
                        }
                    }
                } else if (upper.startsWith("FRIEND_ACCEPTED=")) {
                    String friendName = s.substring("FRIEND_ACCEPTED=".length());
                    if (!friendName.isEmpty()) {
                        addFriendToSession(friendName);
                    }
                } else if (upper.startsWith("MSG=")) {
                    String messagesPart = s.substring("MSG=".length());
                    if (!messagesPart.isEmpty()) {
                        saveReceivedMessages(messagesPart);
                    }
                } else if (upper.startsWith("GROUPS=") || upper.startsWith("GROUP_JOIN=")) { 
                    String groupsPart = s.substring(s.indexOf("=") + 1);
                    if (!groupsPart.isEmpty()) {
                        updateGroupsInSession(groupsPart);
                    }
                } else if (upper.startsWith("GROUP_MSG=")) {
                    String groupMessagesPart = s.substring("GROUP_MSG=".length());
                    if (!groupMessagesPart.isEmpty()) {
                        saveReceivedGroupMessages(groupMessagesPart);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    private void saveReceivedMessages(String messagesPart) {
        // Format: src:dst:content,src:dst:content
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String history = prefs.getString("chat_history", "");
        
        if (history.isEmpty()) {
            history = messagesPart;
        } else {
            history += "," + messagesPart;
        }
        
        prefs.edit().putString("chat_history", history).apply();
        
        runOnUiThread(() -> {
            Toast.makeText(this, "Nouveaux messages reçus !", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveReceivedGroupMessages(String messagesPart) {
        // Format: src:group:content,src:group:content
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String history = prefs.getString("group_chat_history", "");
        
        if (history.isEmpty()) {
            history = messagesPart;
        } else {
            history += "," + messagesPart;
        }
        
        prefs.edit().putString("group_chat_history", history).apply();
        
        runOnUiThread(() -> {
            Toast.makeText(this, "Nouveaux messages de groupe !", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateGroupsInSession(String newList) {
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String existing = prefs.getString("initial_groups", "");
        
        java.util.Set<String> allGroups = new java.util.HashSet<>();
        if (!existing.isEmpty()) {
            for (String g : existing.split(",")) allGroups.add(g.trim());
        }
        for (String g : newList.split(",")) allGroups.add(g.trim());
        
        StringBuilder sb = new StringBuilder();
        for (String g : allGroups) {
            if (!g.isEmpty()) {
                if (sb.length() > 0) sb.append(",");
                sb.append(g);
            }
        }
        prefs.edit().putString("initial_groups", sb.toString()).apply();
    }

    private void addFriendToSession(String friendName) {
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String friends = prefs.getString("initial_friends", "");
        
        if (friends.isEmpty()) {
            friends = friendName;
        } else {
            String[] parts = friends.split(",");
            boolean exists = false;
            for (String p : parts) if (p.trim().equals(friendName)) { exists = true; break; }
            if (!exists) friends += "," + friendName;
        }
        prefs.edit().putString("initial_friends", friends).apply();
    }

    private void savePendingRequests(String newList) {
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String existing = prefs.getString("pending_requests", "");
        
        // Fusionner sans doublons
        String[] newUsers = newList.split(",");
        java.util.Set<String> allRequests = new java.util.HashSet<>();
        
        if (!existing.isEmpty()) {
            for (String u : existing.split(",")) allRequests.add(u.trim());
        }
        for (String u : newUsers) allRequests.add(u.trim());
        
        StringBuilder sb = new StringBuilder();
        for (String u : allRequests) {
            if (!u.isEmpty()) {
                if (sb.length() > 0) sb.append(",");
                sb.append(u);
            }
        }
        prefs.edit().putString("pending_requests", sb.toString()).apply();
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

    private void performUpload() {
        Toast.makeText(this, "Upload en cours...", Toast.LENGTH_SHORT).show();
        loadFriendRequestsCount();
    }

    private void openChat(String name) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("contactName", name);
        startActivity(intent);
    }

    private void openManageGroup(String name) {
        Intent intent = new Intent(this, ManageGroupActivity.class);
        intent.putExtra("groupName", name);
        startActivity(intent);
    }

    private void openGroupChat(String name) {
        Intent intent = new Intent(this, GroupChatActivity.class);
        intent.putExtra("groupName", name);
        startActivity(intent);
    }
}
