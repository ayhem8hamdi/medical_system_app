package com.example.medicalsystem2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignUp extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button signUpButton;
    private TextView loginText;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize Firebase Authentication
        firebaseAuth = FirebaseAuth.getInstance();

        // Connect UI elements to variables
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        signUpButton = findViewById(R.id.signUpButton);
        loginText = findViewById(R.id.loginText);

        // Sign Up button click listener
        signUpButton.setOnClickListener(v -> attemptSignUp());

        // Login text click listener
        loginText.setOnClickListener(v -> goToLogin());
    }

    private void attemptSignUp() {
        // Get email and password from input fields
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate inputs
        if (!validateInputs(email, password)) {
            return;
        }

        // Disable button to prevent multiple clicks
        signUpButton.setEnabled(false);

        // Create user with Firebase
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    signUpButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        // Sign up successful
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        Toast.makeText(SignUp.this, "Account Created Successfully", Toast.LENGTH_SHORT).show();
                        goToHome(user.getEmail());
                    } else {
                        // Sign up failed
                        String errorMessage = task.getException() != null
                                ? task.getException().getMessage()
                                : "Sign up failed";
                        Toast.makeText(SignUp.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validateInputs(String email, String password) {
        // Check if email is empty
        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return false;
        }

        // Check if email format is valid
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email address");
            emailEditText.requestFocus();
            return false;
        }

        // Check if password is empty
        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return false;
        }

        // Check if password is at least 6 characters
        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            passwordEditText.requestFocus();
            return false;
        }

        return true;
    }

    private void goToHome(String email) {
        Intent intent = new Intent(SignUp.this, Home.class);
        intent.putExtra("user_email", email);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void goToLogin() {
        Intent intent = new Intent(SignUp.this, Login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}