package com.example.tripsync.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tripsync.R;
import com.example.tripsync.ui.common.EdgeToEdgeHelper;
import com.example.tripsync.ui.common.LocalUserStore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MyGroupActivity extends AppCompatActivity {

    LinearLayout invitesContainer, groupsContainer;
    TextView tvInviteCount, tvGroupCount;
    FirebaseFirestore db;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_group);
        EdgeToEdgeHelper.apply(this);

        invitesContainer = findViewById(R.id.invitesContainer);
        groupsContainer = findViewById(R.id.groupsContainer);
        tvInviteCount = findViewById(R.id.tvInviteCount);
        tvGroupCount = findViewById(R.id.tvGroupCount);
        findViewById(R.id.btnBackGroups).setOnClickListener(v -> finish());

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadInvites();
        loadGroups();
    }

    private void loadInvites() {
        if (auth.getCurrentUser() == null || auth.getCurrentUser().getEmail() == null) {
            return;
        }

        String currentEmail = auth.getCurrentUser().getEmail().toLowerCase(Locale.US);

        db.collection("group_invites")
                .whereEqualTo("inviteeEmail", currentEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    invitesContainer.removeAllViews();
                    int pendingCount = 0;

                    LayoutInflater inflater = LayoutInflater.from(this);
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        if (!"PENDING".equals(doc.getString("status"))) {
                            continue;
                        }

                        pendingCount++;
                        View card = inflater.inflate(R.layout.item_group_invite, invitesContainer, false);
                        ((TextView) card.findViewById(R.id.tvInviteGroupName)).setText(doc.getString("groupName"));
                        ((TextView) card.findViewById(R.id.tvInviteTripName)).setText(doc.getString("tripName"));
                        ((TextView) card.findViewById(R.id.tvInviteFrom)).setText("From: " + doc.getString("inviterEmail"));

                        card.findViewById(R.id.btnAcceptInvite).setOnClickListener(v -> acceptInvite(doc));
                        card.findViewById(R.id.btnRejectInvite).setOnClickListener(v -> rejectInvite(doc.getId()));
                        invitesContainer.addView(card);
                    }

                    tvInviteCount.setText("Pending Invites: " + pendingCount);
                    if (pendingCount == 0) {
                        addEmptyState(invitesContainer, "No pending group invites.");
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load invites", Toast.LENGTH_SHORT).show()
                );
    }

    private void loadGroups() {
        if (auth.getCurrentUser() == null || auth.getCurrentUser().getEmail() == null) {
            return;
        }

        String currentEmail = auth.getCurrentUser().getEmail().toLowerCase(Locale.US);

        db.collection("group_memberships")
                .whereEqualTo("userEmail", currentEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    groupsContainer.removeAllViews();
                    int activeCount = 0;

                    LayoutInflater inflater = LayoutInflater.from(this);
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        if (!"ACTIVE".equals(doc.getString("status"))) {
                            continue;
                        }

                        activeCount++;
                        View card = inflater.inflate(R.layout.item_group_card, groupsContainer, false);

                        ((TextView) card.findViewById(R.id.tvGroupName)).setText(doc.getString("groupName"));
                        ((TextView) card.findViewById(R.id.tvGroupTrip)).setText(doc.getString("tripName"));
                        ((TextView) card.findViewById(R.id.tvGroupRole)).setText("Member: " + doc.getString("userName"));

                        Button openButton = card.findViewById(R.id.btnOpenGroup);
                        openButton.setOnClickListener(v -> {
                            Intent intent = new Intent(this, CollaboratorsActivity.class);
                            intent.putExtra("trip_doc_id", doc.getString("tripDocId"));
                            intent.putExtra("trip_owner_user_id", doc.getString("tripOwnerUserId"));
                            startActivity(intent);
                        });

                        groupsContainer.addView(card);
                    }

                    tvGroupCount.setText("Your Groups: " + activeCount);
                    if (activeCount == 0) {
                        addEmptyState(groupsContainer, "You are not part of any trip groups yet.");
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load your groups", Toast.LENGTH_SHORT).show()
                );
    }

    private void acceptInvite(DocumentSnapshot inviteDoc) {
        if (auth.getCurrentUser() == null || auth.getCurrentUser().getEmail() == null) {
            return;
        }

        String inviteeEmail = auth.getCurrentUser().getEmail().toLowerCase(Locale.US);
        String inviteeName = inviteDoc.getString("inviteeName");
        String groupName = inviteDoc.getString("groupName");
        String tripDocId = inviteDoc.getString("tripDocId");
        String tripOwnerUserId = inviteDoc.getString("tripOwnerUserId");
        String tripName = inviteDoc.getString("tripName");

        if (inviteeName == null || inviteeName.isEmpty()) {
            inviteeName = LocalUserStore.getProfileName(this, auth.getCurrentUser() != null
                    ? auth.getCurrentUser().getEmail() : null, "Group Member");
        }

        if (tripDocId == null || tripOwnerUserId == null) {
            Toast.makeText(this, "Invite data is incomplete", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> member = new HashMap<>();
        member.put("name", inviteeName);
        member.put("email", inviteeEmail);
        member.put("status", "ACCEPTED");
        member.put("role", "MEMBER");
        member.put("updatedAt", System.currentTimeMillis());

        Map<String, Object> membership = new HashMap<>();
        membership.put("groupName", groupName);
        membership.put("tripDocId", tripDocId);
        membership.put("tripOwnerUserId", tripOwnerUserId);
        membership.put("tripName", tripName);
        membership.put("userName", inviteeName);
        membership.put("userEmail", inviteeEmail);
        membership.put("status", "ACTIVE");
        membership.put("updatedAt", System.currentTimeMillis());

        db.collection("users")
                .document(tripOwnerUserId)
                .collection("trips")
                .document(tripDocId)
                .collection("groupMembers")
                .document(inviteeEmail)
                .set(member, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    db.collection("group_memberships")
                            .document(tripOwnerUserId + "_" + tripDocId + "_" + inviteeEmail)
                            .set(membership, SetOptions.merge());

                    db.collection("group_invites")
                            .document(inviteDoc.getId())
                            .update("status", "ACCEPTED");

                    Toast.makeText(this, "Joined group", Toast.LENGTH_SHORT).show();
                    loadInvites();
                    loadGroups();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not accept invite", Toast.LENGTH_SHORT).show()
                );
    }

    private void rejectInvite(String inviteId) {
        db.collection("group_invites")
                .document(inviteId)
                .update("status", "REJECTED")
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Invite dismissed", Toast.LENGTH_SHORT).show();
                    loadInvites();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not reject invite", Toast.LENGTH_SHORT).show()
                );
    }

    private void addEmptyState(LinearLayout container, String message) {
        TextView empty = new TextView(this);
        empty.setText(message);
        empty.setTextColor(0xFF6B7280);
        empty.setPadding(8, 8, 8, 8);
        container.addView(empty);
    }
}
