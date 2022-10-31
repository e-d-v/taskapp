package com.evanv.taskapp.ui.main;

import static com.evanv.taskapp.logic.Task.clearDate;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.evanv.taskapp.R;
import com.evanv.taskapp.databinding.ActivityMainBinding;
import com.evanv.taskapp.logic.LogicSubsystem;
import com.evanv.taskapp.ui.additem.AddItem;
import com.evanv.taskapp.ui.main.recycler.DayItem;
import com.evanv.taskapp.ui.main.recycler.DayItemAdapter;
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kotlin.Pair;

/**
 * Main Activity for the app. Display's the user's schedule of Tasks/Events, while allowing for
 * Task completion/addition (with the latter done through the use of a separate AddItem activity).
 *
 * @author Evan Voogd
 */
@SuppressWarnings("unused")
public class MainActivity extends AppCompatActivity implements ClickListener {
    // Fields
    @SuppressWarnings("unused")
    private ActivityMainBinding mBinding;          // Binding for the MainActivity
    public static final int ITEM_REQUEST = 1;      // requestCode for task/item entry
    private DayItemAdapter mDayItemAdapter;        // Adapter for recycler showing user commitments
    private ViewFlipper mVF;                       // Swaps between loading screen and recycler
    // Allows data to be pulled from activity
    private ActivityResultLauncher<Intent> mStartForResult;
    // Allows us to manually show FAB when task/event completed/deleted.
    LogicSubsystem mLogicSubsystem;                // Subsystem that handles logic for taskapp
    private Date mStartDate;                       // The current date

    // Key for the extra that stores the list of Task names for the Parent Task Picker Dialog in
    public static final String EXTRA_TASKS = "com.evanv.taskapp.ui.main.extras.TASKS";
    // Keys into SharedPrefs to store todayTime
    private static final String PREF_FILE = "taskappPrefs"; // File name for sharedPrefs
    private static final String PREF_DAY = "taskappDay";    // Day for todayTime
    private static final String PREF_TIME = "taskappTime";  // Time for todayTime

    /**
     * Handles activities started for a result, in this case when the AddItem activity returns with
     * a new Event/Task to be added. Parses the data in the BundleExtra AddItem.EXTRA_ITEM into a
     * Task/Event depending on their AddItem.EXTRA_TYPE.
     *
     * @param resultCode RESULT_OK if there were no issues with user input
     * @param data Contains the BundleExtra AddItem.EXTRA_ITEM, with all the data needed to build
     *             the item.
     */
    protected void onActivityResult(int resultCode, @Nullable Intent data) {
        // Show loading screen
        mVF.setDisplayedChild(0);

        if (resultCode != RESULT_OK) {
            if (!mLogicSubsystem.isEmpty()) {
                mVF.setDisplayedChild(1);
            }
            else {
                mVF.setDisplayedChild(2);
            }
        }

        List<Integer> updatedIndices = mLogicSubsystem.addItem(data);

        if (updatedIndices == null) {
            Toast.makeText(this, "Error occurred when adding new item, try again.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        for (int index : updatedIndices) {
            mDayItemAdapter.notifyItemChanged(index);
        }

        // As the task dependency graph has been updated, we must reoptimize it
        Optimize();
        // Show recycler as Optimize is finished
        mVF.setDisplayedChild(1);
    }

    /**
     * Calls the Optimizer to find an optimal schedule for the user's tasks, given the user's
     * scheduled events.
     */
    private void Optimize() {
        Pair<Pair<Integer, Integer>, List<Pair<Integer, Integer>>> updatedInfo =
                mLogicSubsystem.Optimize();

        // As the Optimizer may have changed tasks' dates, we must refresh the recycler
        int eventLowIndex = updatedInfo.getFirst().getFirst();
        int eventScheduleSize = updatedInfo.getFirst().getSecond();
        List<Pair<Integer, Integer>> changedIndices = updatedInfo.getSecond();

        if (eventScheduleSize > eventLowIndex) {
            mDayItemAdapter.notifyItemRangeRemoved(eventLowIndex, eventScheduleSize);
        }

        mDayItemAdapter.mDayItemList = mLogicSubsystem.DayItemList();

        // Tell the recycler about moved tasks.
        for (Pair<Integer, Integer> indices : changedIndices) {
            int oldIndex = indices.getFirst();
            int newIndex = indices.getSecond();

            // We must use changed instead of moved, as an item in the dayItem entry is moved
            // not the dayItem itself.
            if (oldIndex >= 0) {
                mDayItemAdapter.notifyItemChanged(oldIndex);
            }
            if (newIndex >= mDayItemAdapter.getItemCount()) {
                mDayItemAdapter.notifyItemInserted(newIndex);
            }
            else {
                mDayItemAdapter.notifyItemChanged(newIndex);
            }
        }
    }

    /**
     * Runs on the start of the app. Most importantly it loads the user data from the file.
     *
     * @param savedInstanceState Not used.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create layout
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        // startDate is our representation for the current date upon the launch of TaskApp.
        mStartDate = clearDate(new Date());

        // Get todayTime from shared preferences
        SharedPreferences sp = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
        Date todayTimeDate = new Date(sp.getLong(PREF_DAY, -1L));
        int todayTime = 0;
        if (!todayTimeDate.before(mStartDate)) {
            todayTime = sp.getInt(PREF_TIME, 0);
        }

        mLogicSubsystem = new LogicSubsystem(this, todayTime);

        String[] overdueNames = mLogicSubsystem.getOverdueTasks();

        // Prompt the user with a dialog containing overdue tasks so they can mark overdue tasks
        // so taskapp can reoptimize the schedule if some tasks are overdue.
        if (overdueNames.length != 0) {
            // List of indices to tasks that were completed.
            ArrayList<Integer> selectedItems = new ArrayList<>();

            // Show a dialog prompting the user to mark tasks that were completed as complete
            android.app.AlertDialog.Builder builder =
                    new android.app.AlertDialog.Builder(this);
            builder.setTitle(R.string.overdue_dialog_title)
                    .setMultiChoiceItems(overdueNames, null,
                            new DialogInterface.OnMultiChoiceClickListener() {

                                /**
                                 * Adds the selected task to the toRemove list, or removes it if the
                                 * task was unselected
                                 *
                                 * @param dialogInterface not used
                                 * @param index           the index into the tasks ArrayList of the
                                 *                        parent
                                 * @param isChecked       true if checked, false if unchecked
                                 */
                                @Override
                                public void onClick(DialogInterface dialogInterface, int index,
                                                    boolean isChecked) {
                                    // If checked, add to list of Tasks to be added as complete
                                    if (isChecked) {
                                        selectedItems.add(index);
                                    }
                                    // If unchecked, remove form list of Tasks to be added as
                                    // complete
                                    else if (selectedItems.contains(index)) {
                                        selectedItems.remove(index);
                                    }
                                }
                            }).setPositiveButton(R.string.overdue_dialog_button,
                            new DialogInterface.OnClickListener() {

                                /**
                                 * Continues starter code when overdue tasks exist
                                 *
                                 * @param di    not used
                                 * @param index not used
                                 */
                                @Override
                                public void onClick(DialogInterface di, int index) {
                                    mLogicSubsystem.updateOverdueTasks(selectedItems);

                                    finishProcessing(true);
                                }
                            });

            builder.create();
            builder.show();

        }
        else {
            finishProcessing(false);
        }
    }

    /**
     * Now that the user has handled overdue tasks, we're ready to start the app. This function
     * adds data to the recycler and moves to the working fragment.
     *
     * @param reoptimize If the task list has changed due to overdue tasks, we might have to
     *                   reoptimize it
     */
    private void finishProcessing(boolean reoptimize) {
        List<DayItem> recyclerData = mLogicSubsystem.prepForDisplay(reoptimize);

        // Initialize the main recyclerview with data calculated in helper function DayItemList
        RecyclerView dayRecyclerView = findViewById(R.id.main_recyclerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(MainActivity.this);
        mDayItemAdapter = new DayItemAdapter(recyclerData, this);
        dayRecyclerView.setAdapter(mDayItemAdapter);
        dayRecyclerView.setLayoutManager(layoutManager);

        // Adds the action bar at the top of the screen
        setSupportActionBar(mBinding.toolbar);

        // When the FAB is clicked, run intentAddItem to open the AddItem Activity
        mBinding.fab.setOnClickListener(view -> intentAddItem());

        // Make visible the main content
        mVF = findViewById(R.id.vf);

        if (!mLogicSubsystem.isEmpty()) {
            mVF.setDisplayedChild(1);
        }
        else {
            mVF.setDisplayedChild(2);
        }

        // Create activity result handler for AddItem
        mStartForResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> MainActivity.this.onActivityResult(result.getResultCode(),
                        result.getData()));
    }

    /**
     * Updates todayTime in SharedPreferences
     */
    @Override
    protected void onPause() {
        super.onPause();

        // Update todayTime in SharedPreferences
        SharedPreferences sp = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
        SharedPreferences.Editor edit = sp.edit();
        edit.putLong(PREF_DAY, mStartDate.getTime());
        edit.putInt(PREF_TIME, mLogicSubsystem.getTodayTime());
        edit.apply();
    }

    /**
     * Launches the AddItem activity. Must be separate function so FAB handler can call it.
     */
    private void intentAddItem() {
        Intent intent = new Intent(this, AddItem.class);

        // Get a list of task names for prerequisite list
        ArrayList<String> taskNames = mLogicSubsystem.getTaskNames();

        intent.putStringArrayListExtra(EXTRA_TASKS, taskNames);
        mStartForResult.launch(intent);
    }

    /**
     * Adds the items to the three dot menu in the ActionBar. Left to defaults for now.
     *
     * @param menu The menu in the top right of the screen
     * @return always true
     */
    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Handles action bar clicks. Left to defaults for now
     *
     * @param item The MenuItem selected
     * @return true in virtually every scenario
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will automatically handle clicks on
        // the Home/Up button, so long as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // Handles the settings menu item being chosen
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Handles one of the buttons being clicked to complete a task. Weird structure where it's
     * called by the DayItemHolder for the day associated with the task, which in turn is called by
     * the TaskItemHolder for the task.
     *
     * @param position The index into the taskSchedule.get(day) ArrayList where the task is stored
     * @param day The date this task is scheduled for is day days past today's date
     */
    @Override
    public void onButtonClick(int position, int day, int action) {
        // Remove the given task from the task dependency graph
        mVF.setDisplayedChild(0);

        boolean success = mLogicSubsystem.onButtonClick(position, day, action);

        if (action == 0 || action == 1) {
            if (!success) {
                mVF.setDisplayedChild(1);
                return;
            }
        }

        if (action == 0) {
            int completionTime = -1;

            // Prompt the user to ask how long it took to complete the task, and add this time to
            // todayTime to prevent the user from being overscheduled on today's date.
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.complete_dialog_message));

            builder.setTitle(R.string.complete_dialog_title);

            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            builder.setView(input);

            builder.setPositiveButton(R.string.complete_task, (dialogInterface, i) -> {
                mLogicSubsystem.addTodayTime(Integer.parseInt(input.getText().toString()));

                // As the task dependency graph has been updated, we must reoptimize it
                Optimize();

                // If there are any tasks/events scheduled, show the recycler
                if (!mLogicSubsystem.isEmpty()) {
                    mVF.setDisplayedChild(1);
                }
                // If there aren't any, show the "add a task" fragment
                else {
                    mVF.setDisplayedChild(2);
                }
            });

            builder.show();
            return;
        }
        else if (action == 1) {
            Optimize();
        }
        // Remove the given event from the schedule and re-optimize.
        else if (action == 2) {
            if (!success) {
                mVF.setDisplayedChild(1);
                return;
            }
            mDayItemAdapter.mDayItemList.set(day, mLogicSubsystem.DayItemHelper(day));
            mDayItemAdapter.notifyItemChanged(day);
        }


        // If there are any tasks/events scheduled, show the recycler
        if (!mLogicSubsystem.isEmpty()) {
            mVF.setDisplayedChild(1);
        }
        // If there aren't any, show the "add a task" fragment
        else {
            mVF.setDisplayedChild(2);
        }

        // If the FAB is currently hidden, show the FAB again, to prevent it from being lost as the
        // FAB hides if you scroll down currently, and if we don't do this and the recycler doesn't
        // have enough content to scroll, the FAB will be lost until a restart.
        HideBottomViewOnScrollBehavior<FloatingActionButton> fabBehavior =
                (HideBottomViewOnScrollBehavior<FloatingActionButton>)
                        ((CoordinatorLayout.LayoutParams) mBinding.fab.getLayoutParams())
                                .getBehavior();
        if (fabBehavior != null) {
            fabBehavior.slideUp(mBinding.fab);
        }
    }
}