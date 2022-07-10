package com.evanv.taskapp;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.text.InputType;
import android.util.Log;
import android.view.View;

import androidx.navigation.ui.AppBarConfiguration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.evanv.taskapp.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Scanner;

/**
 * Main Activity for the app. Display's the user's schedule of Tasks/Events, while allowing for
 * Task completion/addition (with the latter done through the use of a separate AddItem activity).
 *
 * @author Evan Voogd
 */
public class MainActivity extends AppCompatActivity implements ClickListener {

    private ActivityMainBinding binding;      // Binding for the MainActivity
    private MyTime startDate;                 // The current date
    private int todayTime;                    // The amount of time spent completing tasks today
    private int numEvents;                    // Number of events for user
    public static final int ITEM_REQUEST = 1; // requestCode for task/item entry
    private DayItemAdapter dayItemAdapter;    // Adapter for recyclerview showing user commitments
    private final ArrayList<Task> tasks = new ArrayList<Task>(); // List of all tasks for user
    // taskSchedule[i] represents the list of tasks for the day i days past startDate
    private final ArrayList<ArrayList<Task>> taskSchedule = new ArrayList<ArrayList<Task>>();
    // eventSchedule[i] represents the list of events for the day i days past startDate
    private final ArrayList<ArrayList<Event>> eventSchedule = new ArrayList<ArrayList<Event>>();
    // Key for the extra that stores the list of Task names for the Parent Task Picker Dialog in
    // TaskEntry
    public static final String EXTRA_TASKS = "com.evanv.taskapp.extras.TASKS";

    /**
     * Removes a task from the task dependency graph, while asking the user how long it took to
     * complete and adding that time to todayTime to prevent overscheduling on today's date.
     *
     * @param task The task to be removed from the task dependency graph
     * @param showDialog true if dialog is needed, false if dialog isn't
     */
    private void Complete(Task task, boolean showDialog) {
        tasks.remove(task);

        MyTime doDate = task.getDoDate();

        // Get the number of days past the start date this task is scheduled for, so we can get the
        // index of the taskSchedule member for it's do date.
        int diff = (int) (((doDate.getDateTime() - startDate.getDateTime()) / 1440));

        if (diff >= 0) {
            taskSchedule.get(diff).remove(task);
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

            builder.setPositiveButton(R.string.complete_task, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    todayTime += Integer.parseInt(input.getText().toString());
                    // As the task dependency graph has been updated, we must reoptimize it
                    Optimize(true);
                }
            });

            builder.show();
        }
    }

    /**
     * Handles activities started for a result, in this case when the AddItem activity returns with
     * a new Event/Task to be added. Parses the data in the BundleExtra AddItem.EXTRA_ITEM into a
     * Task/Event depending on their AddItem.EXTRA_TYPE.
     *
     * @param requestCode ITEM_REQUEST if request was for AddItem
     * @param resultCode RESULT_OK if there were no issues with user input
     * @param data Contains the BundleExtra AddItem.EXTRA_ITEM, with all the data needed to build
     *             the item.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // If the request is for AddItem
        if (requestCode == ITEM_REQUEST) {
            if (resultCode == RESULT_OK) {
                // Get the data to build the item
                assert data != null;
                Bundle result = data.getBundleExtra(AddItem.EXTRA_ITEM);
                String type = result.getString(AddItem.EXTRA_TYPE);

                // If the item type is Event
                if (type.equals(AddItem.EXTRA_VAL_EVENT)) {
                    // Get the fields from the bundle
                    String name = result.getString(AddItem.EXTRA_NAME);
                    int ttc = Integer.parseInt(result.getString(AddItem.EXTRA_TTC));
                    String startStr = result.getString(AddItem.EXTRA_START);
                    int recur = Integer.parseInt(result.getString(AddItem.EXTRA_RECUR));


                    // Convert the String start time into a MyTime
                    MyTime start;
                    try {
                        String[] fullTokens = startStr.split(" ");
                        String[] dateTokens = fullTokens[0].split("/");
                        String[] timeTokens = fullTokens[1].split(":");

                        int month = Integer.parseInt(dateTokens[0]);
                        int day = Integer.parseInt(dateTokens[1]);
                        int year = 2000 + Integer.parseInt(dateTokens[2]);
                        int hour = Integer.parseInt(timeTokens[0]);
                        int minute = Integer.parseInt(timeTokens[1]);

                        if (hour == 12) {
                            if (fullTokens[2].equals(getString(R.string.am))) {
                                hour = 0;
                            }
                        }
                        else if (fullTokens[2].equals(getString(R.string.pm))) {
                            hour += 12;
                        }

                        start = new MyTime(month, day, year, hour, minute);
                    }
                    catch (Exception e) {
                        System.out.println(e.getMessage());
                        return;
                    }

                    // Calculate how many days past today's date this event is scheduled (used to
                    // index into eventSchedule
                    int diff = (int) (((start.getDateTime() - startDate.getDateTime()) / 1440));

                    // As the eventSchedule's indices are based on how many days past the start day,
                    // we make sure to add enough lists to get to the needed index
                    for (int i = eventSchedule.size(); i <= diff + (7*(recur-1)); i++) {
                        eventSchedule.add(new ArrayList<Event>());;
                    }

                    // Add the new event to the data structure
                    Event toAdd = new Event(name, start, ttc);
                    eventSchedule.get(diff).add(toAdd);
                    numEvents++;

                    // Recurring is only on a weekly basis (this is a prototype after all), this is
                    // how we achieve it.
                    for (int i = 1; i < recur; i++) {
                        eventSchedule.get(diff + (7*i)).add(new Event(name,
                                new MyTime(start.getDateTime() + ((long) i * 7 * 1440)),
                                ttc));
                        numEvents++;
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
                    MyTime early;
                    try {
                        String[] earlyStrs = ecd.split("/");
                        early = new MyTime(Integer.parseInt(earlyStrs[0]),
                                Integer.parseInt(earlyStrs[1]),
                                2000 + Integer.parseInt(earlyStrs[2]));
                    }
                    catch (Exception e) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Convert the due date String to a MyTime
                    MyTime due;
                    try {
                        String[] dueStrs = dd.split("/");
                        due = new MyTime(Integer.parseInt(dueStrs[0]), Integer.parseInt(dueStrs[1]),
                                2000 + Integer.parseInt(dueStrs[2]));
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
                    if (!parentIndices[0].equals("-1")) {
                        for (String parentIndex : parentIndices) {
                            Task parent = tasks.get(Integer.parseInt(parentIndex));
                            toAdd.addParent(parent);
                            parent.addChild(toAdd);
                        }
                    }

                    tasks.add(toAdd);
                }

                // As the task dependency graph has been updated, we must reoptimize it
                Optimize(true);
            }
        }
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
        opt.Optimize(tasks, taskSchedule, eventSchedule, startDate, todayTime);

        // As the Optimizer may have changed tasks' dates, we must refresh the recycler
        if (refresh) {
            dayItemAdapter.mDayItemList = DayItemList();
            dayItemAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Runs on the start of the app. Most importantly it loads the user data from the file.
     *
     * @param savedInstanceState Not used. (Except internally by Android support libraries maybe)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create layout
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // startDate is our representation for the current date upon the launch of TaskApp.
        GregorianCalendar rightNow = new GregorianCalendar();
        startDate = new MyTime(rightNow.get(Calendar.MONTH) + 1,
                rightNow.get(Calendar.DAY_OF_MONTH), rightNow.get(Calendar.YEAR));

        numEvents = 0;
        todayTime = 0;

        try {
            // Populate from file
            Scanner fileRead = new Scanner(new File(getFilesDir() + "state.tsk"));

            // Get todayTime from the file to make sure the optimizer doesn't overschedule today
            String todTimeString = fileRead.nextLine();
            String[] todTimeSplit = todTimeString.split(": ");
            if (startDate.getDateTime() == Integer.parseInt(todTimeSplit[0])) {
                todayTime = Integer.parseInt(todTimeSplit[1]);
            }

            String ntString = fileRead.nextLine();

            // Read the task count from the file
            int numTasks = Integer.parseInt(ntString);
            ArrayList<Task> overdueTasks = new ArrayList<Task>();

            // Read in all the tasks from the file, in the general format:
            // Task Name|earlyDate|dueDate|doDate|timeToComplete|head,parent1,parent2,etc.\n
            for (int i = 0; i < numTasks; i++) {
                String temp = fileRead.nextLine();

                String[] data = temp.split("\\|");

                // Read in values from file
                String name = data[0];
                MyTime earlyDate = new MyTime(Long.parseLong(data[1]));
                MyTime dueDate = new MyTime(Long.parseLong(data[2]));
                MyTime doDate = new MyTime(Long.parseLong(data[3]));
                int timeToComplete = Integer.parseInt(data[4]);
                String parents = data[5];
                String[] parentList = parents.split(",");

                // If earlyDate has past, set today as the earlyDate
                if (earlyDate.getDateTime() < startDate.getDateTime()) {
                    earlyDate = startDate;
                }

                // Create the task
                Task toAdd = new Task(name, earlyDate, dueDate, timeToComplete);
                toAdd.setDoDate(doDate);

                // Add parents based on the parent list. Start at 1 as 0 is the head
                // (there for ease of writing)
                for (int j = 1; j < parentList.length; j++) {
                    toAdd.addParent(tasks.get(Integer.parseInt(parentList[j])));
                    tasks.get(Integer.parseInt(parentList[j])).addChild(toAdd);
                }

                tasks.add(toAdd);

                // Calculate how many days past today's date this task is scheduled for. Used to
                // index into taskSchedule
                int doDateIndex = (int) (((toAdd.getDoDate().getDateTime() -
                        startDate.getDateTime()) / 1440));

                // Adds file to taskSchedule if it is scheduled for today or later.
                if (doDateIndex >= 0) {
                    for (int j = taskSchedule.size(); j <= doDateIndex; j++) {
                        taskSchedule.add(new ArrayList<Task>());
                    }

                    taskSchedule.get(doDateIndex).add(toAdd);
                }

                // If a task is overdue, add it to the overdue list so the user can mark it as
                // complete or not.
                if (doDate.getDateTime() < startDate.getDateTime()) {
                    overdueTasks.add(toAdd);
                }
            }

            // Prompt the user with a dialog containing overdue tasks so they can mark overdue tasks
            // so taskapp can reoptimize the schedule if some tasks are overdue.
            if (overdueTasks.size() != 0) {
                String[] overdueNames = new String[overdueTasks.size()];

                // Create a list of overdue task names for the dialog
                for (int i = 0; i < overdueNames.length; i++) {
                    Task t = overdueTasks.get(i);
                    MyTime tDate = t.getDueDate();
                    overdueNames[i] = t.getName() + getString(R.string.due_when) + tDate.getMonth() +
                            "/" + tDate.getDate() + "/" + (tDate.getYear() - 2000) + ")";
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
                                     * Adds the selected task to the toRemove list, or removes it if
                                     * the task was unselected
                                     *
                                     * @param dialogInterface not used
                                     * @param index           the index into the tasks ArrayList of
                                     *                        the parent
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
                                        // As the user has marked these tasks as completed, remove
                                        // them. Pass in false as the user completed them on a prior
                                        // day.
                                        for (int i = 0; i < selectedItems.size(); i++) {
                                            Complete(overdueTasks.get(selectedItems.get(i)),
                                                    false);
                                        }

                                        // Change due date for overdue tasks if it has already been
                                        // passed to today.
                                        if (overdueTasks.size() != selectedItems.size()) {
                                            for (int i = 0; i < overdueTasks.size(); i++) {
                                                if (selectedItems.contains(i)) {
                                                    continue;
                                                }

                                                Task t = overdueTasks.get(i);

                                                if (t.getDueDate().getDateTime() <
                                                        startDate.getDateTime()) {
                                                    t.setDueDate(startDate);
                                                }
                                            }
                                        }

                                        finishProcessing(fileRead, true);
                                    }
                                });

                builder.create();
                builder.show();

            }
            else {
                finishProcessing(fileRead, false);
            }
        }
        catch (Exception e) {
            Log.d("TaskApp.MainActivity", "Storage file empty/misformatted");
        }
    }

    /**
     * Extremely sketchy, but allows us to wait to finish processing events and displaying the
     * recycler until the user has completed the overdue tasks dialog
     * @param fileRead The scanner used to read the state file
     */
    private void finishProcessing(Scanner fileRead, boolean reoptimize) {
        try {
            // Read the number of events in from the file.
            numEvents = fileRead.nextInt();
            fileRead.nextLine();
            // Allows us to decrement numEvents without messing up iteration
            int numEventsCopy = numEvents;

            // Read in all the events in the file, with the general format of:
            // Event Name|Time to Complete|Start Time\n
            for (int i = 0; i < numEventsCopy; i++) {
                String temp = fileRead.nextLine();

                String[] data = temp.split("\\|");

                // Load in the fields
                String name = data[0];
                int length = Integer.parseInt(data[1]);
                MyTime doDate = new MyTime(Long.parseLong(data[2]));

                Event toAdd = new Event(name, doDate, length);

                // Calculate how many days past today's date this event is scheduled for. Used to
                // index into eventSchedule
                int doDateIndex = (int) (((toAdd.getDoDate().getDateTime() -
                        startDate.getDateTime()) / 1440));

                // Add the events to the list if they aren't for an earlier date
                if (doDate.getDateTime() >= startDate.getDateTime()) {
                    for (int j = eventSchedule.size(); j <= doDateIndex; j++) {
                        eventSchedule.add(new ArrayList<Event>());
                    }

                    eventSchedule.get(doDateIndex).add(toAdd);
                }
                else {
                    numEvents--;
                }
            }

            fileRead.close();
        }
        catch (Exception e) {
            Log.d("TaskApp.MainActivity", "Storage file empty/misformatted");
        }

        // If tasks were changed, make sure to reoptimize the schedule in case it's necessary
        if (reoptimize) {
            Optimize(false);
        }

        // Initialize the main recyclerview with data calculated in helper function DayItemList
        RecyclerView dayRecyclerView = findViewById(R.id.main_recyclerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(MainActivity.this);
        dayItemAdapter = new DayItemAdapter(DayItemList(), this);
        dayRecyclerView.setAdapter(dayItemAdapter);
        dayRecyclerView.setLayoutManager(layoutManager);

        // Adds the action bar at the top of the screen
        setSupportActionBar(binding.toolbar);

        // When the FAB is clicked, run intentAddItem to open the AddItem Activity
        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                intentAddItem();
            }
        });
    }

    /**
     * Launches the AddItem activity. Must be separate function so FAB handler can call it.
     */
    private void intentAddItem() {
        Intent intent = new Intent(this, AddItem.class);

        // Create an ArrayList of task names so the multiselect dialog can use it to choose parent
        // tasks
        ArrayList<String> taskNames = new ArrayList<>();
        for (Task t : tasks) {
            MyTime tDate = t.getDueDate();
            taskNames.add(t.getName() + getString(R.string.due_when) + tDate.getMonth() +
                    "/" + tDate.getDate() + "/" + (tDate.getYear()-2000) + ")");
        }

        intent.putStringArrayListExtra(EXTRA_TASKS, taskNames);
        startActivityForResult(intent, ITEM_REQUEST);
    }

    /**
     * Saves internal data structures to file to be rebuilt in onCreate()
     *
     * @param outState unused, required by override
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save data structure to file
        try {
            BufferedWriter out = new BufferedWriter(
                    new FileWriter(getFilesDir() + "state.tsk"));
            // Save todayTime to file to prevent from overscheduling on today's date
            out.write(Long.toString(startDate.getDateTime()) + ": " +
                    Integer.toString(todayTime) + "\n");
            out.write(Integer.toString(tasks.size()) + "\n");

            // Add all tasks to file, in the structure described in onCreate.
            for (int i = 0; i < tasks.size(); i++) {
                Task t = tasks.get(i);
                StringBuilder parents = new StringBuilder("head,");

                for (int j = 0; j < t.getParents().size(); j++) {
                    parents.append(tasks.indexOf(t.getParents().get(j))).append(",");
                }

                String taskLine = t.getName() + "|" + t.getEarlyDate().getDateTime() + "|" +
                        t.getDueDate().getDateTime() + "|" + t.getDoDate().getDateTime() + "|" +
                        t.getTimeToComplete() + "|" + parents.toString() + "\n";

                out.write(taskLine);
            }

            // Write the number of scheduled events to the file.
            out.write(Integer.toString(numEvents) + "\n");

            // Add all events to file, in the structure described in onCreate
            for (int i = 0; i < eventSchedule.size(); i++) {
                for (int j = 0; j < eventSchedule.get(i).size(); j++) {
                    Event e = eventSchedule.get(i).get(j);

                    out.write(new StringBuilder().append(e.getName()).append("|")
                            .append(e.getLength()).append("|").append(e.getDoDate().getDateTime())
                            .append("\n").toString());
                }
            }

            out.close();
        }
        catch (Exception e) {
            Log.e("TaskApp.MainActivity", e.toString());
        }
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
        for (int i = 0; i < taskSchedule.size() || i < eventSchedule.size(); i++) {
            // Fields for the DayItem
            String dayString;
            List<EventItem> events;
            List<TaskItem> tasks;

            // MyTime representing the date i days past today's date
            MyTime curr = new MyTime(startDate.getDateTime() + (i * 1440L));

            // Number representing totalTime this date has scheduled. If it's today's date, add
            // todayTime to represent the time already completed tasks took.
            int totalTime = (i == 0) ? todayTime : 0;

            // Adds the total event time for the day to the total time
            if (i < eventSchedule.size() && eventSchedule.get(i).size() > 0) {
                for (int j = 0; j < eventSchedule.get(i).size(); j++) {
                    Event event = eventSchedule.get(i).get(j);
                    totalTime += event.getLength();
                }
            }

            // Adds the total task time for the day to the total time
            if (i < taskSchedule.size() && taskSchedule.get(i).size() > 0) {
                for (int j = 0; j < taskSchedule.get(i).size(); j++) {
                    Task task = taskSchedule.get(i).get(j);

                    totalTime += task.getTimeToComplete();
                }
            }

            // Set the fields
            dayString = getString(R.string.schedule_for) + curr.getMonth() + "/" + curr.getDate()
                    + "/" + (curr.getYear()-2000) + " (" + totalTime + getString(R.string.minutes)
                    + ":";
            events = EventItemList(i);
            tasks = TaskItemList(i);

            itemList.add(new DayItem(dayString, events, tasks));
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
        if (index < eventSchedule.size() && eventSchedule.get(index).size() > 0) {
            for (int j = 0; j < eventSchedule.get(index).size(); j++) {
                // Fields for itemList
                String name;
                String timespan;

                // Get the jth event from the given date
                Event event = eventSchedule.get(index).get(j);

                // Get the start/end time in mytime objects
                MyTime eventTime = event.getDoDate();
                MyTime endTime = new MyTime(eventTime.getDateTime() + event.getLength());

                // Format the start time in the format HH:MM AM/PM
                short eventHour = eventTime.getHour();
                boolean event_am = eventHour < 12;
                String event_ampm = event_am ? " " + getString(R.string.am) :
                        " " + getString(R.string.pm);
                eventHour -= event_am ? 0 : 12;
                eventHour = (eventHour == 0) ? 12 : eventHour;
                short eventMinute = eventTime.getMinute();
                String eventMinuteString = eventMinute + "";
                eventMinuteString = (eventMinute < 10) ? "0" + eventMinuteString :
                        eventMinuteString;

                // Format the end time in the format HH:MM AM/PM
                short endHour = endTime.getHour();
                boolean end_am = endHour < 12;
                String end_ampm = end_am ? " " + getString(R.string.am) :
                        " " + getString(R.string.pm);;
                endHour -= end_am ? 0 : 12;
                endHour = (endHour == 0) ? 12 : endHour;
                short endMinute = endTime.getMinute();
                String endMinuteString = endMinute + "";
                endMinuteString = (endMinute < 10) ? "0" + endMinuteString : endMinuteString;

                // Format the event name as Name: StartTime-EndTime
                name = event.getName();
                timespan = eventHour + ":" + eventMinuteString + event_ampm + "-" + endHour + ":" +
                        endMinuteString + end_ampm;

                itemList.add(new EventItem(name, timespan));
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
        if (index < taskSchedule.size() && taskSchedule.get(index).size() > 0) {
            for (int j = 0; j < taskSchedule.get(index).size(); j++) {
                // DayItem's only field
                String name;

                // Get the jth task scheduled for the given day.
                Task task = taskSchedule.get(index).get(j);

                // Create the name in the format NAME (TTC minutes to complete)
                name = task.getName() + " (" + task.getTimeToComplete() +
                        " " + getString(R.string.minutes_to_complete) + ")";

                itemList.add(new TaskItem(name));
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
    public boolean onCreateOptionsMenu(Menu menu) {
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

        //noinspection SimplifiableIfStatement
        // Handles the settings menu item being chosen
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
    public void onButtonClick(int position, int day) {
        // Remove the given task from the task dependency graph
        Complete(taskSchedule.get(day).get(position), true);
    }
}