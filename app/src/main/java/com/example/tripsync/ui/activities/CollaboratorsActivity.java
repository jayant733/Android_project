package com.example.tripsync.ui.activities;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.tripsync.R;
import com.example.tripsync.data.db.TripDatabaseHelper;
import com.example.tripsync.data.provider.TripContentProvider;
import java.util.*;

public class CollaboratorsActivity extends AppCompatActivity {

    EditText etMemberName, etMemberEmail;
    EditText etExpenseAmount, etExpenseDesc;

    Button btnAddMember, btnAddExpense, btnCalculate;

    ListView listMembers, listExpenses;

    Spinner spinnerPaidBy;

    ArrayList<String> members;
    ArrayList<String> expenses;

    ArrayAdapter<String> memberAdapter, expenseAdapter;

    long tripId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collaborators);

        tripId = getIntent().getLongExtra("trip_id", -1);

        Button btnBackMembers = findViewById(R.id.btnBackMembers);
        etMemberName = findViewById(R.id.etMemberName);
        etMemberEmail = findViewById(R.id.etMemberEmail);
        etExpenseAmount = findViewById(R.id.etExpenseAmount);
        etExpenseDesc = findViewById(R.id.etExpenseDesc);

        btnAddMember = findViewById(R.id.btnAddMember);
        btnAddExpense = findViewById(R.id.btnAddExpense);
        btnCalculate = findViewById(R.id.btnCalculateSettlement);

        btnAddMember = findViewById(R.id.btnAddMember);

        listMembers = findViewById(R.id.listMembers);
        listExpenses = findViewById(R.id.listExpenses);

        spinnerPaidBy = findViewById(R.id.spinnerPaidBy);

        members = new ArrayList<>();
        expenses = new ArrayList<>();

        memberAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, members);

        expenseAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, expenses);

        listMembers.setAdapter(memberAdapter);
        listExpenses.setAdapter(expenseAdapter);

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

        if (name.isEmpty() || email.isEmpty()) return;

        ContentValues values = new ContentValues();
        values.put(TripDatabaseHelper.COLUMN_TRIP_ID, tripId);
        values.put(TripDatabaseHelper.COLUMN_COLLAB_NAME, name);
        values.put(TripDatabaseHelper.COLUMN_COLLAB_EMAIL, email);

        getContentResolver().insert(
                TripContentProvider.COLLABORATORS_URI,
                values
        );

        etMemberName.setText("");
        etMemberEmail.setText("");

        loadMembers();
    }

    private void addExpense() {

        String paidBy = spinnerPaidBy.getSelectedItem().toString();
        String amountStr = etExpenseAmount.getText().toString();
        String desc = etExpenseDesc.getText().toString();

        if (amountStr.isEmpty()) return;

        double amount = Double.parseDouble(amountStr);

        ContentValues values = new ContentValues();
        values.put(TripDatabaseHelper.COLUMN_EXPENSE_TRIP_ID, tripId);
        values.put(TripDatabaseHelper.COLUMN_PAID_BY, paidBy);
        values.put(TripDatabaseHelper.COLUMN_AMOUNT, amount);
        values.put(TripDatabaseHelper.COLUMN_DESCRIPTION, desc);

        getContentResolver().insert(
                TripContentProvider.EXPENSES_URI,
                values
        );

        etExpenseAmount.setText("");
        etExpenseDesc.setText("");

        loadExpenses();
    }

    private void loadMembers() {

        members.clear();

        Cursor cursor = getContentResolver().query(
                TripContentProvider.COLLABORATORS_URI,
                null,
                TripDatabaseHelper.COLUMN_TRIP_ID + "=?",
                new String[]{String.valueOf(tripId)},
                null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {

                String name = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                TripDatabaseHelper.COLUMN_COLLAB_NAME));

                members.add(name);
            }
            cursor.close();
        }

        memberAdapter.notifyDataSetChanged();

        ArrayAdapter<String> spinnerAdapter =
                new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_dropdown_item,
                        members);

        spinnerPaidBy.setAdapter(spinnerAdapter);
    }

    private void loadExpenses() {

        expenses.clear();

        Cursor cursor = getContentResolver().query(
                TripContentProvider.EXPENSES_URI,
                null,
                TripDatabaseHelper.COLUMN_EXPENSE_TRIP_ID + "=?",
                new String[]{String.valueOf(tripId)},
                null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {

                String paidBy = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                TripDatabaseHelper.COLUMN_PAID_BY));

                double amount = cursor.getDouble(
                        cursor.getColumnIndexOrThrow(
                                TripDatabaseHelper.COLUMN_AMOUNT));

                String desc = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                TripDatabaseHelper.COLUMN_DESCRIPTION));

                expenses.add(paidBy + " paid ₹" + amount + " for " + desc);
            }
            cursor.close();
        }

        expenseAdapter.notifyDataSetChanged();
    }

    private void calculateSettlement() {

        Map<String, Double> paidMap = new HashMap<>();
        double total = 0;

        Cursor cursor = getContentResolver().query(
                TripContentProvider.EXPENSES_URI,
                null,
                TripDatabaseHelper.COLUMN_EXPENSE_TRIP_ID + "=?",
                new String[]{String.valueOf(tripId)},
                null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {

                String paidBy = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                TripDatabaseHelper.COLUMN_PAID_BY));

                double amount = cursor.getDouble(
                        cursor.getColumnIndexOrThrow(
                                TripDatabaseHelper.COLUMN_AMOUNT));

                total += amount;

                paidMap.put(paidBy,
                        paidMap.getOrDefault(paidBy, 0.0) + amount);
            }
            cursor.close();
        }

        int memberCount = members.size();
        if (memberCount == 0) return;

        double share = total / memberCount;

        StringBuilder result = new StringBuilder();

        for (String member : members) {

            double paid = paidMap.getOrDefault(member, 0.0);
            double balance = paid - share;

            if (balance > 0) {
                result.append(member)
                        .append(" should receive ₹")
                        .append(String.format("%.2f", balance))
                        .append("\n");
            } else {
                result.append(member)
                        .append(" owes ₹")
                        .append(String.format("%.2f", -balance))
                        .append("\n");
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Settlement Summary")
                .setMessage(result.toString())
                .setPositiveButton("OK", null)
                .show();
    }
}