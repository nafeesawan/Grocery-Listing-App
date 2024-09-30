package com.example.grocerylistingapp;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class AddItemDialog extends DialogFragment {

    private String qrData;
    private OnItemAddedListener listener;

    public AddItemDialog(String qrData, OnItemAddedListener listener) {
        this.qrData = qrData;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.grocery_add_dialog);

        EditText itemName = dialog.findViewById(R.id.idEdtItemName);
        EditText itemPrice = dialog.findViewById(R.id.idEdtItemPrice);
        EditText itemQuantity = dialog.findViewById(R.id.idEdtItemQuantity);


        String[] parts = qrData.split(",");
        if (parts.length == 3) {
            itemName.setText(parts[0]);
            itemPrice.setText(parts[1]);
            itemQuantity.setText(parts[2]);
        }

        Button addButton = dialog.findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> {
            GroceryItem item = new GroceryItem(
                    itemName.getText().toString(),
                    Double.parseDouble(itemPrice.getText().toString()),
                    Integer.parseInt(itemQuantity.getText().toString())
            );
            listener.onItemAdded(item);
            dismiss();
        });

        return dialog;
    }

    public interface OnItemAddedListener {
        void onItemAdded(GroceryItem item);
    }
}
