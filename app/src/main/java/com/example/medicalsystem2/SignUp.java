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

    private EditText emailEditText;   // Input field for user email
    private EditText passwordEditText; // Input field for user password
    private Button signUpButton;       // Button to trigger signup
    private TextView loginText;        // Text to navigate to Login screen
    private FirebaseAuth firebaseAuth; // Firebase Authentication instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize Firebase Authentication instance
        firebaseAuth = FirebaseAuth.getInstance();

        // Connect UI elements from layout to variables
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        signUpButton = findViewById(R.id.signUpButton);
        loginText = findViewById(R.id.loginText);

        // When Sign Up button is clicked, attempt to create account
        signUpButton.setOnClickListener(v -> attemptSignUp());

        // When login text is clicked, go to Login activity
        loginText.setOnClickListener(v -> goToLogin());
    }

    /**
     * Attempt to create a new user account using Firebase Authentication
     */
    private void attemptSignUp() {
        // Get input values from EditText fields
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate input before calling Firebase
        if (!validateInputs(email, password)) {
            return; // Stop if input invalid
        }

        // Disable sign up button to prevent multiple clicks
        signUpButton.setEnabled(false);

        // Firebase method to create a new user with email and password
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    // Re-enable button after task completes
                    signUpButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        // Sign up successful
                        FirebaseUser user = firebaseAuth.getCurrentUser(); // Get the newly created user

                        // Show confirmation message
                        Toast.makeText(SignUp.this, "Account Created Successfully", Toast.LENGTH_SHORT).show();

                        // Navigate to Home activity, passing the user's email
                        goToHome(user.getEmail());
                    } else {
                        // Sign up failed
                        String errorMessage = task.getException() != null
                                ? task.getException().getMessage()
                                : "Sign up failed";

                        // Show error message as Toast
                        Toast.makeText(SignUp.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Validate email and password fields
     * @param email user email input
     * @param password user password input
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

        // Check password length (Firebase requires minimum 6 characters)
        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            passwordEditText.requestFocus();
            return false;
        }

        return true; // Inputs are valid
    }

    /**
     * Navigate to Home activity after successful sign up
     * @param email the email of the logged-in user
     */
    private void goToHome(String email) {
        Intent intent = new Intent(SignUp.this, Home.class);
        // Pass the user's email to the Home activity
        intent.putExtra("user_email", email);
        // Clear previous activities from back stack
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Close SignUp activity
    }

    /**
     * Navigate to Login activity if user already has an account
     */
    private void goToLogin() {
        Intent intent = new Intent(SignUp.this, Login.class);
        // Clear previous activities from back stack
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Close SignUp activity
    }
}
