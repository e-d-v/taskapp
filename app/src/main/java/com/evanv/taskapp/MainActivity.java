package com.evanv.taskapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;

import androidx.navigation.ui.AppBarConfiguration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.evanv.taskapp.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity implements ClickListener {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private final ArrayList<Task> tasks = new ArrayList<Task>();
    private final ArrayList<ArrayList<Task>> taskSchedule = new ArrayList<ArrayList<Task>>();
    private final ArrayList<ArrayList<Event>> eventSchedule = new ArrayList<ArrayList<Event>>();
    private MyTime startDate;
    private int todayTime;
    private int numEvents;
    public static final int ITEM_REQUEST = 1;
    private DayItemAdapter dayItemAdapter;

    private void Complete(Task task) {
        tasks.remove(task);

        MyTime doDate = task.getDoDate();

        int diff = (int) (((doDate.getDateTime() - startDate.getDateTime()) / 1440));

        if (diff >= 0) {
            taskSchedule.get(diff).remove(task);
        }
        for (int i = 0; i < task.getChildren().size(); i++) {
            task.getChildren().get(i).removeParent(task);
        }
        for (int i = 0; i < task.getParents().size(); i++) {
            task.getParents().get(i).removeChild(task);
        }

        Optimize();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ITEM_REQUEST) {
            if (resultCode == RESULT_OK) {
                assert data != null;
                Bundle result = data.getBundleExtra(AddItem.EXTRA_ITEM);

                String type = result.getString(AddItem.EXTRA_TYPE);

                if (type.equals(AddItem.EXTRA_VAL_EVENT)) {
                    String name = result.getString(AddItem.EXTRA_NAME);
                    int ttc = Integer.parseInt(result.getString(AddItem.EXTRA_TTC));
                    String startStr = result.getString(AddItem.EXTRA_START);
                    int recur = Integer.parseInt(result.getString(AddItem.EXTRA_RECUR));


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
                            if (fullTokens[2].equals("AM")) {
                                hour = 0;
                            }
                        }
                        else if (fullTokens[2].equals("PM")) {
                            hour += 12;
                        }

                        start = new MyTime(month, day, year, hour, minute);
                    }
                    catch (Exception e) {
                        System.out.println(e.getMessage());
                        return;
                    }

                    int diff = (int) (((start.getDateTime() - startDate.getDateTime()) / 1440));

                    // As the eventSchedule's indices are based on how many days past the start day, we make sure to add
                    // enough lists to get to the needed index
                    for (int i = eventSchedule.size(); i <= diff + (7*(recur-1)); i++) {
                        eventSchedule.add(new ArrayList<Event>());;
                    }

                    Event toAdd = new Event(name, start, ttc);
                    eventSchedule.get(diff).add(toAdd);
                    numEvents++;

                    // Recurring is only on a weekly basis (this is a prototype after all), this is how we achieve it.
                    for (int i = 1; i < recur; i++) {
                        eventSchedule.get(diff + (7*i)).add(new Event(name, new MyTime(start.getDateTime() + ((long) i * 7 * 1440)), ttc));
                        numEvents++;
                    }

                }
                else if (type.equals(AddItem.EXTRA_VAL_TASK)) {
                    String name = result.getString(AddItem.EXTRA_NAME);
                    int timeToComplete = Integer.parseInt(result.getString(AddItem.EXTRA_TTC));
                    String ecd = result.getString(AddItem.EXTRA_ECD);
                    String dd = result.getString(AddItem.EXTRA_DUE);
                    int parents = Integer.parseInt(result.getString(AddItem.EXTRA_PARENTS));

                    MyTime early;
                    try {
                        String[] earlyStrs = ecd.split("/");
                        early = new MyTime(Integer.parseInt(earlyStrs[0]), Integer.parseInt(earlyStrs[1]), 2000 + Integer.parseInt(earlyStrs[2]));
                    }
                    catch (Exception e) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    MyTime due;
                    try {
                        String[] dueStrs = dd.split("/");
                        due = new MyTime(Integer.parseInt(dueStrs[0]), Integer.parseInt(dueStrs[1]), 2000 + Integer.parseInt(dueStrs[2]));
                    }
                    catch (Exception e) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Task toAdd = new Task(name, early, due, timeToComplete);

                    if (parents != -1) {
                        Task parent = tasks.get(parents);
                        toAdd.addParent(parent);
                        parent.addChild(toAdd);
                    }

                    tasks.add(toAdd);
                }

                Optimize();
            }
        }
    }

    private void Optimize() {

        Optimizer opt = new Optimizer();
        opt.Optimize(tasks, taskSchedule, eventSchedule, startDate, todayTime);

        dayItemAdapter.mDayItemList = DayItemList();
        dayItemAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        // startDate is our representation for the current date upon the launch of TaskApp.
        GregorianCalendar rightNow = new GregorianCalendar();
        startDate = new MyTime(rightNow.get(Calendar.MONTH) + 1, rightNow.get(Calendar.DAY_OF_MONTH),
                rightNow.get(Calendar.YEAR));

        numEvents = 0;
        todayTime = 0;

        try {
            // Populate from file
            Scanner fileRead = new Scanner(new File(getFilesDir() + "state.tsk"));

            String todTimeString = fileRead.nextLine();
            String[] todTimeSplit = todTimeString.split(": ");

            if (startDate.getDateTime() == Integer.parseInt(todTimeSplit[0])) {
                todayTime = Integer.parseInt(todTimeSplit[1]);
            }

            String ntString = fileRead.nextLine();

            int numTasks = Integer.parseInt(ntString);
            ArrayList<Task> tasksToComplete = new ArrayList<Task>();

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

                int doDateIndex = (int) (((toAdd.getDoDate().getDateTime() -
                        startDate.getDateTime()) / 1440));

                // Adds file to taskSchedule if it is scheduled for today or later.
                if (doDateIndex >= 0) {
                    for (int j = taskSchedule.size(); j <= doDateIndex; j++) {
                        taskSchedule.add(new ArrayList<Task>());
                    }

                    taskSchedule.get(doDateIndex).add(toAdd);
                }

                // If the task has (presumably) already been completed, mark it as needing to be
                // completed. We have to wait to complete it until later so the indices on the
                // parent tasks work properly.
                if (doDate.getDateTime() < startDate.getDateTime()) {
                    tasksToComplete.add(toAdd);
                }
            }

            // Complete all tasks with do dates before today, as they are presumably completed
            for (int i = 0; i < tasksToComplete.size(); i++) {
                Complete(tasksToComplete.get(i));
            }

            numEvents = fileRead.nextInt();
            fileRead.nextLine();
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
        // Initialize the main recyclerview with data calculated in helper function DayItemList
        RecyclerView dayRecyclerView = findViewById(R.id.main_recyclerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(MainActivity.this);
        dayItemAdapter = new DayItemAdapter(DayItemList(), this);
        dayRecyclerView.setAdapter(dayItemAdapter);
        dayRecyclerView.setLayoutManager(layoutManager);

        setSupportActionBar(binding.toolbar);

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

        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(getFilesDir() + "state.tsk"));
            out.write(Long.toString(startDate.getDateTime()) + ": " +
                    Integer.toString(todayTime) + "\n");
            out.write(Integer.toString(tasks.size()) + "\n");

            for (int i = 0; i < tasks.size(); i++) {
                Task t = tasks.get(i);
                StringBuilder parents = new StringBuilder("head,");

                for (int j = 0; j < t.getParents().size(); j++) {
                    parents.append(tasks.indexOf(t.getParents().get(j))).append(",");
                }

                out.write(t.getName() + "|" + t.getEarlyDate().getDateTime() + "|" +
                        t.getDueDate().getDateTime() + "|" + t.getDoDate().getDateTime() + "|" +
                        t.getTimeToComplete() + "|" + parents.toString() + "\n");
            }

            out.write(Integer.toString(numEvents) + "\n");

            for (int i = 0; i < eventSchedule.size(); i++) {
                for (int j = 0; j < eventSchedule.get(i).size(); j++) {
                    Event e = eventSchedule.get(i).get(j);

                    out.write(new StringBuilder().append(e.getName()).append("|").append(e.getLength()).append("|").append(e.getDoDate().getDateTime()).append("\n").toString());
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
        List<DayItem> itemList = new ArrayList<>();

        for (int i = 0; i < taskSchedule.size() || i < eventSchedule.size(); i++) {
            String dayString;
            List<EventItem> events;
            List<TaskItem> tasks;

            MyTime curr = new MyTime(startDate.getDateTime() + (i * 1440L));

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

            dayString = "Schedule for " + curr.getMonth() + "/" + curr.getDate() + "/" + +
                    (curr.getYear()-2000) + " (" + totalTime + " minutes)" + ":";

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
        List<EventItem> itemList = new ArrayList<>();
        if (index < eventSchedule.size() && eventSchedule.get(index).size() > 0) {
            for (int j = 0; j < eventSchedule.get(index).size(); j++) {
                String name;
                String timespan;

                Event event = eventSchedule.get(index).get(j);

                MyTime eventTime = event.getDoDate();
                MyTime endTime = new MyTime(eventTime.getDateTime() + event.getLength());

                short eventHour = eventTime.getHour();
                boolean event_am = eventHour < 12;
                String event_ampm = event_am ? " AM" : " PM";
                eventHour -= event_am ? 0 : 12;
                eventHour = (eventHour == 0) ? 12 : eventHour;
                String eventHourString = eventHour + "";
                short eventMinute = eventTime.getMinute();
                String eventMinuteString = eventMinute + "";
                eventMinuteString = (eventMinute < 10) ? "0" + eventMinuteString :
                        eventMinuteString;

                short endHour = endTime.getHour();
                boolean end_am = endHour < 12;
                String end_ampm = end_am ? " AM" : " PM";
                endHour -= end_am ? 0 : 12;
                endHour = (endHour == 0) ? 12 : endHour;
                String endHourString = endHour + "";
                short endMinute = endTime.getMinute();
                String endMinuteString = endMinute + "";
                endMinuteString = (endMinute < 10) ? "0" + endMinuteString : endMinuteString;

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
        List<TaskItem> itemList = new ArrayList<>();

        if (index < taskSchedule.size() && taskSchedule.get(index).size() > 0) {
            for (int j = 0; j < taskSchedule.get(index).size(); j++) {
                String name;

                Task task = taskSchedule.get(index).get(j);

                name = task.getName() + " (" + task.getTimeToComplete() +
                        " minutes to complete)";

                itemList.add(new TaskItem(name));
            }
        }

        return itemList;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onButtonClick(int position, int day) {
        Complete(taskSchedule.get(day).get(position));
    }
}