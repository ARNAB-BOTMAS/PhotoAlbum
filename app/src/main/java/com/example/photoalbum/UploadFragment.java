package com.example.photoalbum;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link UploadFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class UploadFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public UploadFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment UploadFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static UploadFragment newInstance(String param1, String param2) {
        UploadFragment fragment = new UploadFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    private ImageView imageView;
    private ConstraintLayout constraintLayout;
    private FrameLayout notification;
    private Button addImgBtn, uploadBtn;
    private Intent intent;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private DatabaseReference databaseReference;
    private LottieAnimationView loadingProgress, loadingSuccess;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_upload, container, false);
        // Inflate the layout for this fragment
        uploadBtn = view.findViewById(R.id.upBtn);
        Button changeBtn = view.findViewById(R.id.changeBtn);
        addImgBtn = view.findViewById(R.id.imgAddBtn);
        constraintLayout = view.findViewById(R.id.loadingUpload);
        imageView = view.findViewById(R.id.imgViewUpload);

        loadingProgress = view.findViewById(R.id.loadingImageAni);
        loadingSuccess = view.findViewById(R.id.succImageAni);

        notification = view.findViewById(R.id.notificationFrame);

        mAuth = FirebaseAuth.getInstance();

        addImgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadImageFirst();
            }
        });

        changeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadImage();
            }
        });

        return view;
    }



    private void uploadImageFirst() {
        intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, 100);
        addImgBtn.setVisibility(View.INVISIBLE);

    }

    private void uploadImage() {
        intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, 100);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == 100) && (data != null) && (data.getData() != null)) {
            Uri selectedImageUri = data.getData();
            imageView.setImageURI(selectedImageUri);
            constraintLayout.setVisibility(View.VISIBLE);
            uploadBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    uploadImageInDB(selectedImageUri);
                }
            });

        }
    }
    private void uploadImageInDB(Uri uri) {
//        onActivityResult();
//        startActivityForResult(intent, 100);
        user = mAuth.getCurrentUser();
        if(user != null){
            String uid = user.getUid().toString();
            storageReference = FirebaseStorage.getInstance().getReference(uid+"/uploads");
            databaseReference = FirebaseDatabase.getInstance().getReference(uid+"/uploads");
            StorageReference fileReference = storageReference.child(System.currentTimeMillis()+"."+getFileExtension(requireContext(), uri));
            fileReference.putFile(uri).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                    notification.setVisibility(View.VISIBLE);
                    loadingProgress.setVisibility(View.VISIBLE);
                    loadingProgress.playAnimation();
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    loadingProgress.cancelAnimation();
                    loadingProgress.setVisibility(View.INVISIBLE);

                    loadingSuccess.playAnimation();
                    loadingSuccess.setVisibility(View.VISIBLE);

                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            loadingSuccess.cancelAnimation();
                            notification.setVisibility(View.INVISIBLE);
                            constraintLayout.setVisibility(View.INVISIBLE);
                            loadingSuccess.setVisibility(View.INVISIBLE);
                            addImgBtn.setVisibility(View.VISIBLE);
                            Toast.makeText(getActivity(), "Image upload Successful", Toast.LENGTH_SHORT).show();
                        }
                    }, 4000);

                }
            });
        }

    }


    public static String getFileExtension(Context context, Uri uri) {
        ContentResolver contentResolver = context.getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();

        // Check if the URI scheme is "content"
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            // If the URI is a content:// URI, try to get the file extension from the ContentResolver
            String type = contentResolver.getType(uri);
            return mimeTypeMap.getExtensionFromMimeType(type);
        } else {
            // If the URI is a file:// URI, extract the file extension from the file path
            String path = uri.getPath();
            if (path != null) {
                return MimeTypeMap.getFileExtensionFromUrl(path);
            }
        }

        // If we couldn't determine the file extension, return a default value (e.g., ".jpg")
        return null;
    }


}