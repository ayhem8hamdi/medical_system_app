package com.example.medicalsystem2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class Login extends AppCompatActivity {

    private TextView signUpText;
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Find the TextView and Button
        signUpText = findViewById(R.id.signUpText);
        loginButton = findViewById(R.id.loginButton);

        // Set click listener for Sign Up text to navigate to SignUp activity
        signUpText.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, SignUp.class);
            startActivity(intent);
        });

        // Set click listener for Login button to navigate to Home activity
        loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, Home.class);
            startActivity(intent);
            finish(); // Optional: close the login activity so user can't go back with back button
        });
    }
}