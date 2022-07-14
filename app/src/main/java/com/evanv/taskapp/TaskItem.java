package com.evanv.taskapp;

/**
 * Class representing a Task for use in the DayItem's recyclerview
 *
 * @author Evan Voogd
 */
public class TaskItem {
    private String mName; // Name of the task (e.g. "Read Ch. 3")

    /**
     * Creates an item representing a Task
     *
     * @param name a string representing the name of the Task
     */
    public TaskItem(String name) {
        mName = name;
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
     * Sets the string representing the name of the Task
     *
     * @param mName a string representing the name of the Task
     */
    public void setName(String mName) {
        this.mName = mName;
    }

}