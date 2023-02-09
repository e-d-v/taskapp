package com.evanv.taskapp.logic;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.evanv.taskapp.db.Converters;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.temporal.ChronoUnit;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
    private String mName;              // Name of the task
    @NonNull
    @ColumnInfo(name = "earlyDate")
    private LocalDate mEarlyDate;      // Earliest date to complete
    @ColumnInfo(name = "doDate")
    private LocalDate mDoDate;         // Date to do the task
    @NonNull
    @ColumnInfo(name = "dueDate")
    private LocalDate mDueDate;        // Date the task is due
    @ColumnInfo(name = "ttc")
    private int mTimeToComplete;       // Time (in minutes) to complete the tasks
    @ColumnInfo(name = "priority")
    private int mPriority;             // Priority
    @Ignore
    private Project mProject;          // Project for the task.
    @Ignore
    private final List<Label> mLabels;       // Label for the task.

    @ColumnInfo(name = "project")
    private long mProjectID;           // Project ID for the task.
    @ColumnInfo(name = "labels")
    private final ArrayList<Long> mLabelIDs; // Label ID for the task.

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
    private LocalDate mWorkingEarlyDate;      // Working copy of earlyDate for optimizer.
    @Ignore
    private LocalDate mWorkingDoDate;         // Working copy of doDate for optimizer.

    // Static field
    public static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("M/d/yy");
    // Type value representing Task
    public static final String EXTRA_VAL_TASK = "com.evanv.taskapp.extra.val.TASK";

    /**
     * Initializes an object representing a task
     *
     * @param name The name of the task
     * @param early The earliest possible day to complete the task (e.g. when it's assigned)
     * @param due When the task is due
     */
    public Task(@NonNull String name, @NonNull LocalDate early, @NonNull LocalDate due, int time,
                int priority) {
        this.mName = name;
        this.mEarlyDate = early;
        this.mDoDate = LocalDate.MIN;
        this.mDueDate = due;
        this.mTimeToComplete = time;
        mParents = new ArrayList<>();
        mChildren = new ArrayList<>();
        mParentArr = new ArrayList<>();
        mLabelIDs = new ArrayList<>();
        mLabels = new ArrayList<>();
        mPriority = priority;
        mProjectID = -1;
        mProject = null;
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
     * @param projectID ID for the project.
     * @param labelIDs List of IDs for the labels.
     */
    public Task(@NonNull String name, @NonNull LocalDate earlyDate, @NonNull LocalDate dueDate,
                LocalDate doDate, int timeToComplete, @NonNull ArrayList<Long> parentArr,
                int priority, long projectID, @NonNull ArrayList<Long> labelIDs) {
        mName = name;
        mEarlyDate = earlyDate;
        mDueDate = dueDate;
        mDoDate = doDate;
        mTimeToComplete = timeToComplete;
        mParents = new ArrayList<>();
        mChildren = new ArrayList<>();
        mLabels = new ArrayList<>();
        mParentArr = parentArr;
        mPriority = priority;
        mProjectID = projectID;
        mLabelIDs = labelIDs;
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
    public LocalDate getEarlyDate() {
        return mEarlyDate;
    }

    /**
     * Changes the earliest completion date for a task.
     *
     * @param earlyDate The new earliest completion date for the task.
     */
    public void setEarlyDate(LocalDate earlyDate) {
        this.mEarlyDate = earlyDate;
    }

    /**
     * Returns the currently scheduled completion date (by the optimizer) for the task
     *
     * @return The currently scheduled completion date (by the optimizer) for the task
     */
    public LocalDate getDoDate() {
        return mDoDate;
    }

    /**
     * Changes the currently scheduled completion date for the task.
     *
     * @param doDate The new completion date for the task.
     */
    public void setDoDate(LocalDate doDate) {
        this.mDoDate = doDate;
    }

    /**
     * Returns the due date for the task
     *
     * @return The due date for the task
     */
    public LocalDate getDueDate() {
        return mDueDate;
    }

    /**
     * Changes the due date for the task.
     *
     * @param dueDate The new due date for the task.
     */
    public void setDueDate(LocalDate dueDate) {
        this.mDueDate = dueDate;
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
        if (!mParents.contains(parent)) {
            this.mParents.add(parent);
            this.mParentArr.add(parent.getID());
        }
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
        if (!mChildren.contains(child)) {
            this.mChildren.add(child);
        }
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
    public LocalDate getWorkingDoDate() {
        return mWorkingDoDate;
    }

    /**
     * Change the working copy of the earliest completion date for the task
     *
     * @param workingDoDate The new working copy of the earliest completion date for the task
     */
    public void setWorkingDoDate(LocalDate workingDoDate) {
        this.mWorkingDoDate = workingDoDate;
    }

    /**
     * Returns the working copy of the earliest completion date for the task
     *
     * @return The working copy of the earliest completion date for the task
     */
    public LocalDate getWorkingEarlyDate() {
        return mWorkingEarlyDate;
    }

    /**
     * Changes the working copy of the earliest completion date for a task.
     *
     * @param earlyDate The new working copy of the earliest completion date for the task.
     */
    public void setWorkingEarlyDate(LocalDate earlyDate) {
        this.mWorkingEarlyDate = earlyDate;
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
        boolean task1OneDay = mDueDate.isEqual(mEarlyDate);
        boolean task2OneDay = other.getDueDate().isEqual(other.getEarlyDate());
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
        if (mDueDate.isEqual(LocalDate.now())) {
            priority1 = 5;
        }
        if (other.getDueDate().isEqual(LocalDate.now())) {
            priority2 = 5;
        }

        // See if this task has higher priority than other task.
        long diff = priority2 - priority1;

        if (diff != 0) {
            return (int) diff;
        }

        // See if task is due before the other task
        LocalDate otherDueDate = other.getDueDate();
        diff = mDueDate.compareTo(otherDueDate);

        if (diff == 0) {
            // See if task has more children than the other task
            // Go backwards as we want a higher number of children first
            diff = other.getChildren().size() - mChildren.size();

            if (diff == 0) {
                // See if task can be completed earlier than the other task
                LocalDate otherEarlyDate = other.getEarlyDate();

                diff = mEarlyDate.compareTo(otherEarlyDate);
            }
        }

        // Normalize value
        if (diff != 0) {
            diff = diff/Math.abs(diff);
        }
        // Sort by name as last case scenario (mostly for recycler)
        else {
            diff = mName.compareTo(other.getName());
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
     * Get the project the Task is associated with.
     *
     * @return the Project for the task.
     */
    public Project getProject() {
        return mProject;
    }

    /**
     * Get the labels that the Task is associated with.
     *
     * @return a List of Labels the task is associated with.
     */
    public List<Label> getLabels() {
        return mLabels;
    }

    /**
     * Get the ID of the current Project.
     *
     * @return the ID of the Project.
     */
    public long getProjectID() {
        return mProjectID;
    }

    /**
     * Get a List of IDs of the current Labels.
     *
     * @return the ID of the Labels.
     */
    public ArrayList<Long> getLabelIDs() {
        return mLabelIDs;
    }

    /**
     * Change the Task's Project.
     *
     * @param project The new project for the task.
     */
    public void setProject(Project project) {
        mProject = project;
        mProjectID = project.getID();
    }

    /**
     * Add a Label to this Task.
     *
     * @param label The new label for the Task.
     */
    public void addLabel(Label label) {
        mLabels.add(label);
        mLabelIDs.add(label.getID());
    }

    /**
     * Remove a Label from this Task.
     *
     * @param label The label to remove from this Task.
     */
    public void removeLabel(Label label) {
        mLabels.remove(label);
        mLabelIDs.remove(label.getID());
    }

    /**
     * Update Project from Project ID as the DB cannot store references.
     *
     * @param projects List of projects.
     */
    public void initializeProject(List<Project> projects) {
        if (mProjectID == -1) {
            mProject = null;
            return;
        }

        for (Project p : projects) {
            if (p.getID() == mProjectID) {
                mProject = p;
                p.addTask(this);
                return;
            }
        }
    }

    /**
     * Synchronize the list of Label IDs with the List of Labels.
     *
     * @param labels List of labels.
     */
    public void initializeLabels(List<Label> labels) {
        if (labels.isEmpty()) {
            return;
        }

        for (Label l : labels) {
            if (mLabelIDs.contains(l.getID())) {
                mLabels.add(l);
                l.addTask(this);
            }
        }
    }

    /**
     * Get the difference (in days) between two dates. Often used to find index into taskSchedule
     * List, so is included as a static field here.
     *
     * @param endDate The later date in the calculation
     * @param startDate The earlier date in the calculation
     * @return How many days between start and end dates. E.g. 8/21, 8/20 will return 1.
     */
    public static int getDiff(LocalDate endDate, LocalDate startDate) {
        return (int) ChronoUnit.DAYS.between(startDate, endDate);
    }

    /**
     * Get the difference (in days) between two dates. Often used to find index into taskSchedule
     * List, so is included as a static field here.
     *
     * @param endDate The later date in the calculation
     * @param startDate The earlier date in the calculation
     * @return How many days between start and end dates. E.g. 8/21, 8/20 will return 1.
     */
    public static int getDiff(LocalDateTime endDate, LocalDate startDate) {
        return (int) ChronoUnit.DAYS.between(startDate, endDate.toLocalDate());
    }

    public void removeProject() {
        mProject = null;
    }
}
