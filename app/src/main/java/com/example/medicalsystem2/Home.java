package com.example.medicalsystem2;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.imageview.ShapeableImageView;
import android.widget.TextView;

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
    }
}
