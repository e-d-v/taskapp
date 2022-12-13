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

    /**
     * Creates an item representing a Task
     *  @param name a string representing the name of the Task
     * @param completable a boolean representing if the task can be completed today
     * @param hasTimer true if task is timed, false otherwise.
     */
    @SuppressWarnings("unused")
    public TaskItem(String name, int index, boolean completable, boolean hasTimer) {
        mName = name;
        mIndex = index;
        mCompletable = completable;
        mHasTimer = hasTimer;
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
}