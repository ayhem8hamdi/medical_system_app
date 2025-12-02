package com.example.medicalsystem2;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.imageview.ShapeableImageView;

public class Home extends AppCompatActivity {

    private ShapeableImageView avatarImage;
    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Find views
        TextView emailText = findViewById(R.id.emailText);
        avatarImage = findViewById(R.id.avatarImage);

        // Display email from bundle
        String email = getIntent().getStringExtra("user_email");
        if (email != null && !email.isEmpty()) {
            emailText.setText(email);
        }

        // Initialize ActivityResultLauncher for picking image
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            avatarImage.setImageURI(imageUri);
                        }
                    }
                }
        );

        // Set onClickListener to open gallery
        avatarImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        // Setup click listeners for Book Now buttons
        setupBookNowButtons();
    }

    private void setupBookNowButtons() {


        // Method 2: If you don't have individual IDs, use this approach instead:
        // Uncomment the following lines and comment out Method 1 above


        // Find the doctors container
        ViewGroup doctorsContainer = findViewById(R.id.doctorsContainer);
        if (doctorsContainer == null) {
            // Try to find it in the horizontal scroll view
            HorizontalScrollView scrollView = findViewById(R.id.doctorsScrollView);
            if (scrollView != null) {
                doctorsContainer = (ViewGroup) scrollView.getChildAt(0);
            }
        }

        if (doctorsContainer != null) {
            // Loop through all doctor cards in the container
            for (int i = 0; i < doctorsContainer.getChildCount(); i++) {
                View card = doctorsContainer.getChildAt(i);
                Button bookButton = card.findViewById(R.id.bookButton);
                if (bookButton != null) {
                    bookButton.setOnClickListener(v -> openAppointmentScreen());
                }
            }
        }

    }

    private void openAppointmentScreen() {
        // Simple intent to open appointment screen
        Intent intent = new Intent(Home.this, appointment.class);
        startActivity(intent);
    }
}