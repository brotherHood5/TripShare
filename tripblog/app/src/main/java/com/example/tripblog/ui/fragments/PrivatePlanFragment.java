package com.example.tripblog.ui.fragments;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tripblog.R;
import com.example.tripblog.TripBlogApplication;
import com.example.tripblog.api.services.PostService;
import com.example.tripblog.model.Post;
import com.example.tripblog.model.User;
import com.example.tripblog.ui.adapter.PlanListAdapter;
import com.example.tripblog.ui.adapter.PostViewPagerAdapter;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class PrivatePlanFragment extends Fragment {


    private static final String ARG_PARAM1 = "param1";
    private int currUserId;
    private boolean isEditable = true;
    private User loggedUser = TripBlogApplication.getInstance().getLoggedUser();

    public PrivatePlanFragment() {
        // Required empty public constructor
    }

    public static PrivatePlanFragment newInstance(int currUserId) {
        PrivatePlanFragment fragment = new PrivatePlanFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PARAM1, currUserId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    private PlanListAdapter adapter = null;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_private_plan, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            currUserId = getArguments().getInt(ARG_PARAM1);
        }

        ListView planList = view.findViewById(R.id.planList);

        PostService postService = TripBlogApplication.createService(PostService.class);
        postService.getPostByUserId(currUserId, false).enqueue(new Callback<JsonArray>() {
            @Override
            public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                if(response.isSuccessful()) {

                    JsonArray postJsonArray = response.body().getAsJsonArray();
                    Gson gson = new Gson();
                    Type postListType = new TypeToken<List<Post>>(){}.getType();
                    List<Post> postList = gson.fromJson(postJsonArray, postListType);
                    Log.d("Data in", postList.toString());

                    if(postList.size() != 0) {
                        if(currUserId == loggedUser.getId())
                            isEditable = true;
                        else
                            isEditable = false;
                        adapter = new PlanListAdapter(getContext(), R.layout.plan_item, postList, isEditable);
                        planList.setAdapter(adapter);
                    }
                    else{
                        TextView noPlanTxt = view.findViewById(R.id.noPlanTxt);
                        noPlanTxt.setText("You haven't written any posts yet.");
                    }

                }
            }

            @Override
            public void onFailure(Call<JsonArray> call, Throwable t) {
                Toast.makeText(getContext(), "Fail", Toast.LENGTH_SHORT).show();
            }
        });
    }
}