package com.example.tripsync.ui.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tripsync.R;
import com.example.tripsync.ui.common.EdgeToEdgeHelper;
import com.example.tripsync.ui.common.LocalUserStore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CollaboratorsActivity extends AppCompatActivity {

    EditText etGroupName, etMemberName, etMemberEmail;
    EditText etExpenseAmount, etExpenseDesc;
    Button btnSaveGroupName, btnAddMember, btnAddExpense, btnCalculate;
    ListView listMembers, listExpenses;
    Spinner spinnerPaidBy;
    TextView tvGroupTripTitle, tvInviteHint;

    ArrayList<String> memberRows;
    ArrayList<String> acceptedMemberNames;
    ArrayList<String> expenseRows;

    ArrayAdapter<String> memberAdapter, expenseAdapter;

    String tripDocId;
    String tripOwnerUserId;
    String tripName = "Trip Group";
    boolean isOwner;
    FirebaseFirestore db;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collaborators);
        EdgeToEdgeHelper.apply(this);

        tripDocId = getIntent().getStringExtra("trip_doc_id");
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        tripOwnerUserId = getIntent().getStringExtra("trip_owner_user_id");
        if (tripOwnerUserId == null || tripOwnerUserId.isEmpty()) {
            tripOwnerUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        }
        isOwner = auth.getCurrentUser() != null && auth.getCurrentUser().getUid().equals(tripOwnerUserId);

        Button btnBackMembers = findViewById(R.id.btnBackMembers);
        etGroupName = findViewById(R.id.etGroupName);
        tvGroupTripTitle = findViewById(R.id.tvGroupTripTitle);
        tvInviteHint = findViewById(R.id.tvInviteHint);
        etMemberName = findViewById(R.id.etMemberName);
        etMemberEmail = findViewById(R.id.etMemberEmail);
        etExpenseAmount = findViewById(R.id.etExpenseAmount);
        etExpenseDesc = findViewById(R.id.etExpenseDesc);
        btnSaveGroupName = findViewById(R.id.btnSaveGroupName);
        btnAddMember = findViewById(R.id.btnAddMember);
        btnAddExpense = findViewById(R.id.btnAddExpense);
        btnCalculate = findViewById(R.id.btnCalculateSettlement);
        listMembers = findViewById(R.id.listMembers);
        listExpenses = findViewById(R.id.listExpenses);
        spinnerPaidBy = findViewById(R.id.spinnerPaidBy);

        memberRows = new ArrayList<>();
        acceptedMemberNames = new ArrayList<>();
        expenseRows = new ArrayList<>();

        memberAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, memberRows);
        expenseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, expenseRows);

        listMembers.setAdapter(memberAdapter);
        listExpenses.setAdapter(expenseAdapter);

        if (tripDocId == null || auth.getCurrentUser() == null || tripOwnerUserId == null) {
            Toast.makeText(this, "Trip group not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadTripMetaAndSetup();

        btnSaveGroupName.setOnClickListener(v -> saveGroupName());
        btnAddMember.setOnClickListener(v -> sendInvite());
        btnAddExpense.setOnClickListener(v -> addExpense());
        btnCalculate.setOnClickListener(v -> calculateSettlement());
        btnBackMembers.setOnClickListener(v -> finish());
    }

    private void loadTripMetaAndSetup() {
        tripRef()
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Trip not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    String savedTripName = documentSnapshot.getString("name");
                    String savedGroupName = documentSnapshot.getString("groupName");

                    if (savedTripName != null && !savedTripName.isEmpty()) {
                        tripName = savedTripName;
                    }

                    String groupName = (savedGroupName != null && !savedGroupName.isEmpty())
                            ? savedGroupName
                            : tripName + " Group";

                    tvGroupTripTitle.setText(tripName);
                    etGroupName.setText(groupName);
                    tvInviteHint.setText(isOwner
                            ? "Invite friends by email. They will see the invite in My Groups."
                            : "Only the trip owner can send invites. You can still track split bills here.");

                    if (isOwner) {
                        ensureOwnerMembership(groupName);
                    } else {
                        etGroupName.setEnabled(false);
                        btnSaveGroupName.setEnabled(false);
                        etMemberName.setEnabled(false);
                        etMemberEmail.setEnabled(false);
                        btnAddMember.setEnabled(false);
                    }
                    loadMembers();
                    loadExpenses();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load trip group", Toast.LENGTH_SHORT).show()
                );
    }

    private void saveGroupName() {
        if (!isOwner) {
            Toast.makeText(this, "Only the trip owner can rename the group", Toast.LENGTH_SHORT).show();
            return;
        }

        String groupName = etGroupName.getText().toString().trim();
        if (groupName.isEmpty()) {
            Toast.makeText(this, "Enter a group name", Toast.LENGTH_SHORT).show();
            return;
        }

        tripRef()
                .update("groupName", groupName)
                .addOnSuccessListener(unused -> {
                    ensureOwnerMembership(groupName);
                    Toast.makeText(this, "Group name saved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not save group name", Toast.LENGTH_SHORT).show()
                );
    }

    private void ensureOwnerMembership(String groupName) {
        if (auth.getCurrentUser() == null) {
            return;
        }

        String ownerEmail = auth.getCurrentUser().getEmail();
        if (ownerEmail == null || ownerEmail.isEmpty()) {
            return;
        }

        String ownerName = LocalUserStore.getProfileName(this, ownerEmail, "Trip Owner");

        Map<String, Object> member = new HashMap<>();
        member.put("name", ownerName);
        member.put("email", ownerEmail);
        member.put("status", "ACCEPTED");
        member.put("role", "OWNER");
        member.put("updatedAt", System.currentTimeMillis());

        tripRef()
                .collection("groupMembers")
                .document(ownerEmail)
                .set(member, SetOptions.merge());

        saveMembership(groupName, ownerName, ownerEmail);
    }

    private void sendInvite() {
        if (!isOwner) {
            Toast.makeText(this, "Only the trip owner can send invites", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = etMemberName.getText().toString().trim();
        String email = etMemberEmail.getText().toString().trim().toLowerCase(Locale.US);
        String groupName = etGroupName.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Enter member name and email", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentEmail = auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : "";
        if (email.equalsIgnoreCase(currentEmail)) {
            Toast.makeText(this, "You are already in this group", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> invite = new HashMap<>();
        invite.put("tripDocId", tripDocId);
        invite.put("tripOwnerUserId", tripOwnerUserId);
        invite.put("tripName", tripName);
        invite.put("groupName", groupName);
        invite.put("inviteeName", name);
        invite.put("inviteeEmail", email);
        invite.put("inviterEmail", currentEmail);
        invite.put("status", "PENDING");
        invite.put("createdAt", System.currentTimeMillis());

        Map<String, Object> pendingMember = new HashMap<>();
        pendingMember.put("name", name);
        pendingMember.put("email", email);
        pendingMember.put("status", "PENDING");
        pendingMember.put("role", "MEMBER");
        pendingMember.put("updatedAt", System.currentTimeMillis());

        db.collection("group_invites")
                .add(invite)
                .addOnSuccessListener(unused -> {
                    tripRef()
                            .collection("groupMembers")
                            .document(email)
                            .set(pendingMember, SetOptions.merge());

                    etMemberName.setText("");
                    etMemberEmail.setText("");
                    loadMembers();
                    Toast.makeText(this, "Invite sent to My Groups", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not send invite", Toast.LENGTH_SHORT).show()
                );
    }

    private void addExpense() {
        if (spinnerPaidBy.getSelectedItem() == null) {
            Toast.makeText(this, "Accepted members are needed first", Toast.LENGTH_SHORT).show();
            return;
        }

        String paidBy = spinnerPaidBy.getSelectedItem().toString();
        String amountStr = etExpenseAmount.getText().toString().trim();
        String desc = etExpenseDesc.getText().toString().trim();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Enter an expense amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> expense = new HashMap<>();
        expense.put("paidBy", paidBy);
        expense.put("amount", amount);
        expense.put("description", desc);
        expense.put("createdAt", System.currentTimeMillis());

        tripRef()
                .collection("expenses")
                .add(expense)
                .addOnSuccessListener(unused -> {
                    etExpenseAmount.setText("");
                    etExpenseDesc.setText("");
                    loadExpenses();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not add expense", Toast.LENGTH_SHORT).show()
                );
    }

    private void loadMembers() {
        tripRef()
                .collection("groupMembers")
                .orderBy("updatedAt", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    memberRows.clear();
                    acceptedMemberNames.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        String status = doc.getString("status");

                        if (name == null || email == null) {
                            continue;
                        }

                        memberRows.add(name + "\n" + email + " • " + (status != null ? status : "PENDING"));

                        if ("ACCEPTED".equals(status)) {
                            acceptedMemberNames.add(name);
                        }
                    }

                    memberAdapter.notifyDataSetChanged();

                    ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_dropdown_item,
                            acceptedMemberNames
                    );
                    spinnerPaidBy.setAdapter(spinnerAdapter);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load group members", Toast.LENGTH_SHORT).show()
                );
    }

    private void loadExpenses() {
        tripRef()
                .collection("expenses")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    expenseRows.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String paidBy = doc.getString("paidBy");
                        Double amount = doc.getDouble("amount");
                        String desc = doc.getString("description");

                        expenseRows.add(
                                (paidBy != null ? paidBy : "Unknown") +
                                        " paid ₹" +
                                        String.format(Locale.US, "%.2f", amount != null ? amount : 0.0) +
                                        "\n" +
                                        (desc != null && !desc.isEmpty() ? desc : "Shared expense")
                        );
                    }

                    expenseAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load split bills", Toast.LENGTH_SHORT).show()
                );
    }

    private void calculateSettlement() {
        tripRef()
                .collection("expenses")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Double> paidMap = new HashMap<>();
                    double total = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String paidBy = doc.getString("paidBy");
                        Double amount = doc.getDouble("amount");

                        if (paidBy == null || amount == null) {
                            continue;
                        }

                        total += amount;
                        paidMap.put(paidBy, paidMap.getOrDefault(paidBy, 0.0) + amount);
                    }

                    int memberCount = acceptedMemberNames.size();
                    if (memberCount == 0) {
                        Toast.makeText(this, "No accepted group members yet", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double share = total / memberCount;
                    StringBuilder result = new StringBuilder();

                    for (String member : acceptedMemberNames) {
                        double paid = paidMap.getOrDefault(member, 0.0);
                        double balance = paid - share;

                        if (balance > 0) {
                            result.append(member)
                                    .append(" should receive ₹")
                                    .append(String.format(Locale.US, "%.2f", balance))
                                    .append("\n");
                        } else {
                            result.append(member)
                                    .append(" owes ₹")
                                    .append(String.format(Locale.US, "%.2f", -balance))
                                    .append("\n");
                        }
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("Split Bill Summary")
                            .setMessage(result.toString())
                            .setPositiveButton("OK", null)
                            .show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not calculate settlement", Toast.LENGTH_SHORT).show()
                );
    }

    private void saveMembership(String groupName, String name, String email) {
        Map<String, Object> membership = new HashMap<>();
        membership.put("groupName", groupName);
        membership.put("tripDocId", tripDocId);
        membership.put("tripOwnerUserId", tripOwnerUserId);
        membership.put("tripName", tripName);
        membership.put("userName", name);
        membership.put("userEmail", email.toLowerCase(Locale.US));
        membership.put("status", "ACTIVE");
        membership.put("updatedAt", System.currentTimeMillis());

        db.collection("group_memberships")
                .document(buildMembershipId(email))
                .set(membership, SetOptions.merge());
    }

    private String buildMembershipId(String email) {
        return tripOwnerUserId + "_" + tripDocId + "_" + email.toLowerCase(Locale.US);
    }

    private DocumentReference tripRef() {
        return db.collection("users")
                .document(tripOwnerUserId)
                .collection("trips")
                .document(tripDocId);
    }
}
