package com.example.grocerylistingapp;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class GroceryAdapter extends RecyclerView.Adapter<GroceryAdapter.ViewHolder> {

    private static final String TAG = "GroceryAdapter";
    private ArrayList<GroceryItem> groceryItemList;
    private Context context;
    private OnItemClickListener onItemClickListener;

    public GroceryAdapter(ArrayList<GroceryItem> groceryItemList, Context context, OnItemClickListener onItemClickListener) {
        this.groceryItemList = groceryItemList;
        this.context = context;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.grocery_remove_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Ensure position is within bounds
        if (position >= groceryItemList.size()) {
            return;
        }

        GroceryItem item = groceryItemList.get(position);
        holder.itemName.setText(item.getName());
        holder.itemQuantity.setText(String.valueOf(item.getQuantity()));
        holder.itemPrice.setText(String.valueOf(item.getPrice()));

        // Update total amount initially
        updateTotalAmount(holder, item);

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(position);
            }
        });

        // Delete item from Firebase and RecyclerView
        holder.deleteIcon.setOnClickListener(v -> {
            String itemId = item.getId(); // Get the unique Firebase ID of the item
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid(); // Get the current user's ID

            if (itemId != null && userId != null) {
                Log.d(TAG, "Attempting to delete item with ID: " + itemId);

                DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("groceryItems").child(userId).child(itemId);
                databaseRef.removeValue().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Item successfully deleted from Firebase");

                        // Safeguard to avoid IndexOutOfBoundsException
                        if (position < groceryItemList.size()) {
                            // Remove the item from the list and refresh the RecyclerView
                            groceryItemList.remove(position);

                            // Notify RecyclerView that the data set has changed
                            notifyDataSetChanged();
                        } else {
                            Log.e(TAG, "Invalid position: " + position + ", list size: " + groceryItemList.size());
                        }

                        Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "Failed to delete item: " + task.getException().getMessage());
                        Toast.makeText(context, "Failed to delete item: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error removing item: ", e);
                    Toast.makeText(context, "Error removing item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            } else {
                Log.e(TAG, "Item ID or user ID is null");
                Toast.makeText(context, "Item ID or user ID is null", Toast.LENGTH_SHORT).show();
            }
        });

        // Increment quantity
        holder.addButton.setOnClickListener(v -> {
            int newQuantity = item.getQuantity() + 1;
            item.setQuantity(newQuantity);
            holder.itemQuantity.setText(String.valueOf(newQuantity));
            updateQuantityInFirebase(item);
            updateTotalAmount(holder, item); // Update total amount when quantity changes
        });

        // Decrement quantity, but not below 1
        holder.minusButton.setOnClickListener(v -> {
            int currentQuantity = item.getQuantity();
            if (currentQuantity > 1) {
                int newQuantity = currentQuantity - 1;
                item.setQuantity(newQuantity);
                holder.itemQuantity.setText(String.valueOf(newQuantity));
                updateQuantityInFirebase(item);
                updateTotalAmount(holder, item); // Update total amount when quantity changes
            } else {
                Toast.makeText(context, "Quantity cannot be less than 1", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return groceryItemList.size();
    }

    // Method to update item quantity in Firebase
    private void updateQuantityInFirebase(GroceryItem item) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid(); // Get the current user's ID
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("groceryItems").child(userId).child(item.getId());

        databaseRef.setValue(item).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Quantity updated in Firebase for item: " + item.getName());
                Toast.makeText(context, "Quantity updated", Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Failed to update quantity: " + task.getException().getMessage());
                Toast.makeText(context, "Failed to update quantity", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Method to calculate and update total amount
    private void updateTotalAmount(ViewHolder holder, GroceryItem item) {
        double totalAmount = item.getQuantity() * item.getPrice();
        holder.totalAmount.setText(String.format("%.2f", totalAmount));
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView itemName, itemQuantity, itemPrice, totalAmount;
        private ImageView deleteIcon;
        private ImageButton addButton, minusButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.idTVItemName);
            itemQuantity = itemView.findViewById(R.id.idTVQuantity);
            itemPrice = itemView.findViewById(R.id.idTVRate);
            totalAmount = itemView.findViewById(R.id.idTVTotalAmt); // Added for total amount
            deleteIcon = itemView.findViewById(R.id.idIVDelete);
            addButton = itemView.findViewById(R.id.addButton); // Plus Button
            minusButton = itemView.findViewById(R.id.minusButton); // Minus Button
        }
    }
}

