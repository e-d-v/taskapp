package com.evanv.taskapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A fragment that represents a monthly recurrence
 */
public class MonthlyRecurFragment extends Fragment {

    public MonthlyRecurFragment() {
        // Required empty public constructor
    }

    /**
     * Creates a fragment representing a monthly recurrence
     */
    public static MonthlyRecurFragment newInstance(String param1, String param2) {
        MonthlyRecurFragment fragment = new MonthlyRecurFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_monthly_recur, container, false);
    }
}