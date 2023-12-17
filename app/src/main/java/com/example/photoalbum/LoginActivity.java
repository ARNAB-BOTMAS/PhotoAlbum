package com.example.photoalbum;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private EditText etUser, etPass;

    private FirebaseAuth mAuth;
    private String passwordPattern = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";
    private Context context;
    private LottieAnimationView loadingAnimationView;
    private Button logIn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        loadingAnimationView = findViewById(R.id.loadingAnim);
        TextView textRegBtn = findViewById(R.id.regBtn);
        logIn = findViewById(R.id.loginBtn);
//        ProgressBar loadingProgressBar = findViewById(R.id.loadingProgressBar);


        String textReg = "Not Registered? Register Now!!";
        SpannableString spannableString = new SpannableString(textReg);

        int colorForNotRegister = ContextCompat.getColor(this, R.color.blue);
        spannableString.setSpan(new ForegroundColorSpan(colorForNotRegister), 0, 15, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        int colorForRegisterNow = ContextCompat.getColor(this, R.color.bule_sky);
        spannableString.setSpan(new ForegroundColorSpan(colorForRegisterNow), 15, spannableString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

//        TextViewCompat.setTextAppearance(textRegBtn, R.style.TextAppearance_AppCompat_Widget_Button);

        textRegBtn.setText(spannableString);

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        mAuth = FirebaseAuth.getInstance();

        boolean secondActivityLaunched = sharedPreferences.getBoolean("secondActivityLaunched", false);

        if (secondActivityLaunched) {
            // If the user is already logged in, redirect to the main activity
            goToMainActivity();
        }

        textRegBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToRegister();
            }
        });

        etUser = findViewById(R.id.userLog);
        etPass = findViewById(R.id.passLog);

        logIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!validateUsername() || !validatePassword()) {
                    return;
                }
                logInUser();
            }
        });
    }

    private void goToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private boolean validatePassword() {
        String val = etPass.getText().toString();
        if (val.isEmpty()) {
            etPass.setError("Password cannot be empty");
            return false;
        } else if (!Pattern.matches(passwordPattern, val)) {
            etPass.setError("Password must contain at least one capital letter, one special symbol, one digit, and be at least 8 characters long");
            return false;
        } else {
            etPass.setError(null);
            return true;
        }
    }

    private boolean validateUsername() {
        String val = etUser.getText().toString();
        if (val.isEmpty()) {
            etUser.setError("Username cannot be empty");
            return false;
        } else {
            etUser.setError(null);
            return true;
        }
    }

    private void goToRegister() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("secondActivityLaunched", false);
        editor.apply();
        startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        finish();
    }

    private void logInUser() {
        String userName = etUser.getText().toString();
        String userPass = etPass.getText().toString();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("users");
        Query checkUserDatabase = reference.orderByChild("username").equalTo(userName);

        checkUserDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String passwordFromDB = userSnapshot.child("password").getValue(String.class);
                        String emailFromDB = userSnapshot.child("email").getValue(String.class);

                        if (passwordFromDB.equals(userPass)) {
                            loadingAnimationView.setVisibility(View.VISIBLE);
                            logIn.setText("");
                            loadingAnimationView.playAnimation();
                            mAuth.signInWithEmailAndPassword(emailFromDB, passwordFromDB)
                                    .addOnCompleteListener(LoginActivity.this, task -> {
                                        if (task.isSuccessful()) {
                                            FirebaseUser user = mAuth.getCurrentUser();
                                            if (user != null) {
                                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                                editor.putBoolean("secondActivityLaunched", true);
                                                editor.putString("uid", user.getUid());
                                                editor.apply();

                                                // Open the MainActivity
                                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                                startActivity(intent);
                                                Toast.makeText(LoginActivity.this, "Welcome Back " + userName, Toast.LENGTH_SHORT).show();
                                                finish();
                                            } else {
                                                // Handle the case where Firebase user is null
                                                Toast.makeText(LoginActivity.this, "Firebase user is null", Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            // Handle the case where Firebase authentication fails
                                            Toast.makeText(LoginActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            // Passwords do not match, show an error message
                            etPass.setError("Incorrect password");
                        }
                    }
                } else {
                    // User with the given username does not exist, show an error message
                    etUser.setError("User not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle database error
                Toast.makeText(LoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}