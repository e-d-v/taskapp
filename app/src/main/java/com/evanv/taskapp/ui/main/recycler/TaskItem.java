package com.evanv.taskapp.ui.main.recycler;

/**
 * Class representing a Task for use in the DayItem's recyclerview
 *
 * @author Evan Voogd
 */
public class TaskItem {
    private final String mName; // Name of the task (e.g. "Read Ch. 3")
    private final int mIndex;       // Index into taskSchedule.get(day) for this event

    /**
     * Creates an item representing a Task
     *
     * @param name a string representing the name of the Task
     */
    @SuppressWarnings("unused")
    public TaskItem(String name, int index) {
        mName = name;
        mIndex = index;
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

}