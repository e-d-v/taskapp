package com.evanv.taskapp.ui.projects.recycler;

/**
 * An item that represents a project for the project recycler.
 *
 * @author Evan Voogd
 */
public class ProjectItem {
    private final String mName; // The name of the project
    private final String mGoal; // The goal of the project
    private final int mColor;   // The color of the project (index into a color array)
    private final long mID;     // The ID of the project

    /**
     * Construct a new item representing a project for the recycler.
     *
     * @param name Name of the project
     * @param goal Goal of the project
     * @param color Color representing the project
     */
    @SuppressWarnings("unused")
    public ProjectItem(String name, String goal, int color, long id) {
        mName = name;
        mGoal = goal;
        mColor = color;
        mID = id;
    }

    /**
     * Get the name of the project.
     *
     * @return The name of the project.
     */
    public String getName() {
        return mName;
    }

    /**
     * Get the goal of the project.
     *
     * @return The goal of the project.
     */
    public String getGoal() {
        return mGoal;
    }

    /**
     * Get the color representing the project.
     *
     * @return An index into a color array of a color that represents the project.
     */
    public int getColor() {
        return mColor;
    }

    public long getID() {
        return mID;
    }
}
