package com.evanv.taskapp;
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
public class Task implements Comparable<Task> {
    private final String mName;               // Name of the task
    private Date mEarlyDate;                  // Earliest date to complete
    private Date mDoDate;                     // Date to do the task
    private Date mDueDate;                    // Date the task is due
    private int mTimeToComplete;              // Time (in minutes) to complete the tasks
    private final ArrayList<Task> mParents;   // Tasks this task depends on
    private final ArrayList<Task> mChildren;  // Tasks that depend on this task
    private ArrayList<Task> mWorkingParents;  // Working copy of parents for optimizer
    private ArrayList<Task> mWorkingChildren; // Working copy of children for optimizer
    private Date mWorkingEarlyDate;           // Working copy of earlyDate for optimizer.
    // SimpleDateFormat that formats date in the style "08/20/22"
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");

    /**
     * Initializes an object representing a task
     *
     * @param name The name of the task
     * @param early The earliest possible day to complete the task (e.g. when it's assigned)
     * @param due When the task is due
     */
    public Task(String name, Date early, Date due, int time) {
        this.mName = name;
        this.mEarlyDate = clearDate(early);
        this.mDueDate = clearDate(due);
        this.mTimeToComplete = time;
        mParents = new ArrayList<>();
        mChildren = new ArrayList<>();
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
    }

    /**
     * Remove a prerequisite task for this task
     *
     * @param parent The prerequisite task to remove
     */
    public void removeParent(Task parent) {
        this.mParents.remove(parent);
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
     * Compares this task with another given task
     *
     * @param other The other task to compare it with
     * @return Returns a positive number if the other task is greater,
     * negative if this task is greater, and 0 if they are equal
     */
    @Override
    public int compareTo(Task other) {
        // See if task is due before the other task
        Date otherDueDate = other.getDueDate();
        long diff = mDueDate.compareTo(otherDueDate);

        if (diff == 0) {
            // See if task has more children than the other task
            diff = mChildren.size() - other.getChildren().size();

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

    public static int getDiff(Date endDate, Date startDate) {
        return (int) ((clearDate(endDate).getTime() - clearDate(startDate).getTime()) / TimeUnit.DAYS.toMillis(1));
    }
}
