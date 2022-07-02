package com.evanv.taskapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link EventEntry#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EventEntry extends Fragment implements ItemEntry {

    private ViewGroup mContainer;


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public EventEntry() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment EventEntry.
     */
    // TODO: Rename and change types and number of parameters
    public static EventEntry newInstance(String param1, String param2) {
        EventEntry fragment = new EventEntry();
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

    public Bundle getItem() {
        EditText editTextEventName = mContainer.findViewById(R.id.editTextEventName);
        EditText editTextECD = mContainer.findViewById(R.id.editTextECD);
        EditText editTextLength = mContainer.findViewById(R.id.editTextLength);
        EditText editTextRecur = mContainer.findViewById(R.id.editTextRecur);

        String eventName = editTextEventName.getText().toString();
        String ecd = editTextECD.getText().toString();
        String length = editTextLength.getText().toString();
        String recur = editTextRecur.getText().toString();

        if (eventName.equals("") || ecd.equals("") || length.equals("") || recur.equals("")) {
            return null;
        }

        Bundle bundle = new Bundle();
        bundle.putString(AddItem.EXTRA_TYPE, AddItem.EXTRA_VAL_EVENT);
        bundle.putString(AddItem.EXTRA_NAME, eventName);
        bundle.putString(AddItem.EXTRA_START, ecd);
        bundle.putString(AddItem.EXTRA_TTC, length);
        bundle.putString(AddItem.EXTRA_RECUR, recur);

        return bundle;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mContainer = container;

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_event_entry, container, false);
    }
}