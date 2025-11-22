package com.example.medicalsystem2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SignUp extends AppCompatActivity {

    private EditText emailEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        TextView loginText = findViewById(R.id.loginText);
        Button signUpButton = findViewById(R.id.signUpButton);
        emailEditText = findViewById(R.id.emailEditText);

        // Click "Login" → go to Login screen
        loginText.setOnClickListener(v -> {
            Intent intent = new Intent(SignUp.this, Login.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Click Sign Up button → go to Home screen and send email
        signUpButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();

            Intent intent = new Intent(SignUp.this, Home.class);
            intent.putExtra("user_email", email); // send email inside bundle
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
