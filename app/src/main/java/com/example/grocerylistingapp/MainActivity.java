package com.example.grocerylistingapp;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FloatingActionButton fabAdd;
    private GroceryAdapter groceryAdapter;
    private ArrayList<GroceryItem> groceryItemList;
    private DatabaseReference databaseReference;
    private FirebaseAuth auth;
    private TextView userNameTextView;
    private Button logoutButton;
    private EditText edtItemName, edtItemPrice, edtItemQuantity;
    private Button btnAdd;

    private static final int REQUEST_CODE_SCAN = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(MainActivity.this, LoginScreenActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Initialize Firebase Database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("groceryItems").child(currentUser.getUid());

        // Initialize UI components
        recyclerView = findViewById(R.id.idRVItems);
        fabAdd = findViewById(R.id.idFABAdd);
        userNameTextView = findViewById(R.id.userNameTextView);
        logoutButton = findViewById(R.id.logoutButton);

        // Display the user's name
        String displayName = currentUser.getDisplayName();
        if (displayName != null) {
            userNameTextView.setText("Hello, " + displayName);
        } else {
            userNameTextView.setText("Hello, User");
        }

        // Setup RecyclerView
        groceryItemList = new ArrayList<>();
        groceryAdapter = new GroceryAdapter(groceryItemList, this, position -> {
            // Handle item click if needed
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(groceryAdapter);

        // Load grocery items from Firebase
        loadGroceryItems();

        // Set up FloatingActionButton click listener
        fabAdd.setOnClickListener(v -> showAddItemDialog());

        // Set up logout button click listener
        logoutButton.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(MainActivity.this, LoginScreenActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void showAddItemDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.grocery_add_dialog);
        dialog.setCancelable(true);

        edtItemName = dialog.findViewById(R.id.idEdtItemName);
        edtItemQuantity = dialog.findViewById(R.id.idEdtItemQuantity);
        edtItemPrice = dialog.findViewById(R.id.idEdtItemPrice);
        btnAdd = dialog.findViewById(R.id.addButton);
        Button btnScanQRCode = dialog.findViewById(R.id.idBtnScanQRCode);
        Button btnCancel = dialog.findViewById(R.id.idBtnCancel);

        // Disable the Add button initially
        btnAdd.setEnabled(false);

        // TextWatcher to check if all fields are filled
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String itemName = edtItemName.getText().toString().trim();
                String itemPrice = edtItemPrice.getText().toString().trim();
                String itemQuantity = edtItemQuantity.getText().toString().trim();
                btnAdd.setEnabled(!itemName.isEmpty() && !itemPrice.isEmpty() && !itemQuantity.isEmpty());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        edtItemName.addTextChangedListener(textWatcher);
        edtItemPrice.addTextChangedListener(textWatcher);
        edtItemQuantity.addTextChangedListener(textWatcher);

        // Add item logic
        btnAdd.setOnClickListener(v -> {
            String itemName = edtItemName.getText().toString().trim();
            String itemQuantityStr = edtItemQuantity.getText().toString().trim();
            String itemPriceStr = edtItemPrice.getText().toString().trim();

            try {
                int itemQuantity = Integer.parseInt(itemQuantityStr);
                double itemPrice = Double.parseDouble(itemPriceStr);

                GroceryItem newItem = new GroceryItem(itemName, itemPrice, itemQuantity);
                String id = databaseReference.push().getKey();
                if (id != null) {
                    newItem.setId(id); // Set the ID in the item
                    databaseReference.child(id).setValue(newItem)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "Item added successfully", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Failed to add item: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                    dialog.dismiss();
                } else {
                    Toast.makeText(this, "Failed to generate item ID", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid quantity or price", Toast.LENGTH_SHORT).show();
            }
        });

        // Cancel button logic
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // QR code scanner logic
        btnScanQRCode.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Scan a barcode or QR code");
            integrator.setBeepEnabled(true);
            integrator.setBarcodeImageEnabled(true);
            integrator.setOrientationLocked(false);
            integrator.initiateScan();
        });

        dialog.show();
    }

    private void loadGroceryItems() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                groceryItemList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    GroceryItem item = snapshot.getValue(GroceryItem.class);
                    if (item != null) {
                        item.setId(snapshot.getKey()); // Set the ID
                        groceryItemList.add(item);
                    }
                }
                groceryAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Failed to load items: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                String scannedData = result.getContents();
                String[] parts = scannedData.split(",");

                if (parts.length == 3) {
                    edtItemName.setText(parts[0]);
                    edtItemQuantity.setText(parts[1]);
                    edtItemPrice.setText(parts[2]);
                } else {
                    Toast.makeText(this, "Invalid QR Code format. Expecting: itemName,price,quantity", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

