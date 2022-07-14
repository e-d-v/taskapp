package com.evanv.taskapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.evanv.taskapp.databinding.ActivityAddItemBinding;

import java.util.Objects;

/**
 * Class for the AddItem activity. Gets fields for a new Task/Event to be added in MainActivity
 *
 * @author Evan Voogd
 */
@SuppressWarnings("unused")
public class AddItem extends AppCompatActivity {
    @SuppressWarnings("unused")
    private AppBarConfiguration mAppBarConfiguration;

    // Extras used in the Bundle:
    // Used by both
    // Key for the bundle containing item fields
    public static final String EXTRA_ITEM = "com.evanv.taskapp.extra.ITEM";
    // Key for the type of item being added (EXTRA_VAL_TASK) or (EXTRA_VAL_EVENT)
    public static final String EXTRA_TYPE = "com.evanv.taskapp.extra.TYPE";
    // Key for the name of the item being added
    public static final String EXTRA_NAME = "com.evanv.taskapp.extra.NAME";
    // Key for the time to complete/length of the item (in minutes)
    public static final String EXTRA_TTC = "com.evanv.taskapp.extra.TTC";

    // Used by Task
    // Type value representing Task
    public static final String EXTRA_VAL_TASK = "com.evanv.taskapp.extra.val.TASK";
    // Key for the Earliest Completion Date for the task being added
    public static final String EXTRA_ECD = "com.evanv.taskapp.extra.ECD";
    // Key for the due date for the task being added
    public static final String EXTRA_DUE = "com.evanv.taskapp.extra.DUE";
    // Key for the list of parent tasks indices for the task being added
    public static final String EXTRA_PARENTS = "com.evanv.taskapp.extra.PARENTS";

    // Used by Event
    // Type value representing Event
    public static final String EXTRA_VAL_EVENT = "com.evanv.taskapp.extra.val.EVENT";
    // Key for the Start Date for the event being added
    public static final String EXTRA_START = "com.evanv.taskapp.extra.START";
    // Key for the number of reoccurrences for the task being added
    public static final String EXTRA_RECUR = "com.evanv.taskapp.extra.RECUR";

    /**
     * Runs when Activity starts. Most importantly initializes the fragments and sets up the radio
     * group to change fragment when a different task/event is selected.
     *
     * @param savedInstanceState not used
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialization stuff needed by Android
        super.onCreate(savedInstanceState);
        com.evanv.taskapp.databinding.ActivityAddItemBinding binding = ActivityAddItemBinding
                .inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        // Set up fragments/get them to work with the app bar
        NavController navController =
                Navigation.findNavController(this, R.id.nav_host_fragment_content_add_item);
        mAppBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController,
                mAppBarConfiguration);

        // When FAB is clicked, run submit() method
        binding.fab.setOnClickListener(view -> submit());

        // When radio button is changed, switch to respective fragment
        RadioGroup rGroup = (RadioGroup)findViewById(R.id.radioGroupTaskEvent);
        rGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton checkedRadioButton = (RadioButton)group.findViewById(checkedId);
            boolean isChecked = checkedRadioButton.isChecked();
            if (isChecked)
            {
                // If event button is checked, switch from taskEntry to eventEntry
                if (checkedRadioButton.getId() == R.id.radioButtonEvent) {
                    navController.navigate(R.id.action_taskEntry_to_eventEntry);
                }
                // If task button is checked, switch from eventEntry to taskEntry
                else {
                    navController.navigate(R.id.action_eventEntry_to_taskEntry);
                }
            }
        });

    }

    /**
     * Submits the data in the fields to the MainActivity if all fields are filled.
     */
    protected void submit() {
        // Get the currently displayed fragment and cast it to an ItemEntry so we can get a Bundle
        // of it's fields easily
        NavHostFragment navFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_add_item);
        ItemEntry current = (ItemEntry) Objects.requireNonNull(navFragment)
                .getChildFragmentManager().getFragments().get(0);

        // Call getItem so we can get a bundle of the data the user has entered
        Bundle toReturn = current.getItem();

        // If the user correctly entered all fields, send the bundle to MainActivity and return
        if (toReturn != null) {
            Intent replyIntent = new Intent();
            replyIntent.putExtra(EXTRA_ITEM, toReturn);
            setResult(RESULT_OK, replyIntent);
            finish();
        }
    }

    /**
     * When navigated up, use the navController instead of default android behavior
     *
     * @return the value returned by the support library's navigation functions
     */
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this,
                R.id.nav_host_fragment_content_add_item);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}