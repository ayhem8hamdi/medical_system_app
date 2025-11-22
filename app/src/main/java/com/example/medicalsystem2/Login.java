package com.example.medicalsystem2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class Login extends AppCompatActivity {

    private EditText emailEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        TextView signUpText = findViewById(R.id.signUpText);
        Button loginButton = findViewById(R.id.loginButton);
        emailEditText = findViewById(R.id.emailEditText);

        // Click "Sign Up" → go to SignUp screen
        signUpText.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, SignUp.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Click login button → go to Home screen and send email
        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();

            Intent intent = new Intent(Login.this, Home.class);
            intent.putExtra("user_email", email); // send email inside bundle
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
