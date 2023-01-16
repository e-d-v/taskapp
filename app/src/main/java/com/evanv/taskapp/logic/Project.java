package com.evanv.taskapp.logic;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.evanv.taskapp.db.Converters;

import java.util.ArrayList;
import java.util.List;

/**
 * Project - way to organize tasks.
 *
 * @author Evan Voogd
 */
@Entity(tableName = "project_table")
@TypeConverters(Converters.class)
public class Project {
    // Database Fields
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long mID;                // PrimaryKey for Project.
    @ColumnInfo(name = "name")
    private String mName;            // Name of the Project
    @ColumnInfo(name = "color")
    private int mColor;            // Color of the Project, index into ColorList
    @ColumnInfo(name = "goal")
    private String mGoal;            // Goal for completing the Project

    @Ignore
    private final List<Task> mTasks;       // List of Tasks with this Project

    /**
     * Construct a new Project.
     *
     * @param name Name of the Project.
     * @param color Color associated with the Project.
     * @param goal Goal for the Project.
     */
    public Project(String name, int color, String goal) {
        mName = name;
        mColor = color;
        mGoal = goal;

        mTasks = new ArrayList<>();
    }

    /**
     * Get the Name of the Project
     *
     * @return String of the name representing the Project
     */
    public String getName() {
        return mName;
    }

    /**
     * Change the name of the Project.
     *
     * @param name New name of the Project.
     */
    public void setName(String name) {
        mName = name;
    }

    /**
     * Get the color of the Project.
     *
     * @return The color of the Project.
     */
    public int getColor() {
        return mColor;
    }

    /**
     * Change the color of the Project.
     *
     * @param color The new Color of the Project.
     */
    public void setColor(int color) {
        mColor = color;
    }

    /**
     * Add a task to the Project.
     *
     * @param task The task to add to the Project.
     */
    public void addTask(Task task) {
        mTasks.add(task);
    }

    /**
     * Remove a task from the Project.
     *
     * @param task The task to remove from the Project.
     */
    public void removeTask(Task task) {
        mTasks.remove(task);
    }

    /**
     * Get the list of Tasks with this Project.
     *
     * @return The list of Tasks with this Project.
     */
    public List<Task> getTasks() {
        return mTasks;
    }

    /**
     * Get the goal for the Project.
     *
     * @return String representing the Project goal.
     */
    public String getGoal() {
        return mGoal;
    }

    /**
     * Change the goal for the project.
     *
     * @param goal New goal for the project.
     */
    public void setGoal(String goal) {
        mGoal = goal;
    }

    /**
     * Get the ID for this specific project.
     *
     * @return The Project's ID.
     */
    public long getID() {
        return mID;
    }

    /**
     * Change the ID for this specific project.
     *
     * @param id the ID for the project.
     */
    public void setID(long id) {
        mID = id;
    }

}
