package com.evanv.taskapp.ui.additem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.evanv.taskapp.R;
import com.evanv.taskapp.databinding.ActivityAddItemBinding;

import java.util.Objects;

/**
 * Class for the AddItem activity. Gets fields for a new Task/Event to be added in MainActivity
 *
 * @author Evan Voogd
 */
@SuppressWarnings("unused")
public class AddItem extends AppCompatActivity {

    // Extras used in the Bundle:
    // Used by both
    // Key for the bundle containing item fields
    public static final String EXTRA_ITEM = "com.evanv.taskapp.extra.ITEM";
    // Key for the type of item being added (EXTRA_VAL_TASK) or (EXTRA_VAL_EVENT)
    public static final String EXTRA_TYPE = "com.evanv.taskapp.extra.TYPE";
    // Key for the name of the item being added
    public static final String EXTRA_NAME = "com.evanv.taskapp.extra.NAME";
    // Key for the time to complete/length of the item (in minutes)
    public static final String EXTRA_END = "com.evanv.taskapp.extra.TTC";

    // Used by Task
    // Type value representing Task
    public static final String EXTRA_VAL_TASK = "com.evanv.taskapp.extra.val.TASK";
    // Key for the Earliest Completion Date for the task being added
    public static final String EXTRA_ECD = "com.evanv.taskapp.extra.ECD";
    // Key for the due date for the task being added
    public static final String EXTRA_DUE = "com.evanv.taskapp.extra.DUE";
    // Key for the list of parent tasks indices for the task being added
    public static final String EXTRA_PARENTS = "com.evanv.taskapp.extra.PARENTS";
    // Key for the priority of the task.
    public static final String EXTRA_PRIORITY = "com.evanv.taskapp.extra.PRIORITY";
    // Key for the project of the task.
    public static final String EXTRA_PROJECT = "com.evanv.taskapp.extra.PROJECT";
    // Information for new project.
    public static final String EXTRA_NEW_PROJECT = "com.evanv.taskapp.extra.NEW_PROJECT";

    // Used by Event
    // Type value representing Event
    public static final String EXTRA_VAL_EVENT = "com.evanv.taskapp.extra.val.EVENT";
    // Key for the Start Date for the event being added
    public static final String EXTRA_START = "com.evanv.taskapp.extra.START";
    // Key for the number of reoccurrences for the task being added
    public static final String EXTRA_RECUR = "com.evanv.taskapp.extra.RECUR";

    // Fields
    private boolean mTaskDisplayed;       // true if TaskEntry fragment is displayed
    private RadioGroup mRGroup;           // Task/Event selector
    private boolean ignoreCheckedChanged; // If check was changed by back press

    /**
     * When back button is pressed, change views that are impacted by the change in fragment to
     * have the correct values.
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();

        // If task was displayed, set the title and the buttons properly.
        if (mTaskDisplayed) {
            Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.add_event));
            mTaskDisplayed = false;

            ignoreCheckedChanged = true;
            ((RadioButton)mRGroup.getChildAt(0)).setChecked(false);
            ((RadioButton)mRGroup.getChildAt(1)).setChecked(true);
        }
        // If event was displayed, set the title and the buttons properly.
        else {
            Objects.requireNonNull(getSupportActionBar()).setTitle(getString(R.string.add_task));
            mTaskDisplayed = true;

            ignoreCheckedChanged = true;
            ((RadioButton)mRGroup.getChildAt(0)).setChecked(true);
            ((RadioButton)mRGroup.getChildAt(1)).setChecked(false);
        }
        ignoreCheckedChanged = false;
    }

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
        AppBarConfiguration mAppBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController,
                mAppBarConfiguration);

        binding.toolbar.setTitle(getString(R.string.add_task));

        // When FAB is clicked, run submit() method
        binding.fab.setOnClickListener(view -> submit());

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        mTaskDisplayed = true;
        ignoreCheckedChanged = false;

        // When radio button is changed, switch to respective fragment
        mRGroup = findViewById(R.id.radioGroupTaskEvent);
        mRGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton checkedRadioButton = group.findViewById(checkedId);
            boolean isChecked = checkedRadioButton.isChecked();
            if (isChecked && !ignoreCheckedChanged)
            {
                // If event button is checked, switch from taskEntry to eventEntry
                if (checkedRadioButton.getId() == R.id.radioButtonEvent) {
                    navController.navigate(R.id.action_taskEntry_to_eventEntry);
                    binding.toolbar.setTitle(getString(R.string.add_event));

                    mTaskDisplayed = false;
                }
                // If task button is checked, switch from eventEntry to taskEntry
                else {
                    navController.navigate(R.id.action_eventEntry_to_taskEntry);
                    binding.toolbar.setTitle(getString(R.string.add_task));

                    mTaskDisplayed = true;
                }
            }
            // Make sure up button still pressed
            Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
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
}