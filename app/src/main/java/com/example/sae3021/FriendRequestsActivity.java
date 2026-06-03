package com.example.sae3021;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FriendRequestsActivity extends AppCompatActivity {
    private LinearLayout requestsList;
    private TextView debugText;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_requests);

        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        username = prefs.getString("username", "");

        requestsList = findViewById(R.id.requestsList);
        debugText = findViewById(R.id.debugText);
        Button refreshRequestsBtn = findViewById(R.id.refreshRequestsBtn);
        refreshRequestsBtn.setOnClickListener(v -> loadRequests());

        loadRequests();
    }

    private void loadRequests() {
        if (username.isEmpty()) {
            Toast.makeText(this, "Session invalide", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Charger d'abord les demandes sauvegardées par MainActivity
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String savedRequests = prefs.getString("pending_requests", "");
        List<String> requests = new ArrayList<>();
        if (!savedRequests.isEmpty()) {
            for (String u : savedRequests.split(",")) {
                if (!u.trim().isEmpty()) requests.add(u.trim());
            }
        }

        new Thread(() -> {
            DataHandler handler = null;
            try {
                handler = new DataHandler();
                String message = "Update," + username;
                
                runOnUiThread(() -> debugText.append("📤 Envoyé: " + message + "\n"));
                
                String response = handler.sendAndReceive(message);
                
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    debugText.append("📥 Reçu: " + response + "\n");
                    // ... (reste du log)
                });
                
                List<String> newRequests = parseRequestsFromUpdate(response);
                
                // Fusionner avec les anciennes sans doublons
                for (String nr : newRequests) {
                    if (!requests.contains(nr)) requests.add(nr);
                }
                
                // Mettre à jour les SharedPreferences avec la liste complète fusionnée
                updateStoredRequests(requests);

                runOnUiThread(() -> displayRequests(requests));
            } catch (IOException e) {
                // En cas d'erreur réseau, on affiche quand même les demandes locales
                runOnUiThread(() -> {
                    displayRequests(requests);
                    debugText.append("❌ Erreur réseau, affichage des données locales\n");
                });
            } finally {
                if (handler != null) handler.close();
            }
        }).start();
    }

    private void updateStoredRequests(List<String> requests) {
        StringBuilder sb = new StringBuilder();
        for (String r : requests) {
            if (sb.length() > 0) sb.append(",");
            sb.append(r);
        }
        getSharedPreferences("session", MODE_PRIVATE).edit().putString("pending_requests", sb.toString()).apply();
    }

    private List<String> parseRequestsFromUpdate(String response) {
        List<String> requests = new ArrayList<>();
        if (response == null || !response.contains("DATA;")) {
            return requests;
        }

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
                        String[] users = list.split(",");
                        for (String u : users) {
                            if (!u.trim().isEmpty()) {
                                requests.add(u.trim());
                            }
                        }
                    }
                } else if (upper.startsWith("FRIEND_RESPONSE=")) {
                    String responsePart = s.substring("FRIEND_RESPONSE=".length());
                    if (responsePart.contains(":")) {
                        String[] resParts = responsePart.split(":");
                        String friendName = resParts[0];
                        String status = resParts[1];
                        if ("ACCEPTED".equalsIgnoreCase(status)) {
                            addFriendToSession(friendName);
                            removeFromStoredRequests(friendName);
                        } else if ("REFUSED".equalsIgnoreCase(status)) {
                            removeFromStoredRequests(friendName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return requests;
    }

    private void displayRequests(List<String> requests) {
        requestsList.removeAllViews();
        if (requests.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("Aucune demande");
            requestsList.addView(emptyText);
            return;
        }

        for (String requestUsername : requests) {
            requestsList.addView(buildRequestItem(requestUsername));
        }
    }

    private View buildRequestItem(String requestUsername) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(12, 12, 12, 12);
        item.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView usernameText = new TextView(this);
        usernameText.setText(requestUsername);
        usernameText.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        Button acceptBtn = new Button(this);
        acceptBtn.setText("Accepter");
        // F_acc,Src_User,Dst_User,1 (1 = accepter)
        acceptBtn.setOnClickListener(v -> respondToRequest(requestUsername, 1));

        Button refuseBtn = new Button(this);
        refuseBtn.setText("Refuser");
        // F_acc,Src_User,Dst_User,0 (0 = refuser)
        refuseBtn.setOnClickListener(v -> respondToRequest(requestUsername, 0));

        item.addView(usernameText);
        item.addView(acceptBtn);
        item.addView(refuseBtn);

        return item;
    }

    private void respondToRequest(String friendUsername, int status) {
        new Thread(() -> {
            DataHandler handler = null;
            try {
                handler = new DataHandler();
                // F_acc,Src_User,Dst_User,0or1
                String message = "F_acc," + username + "," + friendUsername + "," + status;
                
                runOnUiThread(() -> debugText.append("📤 Envoyé: " + message + "\n"));
                
                String response = handler.sendAndReceive(message);
                
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    debugText.append("📥 Reçu: " + response + "\n");
                    
                    if (response.startsWith("200")) {
                        removeFromStoredRequests(friendUsername);
                        if (status == 1) {
                            addFriendToSession(friendUsername);
                        }
                    }
                    
                    Toast.makeText(this, response, Toast.LENGTH_SHORT).show();
                    loadRequests();
                });
            } catch (IOException e) {
                // ... (reste du catch)
            } finally {
                // ... (reste du finally)
            }
        }).start();
    }

    private void addFriendToSession(String friendName) {
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String friends = prefs.getString("initial_friends", "");
        
        if (friends.isEmpty()) {
            friends = friendName;
        } else {
            // Vérifier les doublons
            String[] parts = friends.split(",");
            boolean exists = false;
            for (String p : parts) if (p.trim().equals(friendName)) { exists = true; break; }
            if (!exists) friends += "," + friendName;
        }
        prefs.edit().putString("initial_friends", friends).apply();
    }

    private void removeFromStoredRequests(String userToRemove) {
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String saved = prefs.getString("pending_requests", "");
        if (saved.isEmpty()) return;

        List<String> remaining = new ArrayList<>();
        for (String u : saved.split(",")) {
            if (!u.trim().equals(userToRemove)) {
                remaining.add(u.trim());
            }
        }
        updateStoredRequests(remaining);
    }
}
