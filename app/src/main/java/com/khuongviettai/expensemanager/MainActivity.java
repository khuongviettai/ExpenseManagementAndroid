package com.khuongviettai.expensemanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.khuongviettai.expensemanager.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnItemsClick {
    ActivityMainBinding binding;
    Intent intent;
    private long income = 0, expense = 0;
    private ExpensesAdapter expensesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        expensesAdapter = new ExpensesAdapter(this, this);
        binding.recycler.setAdapter(expensesAdapter);
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));

         intent = new Intent(MainActivity.this, AddExpenseActivity.class);

        binding.addIncome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                intent.putExtra("type", "Income");
                startActivity(intent);
            }
        });
        binding.addExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                intent.putExtra("type", "Expense");
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please");
        progressDialog.setMessage("Wait");
        progressDialog.setCancelable(true);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            progressDialog.show();
            FirebaseAuth.getInstance()
                    .signInAnonymously()
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            progressDialog.cancel();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.cancel();
                            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        income = 0;
        expense = 0;
        getData();
    }

    private void getData() {
        FirebaseFirestore
                .getInstance()
                .collection("expenses")
                .whereEqualTo("uid", FirebaseAuth.getInstance().getUid())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        expensesAdapter.clear();
                        List<DocumentSnapshot> dsList = queryDocumentSnapshots.getDocuments();
                        for (DocumentSnapshot ds:dsList) {
                            ExpenseModel expenseModel = ds.toObject(ExpenseModel.class);
                            if (expenseModel.getType().equals("Income")) {
                                income +=expenseModel.getAmount();
                            }
                            else {
                                expense +=expenseModel.getAmount();
                            }
                            expensesAdapter.add(expenseModel);
                        }
                        setUpGraph();
                    }
                });
    }

    private void setUpGraph() {
        List<PieEntry> pieEntryList = new ArrayList<>();
        List<Integer> colorList = new ArrayList<>();
        if (income != 0) {
            pieEntryList.add(new PieEntry(income, "Thu nhập"));
            colorList.add(getResources().getColor(R.color.Neon_Green));
        }
        if (expense != 0) {
            pieEntryList.add(new PieEntry(expense, "Chi phí"));
            colorList.add(getResources().getColor(R.color.Red));
        }
        PieDataSet pieDataSet = new PieDataSet(pieEntryList, String.valueOf(income - expense));
        pieDataSet.setColors(colorList);
        PieData pieData = new PieData(pieDataSet);

        binding.pieChart.setData(pieData);
        binding.pieChart.invalidate();

    }

    @Override
    public void onClick(ExpenseModel expenseModel) {
        intent.putExtra("model", expenseModel);
        startActivity(intent);
    }
}