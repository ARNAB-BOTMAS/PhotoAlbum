package com.example.photoalbum;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private EditText etUser, etEmail, etPass, etPhone, etCon;
    private FirebaseAuth mAuth;
    private Button regBtn;
    private LottieAnimationView loadingAnimationView;
    private String passwordPattern = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";

    private static final String TAG = "RegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        boolean secondActivityLaunched = sharedPreferences.getBoolean("secondActivityLaunched", false);

        mAuth = FirebaseAuth.getInstance();
        etUser = findViewById(R.id.userNameText);
        etEmail = findViewById(R.id.emailText);
        etPass = findViewById(R.id.passwordText);
        etPhone = findViewById(R.id.phoneText);
        etCon = findViewById(R.id.conPasswordText);
        loadingAnimationView = findViewById(R.id.loadingAnim);


        if (secondActivityLaunched) {
            hasResAddData();
        }

        String goTxt = "Go back to the Log in";
        regBtn = findViewById(R.id.regAddBtn);
        TextView backTxt = findViewById(R.id.backBtn);

        SpannableString spannableString = new SpannableString(goTxt);

        int colorForNotRegister = getResources().getColor(R.color.blue); // Change to your desired color resource
        spannableString.setSpan(new ForegroundColorSpan(colorForNotRegister), 0, spannableString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        backTxt.setText(spannableString);
        backTxt.setOnClickListener(view -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        regBtn.setOnClickListener(view -> {
            resAddData();
        });
    }

    private void hasResAddData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("secondActivityLaunched", true);
        editor.apply();
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void resAddData() {
        String stUser = etUser.getText().toString();
        String stEmail = etEmail.getText().toString();
        String stPass = etPass.getText().toString();
        String stPhone = etPhone.getText().toString();
        String stCon = etCon.getText().toString();

        if (TextUtils.isEmpty(stUser)) {
            etUser.setError("Username is empty");
        } else if (TextUtils.isEmpty(stEmail)) {
            etEmail.setError("Email is empty");
        } else if (TextUtils.isEmpty(stPhone) && stPhone.length() == 10) {
            etPhone.setError("Phone is empty");
        } else if (TextUtils.isEmpty(stPass)) {
            etPass.setError("Password is empty");
        } else if (!TextUtils.equals(stPass, stCon)) {
            etCon.setError("Password and Confirm Password do not match");
            etCon.requestFocus();
        } else if (!Pattern.matches(passwordPattern, stPass)) {
            etPass.setError("Password must contain at least one capital letter, one special symbol, one digit, and be at least 8 characters long");
            etPass.requestFocus();
        } else {
            loadingAnimationView.setVisibility(View.VISIBLE);
            regBtn.setText("");
            loadingAnimationView.playAnimation();
            mAuth.createUserWithEmailAndPassword(stEmail, stPass).addOnCompleteListener(RegisterActivity.this, task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    ReadWriteUserDetails writeUserDetails = new ReadWriteUserDetails(stUser, stPhone, stEmail, stPass);
                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference("users");

                    reference.child(user.getUid()).setValue(writeUserDetails).addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean("secondActivityLaunched", true);
                            editor.putString("uid", user.getUid());
                            editor.apply();

                            // Open the SecondActivity
                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            Toast.makeText(this, "Welcome " + stUser, Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                } else {
                    // Registration failed, handle the error
                    try {
                        throw task.getException();
                    } catch (FirebaseAuthWeakPasswordException weakPassword) {
                        // Handle weak password error
                        etPass.setError("Weak password. Choose a stronger password.");
                        etPass.requestFocus();
                    } catch (FirebaseAuthInvalidCredentialsException malformedEmail) {
                        // Handle invalid email error
                        etEmail.setError("Invalid email address.");
                        etEmail.requestFocus();
                    } catch (FirebaseAuthUserCollisionException existingUser) {
                        // Handle user already exists error
                        etEmail.setError("Email address already registered.");
                        etEmail.requestFocus();
                    } catch (Exception e) {
                        // Handle other errors
                        Log.e(TAG, "Registration failed: " + e.getMessage());
                    }
                }
            });
        }
    }
}