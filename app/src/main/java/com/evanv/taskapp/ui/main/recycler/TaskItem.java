package com.evanv.taskapp.ui.main.recycler;

/**
 * Class representing a Task for use in the DayItem's recyclerview
 *
 * @author Evan Voogd
 */
public class TaskItem {
    private final String mName; // Name of the task (e.g. "Read Ch. 3")
    private final int mIndex;   // Index into taskSchedule.get(day) for this event
    private final boolean mCompletable; // Can be completed today
    private final boolean mHasTimer;    // Task has active timer.
    private final int mPriority;        // Priority of task
    private final String mProject;      // Name of the Project associated with the Task
    private final int mProjectColor;    // Color of the Project associated with the Task
    private final long mID;              // The ID of the Task

    /**
     * Creates an item representing a Task
     *  @param name a string representing the name of the Task
     * @param completable a boolean representing if the task can be completed today
     * @param hasTimer true if task is timed, false otherwise.
     */
    @SuppressWarnings("unused")
    public TaskItem(String name, int index, boolean completable, boolean hasTimer, int priority,
                    String project, int projectColor, long ID) {
        mName = name;
        mIndex = index;
        mCompletable = completable;
        mHasTimer = hasTimer;
        mPriority = priority;
        mProject = project;
        mProjectColor = projectColor;
        mID = ID;
    }

    /**
     * Returns index of taskSchedule.get(day) this event is stored at.
     *
     * @return index of taskSchedule.get(day) this event is stored at.
     */
    public int getIndex() {
        return mIndex;
    }

    /**
     * Returns string representing the name of the Task
     *
     * @return a string representing the name of the Task
     */
    public String getName() {
        return mName;
    }

    /**
     * Returns boolean that represents, if true, that the task can be completed today
     *
     * @return a boolean that represents if thask can be completed today
     */
    public boolean isCompletable() {
        return mCompletable;
    }

    /**
     * Returns true if task is being timed, false otherwise.
     *
     * @return true if task is being timed, false otherwise.
     */
    public boolean isTimed() {
        return mHasTimer;
    }

    /**
     * Return the priority of the task.
     *
     * @return the priority of the task.
     */
    public int getPriority() {
        return mPriority;
    }

    /**
     * Return the name of the Project.
     *
     * @return the name of the Project associated with the task.
     */
    public String getProject() {
        return mProject;
    }

    /**
     * Return the index into the color array of the Project's color.
     *
     * @return color of the project.
     */
    public int getProjectColor() {
        return mProjectColor;
    }

    public long getID() {
        return mID;
    }
}