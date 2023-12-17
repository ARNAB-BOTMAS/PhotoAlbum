package com.example.photoalbum;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.Manifest;
import android.os.Environment;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FullScreenActivity extends AppCompatActivity {

    private ImageView fullScreenImageView;
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;

    private final float FACTOR = 1.0f;
    private float lastX, lastY;
    private boolean isDragging = false;
    private ImageButton backButton, downloadButton, deleteButton;

    private String imageUrl;
    private ProgressBar progressBar;
    private FrameLayout frameLayout;
    private LottieAnimationView lottieAnimationView;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    private static final float MIN_SCALE_FACTOR = 0.5f;
    private static final float MAX_SCALE_FACTOR = 10.0f;
    private static final float INITIAL_SCALE_FACTOR = 1.0f;

    private float currentScaleX = INITIAL_SCALE_FACTOR;
    private float currentScaleY = INITIAL_SCALE_FACTOR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen);

        fullScreenImageView = findViewById(R.id.fullScreenImageView);
        backButton = findViewById(R.id.backButton);
        downloadButton = findViewById(R.id.downloadButton);
        deleteButton = findViewById(R.id.deleteButton);
        progressBar = findViewById(R.id.progressBar);
        frameLayout = findViewById(R.id.FrameLoading);

        lottieAnimationView = findViewById(R.id.imageLoadingAnime);


        progressBar.setVisibility(View.INVISIBLE);
        lottieAnimationView.setVisibility(View.INVISIBLE);


        // Retrieve the image URL from the intent
        imageUrl = getIntent().getStringExtra("imageUrl");

        // Load and display the image in full screen using Picasso
        Picasso.get().load(imageUrl).into(fullScreenImageView);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(FullScreenActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(FullScreenActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);
                    downloadImage();
                }else{
                    downloadImage();
                }
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteImage();
            }
        });
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                // Handle double-tap event (reset the scale)
                resetScale();
                return true;
            }
        });
//        fullScreenImageView.setScaleType(ImageView.ScaleType.MATRIX);
        fullScreenImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getPointerCount() == 1 && !isDragging) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            lastX = event.getX();
                            lastY = event.getY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            float deltaX = event.getX() - lastX;
                            float deltaY = event.getY() - lastY;
                            lastX = event.getX();
                            lastY = event.getY();
                            v.setTranslationX(v.getTranslationX() + deltaX);
                            v.setTranslationY(v.getTranslationY() + deltaY);
                            break;
                        case MotionEvent.ACTION_UP:
                            isDragging = false;
                            break;
                    }
                    return true;
                } else {
                    gestureDetector.onTouchEvent(event);
                    return scaleGestureDetector.onTouchEvent(event);
                }
            }
        });
    }

    private void downloadImageIMG() {
        Toast.makeText(this, "Permission", Toast.LENGTH_SHORT).show();
    }

    private void deleteImage() {
        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
        storageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Toast.makeText(FullScreenActivity.this, "Delete image", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(FullScreenActivity.this, MainActivity.class));
                finish();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(FullScreenActivity.this, "Failed to delete image", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downloadImage() {
        frameLayout.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        File directory = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "PhotoAlbum");
        if (!directory.exists()) {
            directory.mkdirs();
//            Toast.makeText(FullScreenActivity.this, "MKDIRS", Toast.LENGTH_SHORT).show();
        }
//        Toast.makeText(FullScreenActivity.this, "OK MKDIRS", Toast.LENGTH_SHORT).show();

        String fileName = "photo_" + System.currentTimeMillis() + ".jpg";

        // Create a file to save the image
        File file = new File(directory, fileName);
        Glide.with(this)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true).into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        Bitmap bitmap = ((BitmapDrawable) resource).getBitmap();
                        try {
                            // Save the bitmap to the file
                            FileOutputStream outputStream = new FileOutputStream(file);
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                            outputStream.close();
                            lottieAnimationView.setVisibility(View.VISIBLE);
                            lottieAnimationView.playAnimation();
                            progressBar.setVisibility(View.INVISIBLE);
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    // This code will run after the specified delay (in milliseconds)
                                    // Hide the loading frame and stop the animation here
                                    frameLayout.setVisibility(View.INVISIBLE);
                                    lottieAnimationView.setVisibility(View.INVISIBLE);
                                    lottieAnimationView.cancelAnimation();

//                                    lottieAnimationView.cancelAnimation();
                                    Toast.makeText(FullScreenActivity.this, directory.toString(), Toast.LENGTH_SHORT).show();
                                }
                            }, 4000);


                            // Notify the user that the download is complete
                            // You can replace this with any desired action (e.g., show a message)
//                            Toast.makeText(FullScreenActivity.this, "Image saved in " + directory.toString(), Toast.LENGTH_SHORT).show();

                        } catch (IOException e) {
                            e.printStackTrace();
                            // Handle download error
                            Toast.makeText(FullScreenActivity.this, "Download failed", Toast.LENGTH_SHORT).show();
                        }
                        Toast.makeText(FullScreenActivity.this, "Download Complete", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        Toast.makeText(FullScreenActivity.this, "Complete Task", Toast.LENGTH_SHORT).show();

                    }
                });

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, download and save the image
                downloadImage();
            } else {
                // Permission denied, handle accordingly (e.g., show a message or take alternative action)
                Toast.makeText(this, "Permission denied. Image cannot be saved.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        scaleGestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }


    class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {



        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            float scaleFactor = scaleGestureDetector.getScaleFactor();
            scaleFactor = Math.max(MIN_SCALE_FACTOR, Math.min(scaleFactor, MAX_SCALE_FACTOR));

            // Calculate the new scale values
            float newScaleX = currentScaleX * scaleFactor;
            float newScaleY = currentScaleY * scaleFactor;

            // Apply the scaling factor
            fullScreenImageView.setScaleX(newScaleX);
            fullScreenImageView.setScaleY(newScaleY);

            // Update the current scale values
            currentScaleX = newScaleX;
            currentScaleY = newScaleY;

            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
            // Handle any actions after scaling ends (if needed)
        }

        // Reset the scale to the initial state (original size)

    }
    public void resetScale() {
        fullScreenImageView.setScaleX(INITIAL_SCALE_FACTOR);
        fullScreenImageView.setScaleY(INITIAL_SCALE_FACTOR);
        currentScaleX = INITIAL_SCALE_FACTOR;
        currentScaleY = INITIAL_SCALE_FACTOR;
    }
}