package com.example.photoalbum;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
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

    private RecyclerView imageRecyclerView;
    private ImageAdapter imageAdapter;
    private List<String> imageUrls;

    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private FrameLayout loadingAni, loadingWaiting;
    private LottieAnimationView loadingProgress;


    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        imageRecyclerView = view.findViewById(R.id.imageRecyclerView);
        loadingAni = view.findViewById(R.id.loadingFrame);
        loadingProgress = view.findViewById(R.id.loadAni);
        loadingWaiting = view.findViewById(R.id.loadingWaiting);

        loadingAni.setVisibility(View.VISIBLE);


        imageUrls = new ArrayList<>();

        imageAdapter = new ImageAdapter(imageUrls, requireContext());
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        imageRecyclerView.setLayoutManager(layoutManager);
        imageRecyclerView.setAdapter(imageAdapter);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        if (user != null){
//            imageView.setText(user.getUid().toString());
            storage = FirebaseStorage.getInstance();
            storageReference = storage.getReference().child(user.getUid().toString()+"/uploads/");
            storageReference.listAll().addOnSuccessListener(listResult -> {
                if (listResult.getItems().size() != 0){
                    for (StorageReference item : listResult.getItems()){
                        item.getDownloadUrl().addOnSuccessListener(uri -> {
                            loadingAni.setVisibility(View.INVISIBLE);
                            imageUrls.add(uri.toString());
                            imageAdapter.notifyDataSetChanged();
                        });
                    }
                } else {
                    loadingAni.setVisibility(View.INVISIBLE);
                    loadingWaiting.setVisibility(View.VISIBLE);
                }
            });
        }
        return view;
    }
}