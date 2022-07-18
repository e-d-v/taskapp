package com.evanv.taskapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Fragment that represents a daily recurrence.
 */
public class DailyRecurFragment extends Fragment {

    public DailyRecurFragment() {
        // Required empty public constructor
    }

    /**
     * Creates a new fragment representing daily recurrences
     *
     * @return A new instance of fragment DailyRecurFragment.
     */
    public static DailyRecurFragment newInstance() {
        DailyRecurFragment fragment = new DailyRecurFragment();
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
        return inflater.inflate(R.layout.fragment_daily_recur, container, false);
    }
}