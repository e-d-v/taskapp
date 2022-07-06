package com.evanv.taskapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

/**
 * The fragment that handles data entry for new events
 *
 * @author Evan Daniel Voogd
 * @author evanv.com
 */
public class EventEntry extends Fragment implements ItemEntry {
    private ViewGroup mContainer; // The ViewGroup for the activity, allows easy access to views

    /**
     * Required empty public constructor
     */
    public EventEntry() {
    }

    /**
     * Required empty onCreate method
     *
     * @param savedInstanceState not used
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * If no required fields are empty, pack user input into a bundle and return it.
     *
     * @return a Bundle containing user input if all fields are filled, null otherwise
     */
    public Bundle getItem() {
        // Get needed views
        EditText editTextEventName = mContainer.findViewById(R.id.editTextEventName);
        EditText editTextECD = mContainer.findViewById(R.id.editTextECD);
        EditText editTextLength = mContainer.findViewById(R.id.editTextLength);
        EditText editTextRecur = mContainer.findViewById(R.id.editTextRecur);

        // Get user input from views
        String eventName = editTextEventName.getText().toString();
        String ecd = editTextECD.getText().toString();
        String length = editTextLength.getText().toString();
        String recur = editTextRecur.getText().toString();

        // If any required views are empty, return null to signify invalid input
        if (eventName.equals("") || ecd.equals("") || length.equals("") || recur.equals("")) {
            return null;
        }

        // Put user input into a bundle
        Bundle bundle = new Bundle();
        bundle.putString(AddItem.EXTRA_TYPE, AddItem.EXTRA_VAL_EVENT);
        bundle.putString(AddItem.EXTRA_NAME, eventName);
        bundle.putString(AddItem.EXTRA_START, ecd);
        bundle.putString(AddItem.EXTRA_TTC, length);
        bundle.putString(AddItem.EXTRA_RECUR, recur);

        // Return bundle containing user input
        return bundle;
    }

    /**
     * Required empty onCreateView method
     *
     * @param inflater not used
     * @param container not used
     * @param savedInstanceState not used
     * @return not used
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mContainer = container;

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_event_entry, container, false);
    }
}