package com.evanv.taskapp.ui.additem.recur;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.evanv.taskapp.R;

/**
 * Fragment that represents no recurrences
 *
 * @author Evan Voogd
 */
public class NoRecurFragment extends Fragment implements RecurInput {
    // Value for a Bundle extra that represents no recurrence happening.
    public static final String EXTRA_VAL_TYPE = "com.evanv.taskapp.ui.additem.recur.NoRecurFragment.extra.val.TYPE";

    /**
     * Required empty public constructor
     */
    public NoRecurFragment() {
    }

    /**
     * Creates a new NoRecurFragment
     */
    @SuppressWarnings("unused")
    public static NoRecurFragment newInstance() {
        NoRecurFragment fragment = new NoRecurFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_no_recur, container, false);
    }

    /**
     * Returns a bundle containing the information that the user chose not to recur the event.
     *
     * @return a bundle containing the extra type relating to this fragment.
     */
    @Override
    public Bundle getRecurInfo() {
        Bundle toReturn = new Bundle();
        toReturn.putString(RecurInput.EXTRA_TYPE, EXTRA_VAL_TYPE);

        return toReturn;
    }
}