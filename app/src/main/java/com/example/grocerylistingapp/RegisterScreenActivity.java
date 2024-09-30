package com.example.grocerylistingapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterScreenActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextInputEditText userName, signupEmailAdd, signupPassword;
    private Button signupBtn;
    private TextView registeredOldBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_screen);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        userName = findViewById(R.id.userName);
        signupEmailAdd = findViewById(R.id.signupEmailAdd);
        signupPassword = findViewById(R.id.signupPassword);
        signupBtn = findViewById(R.id.signupBtn);
        registeredOldBtn = findViewById(R.id.registeredOldBtn);

        signupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = userName.getText().toString().trim();
                String email = signupEmailAdd.getText().toString().trim();
                String password = signupPassword.getText().toString().trim();

                if (email.isEmpty()) {
                    signupEmailAdd.setError("Email cannot be empty");
                } else if (password.isEmpty()) {
                    signupPassword.setError("Password cannot be empty");
                } else {
                    mAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(RegisterScreenActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        // User registration successful
                                        FirebaseUser user = mAuth.getCurrentUser();
                                        if (user != null) {
                                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                                    .setDisplayName(name) // Set the user's display name
                                                    .build();

                                            user.updateProfile(profileUpdates)
                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            if (task.isSuccessful()) {
                                                                Toast.makeText(RegisterScreenActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                                                                startActivity(new Intent(RegisterScreenActivity.this, LoginScreenActivity.class));
                                                                finish(); // Close the registration screen
                                                            } else {
                                                                Toast.makeText(RegisterScreenActivity.this, "Profile update failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                            }
                                                        }
                                                    });
                                        }
                                    } else {
                                        Toast.makeText(RegisterScreenActivity.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });

        registeredOldBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(RegisterScreenActivity.this, LoginScreenActivity.class));
            }
        });
    }
}
