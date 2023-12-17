package com.example.photoalbum;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.airbnb.lottie.LottieAnimationView;
import com.example.photoalbum.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;

    private SharedPreferences sharedPreferences;
    private Toolbar toolbar;
    private String uid, userDB;
    private FirebaseAuth mAuth;
    private LottieAnimationView loadingAnimationView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loadingAnimationView = findViewById(R.id.loadingAnim);
        loadingAnimationView.setSpeed(1.0f);
        loadingAnimationView.setVisibility(View.VISIBLE);


        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        toolbar = findViewById(R.id.topNavBar);
        uid = sharedPreferences.getString("uid", "");
        toolbar.setTitle("");
        getUserInfoByUid(uid);


        setSupportActionBar(toolbar);

        replaceFragment(new HomeFragment());

        binding.navBarBottom.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.homeBtn) {
                replaceFragment(new HomeFragment());
            } else if (item.getItemId() == R.id.uploadBtn) {
                replaceFragment(new UploadFragment());
            } else if (item.getItemId() == R.id.monthBtn) {
                replaceFragment(new MonthFragment());
            } else if (item.getItemId() == R.id.setBtn) {
                replaceFragment(new SettingsFragment());
            }
            return true;
        });
    }

    private void getUserInfoByUid(String uid) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users").child(uid);

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    loadingAnimationView.setVisibility(View.INVISIBLE);
                    String username = dataSnapshot.child("username").getValue(String.class);
                    String email = dataSnapshot.child("email").getValue(String.class);
                    String phone = dataSnapshot.child("phone").getValue(String.class);

                    toolbar.setTitle(username);

                    // Now you have the user information
                    // Update your UI or perform any required actions
                } else {
                    // Handle the case where the user does not exist in the database
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle database read error
            }
        });
    }

    private void replaceFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        @SuppressLint("CommitTransaction") FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.logout_btn, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_logout) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("secondActivityLaunched", false);
            editor.putString("uid", "");
            mAuth.signOut();
            editor.apply();

            // Return to the FirstActivity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
