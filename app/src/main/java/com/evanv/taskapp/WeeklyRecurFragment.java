package com.evanv.taskapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Fragment Representing a Weekly Recurrence
 */
public class WeeklyRecurFragment extends Fragment {


    public WeeklyRecurFragment() {
        // Required empty public constructor
    }

    /**
     * Creates a fragment representing weekly recurrences
     */
    public static WeeklyRecurFragment newInstance() {
        WeeklyRecurFragment fragment = new WeeklyRecurFragment();
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
        return inflater.inflate(R.layout.fragment_weekly_recur, container, false);
    }
}