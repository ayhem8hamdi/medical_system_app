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

public class Login extends AppCompatActivity {

    private EditText emailEditText;   // Input field for user email
    private EditText passwordEditText; // Input field for user password
    private Button loginButton;       // Button to trigger login
    private TextView signUpText;      // Text to navigate to SignUp activity
    private FirebaseAuth firebaseAuth; // Firebase Authentication instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Authentication
        firebaseAuth = FirebaseAuth.getInstance();

        // Connect UI elements to variables
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        signUpText = findViewById(R.id.signUpText);

        // Check if user is already logged in
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            // If already logged in, skip login screen and go directly to Home
            goToHome(currentUser.getEmail());
        }

        // Login button click listener
        loginButton.setOnClickListener(v -> attemptLogin());

        // Sign Up text click listener: navigate to SignUp activity
        signUpText.setOnClickListener(v -> goToSignUp());
    }

    /**
     * Attempt to log in using Firebase Authentication
     */
    private void attemptLogin() {
        // Get email and password entered by user
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate input fields
        if (!validateInputs(email, password)) {
            return; // Stop if input invalid
        }

        // Disable button temporarily to prevent multiple clicks
        loginButton.setEnabled(false);

        // Firebase method to sign in with email and password
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    // Re-enable button after Firebase task completes
                    loginButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        // Login successful
                        FirebaseUser user = firebaseAuth.getCurrentUser(); // Get logged-in user
                        Toast.makeText(Login.this, "Login Successful", Toast.LENGTH_SHORT).show();

                        // Navigate to Home activity and pass user email
                        goToHome(user.getEmail());
                    } else {
                        // Login failed, show error message
                        String errorMessage = task.getException() != null
                                ? task.getException().getMessage()
                                : "Login failed";

                        Toast.makeText(Login.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Validate email and password input
     * @param email user email
     * @param password user password
     * @return true if inputs are valid, false otherwise
     */
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

        // Check if password length meets Firebase minimum requirement
        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            passwordEditText.requestFocus();
            return false;
        }

        return true; // Inputs are valid
    }

    /**
     * Navigate to Home activity
     * @param email email of logged-in user
     */
    private void goToHome(String email) {
        Intent intent = new Intent(Login.this, Home.class);
        intent.putExtra("user_email", email); // Pass email to Home
        // Clear previous activities to prevent going back to Login
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Close Login activity
    }

    /**
     * Navigate to SignUp activity
     */
    private void goToSignUp() {
        Intent intent = new Intent(Login.this, SignUp.class);
        // Clear previous activities
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Close Login activity
    }
}
