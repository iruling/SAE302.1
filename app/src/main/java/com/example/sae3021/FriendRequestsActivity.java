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
        new Thread(() -> {
            DataHandler handler = null;
            try {
                handler = new DataHandler();
                // Utiliser Update pour récupérer les demandes en attente selon le README
                String message = "Update," + username;
                
                runOnUiThread(() -> debugText.append("📤 Envoyé: " + message + "\n"));
                
                String response = handler.sendAndReceive(message);
                
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    debugText.append("📥 Reçu: " + response + "\n");
                    
                    // Analyse du code de retour selon le README
                    String[] parts = response.split(",");
                    if (parts.length > 0) {
                        String code = parts[0];
                        if (response.contains("NO_DATA")) {
                            debugText.append("ℹ️ Aucune donnée disponible\n");
                        } else if (!code.equals("200")) {
                            debugText.append("⚠️ Code d'erreur reçu: " + code + "\n");
                        }
                    }
                });
                
                List<String> requests = parseRequestsFromUpdate(response);
                runOnUiThread(() -> displayRequests(requests));
            } catch (IOException e) {
                runOnUiThread(() -> {
                    debugText.append("❌ Erreur: " + e.getMessage() + "\n");
                    Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show();
                });
            } finally {
                if (handler != null) handler.close();
            }
        }).start();
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
                if (segment.startsWith("FRIEND_REQUEST=")) {
                    String list = segment.substring("FRIEND_REQUEST=".length());
                    if (!list.isEmpty()) {
                        String[] users = list.split(",");
                        for (String u : users) {
                            if (!u.trim().isEmpty()) {
                                requests.add(u.trim());
                            }
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
                    
                    // Vérification sommaire du code de retour
                    String[] parts = response.split(",");
                    if (parts.length > 0 && !parts[0].equals("200")) {
                        debugText.append("⚠️ Erreur lors du traitement (Code " + parts[0] + ")\n");
                    }
                    
                    Toast.makeText(this, response, Toast.LENGTH_SHORT).show();
                    loadRequests();
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    debugText.append("❌ Erreur: " + e.getMessage() + "\n");
                    Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show();
                });
            } finally {
                if (handler != null) handler.close();
            }
        }).start();
    }
}
