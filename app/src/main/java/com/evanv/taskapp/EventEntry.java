package com.evanv.taskapp;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * The fragment that handles data entry for new events
 *
 * @author Evan Voogd
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
    @SuppressWarnings("unused")
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

        boolean flag = false; // Allows us to Toast multiple errors at once.

        // Check if eventName is valid
        if (eventName.length() == 0) {
            Toast.makeText(getActivity(), R.string.name_error_event,
                    Toast.LENGTH_LONG).show();
            flag = true;
        }
        // Check if ECD is valid
        if (ecd.length() == 0) {
            Toast.makeText(getActivity(),
                    R.string.ecd_empty_event,
                    Toast.LENGTH_LONG).show();
            flag = true;
        }
        else {
            String[] fullTokens = ecd.split(" ");

            boolean ecdFlag = false; // true if there is an issue with ecd input

            // Check if ecd follows format MM/DD/YY HH:MM AM/PM
            if (fullTokens.length == 3) {
                String[] dateTokens = fullTokens[0].split("/");
                String[] timeTokens = fullTokens[1].split(":");

                if (dateTokens.length == 3 && timeTokens.length == 2) {
                    // Check if everything that's supposed to be a number is an integer
                    try {
                        int month = Integer.parseInt(dateTokens[0]);
                        int day = Integer.parseInt(dateTokens[1]);
                        int year = Integer.parseInt(dateTokens[2]);
                        int hour = Integer.parseInt(timeTokens[0]);
                        int minute = Integer.parseInt(timeTokens[1]);

                        // Make sure input makes sense
                        if (month > 12 || month < 1 || day > 31 || day < 1 || year < 0 ||
                                hour < 1 || hour > 12 || minute < 0 || minute > 59) {
                            ecdFlag = true;
                        }

                        // Make sure we're not scheduling an event for before today.
                        GregorianCalendar rightNow = new GregorianCalendar();
                        MyTime start = new MyTime(rightNow.get(Calendar.MONTH) + 1,
                                rightNow.get(Calendar.DAY_OF_MONTH), rightNow.get(Calendar.YEAR));

                        MyTime thisDate = new MyTime(month, day, 2000 + year);
                        if (start.getDateTime() - thisDate.getDateTime() > 0) {
                            Toast.makeText(getActivity(),
                                    R.string.ecd_format_event,
                                    Toast.LENGTH_LONG).show();
                            flag = true;
                        }
                    }
                    catch (Exception e) {
                        ecdFlag = true;
                    }

                    if (!fullTokens[2].equals(getString(R.string.am)) &&
                            !fullTokens[2].equals(getString(R.string.pm))) {
                        ecdFlag = true;
                    }
                }
            }
            else {
                ecdFlag = true;
            }

            if (ecdFlag) {
                Toast.makeText(getActivity(),
                        R.string.ecd_help_text_format,
                        Toast.LENGTH_LONG).show();
                flag = true;
            }
        }
        // Check if length is valid
        if (length.length() == 0) {
            Toast.makeText(getActivity(), R.string.ttc_error_empty, Toast.LENGTH_LONG).show();
            flag = true;
        }
        else {
            try {
                Integer.parseInt(length);
            }
            catch (Exception e) {
                Toast.makeText(getActivity(),
                        R.string.ttc_format_event,
                        Toast.LENGTH_LONG).show();
                flag = true;
            }
        }
        // Check if recur is valid
        if (recur.length() == 0) {
            Toast.makeText(getActivity(),
                    R.string.recur_empty_event,
                    Toast.LENGTH_LONG).show();
            flag = true;
        }
        else {
            try {
                Integer.parseInt(recur);
            }
            catch (Exception e) {
                Toast.makeText(getActivity(),
                        R.string.recur_format_event,
                        Toast.LENGTH_LONG).show();
                flag = true;
            }
        }

        // If any required views are empty, return null to signify invalid input
        if (flag) {
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