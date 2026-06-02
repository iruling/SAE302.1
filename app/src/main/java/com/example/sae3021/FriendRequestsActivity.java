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
    private String username;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_requests);

        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        username = prefs.getString("username", "");
        token = prefs.getString("token", "");

        requestsList = findViewById(R.id.requestsList);
        Button refreshRequestsBtn = findViewById(R.id.refreshRequestsBtn);
        refreshRequestsBtn.setOnClickListener(v -> loadRequests());

        loadRequests();
    }

    private void loadRequests() {
        if (username.isEmpty() || token.isEmpty()) {
            Toast.makeText(this, "Session invalide", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            try {
                DataHandler handler = new DataHandler();
                String response = handler.sendAndReceive("getFriendsRequests," + username + "," + token);
                handler.close();
                List<String> requests = parseRequests(response);
                runOnUiThread(() -> displayRequests(requests));
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private List<String> parseRequests(String response) {
        List<String> requests = new ArrayList<>();
        if (response == null || response.trim().isEmpty()) {
            return requests;
        }
        String trimmed = response.trim();
        if (trimmed.matches("\\d+")) {
            return requests;
        }
        String[] values = trimmed.split(",");
        for (String value : values) {
            String friendUsername = value.trim();
            if (!friendUsername.isEmpty() && !friendUsername.matches("\\d+")
                    && !"OK".equalsIgnoreCase(friendUsername)
                    && !"200".equals(friendUsername)) {
                requests.add(friendUsername);
            }
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
        acceptBtn.setOnClickListener(v -> respondToRequest("acceptFriend", requestUsername));

        Button refuseBtn = new Button(this);
        refuseBtn.setText("Refuser");
        refuseBtn.setOnClickListener(v -> respondToRequest("refuseFriend", requestUsername));

        item.addView(usernameText);
        item.addView(acceptBtn);
        item.addView(refuseBtn);

        return item;
    }

    private void respondToRequest(String action, String friendUsername) {
        new Thread(() -> {
            try {
                DataHandler handler = new DataHandler();
                String response = handler.sendAndReceive(action + "," + username + "," + friendUsername + "," + token);
                handler.close();
                runOnUiThread(() -> {
                    Toast.makeText(this, response, Toast.LENGTH_SHORT).show();
                    loadRequests();
                });
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Erreur réseau", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
