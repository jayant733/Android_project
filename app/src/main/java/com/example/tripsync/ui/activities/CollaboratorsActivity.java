package com.example.tripsync.ui.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tripsync.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CollaboratorsActivity extends AppCompatActivity {

    EditText etMemberName, etMemberEmail;
    EditText etExpenseAmount, etExpenseDesc;
    Button btnAddMember, btnAddExpense, btnCalculate;
    ListView listMembers, listExpenses;
    Spinner spinnerPaidBy;

    ArrayList<String> members;
    ArrayList<String> expenses;

    ArrayAdapter<String> memberAdapter, expenseAdapter;

    String tripDocId;
    FirebaseFirestore db;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collaborators);

        tripDocId = getIntent().getStringExtra("trip_doc_id");
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        Button btnBackMembers = findViewById(R.id.btnBackMembers);
        etMemberName = findViewById(R.id.etMemberName);
        etMemberEmail = findViewById(R.id.etMemberEmail);
        etExpenseAmount = findViewById(R.id.etExpenseAmount);
        etExpenseDesc = findViewById(R.id.etExpenseDesc);
        btnAddMember = findViewById(R.id.btnAddMember);
        btnAddExpense = findViewById(R.id.btnAddExpense);
        btnCalculate = findViewById(R.id.btnCalculateSettlement);
        listMembers = findViewById(R.id.listMembers);
        listExpenses = findViewById(R.id.listExpenses);
        spinnerPaidBy = findViewById(R.id.spinnerPaidBy);

        members = new ArrayList<>();
        expenses = new ArrayList<>();

        memberAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, members);
        expenseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, expenses);

        listMembers.setAdapter(memberAdapter);
        listExpenses.setAdapter(expenseAdapter);

        if (tripDocId == null || auth.getCurrentUser() == null) {
            Toast.makeText(this, "Trip not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadMembers();
        loadExpenses();

        btnAddMember.setOnClickListener(v -> addMember());
        btnAddExpense.setOnClickListener(v -> addExpense());
        btnCalculate.setOnClickListener(v -> calculateSettlement());
        btnBackMembers.setOnClickListener(v -> finish());
    }

    private void addMember() {
        String name = etMemberName.getText().toString().trim();
        String email = etMemberEmail.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Enter member name and email", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> member = new HashMap<>();
        member.put("name", name);
        member.put("email", email);
        member.put("createdAt", System.currentTimeMillis());

        tripRef()
                .collection("collaborators")
                .add(member)
                .addOnSuccessListener(unused -> {
                    etMemberName.setText("");
                    etMemberEmail.setText("");
                    loadMembers();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not add member", Toast.LENGTH_SHORT).show()
                );
    }

    private void addExpense() {
        if (spinnerPaidBy.getSelectedItem() == null) {
            Toast.makeText(this, "Add a member first", Toast.LENGTH_SHORT).show();
            return;
        }

        String paidBy = spinnerPaidBy.getSelectedItem().toString();
        String amountStr = etExpenseAmount.getText().toString().trim();
        String desc = etExpenseDesc.getText().toString().trim();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Enter an expense amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);

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
                .collection("collaborators")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    members.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String name = doc.getString("name");
                        if (name != null && !name.isEmpty()) {
                            members.add(name);
                        }
                    }

                    memberAdapter.notifyDataSetChanged();

                    ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_dropdown_item,
                            members
                    );
                    spinnerPaidBy.setAdapter(spinnerAdapter);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load members", Toast.LENGTH_SHORT).show()
                );
    }

    private void loadExpenses() {
        tripRef()
                .collection("expenses")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    expenses.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String paidBy = doc.getString("paidBy");
                        Double amount = doc.getDouble("amount");
                        String desc = doc.getString("description");

                        expenses.add(
                                (paidBy != null ? paidBy : "Unknown") +
                                        " paid ₹" +
                                        String.format(Locale.US, "%.2f", amount != null ? amount : 0.0) +
                                        " for " +
                                        (desc != null ? desc : "")
                        );
                    }

                    expenseAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load expenses", Toast.LENGTH_SHORT).show()
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

                    int memberCount = members.size();
                    if (memberCount == 0) {
                        Toast.makeText(this, "Add members first", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double share = total / memberCount;
                    StringBuilder result = new StringBuilder();

                    for (String member : members) {
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
                            .setTitle("Settlement Summary")
                            .setMessage(result.toString())
                            .setPositiveButton("OK", null)
                            .show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not calculate settlement", Toast.LENGTH_SHORT).show()
                );
    }

    private com.google.firebase.firestore.DocumentReference tripRef() {
        return db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .collection("trips")
                .document(tripDocId);
    }
}
