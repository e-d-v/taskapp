package com.evanv.taskapp.logic;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Label - way to organize tasks.
 *
 * @author Evan Voogd
 */
@Entity(tableName = "label_table")
public class Label {
    // Database Fields
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long mID;                // PrimaryKey for Label.
    @ColumnInfo(name = "name")
    private String mName;            // Name of the Label
    @ColumnInfo(name = "color")
    private int mColor;              // Color of the Label, index into ColorList
    @Ignore
    private final List<Task> mTasks; // List of Tasks with this Label

    /**
     * Construct a new Label.
     *
     * @param name Name of the Label.
     * @param color Color associated with the Label.
     */
    public Label(String name, int color) {
        mName = name;
        mColor = color;

        mTasks = new ArrayList<>();
    }

    /**
     * Get the Name of the Label
     *
     * @return String of the name representing the Label
     */
    public String getName() {
        return mName;
    }

    /**
     * Change the name of the Label.
     *
     * @param name New name of the Label.
     */
    public void setName(String name) {
        mName = name;
    }

    /**
     * Get the color of the Label.
     *
     * @return The color of the Label.
     */
    public int getColor() {
        return mColor;
    }

    /**
     * Change the color of the Label.
     *
     * @param color The new Color of the Label.
     */
    public void setColor(int color) {
        mColor = color;
    }

    /**
     * Add a task to the Label.
     *
     * @param task The task to add to the Label.
     */
    public void addTask(Task task) {
        mTasks.add(task);
    }

    /**
     * Remove a task from the Label.
     *
     * @param task The task to remove from the Label.
     */
    public void removeTask(Task task) {
        mTasks.remove(task);
    }

    /**
     * Get the list of Tasks with this Label.
     *
     * @return The list of Tasks with this Label.
     */
    public List<Task> getTasks() {
        return mTasks;
    }

    /**
     * Get the ID for this specific Label.
     *
     * @return The Label's ID.
     */
    public long getID() {
        return mID;
    }

    /**
     * Change the ID for this specific Label.
     *
     * @param id the ID for the Label.
     */
    public void setID(long id) {
        mID = id;
    }

}
