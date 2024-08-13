package com.evanv.taskapp.ui.main.recycler;

import java.util.List;

/**
 * Class representing a Task for use in the DayItem's recyclerview
 *
 * @author Evan Voogd
 */
public class TaskItem {
    private final String mName;               // Name of the task (e.g. "Read Ch. 3")
    private int mIndex;                       // Index into taskSchedule.get(day) for this event
    private final int mPriority;              // Priority of task
    private final String mProject;            // Name of the Project associated with the Task
    private final int mProjectColor;          // Color of the Project associated with the Task
    private final long mID;                   // The ID of the Task
    private final List<String> mLabels;       // Names of Labels associated with the Task
    private final List<Integer> mLabelColors; // Colors of the Labels associated with the Task

    /**
     * Creates an item representing a Task
     *
     * @param name The name of the Task
     * @param index Index in the recycler of the task
     * @param completable Can the task be completed right now?
     * @param priority Priority of the Task
     * @param project Name of the task's project
     * @param projectColor Color of the task's project
     * @param labels List of names of the Labels associated with this Task
     * @param labelColors List of colors of the Labels associated with this Task
     * @param ID the ID of the Task.
     */
    @SuppressWarnings("unused")
    public TaskItem(String name, int index, boolean completable, int priority,
                    String project, int projectColor, List<String> labels, List<Integer> labelColors,
                    long ID) {
        mName = name;
        mIndex = index;
        // Can be completed today
        mPriority = priority;
        mProject = project;
        mProjectColor = projectColor;
        mID = ID;
        mLabels = labels;
        mLabelColors = labelColors;
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
     * Changes the index of the TaskItem to reflect it's new state in the recycler
     * @param index The new index in the task list
     */
    public void setIndex(int index) {
        mIndex = index;
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

    /**
     * Return the ID of the Task
     *
     * @return The ID of the Task
     */
    public long getID() {
        return mID;
    }

    /**
     * Return the names of the Labels
     *
     * @return a list of names of the Labels
     */
    public List<String> getLabels() {
        return mLabels;
    }

    /**
     * Return the colors of the Labels
     *
     * @return a list of colors of the Labels
     */
    public List<Integer> getLabelColors() {
        return mLabelColors;
    }
}