package com.evanv.taskapp;

import static com.evanv.taskapp.Event.clearTime;
import static com.evanv.taskapp.Task.clearDate;
import static com.evanv.taskapp.Task.getDiff;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.evanv.taskapp.databinding.ActivityMainBinding;
import com.evanv.taskapp.db.TaskAppViewModel;
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Main Activity for the app. Display's the user's schedule of Tasks/Events, while allowing for
 * Task completion/addition (with the latter done through the use of a separate AddItem activity).
 *
 * @author Evan Voogd
 */
@SuppressWarnings("unused")
public class MainActivity extends AppCompatActivity implements ClickListener {

    @SuppressWarnings("unused")
    private ActivityMainBinding mBinding;      // Binding for the MainActivity
    private Date mStartDate;                 // The current date
    private int mTodayTime;                    // The amount of time spent completing tasks today
    public static final int ITEM_REQUEST = 1; // requestCode for task/item entry
    private DayItemAdapter mDayItemAdapter;    // Adapter for recyclerview showing user commitments
    private List<Task> mTasks = new ArrayList<>(); // List of all tasks for user
    // taskSchedule[i] represents the list of tasks for the day i days past startDate
    private final ArrayList<ArrayList<Task>> mTaskSchedule = new ArrayList<>();
    // eventSchedule[i] represents the list of events for the day i days past startDate
    private final ArrayList<ArrayList<Event>> mEventSchedule = new ArrayList<>();
    // Key for the extra that stores the list of Task names for the Parent Task Picker Dialog in
    // TaskEntry
    public static final String EXTRA_TASKS = "com.evanv.taskapp.extras.TASKS";
    private ViewFlipper mVF; // Swaps between loading screen and recycler
    // Allows data to be pulled from activity
    private ActivityResultLauncher<Intent> mStartForResult;
    // Allows us to manually show FAB when task/event completed/deleted.
    private TaskAppViewModel mTaskAppViewModel; // ViewModel to interact with Database
    private Event mTodayTimeEvent;              // Allows replacing todayTime event.
    // Keys into SharedPrefs to store todayTime
    private static final String PREF_FILE = "taskappPrefs";
    private static final String PREF_DAY = "taskappDay";   // Day for todayTime
    private static final String PREF_TIME = "taskappTime"; // Time for todayTime

    /**
     * Removes a task from the task dependency graph, while asking the user how long it took to
     * complete and adding that time to todayTime to prevent overscheduling on today's date.
     *
     * @param task The task to be removed from the task dependency graph
     * @param showDialog true if dialog is needed, false if dialog isn't
     * @param refresh true if we should refresh recycler
     */
    private void Complete(Task task, boolean showDialog, boolean refresh) {
        // Show loading screen
        mTasks.remove(task);

        Date doDate = task.getDoDate();

        // Get the number of days past the start date this task is scheduled for, so we can get the
        // index of the taskSchedule member for it's do date.
        int diff = getDiff(doDate, mStartDate);

        if (diff >= 0) {
            mTaskSchedule.get(diff).remove(task);
            mDayItemAdapter.mDayItemList.set(diff, DayItemHelper(diff));
            mDayItemAdapter.notifyItemChanged(diff);
        }

        // Remove the task from the task dependency graph
        for (int i = 0; i < task.getChildren().size(); i++) {
            task.getChildren().get(i).removeParent(task);
        }
        for (int i = 0; i < task.getParents().size(); i++) {
            task.getParents().get(i).removeChild(task);
        }

        if (showDialog) {
            // Prompt the user to ask how long it took to complete the task, and add this time to
            // todayTime to prevent the user from being overscheduled on today's date.
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(String.format(getString(R.string.complete_dialog_message),
                    task.getName()));
            builder.setTitle(R.string.complete_dialog_title);

            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            builder.setView(input);

            builder.setPositiveButton(R.string.complete_task, (dialogInterface, i) -> {
                mTodayTime += Integer.parseInt(input.getText().toString());
                // As the task dependency graph has been updated, we must reoptimize it
                Optimize(refresh);
                // Show recycler
                mVF.setDisplayedChild(1);
            });

            builder.show();
        }
        else {
            // As the task dependency graph has been updated, we must reoptimize it
            Optimize(refresh);
        }
    }

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
            if (mTaskSchedule.size() != 0 || mEventSchedule.size() != 0) {
                mVF.setDisplayedChild(1);
            }
            else {
                mVF.setDisplayedChild(2);
            }
        }

        // Get the data to build the item
        Bundle result = Objects.requireNonNull(data).getBundleExtra(AddItem.EXTRA_ITEM);
        String type = result.getString(AddItem.EXTRA_TYPE);

        // If the item type is Event
        if (type.equals(AddItem.EXTRA_VAL_EVENT)) {
            // Get the fields from the bundle
            String name = result.getString(AddItem.EXTRA_NAME);
            int ttc = Integer.parseInt(result.getString(AddItem.EXTRA_TTC));
            String startStr = result.getString(AddItem.EXTRA_START);
            Bundle recur = result.getBundle(AddItem.EXTRA_RECUR);

            // Convert the String start time into a MyTime
            Date start;
            Calendar userStart;
            try {
                start = Event.dateFormat.parse(startStr);
                userStart = Calendar.getInstance();
                if (start != null) {
                    userStart.setTime(start);
                }
                else {
                    return;
                }
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
                return;
            }

            // Calculate how many days past today's date this event is scheduled (used to
            // index into eventSchedule
            int diff = getDiff(start, mStartDate);

            // Add the new event to the data structure
            if (!recur.get(RecurInput.EXTRA_TYPE).equals(NoRecurFragment.EXTRA_VAL_TYPE)) {
                boolean until = recur.get(RecurActivity.EXTRA_UNTIL_TYPE)
                        .equals(RecurActivity.EXTRA_VAL_UNTIL);

                // Initialize until holders. Only the one associated with the user's choice
                // of recurrence is used.
                int endIndex = -1;
                int numTimes = -1;
                Date endDate = new Date();

                // Recur until end date
                if (until) {
                    try {
                        endDate = Task.dateFormat.parse
                                (recur.getString(RecurActivity.EXTRA_UNTIL));
                        endIndex = getDiff(endDate, mStartDate);
                    } catch (ParseException e) {
                        Log.e("TaskApp.MainActivity", e.getMessage());
                    }
                }
                // Recur set number of times
                else {
                    numTimes = Integer.parseInt(recur.getString(RecurActivity.EXTRA_UNTIL));
                }

                String recurType = recur.getString(RecurInput.EXTRA_TYPE);
                if (DailyRecurFragment.EXTRA_VAL_TYPE.equals(recurType)) {
                    // Number of days between recurrences
                    int interval = recur.getInt(DailyRecurFragment.EXTRA_INTERVAL);

                    // Calculates the number of times recurrence occurs.
                    numTimes = (until) ? ((endIndex - diff) / interval) + 1 : numTimes;

                    // Make sure there is enough mEventSchedules.
                    for (int i = mEventSchedule.size(); i <= diff + (numTimes * interval);
                         i++) {
                        mEventSchedule.add(new ArrayList<>());
                        mDayItemAdapter.notifyItemChanged(i);
                    }

                    // Add the event once every interval days.
                    for (int i = 0; i < (numTimes * interval); i += interval) {
                        // Calculate date to add event on.
                        Calendar recurCal = Calendar.getInstance();
                        recurCal.setTime(start);
                        recurCal.add(Calendar.DAY_OF_YEAR, i);

                        Event toAdd = new Event(name, recurCal.getTime(), ttc);
                        mEventSchedule.get(diff + i).add(toAdd);
                        mTaskAppViewModel.insert(toAdd);
                        mDayItemAdapter.notifyItemChanged(diff + i);
                    }
                }
                else if (WeeklyRecurFragment.EXTRA_VAL_TYPE.equals(recurType)) {
                    // Number of days between recurrences
                    int interval = recur.getInt(WeeklyRecurFragment.EXTRA_INTERVAL);
                    // day[0] = if recurs on sunday, day[6] if recurs on saturday
                    boolean[] days = recur.getBooleanArray(WeeklyRecurFragment.EXTRA_DAYS);

                    for (int i = 1; i <= 7; i++) {
                        if (days[i - 1]) {
                            Calendar recurCal = Calendar.getInstance();
                            recurCal.setTime(start);

                            // Calculates how many days we must add to recurCal to get next
                            // day of week
                            int currIndex = ((i - recurCal.get(Calendar.DAY_OF_WEEK)) + 7)
                                    % 7;

                            // oneLess shows us if this day occurs before the initial day of the
                            // week. This is useful as it stops the days before from being
                            // skewed away from the actual repetition
                            boolean oneLess = (i - recurCal.get(Calendar.DAY_OF_WEEK)) < 0;

                            recurCal.add(Calendar.DAY_OF_YEAR, currIndex);

                            // Gets start index into eventSchedule
                            currIndex += diff;

                            if (until) {
                                numTimes = Integer.MAX_VALUE;
                            }
                            else {
                                endDate.setTime(Long.MAX_VALUE);
                            }

                            // This means that our week iterator is off by one.
                            boolean offByOne = oneLess && (interval != 1);

                            for (int j = 0; j < numTimes && !recurCal.getTime().after(endDate);
                                 j++) {
                                // Add event to schedule
                                // Make sure an edge case isn't reached. oneLess only matters if
                                // interval == 1, as the offByOne if clause fixes it otherwise
                                if (!(j == 0 && offByOne) && !(j == numTimes - 1 && oneLess &&
                                        !offByOne)) {
                                    Event toAdd = new Event(name, recurCal.getTime(), ttc);

                                    for (int k = mEventSchedule.size(); k <= currIndex; k++) {
                                        mEventSchedule.add(new ArrayList<>());
                                        mDayItemAdapter.notifyItemChanged(k);
                                    }

                                    mEventSchedule.get(currIndex).add(toAdd);
                                    mTaskAppViewModel.insert(toAdd);
                                    mDayItemAdapter.notifyItemChanged(currIndex);
                                }

                                // Calculate next date
                                recurCal.add(Calendar.WEEK_OF_YEAR, interval);
                                currIndex += (7 * interval);

                                // Fix the skew caused by an event recurring at a non-1 interval
                                if (j == 0 && offByOne) {
                                    recurCal.add(Calendar.WEEK_OF_YEAR, -1);
                                    currIndex -= 7;
                                }
                            }
                        }
                    }
                }
                else if (MonthlyRecurFragment.EXTRA_VAL_TYPE.equals(recurType)) {
                    int interval = recur.getInt(MonthlyRecurFragment.EXTRA_INTERVAL);
                    String intervalType = recur.getString(MonthlyRecurFragment.EXTRA_RECUR_TYPE);

                    Calendar recurCal = Calendar.getInstance();
                    recurCal.setTime(start);

                    int currIndex = diff;

                    if (intervalType.equals(MonthlyRecurFragment.EXTRA_VAL_STATIC)) {
                        if (!until) {
                            Calendar endDateCal = Calendar.getInstance();
                            endDateCal.setTime(start);
                            endDateCal.add(Calendar.MONTH, interval * (numTimes - 1));
                            endDate = endDateCal.getTime();
                            endIndex = getDiff(endDate, mStartDate);
                        }

                        for (int i = mEventSchedule.size(); i <= endIndex; i++) {
                            mEventSchedule.add(new ArrayList<>());
                            mDayItemAdapter.notifyItemChanged(i);
                        }
                        while (!recurCal.getTime().after(endDate)) {
                            // Add event to schedule
                            Event toAdd = new Event(name, recurCal.getTime(), ttc);
                            mEventSchedule.get(currIndex).add(toAdd);
                            mTaskAppViewModel.insert(toAdd);
                            mDayItemAdapter.notifyItemChanged(currIndex);

                            // Calculate next date
                            recurCal.add(Calendar.MONTH, interval);
                            currIndex = getDiff(recurCal.getTime(), mStartDate);
                        }
                    }
                    if (intervalType.equals(MonthlyRecurFragment.EXTRA_VAL_DYNAMIC)) {
                        int dayOfWeek = recurCal.get(Calendar.DAY_OF_WEEK);
                        int dayOfWeekInMonth = recurCal.get(Calendar.DAY_OF_WEEK_IN_MONTH);

                        // We will repeat until the other clause (date after last day) is
                        // reached
                        if (until) {
                            numTimes = Integer.MAX_VALUE;
                        }
                        else {
                            Calendar endCal = Calendar.getInstance();
                            // 2100 as this is when the display functionality breaks
                            endCal.set(Calendar.YEAR, 2100);

                            endDate = endCal.getTime();
                        }

                        for (int i = 0; i < numTimes && !recurCal.getTime().after(endDate); i++) {
                            // Add event to schedule
                            Event toAdd = new Event(name, recurCal.getTime(), ttc);

                            for (int j = mEventSchedule.size(); j <= currIndex; j++) {
                                mEventSchedule.add(new ArrayList<>());
                                mDayItemAdapter.notifyItemChanged(j);
                            }

                            mEventSchedule.get(currIndex).add(toAdd);
                            mTaskAppViewModel.insert(toAdd);
                            mDayItemAdapter.notifyItemChanged(currIndex);

                            // Calculate next date
                            recurCal.add(Calendar.MONTH, interval);
                            recurCal.set(Calendar.DAY_OF_WEEK, dayOfWeek);
                            recurCal.set(Calendar.DAY_OF_WEEK_IN_MONTH, dayOfWeekInMonth);
                            currIndex = getDiff(recurCal.getTime(), mStartDate);
                        }
                    }
                    if (intervalType.equals(MonthlyRecurFragment.EXTRA_VAL_SPECIFIC)) {
                        String[] strs = recur.getString(MonthlyRecurFragment.EXTRA_DAYS)
                                .split(",");
                        int[] days = new int[strs.length];

                        for (int i = 0; i < strs.length; i++) {
                            days[i] = Integer.parseInt(strs[i]);
                        }

                        Arrays.sort(days);

                        for (int day : days) {
                            recurCal.setTime(start);
                            int daysInMonth = recurCal.getActualMaximum(Calendar.DAY_OF_MONTH);
                            int dayDiff = (day - recurCal.get(Calendar.DAY_OF_MONTH)
                                    + daysInMonth) % daysInMonth;

                            boolean oneLess = (day - recurCal.get(Calendar.DAY_OF_MONTH)) < 0;
                            boolean offByOne = oneLess && (interval != 1);

                            recurCal.add(Calendar.DAY_OF_YEAR, dayDiff);
                            currIndex = getDiff(recurCal.getTime(), mStartDate);

                            // We will repeat until the other clause (date after last day) is
                            // reached
                            if (until) {
                                numTimes = Integer.MAX_VALUE;
                            }
                            else {
                                Calendar endCal = Calendar.getInstance();
                                // 2100 as this is when the display functionality breaks
                                endCal.set(Calendar.YEAR, 2100);

                                endDate = endCal.getTime();
                            }

                            for (int i = 0; i < numTimes && !recurCal.getTime().after(endDate);
                                 i++) {
                                // Add event to schedule
                                if (!(i == 0 && offByOne) && !(i == numTimes - 1 && oneLess &&
                                        !offByOne)) {
                                    Event toAdd = new Event(name, recurCal.getTime(), ttc);
                                    for (int k = mEventSchedule.size(); k <= currIndex; k++) {
                                        mEventSchedule.add(new ArrayList<>());
                                        mDayItemAdapter.notifyItemChanged(k);
                                    }
                                    mEventSchedule.get(currIndex).add(toAdd);
                                    mTaskAppViewModel.insert(toAdd);
                                    mDayItemAdapter.notifyItemChanged(currIndex);
                                }

                                // Calculate next date
                                if (i == 0 && offByOne) {
                                    recurCal.add(Calendar.MONTH, -1);
                                }
                                recurCal.add(Calendar.MONTH, interval);
                                currIndex = getDiff(recurCal.getTime(), mStartDate);
                            }
                        }
                    }
                }
                else if (YearlyRecurFragment.EXTRA_VAL_TYPE.equals(recurType)) {
                    // How many years between each recurrence of this event.
                    int interval = recur.getInt(YearlyRecurFragment.EXTRA_INTERVAL);
                    // How the event will recur (the 18th, 3rd monday, 18/21st etc.)
                    String intervalType = recur.getString(YearlyRecurFragment.EXTRA_RECUR_TYPE);
                    // What months to recur on if necessary.
                    boolean[] months = new boolean[12];



                    if (intervalType.equals(YearlyRecurFragment.EXTRA_VAL_MULTIPLE_DYNAMIC)
                            || intervalType.equals(YearlyRecurFragment.EXTRA_VAL_MULTIPLE_STATIC)
                            || intervalType.equals(YearlyRecurFragment.EXTRA_VAL_SPECIFIC)) {
                        List<String> monthsStr = Arrays.asList(
                                recur.getString(YearlyRecurFragment.EXTRA_MONTHS).split(","));

                        months[0] = monthsStr.contains(getString(R.string.jan));
                        months[1] = monthsStr.contains(getString(R.string.feb));
                        months[2] = monthsStr.contains(getString(R.string.mar));
                        months[3] = monthsStr.contains(getString(R.string.apr));
                        months[4] = monthsStr.contains(getString(R.string.may));
                        months[5] = monthsStr.contains(getString(R.string.jun));
                        months[6] = monthsStr.contains(getString(R.string.jul));
                        months[7] = monthsStr.contains(getString(R.string.aug));
                        months[8] = monthsStr.contains(getString(R.string.sep));
                        months[9] = monthsStr.contains(getString(R.string.oct));
                        months[10] = monthsStr.contains(getString(R.string.nov));
                        months[11] = monthsStr.contains(getString(R.string.dec));
                    }

                    Calendar recurCal = Calendar.getInstance();
                    recurCal.setTime(start);

                    int currIndex = diff;

                    if (intervalType.equals(YearlyRecurFragment.EXTRA_VAL_STATIC)) {

                        if (!until) {
                            Calendar endDateCal = Calendar.getInstance();
                            endDateCal.setTime(start);
                            endDateCal.add(Calendar.YEAR, interval * (numTimes - 1));
                            endDate = endDateCal.getTime();
                        }

                        for (int i = mEventSchedule.size(); i <= endIndex; i++) {
                            mEventSchedule.add(new ArrayList<>());
                            mDayItemAdapter.notifyItemChanged(i);
                        }

                        while (!recurCal.getTime().after(endDate)) {
                            // Add event to schedule
                            Event toAdd = new Event(name, recurCal.getTime(), ttc);
                            for (int k = mEventSchedule.size(); k <= currIndex; k++) {
                                mEventSchedule.add(new ArrayList<>());
                                mDayItemAdapter.notifyItemChanged(k);
                            }
                            mEventSchedule.get(currIndex).add(toAdd);
                            mTaskAppViewModel.insert(toAdd);
                            mDayItemAdapter.notifyItemChanged(currIndex);

                            // Calculate next date
                            recurCal.add(Calendar.YEAR, interval);
                            currIndex = getDiff(recurCal.getTime(), mStartDate);
                        }
                    }
                    if (intervalType.equals(YearlyRecurFragment.EXTRA_VAL_DYNAMIC)) {
                        int dayOfWeek = recurCal.get(Calendar.DAY_OF_WEEK);
                        int dayOfWeekInMonth = recurCal.get(Calendar.DAY_OF_WEEK_IN_MONTH);

                        if (until) {
                            numTimes = Integer.MAX_VALUE;
                        }
                        else {
                            endDate.setTime(Long.MAX_VALUE);
                        }

                        for (int i = 0; i < numTimes && !recurCal.getTime().after(endDate); i++) {
                            // Add event to schedule
                            Event toAdd = new Event(name, recurCal.getTime(), ttc);
                            for (int k = mEventSchedule.size(); k <= currIndex; k++) {
                                mEventSchedule.add(new ArrayList<>());
                                mDayItemAdapter.notifyItemChanged(k);
                            }
                            mEventSchedule.get(currIndex).add(toAdd);
                            mTaskAppViewModel.insert(toAdd);
                            mDayItemAdapter.notifyItemChanged(currIndex);

                            // Calculate next date
                            recurCal.add(Calendar.YEAR, interval);
                            recurCal.set(Calendar.DAY_OF_WEEK, dayOfWeek);
                            recurCal.set(Calendar.DAY_OF_WEEK_IN_MONTH, dayOfWeekInMonth);
                            currIndex = getDiff(recurCal.getTime(), mStartDate);
                        }
                    }
                    if (intervalType.equals(YearlyRecurFragment.EXTRA_VAL_MULTIPLE_STATIC)) {
                        for (int i = 0; i < months.length; i++) {
                            if (months[i]) {
                                boolean oneLess = (i - recurCal.get(Calendar.MONTH)) < 0;
                                boolean offByOne = oneLess && (interval != 1);
                                recurCal = Calendar.getInstance();
                                recurCal.setTime(start);
                                recurCal.add(Calendar.MONTH,
                                        (i - recurCal.get(Calendar.MONTH) + 12) % 12);
                                currIndex = getDiff(recurCal.getTime(), mStartDate);

                                if (until) {
                                    numTimes = Integer.MAX_VALUE;
                                }
                                else {
                                    endDate.setTime(Long.MAX_VALUE);
                                }

                                for (int j = 0; j < numTimes && !recurCal.getTime()
                                        .after(endDate); j++) {
                                    // Add event to schedule
                                    if (!(j == 0 && offByOne) && !(j == numTimes - 1 && oneLess &&
                                            !offByOne)) {
                                        Event toAdd = new Event(name, recurCal.getTime(), ttc);
                                        for (int k = mEventSchedule.size(); k <= currIndex; k++) {
                                            mEventSchedule.add(new ArrayList<>());
                                            mDayItemAdapter.notifyItemChanged(k);
                                        }
                                        mEventSchedule.get(currIndex).add(toAdd);
                                        mTaskAppViewModel.insert(toAdd);
                                        mDayItemAdapter.notifyItemChanged(currIndex);
                                    }

                                    // Calculate next date
                                    if (j == 0 && offByOne) {
                                        recurCal.add(Calendar.YEAR, -1);
                                    }
                                    recurCal.add(Calendar.YEAR, interval);
                                    currIndex = getDiff(recurCal.getTime(), mStartDate);
                                }
                            }
                        }
                    }
                    if (intervalType.equals(YearlyRecurFragment.EXTRA_VAL_MULTIPLE_DYNAMIC)) {
                        int dayOfWeek = recurCal.get(Calendar.DAY_OF_WEEK);
                        int dayOfWeekInMonth = recurCal.get(Calendar.DAY_OF_WEEK_IN_MONTH);

                        for (int i = 0; i < months.length; i++) {
                            if (months[i]) {
                                boolean oneLess = (i - recurCal.get(Calendar.MONTH)) < 0;
                                boolean offByOne = oneLess && (interval != 1);
                                recurCal = Calendar.getInstance();
                                recurCal.setTime(start);
                                recurCal.add(Calendar.MONTH,
                                        (i - recurCal.get(Calendar.MONTH) + 12) % 12);
                                recurCal.set(Calendar.DAY_OF_WEEK, dayOfWeek);
                                recurCal.set(Calendar.DAY_OF_WEEK_IN_MONTH, dayOfWeekInMonth);
                                currIndex = getDiff(recurCal.getTime(), mStartDate);

                                if (until) {
                                    numTimes = Integer.MAX_VALUE;
                                }
                                else {
                                    endDate.setTime(Long.MAX_VALUE);
                                }

                                for (int j = 0; j < numTimes && !recurCal.getTime()
                                        .after(endDate); j++) {
                                    // Add event to schedule
                                    if (!(j == 0 && offByOne) && !(j == numTimes - 1 && oneLess
                                            && !offByOne)) {
                                        Event toAdd = new Event(name, recurCal.getTime(), ttc);
                                        for (int k = mEventSchedule.size(); k <= currIndex; k++) {
                                            mEventSchedule.add(new ArrayList<>());
                                            mDayItemAdapter.notifyItemChanged(k);
                                        }
                                        mEventSchedule.get(currIndex).add(toAdd);
                                        mTaskAppViewModel.insert(toAdd);
                                        mDayItemAdapter.notifyItemChanged(currIndex);
                                    }

                                    // Calculate next date
                                    if (j == 0 && offByOne) {
                                        recurCal.add(Calendar.YEAR, -1);
                                    }
                                    recurCal.add(Calendar.YEAR, interval);
                                    recurCal.set(Calendar.DAY_OF_WEEK, dayOfWeek);
                                    recurCal.set(Calendar.DAY_OF_WEEK_IN_MONTH, dayOfWeekInMonth);
                                    currIndex = getDiff(recurCal.getTime(), mStartDate);
                                }
                            }
                        }
                    }
                    if (intervalType.equals(YearlyRecurFragment.EXTRA_VAL_SPECIFIC)) {
                        String[] daysStrs = recur.getString(YearlyRecurFragment.EXTRA_DAYS)
                                .split(",");
                        int[] days = new int[daysStrs.length];

                        for (int i = 0; i < days.length; i++) {
                            days[i] = Integer.parseInt(daysStrs[i]);
                        }

                        for (int i = 0; i < months.length; i++) {
                            if (months[i]) {
                                boolean oneLess = (i - recurCal.get(Calendar.MONTH)) < 0;
                                boolean offByOne = oneLess && (interval != 1);
                                recurCal = Calendar.getInstance();
                                recurCal.setTime(start);
                                recurCal.add(Calendar.MONTH,
                                        (i - recurCal.get(Calendar.MONTH) + 12) % 12);

                                if (until) {
                                    numTimes = Integer.MAX_VALUE;
                                }
                                else {
                                    endDate.setTime(Long.MAX_VALUE);
                                }

                                for (int j = 0; j < numTimes && !recurCal.getTime()
                                        .after(endDate); j++) {
                                    for (int day : days) {
                                        if (day <= recurCal.getActualMaximum
                                                (Calendar.DAY_OF_MONTH)) {
                                            recurCal.set(Calendar.DAY_OF_MONTH, day);
                                            currIndex = getDiff(recurCal.getTime(), mStartDate);

                                            if (!recurCal.getTime().before(start) &&
                                                    !recurCal.getTime().after(endDate) &&
                                                    !(j == 0 && offByOne) && !(j == numTimes - 1
                                                    && oneLess && !offByOne)) {
                                                Event toAdd = new Event(name, recurCal.getTime(),
                                                        ttc);
                                                for (int k = mEventSchedule.size(); k <=
                                                        currIndex; k++) {
                                                    mEventSchedule.add(new ArrayList<>());
                                                    mDayItemAdapter.notifyItemChanged(k);
                                                }
                                                mEventSchedule.get(currIndex).add(toAdd);
                                                mTaskAppViewModel.insert(toAdd);
                                                mDayItemAdapter.notifyItemChanged(currIndex);
                                            }
                                        }
                                    }

                                    if (j == 0 && offByOne) {
                                        recurCal.add(Calendar.YEAR, -1);
                                    }
                                    recurCal.add(Calendar.YEAR, interval);
                                }
                            }
                        }
                    }
                }
            }
            else {
                for (int i = mEventSchedule.size(); i <= diff; i++) {
                    mEventSchedule.add(new ArrayList<>());
                    mDayItemAdapter.notifyItemChanged(i);
                }

                Event toAdd = new Event(name, start, ttc);
                mEventSchedule.get(diff).add(toAdd);
                mTaskAppViewModel.insert(toAdd);
                mDayItemAdapter.notifyItemChanged(diff);
            }
        }
        // If the item type is Task
        else if (type.equals(AddItem.EXTRA_VAL_TASK)) {
            // Get the fields from the Bundle
            String name = result.getString(AddItem.EXTRA_NAME);
            int timeToComplete = Integer.parseInt(result.getString(AddItem.EXTRA_TTC));
            String ecd = result.getString(AddItem.EXTRA_ECD);
            String dd = result.getString(AddItem.EXTRA_DUE);
            String parents = result.getString(AddItem.EXTRA_PARENTS);

            // Convert the earliest completion date String to a MyTime
            Date early;
            try {
                early = Task.dateFormat.parse(ecd);
            }
            catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            // Convert the due date String to a MyTime
            Date due;
            try {
                due = Task.dateFormat.parse(dd);
            }
            catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            Task toAdd = new Task(name, early, due, timeToComplete);

            // The parents string in the Bundle is a String of the format "n1,n2,n3,...nN,"
            // where each nx is an index to a Task in tasks that should be used as a parent
            // for the task to be added.
            String[] parentIndices = parents.split(",");
            for (String parentIndex : parentIndices) {
                if (!parentIndex.equals("-1")) {
                    Task parent = mTasks.get(Integer.parseInt(parentIndex));
                    toAdd.addParent(parent);
                    parent.addChild(toAdd);
                }
            }

            mTasks.add(toAdd);
            mTaskAppViewModel.insert(toAdd);
        }

        // As the task dependency graph has been updated, we must reoptimize it
        Optimize(true);
        // Show recycler as Optimize is finished
        mVF.setDisplayedChild(1);
    }

    /**
     * Calls the Optimizer to find an optimal schedule for the user's tasks, given the user's
     * scheduled events.
     *
     * @param refresh true refreshes recycler, false doesn't allows us to call Optimize() before
     *                recycler initialization
     */
    private void Optimize(boolean refresh) {
        Optimizer opt = new Optimizer();
        ArrayList<Task> changedTasks = opt.Optimize
                (mTasks, mTaskSchedule, mEventSchedule, mStartDate, mTodayTime);

        // Delete any empty lists at the end of the taskSchedule.
        int taskLowIndex = mTaskSchedule.size();
        for (int i = mTaskSchedule.size() - 1; i >= 0; i--) {
            if (mTaskSchedule.get(i).size() != 0) {
                break;
            }
            else {
                taskLowIndex--;
            }
        }
        if (mTaskSchedule.size() > taskLowIndex) {
            mTaskSchedule.subList(taskLowIndex, mTaskSchedule.size()).clear();
        }

        // Delete any empty lists at the end of the event schedule.
        int eventLowIndex = mEventSchedule.size();
        for (int i = mEventSchedule.size() - 1; i >= 0; i--) {
            if (mEventSchedule.get(i).size() != 0) {
                break;
            }
            else {
                eventLowIndex--;
            }
        }

        int eventScheduleSize = mEventSchedule.size();

        if (eventScheduleSize > eventLowIndex) {
            mEventSchedule.subList(eventLowIndex, eventScheduleSize).clear();

            // Remove hanging days from adapter as well
            eventLowIndex = Math.max(eventLowIndex, taskLowIndex);
            if (eventScheduleSize > eventLowIndex) {
                mDayItemAdapter.notifyItemRangeRemoved(eventLowIndex, eventScheduleSize);
            }
        }

        // As the Optimizer may have changed tasks' dates, we must refresh the recycler
        if (refresh) {
            mDayItemAdapter.mDayItemList = DayItemList();

            // Tell the recycler about moved tasks.
            for (Task t : changedTasks) {
                int oldIndex = getDiff(t.getDoDate(), mStartDate);
                int newIndex = getDiff(t.getWorkingDoDate(), mStartDate);

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

        // Update the task with the new do date, and reflect this change in the database.
        for (Task t : changedTasks) {
            t.setDoDate(t.getWorkingDoDate());
            Log.d("TEST", String.valueOf(t.getID()));
            mTaskAppViewModel.update(t);
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

        mTodayTime = 0;

        // Get todayTime from shared preferences
        SharedPreferences sp = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
        Date todayTimeDate = new Date(sp.getLong(PREF_DAY, -1L));

        if (!todayTimeDate.before(mStartDate)) {
            mTodayTime = sp.getInt(PREF_TIME, 0);
        }

        ViewModelStoreOwner vmso = this;

        // Populate from database;
        Thread thread = new Thread() {
            public void run() {
                mTaskAppViewModel = new ViewModelProvider(vmso).get(TaskAppViewModel.class);
            }
        };
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mTasks = mTaskAppViewModel.getAllTasks();

        mTasks = (mTasks == null) ? new ArrayList<>() : mTasks;

        ArrayList<Task> overdueTasks = new ArrayList<>(); // Tasks that are overdue.

        // Add tasks to taskSchedule/add parents
        for (Task t : mTasks) {
            // Calculate how many days past today's date this task is scheduled for. Used to
            // index into taskSchedule
            Date doDate = t.getDoDate();
            int index = getDiff(doDate, mStartDate);

            // Adds file to taskSchedule if it is scheduled for today or later.
            if (index >= 0) {
                // Make sure taskSchedule is big enough
                for (int i = mTaskSchedule.size(); i <= index; i++) {
                    mTaskSchedule.add(new ArrayList<>());
                }

                // Add to taskSchedule
                mTaskSchedule.get(index).add(t);
            } else {
                overdueTasks.add(t);
            }
        }

        // Add parent/child structure to task lists, as Room DB cannot do this
        for (Task t : mTasks) {
            if (t.getParentArr().size() != 0) {
                for (Task other : mTasks) {
                    if (t.getParentArr().contains(other.getID())) {
                        t.addParent(other);
                        other.addChild(t);
                    }
                }
            }
        }

        // Prompt the user with a dialog containing overdue tasks so they can mark overdue tasks
        // so taskapp can reoptimize the schedule if some tasks are overdue.
        if (overdueTasks.size() != 0) {
            String[] overdueNames = new String[overdueTasks.size()];

            // Create a list of overdue task names for the dialog
            for (int i = 0; i < overdueNames.length; i++) {
                Task t = overdueTasks.get(i);
                Date tDate = t.getDueDate();
                overdueNames[i] = String.format(getString(R.string.due_when), t.getName(),
                        Task.dateFormat.format(tDate));
            }

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
                                    // As the user has marked these tasks as completed, remove them.
                                    // Pass in false as the user completed them on a prior day.
                                    for (int i = 0; i < selectedItems.size(); i++) {
                                        Complete(overdueTasks.get(selectedItems.get(i)),
                                                false, false);
                                    }

                                    // Change due date for overdue tasks if it has already been
                                    // passed to today.
                                    if (overdueTasks.size() != selectedItems.size()) {
                                        for (int i = 0; i < overdueTasks.size(); i++) {
                                            if (selectedItems.contains(i)) {
                                                continue;
                                            }

                                            Task t = overdueTasks.get(i);

                                            if (t.getDueDate().before(mStartDate)) {
                                                t.setDueDate(mStartDate);
                                            }
                                        }
                                    }

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
     * Extremely sketchy, but allows us to wait to finish processing events and displaying the
     * recycler until the user has completed the overdue tasks dialog
     *
     * @param reoptimize If the task list has changed due to overdue tasks, we might have to
     *                   reoptimize it
     */
    private void finishProcessing(boolean reoptimize) {
        // Get the event list.
        List<Event> events = mTaskAppViewModel.getAllEvents();

        events = (events == null) ? new ArrayList<>() : events;

        // Add events from the database into the eventSchedule
        for (Event e : events) {
            // Calculate how many days past today's date this event is scheduled for. Used to
            // index into eventSchedule
            Date doDate = e.getDoDate();
            int doDateIndex = getDiff(doDate, mStartDate);

            // Add the events to the list if they aren't for an earlier date
            if (!doDate.before(mStartDate)) {
                for (int j = mEventSchedule.size(); j <= doDateIndex; j++) {
                    mEventSchedule.add(new ArrayList<>());
                }

                mEventSchedule.get(doDateIndex).add(e);
            }
        }

        // If tasks were changed, make sure to reoptimize the schedule in case it's necessary
        if (reoptimize) {
            Optimize(false);
        }

        // Initialize the main recyclerview with data calculated in helper function DayItemList
        RecyclerView dayRecyclerView = findViewById(R.id.main_recyclerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(MainActivity.this);
        mDayItemAdapter = new DayItemAdapter(DayItemList(), this);
        dayRecyclerView.setAdapter(mDayItemAdapter);
        dayRecyclerView.setLayoutManager(layoutManager);

        // Adds the action bar at the top of the screen
        setSupportActionBar(mBinding.toolbar);

        // When the FAB is clicked, run intentAddItem to open the AddItem Activity
        mBinding.fab.setOnClickListener(view -> intentAddItem());

        // Make visible the main content
        mVF = findViewById(R.id.vf);

        if (mTaskSchedule.size() != 0 || mEventSchedule.size() != 0) {
            mVF.setDisplayedChild(1);
        }
        else {
            mVF.setDisplayedChild(2);
        }

        mStartForResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> MainActivity.this.onActivityResult(result.getResultCode(),
                        result.getData()));
    }

    @Override
    protected void onResume() {
        super.onResume();

        List<Event> todayTimes = mTaskAppViewModel.getTodayTimes();

        int todayTime = -1;

        for (Event e : todayTimes) {
            if (e.getLength() > todayTime && !e.getDoDate().before(mStartDate)) {
                todayTime = e.getLength();
            }
            mTaskAppViewModel.delete(e);
        }
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
        edit.putInt(PREF_TIME, mTodayTime);
        edit.apply();
    }

    /**
     * Launches the AddItem activity. Must be separate function so FAB handler can call it.
     */
    private void intentAddItem() {
        Intent intent = new Intent(this, AddItem.class);

        // Create an ArrayList of task names so the multiselect dialog can use it to choose parent
        // tasks
        ArrayList<String> taskNames = new ArrayList<>();
        for (Task t : mTasks) {
            Date tDate = t.getDueDate();
            taskNames.add(String.format(getString(R.string.due_when), t.getName(),
                    Task.dateFormat.format(tDate)));
        }

        intent.putStringArrayListExtra(EXTRA_TASKS, taskNames);
        mStartForResult.launch(intent);
    }

    private DayItem DayItemHelper(int i) {
        // Fields for the DayItem
        String dayString;
        List<EventItem> events;
        List<TaskItem> tasks;

        // MyTime representing the date i days past today's date
        Calendar currCal = Calendar.getInstance();
        currCal.add(Calendar.DAY_OF_YEAR, i);
        Date curr = clearDate(currCal.getTime());

        // Number representing totalTime this date has scheduled. If it's today's date, add
        // todayTime to represent the time already completed tasks took.
        int totalTime = (i == 0) ? mTodayTime : 0;

        // Adds the total event time for the day to the total time
        if (i < mEventSchedule.size() && mEventSchedule.get(i).size() > 0) {
            for (int j = 0; j < mEventSchedule.get(i).size(); j++) {
                Event event = mEventSchedule.get(i).get(j);
                totalTime += event.getLength();
            }
        }

        // Adds the total task time for the day to the total time
        if (i < mTaskSchedule.size() && mTaskSchedule.get(i).size() > 0) {
            for (int j = 0; j < mTaskSchedule.get(i).size(); j++) {
                Task task = mTaskSchedule.get(i).get(j);

                totalTime += task.getTimeToComplete();
            }
        }

        // Set the fields
        dayString = String.format(getString(R.string.schedule_for), Task.dateFormat.format(curr),
                totalTime);
        events = EventItemList(i);
        tasks = TaskItemList(i);

        return new DayItem(dayString, events, tasks, i);
    }

    /**
     * Builds a DayItem List representation of a user's tasks/events
     *
     * @return a DayItem List representation of a user's tasks/events
     */
    private List<DayItem> DayItemList() {
        // The list of DayItem's to be displayed in the recycler
        List<DayItem> itemList = new ArrayList<>();

        // Generate a DayItem for the date i days past today's date
        for (int i = 0; i < mTaskSchedule.size() || i < mEventSchedule.size(); i++) {
            itemList.add(DayItemHelper(i));
        }

        return itemList;
    }

    /**
     * Builds an EventItem List representation of a user's events on a given day
     *
     * @param index The index into the data structure representing the day
     *
     * @return an EventItem List representation of a user's events on a given day
     */
    private List<EventItem> EventItemList(int index) {
        // The list of EventItems representing the events scheduled for index days past today's date
        List<EventItem> itemList = new ArrayList<>();

        // Add all the events scheduled for the given day to itemList, if any are scheduled
        if (index < mEventSchedule.size() && mEventSchedule.get(index).size() > 0) {
            for (int j = 0; j < mEventSchedule.get(index).size(); j++) {
                // Fields for itemList
                String name;
                String timespan;

                // Get the jth event from the given date
                Event event = mEventSchedule.get(index).get(j);

                // Get the start/end time in MyTime objects
                Date eventTime = event.getDoDate();
                Calendar endCal = Calendar.getInstance();
                endCal.setTime(eventTime);
                endCal.add(Calendar.MINUTE, event.getLength());
                Date endTime = clearTime(endCal.getTime());

                // Format the event name as Name: StartTime-EndTime
                name = event.getName();
                timespan = Event.timeFormat.format(eventTime) + "-" +
                        Event.timeFormat.format(endTime);

                itemList.add(new EventItem(name, timespan, j));
            }
        }
        return itemList;
    }

    /**
     * Builds a TaskItem List representation of a user's tasks on a given day
     *
     * @param index The index into the data structure representing the day
     *
     * @return a TaskItem List representation of a user's tasks on a given day
     */
    private List<TaskItem> TaskItemList(int index) {
        // The list of TaskItems representing the tasks scheduled for the date index days past
        // today's date
        List<TaskItem> itemList = new ArrayList<>();

        // Add all the tasks scheduled for the given date to itemList
        if (index < mTaskSchedule.size() && mTaskSchedule.get(index).size() > 0) {
            for (int j = 0; j < mTaskSchedule.get(index).size(); j++) {
                // DayItem's only field
                String name;

                // Get the jth task scheduled for the given day.
                Task task = mTaskSchedule.get(index).get(j);

                // Create the name in the format NAME (TTC minutes to complete)
                name = task.getName() + "\n" + String.format(getString(R.string.minutes_to_complete),
                        task.getTimeToComplete());

                itemList.add(new TaskItem(name, j));
            }
        }

        return itemList;
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
        if (action == 0) {
            if (day == -1 || mTaskSchedule.get(day).size() <= position) {
                mVF.setDisplayedChild(1);
                return;
            }

            mTaskAppViewModel.delete(mTaskSchedule.get(day).get(position));
            Complete(mTaskSchedule.get(day).get(position), true, true);
        }
        // Remove the given task from the task dependency graph without completion time dialog
        if (action == 1) {
            if (day == -1 || mTaskSchedule.get(day).size() <= position) {
                mVF.setDisplayedChild(1);
                return;
            }
            mTaskAppViewModel.delete(mTaskSchedule.get(day).get(position));
            Complete(mTaskSchedule.get(day).get(position), false, true);
        }
        // Remove the given event from the schedule and re-optimize.
        if (action == 2) {
            if (day == -1 || mEventSchedule.get(day).size() <= position) {
                mVF.setDisplayedChild(1);
                return;
            }
            mTaskAppViewModel.delete(mEventSchedule.get(day).get(position));
            mEventSchedule.get(day).remove(position);
            mDayItemAdapter.mDayItemList.set(day, DayItemHelper(day));
            mDayItemAdapter.notifyItemChanged(day);
            Optimize(true);
        }

        if (mTaskSchedule.size() != 0 || mEventSchedule.size() != 0) {
            mVF.setDisplayedChild(1);
        }
        else {
            mVF.setDisplayedChild(2);
        }

        //noinspection unchecked
        HideBottomViewOnScrollBehavior<FloatingActionButton> fabBehavior =
                (HideBottomViewOnScrollBehavior<FloatingActionButton>)
                        ((CoordinatorLayout.LayoutParams) mBinding.fab.getLayoutParams())
                                .getBehavior();

        if (fabBehavior != null) {
            fabBehavior.slideUp(mBinding.fab);
        }
    }
}