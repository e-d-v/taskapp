package com.evanv.taskapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Fragment that represents no recurrences
 */
public class NoRecurFragment extends Fragment {

    public NoRecurFragment() {
        // Required empty public constructor
    }

    /**
     * Creates a new NoRecurFragment
     */
    public static NoRecurFragment newInstance() {
        NoRecurFragment fragment = new NoRecurFragment();
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
        return inflater.inflate(R.layout.fragment_no_recur, container, false);
    }
}