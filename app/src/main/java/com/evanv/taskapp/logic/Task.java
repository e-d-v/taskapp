package com.evanv.taskapp.logic;
import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.evanv.taskapp.db.Converters;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Represents a single task. Conceptually a Task is a node on a large task dependency graph that
 * spans the entire list of tasks. Tasks that aren't connected can be done in any order, tasks that
 * are connected must be done in an order specified by the child/parent relationships -> children
 * require their parents to complete before they can be started. Definitely overengineered, but this
 * Task class is designed to make the Optimizer work.
 *
 * @author Evan Voogd
 */
@SuppressWarnings("unused")
@Entity(tableName = "task_table")
@TypeConverters(Converters.class)
public class Task implements Comparable<Task> {
    // Database Fields
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long mID;                  // PrimaryKey for Task. Used as duplicate task names is allowed
    @NonNull
    @ColumnInfo(name = "name")
    private String mName;        // Name of the task
    @NonNull
    @ColumnInfo(name = "earlyDate")
    private Date mEarlyDate;           // Earliest date to complete
    @ColumnInfo(name = "doDate")
    private Date mDoDate;              // Date to do the task
    @NonNull
    @ColumnInfo(name = "dueDate")
    private Date mDueDate;             // Date the task is due
    @ColumnInfo(name = "ttc")
    private int mTimeToComplete;       // Time (in minutes) to complete the tasks
    @ColumnInfo(name = "priority")
    private int mPriority;             // Priority

    @NonNull
    @ColumnInfo(name = "parents_list")
    // Runtime fields
    private final ArrayList<Long> mParentArr; // List of parent ids to be stored in Room.
    @Ignore
    private final ArrayList<Task> mParents;   // Tasks this task depends on
    @Ignore
    private final ArrayList<Task> mChildren;  // Tasks that depend on this task

    // Optimizer fields
    @Ignore
    private ArrayList<Task> mWorkingParents;  // Working copy of parents for optimizer
    @Ignore
    private ArrayList<Task> mWorkingChildren; // Working copy of children for optimizer
    @Ignore
    private Date mWorkingEarlyDate;           // Working copy of earlyDate for optimizer.
    @Ignore
    private Date mWorkingDoDate;              // Working copy of doDate for optimizer.

    // Static field
    // SimpleDateFormat that formats date in the style "08/20/22"
    @SuppressLint("SimpleDateFormat")
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");

    /**
     * Initializes an object representing a task
     *
     * @param name The name of the task
     * @param early The earliest possible day to complete the task (e.g. when it's assigned)
     * @param due When the task is due
     */
    public Task(@NonNull String name, Date early, Date due, int time, int priority) {
        this.mName = name;
        this.mEarlyDate = clearDate(early);
        this.mDoDate = new Date(0); // To stop null pointer exceptions
        this.mDueDate = clearDate(due);
        this.mTimeToComplete = time;
        mParents = new ArrayList<>();
        mChildren = new ArrayList<>();
        mParentArr = new ArrayList<>();
        mParentArr.add(-1L);
        mPriority = priority;
    }

    /**
     * Initializes an object representing a task
     *
     * @param name The name of the task
     * @param parentArr Array of id's for the priority
     * @param priority Priority of the task
     * @param doDate When the task is scheduled to be completed.
     * @param dueDate When the task is due.
     * @param earlyDate Earliest date the task can be completed.
     * @param timeToComplete Amount of time in minutes the task is estimated to complete.
     */
    public Task(@NonNull String name, @NonNull Date earlyDate, @NonNull Date dueDate,
                Date doDate, int timeToComplete, @NonNull ArrayList<Long> parentArr, int priority) {
        mName = name;
        mEarlyDate = earlyDate;
        mDueDate = dueDate;
        mDoDate = doDate;
        mTimeToComplete = timeToComplete;
        mParents = new ArrayList<>();
        mChildren = new ArrayList<>();
        mParentArr = parentArr;
        mPriority = priority;
    }

    /**
     * Returns the id of this task.
     *
     * @return id of the task
     */
    public long getID() {
        return mID;
    }

    /**
     * Sets the id of this task.
     *
     * @param id id of the task
     */
    public void setID(long id) {
        mID = id;
    }

    /**
     * Returns the name of the task
     *
     * @return The name of the task
     */
    public String getName() {
        return mName;
    }

    /**
     * Changes the name of the task
     *
     * @param name THe new name of the task
     */
    public void setName(@NonNull String name) {
        this.mName = name;
    }

    /**
     * Returns the earliest completion date for the task
     *
     * @return The earliest completion date for the task
     */
    public Date getEarlyDate() {
        return mEarlyDate;
    }

    /**
     * Changes the earliest completion date for a task.
     *
     * @param earlyDate The new earliest completion date for the task.
     */
    public void setEarlyDate(Date earlyDate) {
        this.mEarlyDate = clearDate(earlyDate);
    }

    /**
     * Returns the currently scheduled completion date (by the optimizer) for the task
     *
     * @return The currently scheduled completion date (by the optimizer) for the task
     */
    public Date getDoDate() {
        return mDoDate;
    }

    /**
     * Changes the currently scheduled completion date for the task.
     *
     * @param doDate The new completion date for the task.
     */
    public void setDoDate(Date doDate) {
        this.mDoDate = clearDate(doDate);
    }

    /**
     * Returns the due date for the task
     *
     * @return The due date for the task
     */
    public Date getDueDate() {
        return mDueDate;
    }

    /**
     * Changes the due date for the task.
     *
     * @param dueDate The new due date for the task.
     */
    public void setDueDate(Date dueDate) {
        this.mDueDate = clearDate(dueDate);
    }

    /**
     * Returns the amount of time (in minutes) it takes to complete the task
     *
     * @return The amount of time (in minutes) it takes to complete the task
     */
    public int getTimeToComplete() {
        return mTimeToComplete;
    }

    /**
     * Changes the amount of time it takes to complete the task.
     *
     * @param timeToComplete The new amount of time it takes to complete the task.
     */
    public void setTimeToComplete(int timeToComplete) {
        this.mTimeToComplete = timeToComplete;
    }

    /**
     * Returns the prerequisite tasks for the task
     *
     * @return The prerequisite tasks for the task
     */
    public ArrayList<Task> getParents() {
        return mParents;
    }

    /**
     * Add a prerequisite task for this task
     *
     * @param parent The task that whose completion is required for the completion of the task
     */
    public void addParent(Task parent) {
        this.mParents.add(parent);
        this.mParentArr.add(parent.getID());
    }

    /**
     * Remove a prerequisite task for this task
     *
     * @param parent The prerequisite task to remove
     */
    public void removeParent(Task parent) {
        this.mParents.remove(parent);
        this.mParentArr.remove(parent.getID());
    }

    /**
     * Returns the tasks dependent on the completion of the task
     *
     * @return The tasks dependent on the completion of the task
     */
    public ArrayList<Task> getChildren() {
        return mChildren;
    }

    /**
     * Add a task that depends on this task
     *
     * @param child The dependent task to add
     */
    public void addChild(Task child) {
        this.mChildren.add(child);
    }

    /**
     * Remove a task that depends on this task
     *
     * @param child The dependent task to remove
     */
    public void removeChild(Task child) {
        this.mChildren.remove(child);
    }

    /**
     * Get list of parent ids
     *
     * @return parent array
     */
    @NonNull
    public ArrayList<Long> getParentArr() {
        return mParentArr;
    }

    /**
     * Get the working copy of the do date for the task
     *
     * @return The working copy of the do date for the task
     */
    public Date getWorkingDoDate() {
        return mWorkingDoDate;
    }

    /**
     * Change the working copy of the earliest completion date for the task
     *
     * @param mWorkingDoDate The new working copy of the earliest completion date for the task
     */
    public void setWorkingDoDate(Date mWorkingDoDate) {
        this.mWorkingDoDate = mWorkingDoDate;
    }

    /**
     * Returns the working copy of the earliest completion date for the task
     *
     * @return The working copy of the earliest completion date for the task
     */
    public Date getWorkingEarlyDate() {
        return mWorkingEarlyDate;
    }

    /**
     * Changes the working copy of the earliest completion date for a task.
     *
     * @param earlyDate The new working copy of the earliest completion date for the task.
     */
    public void setWorkingEarlyDate(Date earlyDate) {
        this.mWorkingEarlyDate = clearDate(earlyDate);
    }

    /**
     * Returns the "working" parents list. The working list exists so the optimizer can remove
     * parents from children as they are completed, so checking if all prerequisite tasks are
     * completed is as easy as getWorkingParents().size() == 0.
     *
     * @return The working parents list.
     */
    public ArrayList<Task> getWorkingParents() {
        return mWorkingParents;
    }

    /**
     * Remove a prerequisite task for this task in the optimizer's working task dependency graph
     *
     * @param parent The prerequisite task to remove
     */
    public void removeWorkingParent(Task parent) {
        this.mWorkingParents.remove(parent);
    }

    /**
     * Returns the "working" children list. Exists mostly for symmetry with workingParents.
     *
     * @return The working children list.
     */
    public ArrayList<Task> getWorkingChildren() {
        return mWorkingChildren;
    }

    /**
     * Copies children into working children and parents into working parents, so the optimizer can
     * utilize the inherent dependency tree.
     */
    @SuppressWarnings("unchecked")
    public void initializeForOptimization() {
        mWorkingChildren = (ArrayList<Task>) mChildren.clone();
        mWorkingParents = (ArrayList<Task>) mParents.clone();
        mWorkingEarlyDate = mEarlyDate;
    }

    /**
     * Compares this task with another given task
     *
     * @param other The other task to compare it with
     * @return Returns a positive number if this task is greater,
     * negative if the other task is greater, and 0 if they are equal
     */
    @Override
    public int compareTo(Task other) {
        // Check if task can only be completed for one day to get these tasks scheduled first.
        boolean task1OneDay = mDueDate.getTime() == mEarlyDate.getTime();
        boolean task2OneDay = other.getDueDate().getTime() == other.getEarlyDate().getTime();
        if (task1OneDay && !task2OneDay) {
            return -1;
        }
        else if (!task1OneDay && task2OneDay) {
            return 1;
        }

        // Get task priorities
        int priority1 = mPriority;
        int priority2 = other.getPriority();

        // Set priority to 5 if task is due today
        if (mDueDate.getTime() == clearDate(new Date()).getTime()) {
            priority1 = 5;
        }
        if (mDueDate.getTime() == clearDate(new Date()).getTime()) {
            priority2 = 5;
        }

        // See if this task has higher priority than other task.
        long diff = priority2 - priority1;

        if (diff != 0) {
            return (int) diff;
        }

        // See if task is due before the other task
        Date otherDueDate = other.getDueDate();
        diff = mDueDate.compareTo(otherDueDate);

        if (diff == 0) {
            // See if task has more children than the other task
            // Go backwards as we want a higher number of children first
            diff = other.getChildren().size() - mChildren.size();

            if (diff == 0) {
                // See if task can be completed earlier than the other task
                Date otherEarlyDate = other.getEarlyDate();

                diff = mEarlyDate.compareTo(otherEarlyDate);
            }
        }

        if (diff != 0) {
            diff = diff/Math.abs(diff);
        }

        return (int)diff;
    }

    /**
     * Clears time information from Date, as LocalDate wasn't implemented until API 26. Very useful
     * for Tasks as Tasks have no inherent time information.
     *
     * @param toClear The date to return without a time
     * @return toClear but at the time 00:00:00.0000
     */
    public static Date clearDate(Date toClear) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(toClear);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * Update the priority of the task.
     * 3 = highest priority, 0 = minimum priority
     *
     * @param priority new priority
     */
    public void setPriority(int priority) {
        mPriority = priority >= 0 && priority <= 3 ? priority : mPriority;
    }

    /**
     * Get the priority of the task.
     * 3 = highest priority, 0 = minimum priority
     *
     * @return priority number
     */
    public int getPriority() {
        return mPriority;
    }

    /**
     * Get the difference (in days) between two dates. Often used to find index into taskSchedule
     * List, so is included as a static field here.
     *
     * @param endDate The later date in the calculation
     * @param startDate The earlier date in the calculation
     * @return How many days between start and end dates. E.g. 8/21, 8/20 will return 1.
     */
    public static int getDiff(Date endDate, Date startDate) {
        endDate = clearDate(endDate);
        startDate = clearDate(startDate);
        return (int) ((endDate.getTime() - startDate.getTime()) / TimeUnit.DAYS.toMillis(1));
    }
}
