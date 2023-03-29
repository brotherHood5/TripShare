package com.example.tripblog.ui.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.example.tripblog.R;
import com.example.tripblog.ui.PlanListAdapter;
import com.example.tripblog.ui.ViewPagerAdapter;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PrivatePlanFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PrivatePlanFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    ViewPagerAdapter viewPagerAdapter;
    ViewPager2 viewPager;


    public PrivatePlanFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment PublicPlanFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PrivatePlanFragment newInstance(String param1, String param2) {
        PrivatePlanFragment fragment = new PrivatePlanFragment();
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_private_plan, container, false);
    }
    String[] name = {"Thailand Plan", "Trekking Plan", "Must-go Places", "Nevada Plan"};
    Integer[] img = {R.drawable.da_lat, R.drawable.da_lat, R.drawable.da_lat, R.drawable.da_lat};
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ListView planList = (ListView) view.findViewById(R.id.userList);
        planList.setAdapter(new PlanListAdapter(getContext(), R.layout.plan_item, name, img));
    }
}